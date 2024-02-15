package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveTaskWithOutcome
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
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
}
