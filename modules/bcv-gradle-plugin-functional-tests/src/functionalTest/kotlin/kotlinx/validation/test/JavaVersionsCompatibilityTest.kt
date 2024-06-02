package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveTaskWithOutcome
import org.gradle.internal.impldep.org.junit.Assume.assumeFalse
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test

class JavaVersionsCompatibilityTest : BaseKotlinGradleTest() {
  private fun skipInDebug(runner: GradleRunner) {
    assumeFalse(
      "The test requires a separate Gradle process as it uses a different JVM version, so it cannot be executed with debug turned on.",
      runner.isDebug,
    )
  }

  private fun checkCompatibility(useMaxSupportedJdk: Boolean): Unit = checkCompatibility {
    buildGradleKts {
      resolve("/examples/gradle/base/jdkCompatibility.gradle.kts")
    }
    runner {
      arguments.add("-PuseMaxSupportedJdk=$useMaxSupportedJdk")
    }
  }

  private fun checkCompatibility(jvmTarget: String): Unit = checkCompatibility {
    buildGradleKts {
      resolve("/examples/gradle/base/jdkCompatibilityWithExactVersion.gradle.kts")
    }
    runner {
      arguments.add("-PjvmTarget=$jvmTarget")
    }
  }

  private fun checkCompatibility(configure: BaseKotlinScope.() -> Unit = {}) {
    val runner = test {
      settingsGradleKts {
        resolve("/examples/gradle/settings/jdk-provisioning.gradle.kts")
      }
      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }
      apiFile(projectName = rootProjectDir.name) {
        resolve("/examples/classes/AnotherBuildConfig.dump")
      }

      runner {
        gradleVersion = "8.5"
        arguments.add(":apiCheck")
      }

      configure()
    }

    skipInDebug(runner)

    runner.build {
      shouldHaveTaskWithOutcome(":apiCheck", SUCCESS)
    }
  }

  @Test
  fun testMaxSupportedJvmVersion(): Unit = checkCompatibility(true)

  @Test
  fun testMinSupportedJvmVersion(): Unit = checkCompatibility(false)

  @Test
  fun testJvm8(): Unit = checkCompatibility("1.8")

  @Test
  fun testJvm11(): Unit = checkCompatibility("11")

  @Test
  fun testJvm17(): Unit = checkCompatibility("17")

  @Test
  fun testJvm21(): Unit = checkCompatibility("21")
}
