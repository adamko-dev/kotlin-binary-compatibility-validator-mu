package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
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

    runner.build().apply {
      assertTaskSuccess(":apiCheck")
    }
  }
}
