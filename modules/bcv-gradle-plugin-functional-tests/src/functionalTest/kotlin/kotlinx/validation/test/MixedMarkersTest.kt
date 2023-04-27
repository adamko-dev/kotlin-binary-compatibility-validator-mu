package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveTaskWithOutcome
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test

class MixedMarkersTest : BaseKotlinGradleTest() {

  @Test
  fun testMixedMarkers() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/publicMarkers/mixedMarkers.gradle.kts")
      }

      kotlin("MixedAnnotations.kt") {
        resolve("/examples/classes/MixedAnnotations.kt")
      }

      apiFile(projectName = rootProjectDir.name) {
        resolve("/examples/classes/MixedAnnotations.dump")
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
