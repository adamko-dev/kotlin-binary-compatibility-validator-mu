package dev.adamko.kotlin.binary_compatibility_validator.test

import dev.adamko.kotlin.binary_compatibility_validator.buildAndFail
import dev.adamko.kotlin.binary_compatibility_validator.buildGradleKts
import dev.adamko.kotlin.binary_compatibility_validator.gradleKtsProjectTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.file.shouldBeEmpty
import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.paths.shouldBeEmptyDirectory
import io.kotest.matchers.paths.shouldContainFiles
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Paths
import org.gradle.testkit.runner.TaskOutcome

internal class DefaultConfigTests : FunSpec({

  context("given a Kotlin project with no Kotlin sources") {
    val project = gradleKtsProjectTest("api-check-fail-no-src") {
      buildGradleKts += """
        |plugins {
        |  kotlin("jvm") version "1.7.20"
        |  id("dev.adamko.kotlin.binary-compatibility-validator") version "0.2.0-SNAPSHOT"
        |}
        |
      """.trimMargin()
    }

    val apiDir = project.projectDir.resolve("api")

    context("and there is no api directory") {
      listOf(
        ":apiCheck",
        ":check",
      ).forEach { testedTask ->
        context("when $testedTask task runs") {

          apiDir.toFile().deleteRecursively()

          project.runner
            .withArguments(
              testedTask,
              "--stacktrace",
            ).buildAndFail {
              test("expect api dir is not present") {
                apiDir.shouldNotExist()
              }
              test("expect failure reason is logged") {
                output shouldContain "Expected folder with API declarations"
                output shouldContain "Please ensure that task ':apiDump' was executed"
              }
              test("expect :apiCheck task fails") {
                task(":apiCheck")?.outcome shouldBe TaskOutcome.FAILED
              }
              test("expect :check task is not run") {
                // :apiCheck fails before :check can run
                task(":check") shouldBe null
              }
            }
        }
      }
    }

    context("and there is an empty api directory") {

      listOf(
        ":apiCheck",
        ":check",
      ).forEach { testedTask ->
        context("when $testedTask task runs") {

          apiDir.toFile().apply {
            deleteRecursively()
            mkdirs()
          }

          project.runner
            .withArguments(
              testedTask,
              "--stacktrace",
            ).buildAndFail {
              test("expect api dir is present, but empty") {
                apiDir.shouldBeEmptyDirectory()
              }
              test("expect failure reason is logged") {
                output shouldContain "Expected folder with API declarations"
                output shouldContain "Please ensure that task ':apiDump' was executed"
              }
              test("expect :apiCheck task fails") {
                task(":apiCheck")?.outcome shouldBe TaskOutcome.FAILED
              }
              test("expect :check task is not run") {
                // :apiCheck fails before :check can run
                task(":check") shouldBe null
              }
            }
        }
      }
    }

    context("and an empty api file") {

      listOf(
        ":apiCheck",
        ":check",
      ).forEach { testedTask ->
        context("when $testedTask task runs") {

          apiDir.toFile().apply {
            deleteRecursively()
            mkdirs()
            resolve("test.api").createNewFile()
          }

          project.runner
            .withArguments(
              testedTask,
              "--stacktrace",
            ).buildAndFail {
              test("expect api file is present, but empty") {
                apiDir.shouldBeADirectory()
                apiDir.shouldContainFiles("test.api")
                apiDir.resolve("test.api").toFile().shouldBeEmpty()
//                apiDir.shouldContainExactly(Paths.get("test.api"))
              }
              test("expect failure reason is logged") {
                output shouldContain "Expected folder with API declarations"
                output shouldContain "Please ensure that task ':apiDump' was executed"
              }
              test("expect :apiCheck task fails") {
                task(":apiCheck")?.outcome shouldBe TaskOutcome.FAILED
              }
              test("expect :check task is not run") {
                // :apiCheck fails before :check can run
                task(":check") shouldBe null
              }
            }
        }
      }
    }
  }
})
