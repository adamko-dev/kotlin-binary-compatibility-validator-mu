package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveTaskWithOutcome
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
}
