package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.invariantNewlines
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveTaskWithOutcome
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test

class PublicMarkersTest : BaseKotlinGradleTest() {

  @Test
  fun testPublicMarkers() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/publicMarkers/markers.gradle.kts")
      }
      kotlin("ClassWithPublicMarkers.kt") {
        resolve("/examples/classes/ClassWithPublicMarkers.kt")
      }
      kotlin("ClassInPublicPackage.kt") {
        resolve("/examples/classes/ClassInPublicPackage.kt")
      }
      apiFile(projectName = rootProjectDir.name) {
        resolve("/examples/classes/ClassWithPublicMarkers.dump")
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
        resolve("/examples/gradle/configuration/publicMarkers/packages.gradle.kts")
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

    runner.build {
      shouldHaveTaskWithOutcome(":apiDump", SUCCESS)

      rootProjectApiDump.shouldExist()
      rootProjectApiDump.shouldBeAFile()
      val expected = readResourceFile("/examples/classes/AnnotatedPackage.dump")
      rootProjectApiDump.readText().invariantNewlines() shouldBe expected
    }
  }
}
