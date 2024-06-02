package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.*
import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.io.path.readText
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.intellij.lang.annotations.Language

internal class SettingsPluginDslTest : FunSpec({

  context("when BCV-MU is applied as a settings plugin") {

    val kotlinJvmTest = TestCase(
      projectType = "Kotlin/JVM",
      project = kotlinJvmProjectWithBcvSettingsPlugin(),
      expectedPrintedBCVTargets = """
        |type: class dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget_Decorated
        |name: kotlinJvm
        |enabled: true
        |ignoredClasses: [com.package.MyIgnoredClass]
        |ignoredMarkers: [com.package.MyInternalApiAnnotationMarker]
        |ignoredPackages: [com.package.my_ignored_package]
        |publicMarkers: [com.package.MyIgnoredClass]
        |publicPackages: [com.package.MyInternalApiAnnotationMarker]
        |publicClasses: [com.package.my_ignored_package]
        |platformType: kotlinJvm
        |inputJar: null
        |inputClasses: [main, main]
        |------------------------------
      """.trimMargin()
    )

    val kotlinMultiplatformTest = TestCase(
      projectType = "Kotlin/Multiplatform",
      project = kotlinMultiplatformProjectWithBcvSettingsPlugin(),
      expectedPrintedBCVTargets = """
        |type: class dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget_Decorated
        |name: jvm
        |enabled: true
        |ignoredClasses: [com.package.MyIgnoredClass]
        |ignoredMarkers: [com.package.MyInternalApiAnnotationMarker]
        |ignoredPackages: [com.package.my_ignored_package]
        |publicMarkers: [com.package.MyIgnoredClass]
        |publicPackages: [com.package.MyInternalApiAnnotationMarker]
        |publicClasses: [com.package.my_ignored_package]
        |platformType: jvm
        |inputJar: null
        |inputClasses: [main]
        |------------------------------
      """.trimMargin()
    )

    listOf(
      kotlinJvmTest,
      kotlinMultiplatformTest,
    ).forEach { testCase ->
      context("in a ${testCase.projectType} project") {

        test("apiDump should be generated in all non-excluded subprojects") {
          testCase.project.projectDir.toFile()
            .walk()
            .filter { it.isDirectory && it.name == "api" }
            .forEach { it.deleteRecursively() }

          testCase.project.runner
            .withArguments("apiDump", "--stacktrace")
            .build {
              output shouldContain "SUCCESSFUL"

              withClue("root project is excluded from BCV") {
                shouldNotHaveRunTask(":apiDump")
              }

              shouldHaveRunTask(":sub1:apiDump", SUCCESS)
              shouldHaveRunTask(":sub2:apiDump", SUCCESS)
            }

          testCase.project.projectDir.resolve("sub1/api/sub1.api").asClue { apiDump ->
            apiDump.shouldExist()
            apiDump.shouldBeAFile()
            apiDump.readText().invariantNewlines() shouldBe /* language=TEXT */ """
                |
              """.trimMargin()
          }

          testCase.project.projectDir.resolve("sub2/api/sub2.api").asClue { apiDump ->
            apiDump.shouldExist()
            apiDump.shouldBeAFile()
            apiDump.readText().invariantNewlines() shouldBe /* language=TEXT */ """
                |
              """.trimMargin()
          }
        }

        test("expect the conventions set in the settings plugin are used in the subprojects") {
          testCase.project.runner.withArguments("printBCVTargets", "-q", "--stacktrace").build {
            output.invariantNewlines().trim() shouldBe testCase.expectedPrintedBCVTargets.trim()
          }
        }
      }
    }
  }
})

private data class TestCase(
  val projectType: String,
  val project: GradleProjectTest,
  @Language("TEXT")
  val expectedPrintedBCVTargets: String,
)

