package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveTaskWithOutcome
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class NonPublicMarkersTest : BaseKotlinGradleTest() {

  @Test
  fun testIgnoredMarkersOnProperties() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/nonPublicMarkers/markers.gradle.kts")
      }

      kotlin("Properties.kt") {
        resolve("/examples/classes/Properties.kt")
      }

      apiFile(projectName = rootProjectDir.name) {
        resolve("/examples/classes/Properties.dump")
      }

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.build {
      shouldHaveTaskWithOutcome(":apiCheck", SUCCESS)
    }
  }

  @Test
  @Disabled("https://youtrack.jetbrains.com/issue/KT-62259")
  fun testIgnoredMarkersOnPropertiesForNativeTargets() {
    val runner = test {
      settingsGradleKts {
        resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
      }

      buildGradleKts {
        resolve("/examples/gradle/base/withNativePlugin.gradle.kts")
        resolve("/examples/gradle/configuration/nonPublicMarkers/markers.gradle.kts")
      }

      kotlin("Properties.kt", sourceSet = "commonMain") {
        resolve("/examples/classes/Properties.kt")
      }

      commonNativeTargets.forEach {
        abiFile(projectName = "testproject", target = it) {
          resolve("/examples/classes/Properties.klib.dump")
        }
      }

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.build {
      shouldHaveTaskWithOutcome(":apiCheck", SUCCESS)
    }
  }

  @Test
  fun testFiltrationByPackageLevelAnnotations() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/nonPublicMarkers/packages.gradle.kts")
      }
      java("annotated/PackageAnnotation.java") {
        resolve("/examples/classes/PackageAnnotation.java")
      }
      java("annotated/package-info.java") {
        resolve("/examples/classes/package-info.java")
      }
      kotlin("ClassFromAnnotatedPackage.kt") {
        resolve("/examples/classes/ClassFromAnnotatedPackage.kt")
      }
      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }
      runner {
        arguments.add(":apiDump")
      }
    }

    runner
      .forwardOutput()
      .build {
        shouldHaveTaskWithOutcome(":apiDump", SUCCESS)

        rootProjectApiDump.shouldExist()

        val dumpFile = readResourceFile("/examples/classes/AnotherBuildConfig.dump")
        rootProjectApiDump.readText() shouldBe dumpFile
      }
  }

  companion object {
    private val commonNativeTargets: Set<String> = setOf(
      "linuxX64",
      "linuxArm64",
      "mingwX64",
      "androidNativeArm32",
      "androidNativeArm64",
      "androidNativeX64",
      "androidNativeX86"
    )
  }
}