private fun kotlinJvmProjectWithBcvSettingsPlugin() =
  gradleKtsProjectTest("settings-plugin-dsl-test/kotlin-jvm") {

    settingsGradleKts += settingsGradleKtsWithBcvPlugin

    buildGradleKts = "\n"

    dir("sub1") {
      buildGradleKts = buildGradleKtsWithKotlinJvmAndBcvConfig
    }

    dir("sub2") {
      buildGradleKts = buildGradleKtsWithKotlinJvmAndBcvConfig
      buildGradleKts += printBcvTargetsTask
    }
  }


private fun kotlinMultiplatformProjectWithBcvSettingsPlugin() =
  gradleKtsProjectTest("settings-plugin-dsl-test/kotlin-multiplatform-jvm") {

    settingsGradleKts += settingsGradleKtsWithBcvPlugin

    buildGradleKts = "\n"

    dir("sub1") {
      buildGradleKts = buildGradleKtsWithKotlinMultiplatformJvmAndBcvConfig
    }

    dir("sub2") {
      buildGradleKts = buildGradleKtsWithKotlinMultiplatformJvmAndBcvConfig
      buildGradleKts += printBcvTargetsTask
    }
  }

@Language("kts")
private val settingsGradleKtsWithBcvPlugin = """
buildscript {
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.9.23")
  }
}

plugins {
  id("dev.adamko.kotlin.binary-compatibility-validator") version "+"
}

include(
  ":sub1",
  ":sub2",
)

binaryCompatibilityValidator { 
  ignoredProjects.add(":")
  defaultTargetValues {
    enabled.convention(true)
    ignoredClasses.set(listOf("com.package.MyIgnoredClass"))
    ignoredMarkers.set(listOf("com.package.MyInternalApiAnnotationMarker"))
    ignoredPackages.set(listOf("com.package.my_ignored_package"))
  }
}

"""

@Language("kts")
private val buildGradleKtsWithKotlinJvmAndBcvConfig = """
plugins {
  kotlin("jvm") version "1.9.23"
}

// check that the DSL is available:
binaryCompatibilityValidator { }

"""

/**
 * Note: the `kotlin("multiplatform")` version should be set in [settingsGradleKtsWithBcvPlugin].
 */
@Language("kts")
private val buildGradleKtsWithKotlinMultiplatformJvmAndBcvConfig = """
plugins {
  kotlin("multiplatform") version "1.9.23"
}

kotlin {
  jvm()
}

// check that the DSL is available:
binaryCompatibilityValidator { }

"""

@Language("kts")
private val printBcvTargetsTask = """

val printBCVTargets by tasks.registering {

  val bcvTargets = binaryCompatibilityValidator.targets

  doLast {
    bcvTargets.forEach { t ->
      println("type: " + t::class.toString())
      println("name: " + t.name)
      println("enabled: " + t.enabled.get())
      println("ignoredClasses: " + t.ignoredClasses.get())
      println("ignoredMarkers: " + t.ignoredMarkers.get())
      println("ignoredPackages: " + t.ignoredPackages.get())
      println("publicMarkers: " + t.ignoredClasses.get())
      println("publicPackages: " + t.ignoredMarkers.get())
      println("publicClasses: " + t.ignoredPackages.get())
      when (t) {

        is dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget -> {
          println("platformType: " + t.platformType)
          println("inputJar: " + t.inputJar.orNull)
          println("inputClasses: " + t.inputClasses.files.map { f -> f.name })
        }

        is dev.adamko.kotlin.binary_compatibility_validator.targets.BCVKLibTarget    -> {
          println("klibFile: " + t.klibFile.files.map { f -> f.name })
          println("compilationDependencies: " + t.compilationDependencies.files.map { f -> f.name })
          println("currentPlatform: " + t.currentPlatform.orNull)
          println("supportedByCurrentHost: " + t.supportedByCurrentHost.orNull)
          println("hasKotlinSources: " + t.hasKotlinSources.orNull)
        }
      }
      println("------------------------------")
    }
  }
}
"""
