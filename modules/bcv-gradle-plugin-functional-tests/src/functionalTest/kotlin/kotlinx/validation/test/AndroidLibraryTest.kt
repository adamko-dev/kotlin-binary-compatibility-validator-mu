package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveRunTask
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Leftovers after revert of https://github.com/Kotlin/binary-compatibility-validator/issues/94")
internal class AndroidLibraryTest : BaseKotlinGradleTest() {

  // region Kotlin Android Library

  @Test
  fun `Given a Kotlin Android Library, when api is dumped, then task should be successful`() {
    assumeHasAndroid()
    val runner = test {
      createProjectWithSubModules()
      runner {
        arguments.add(":kotlin-library:apiDump")
      }
    }

    runner.build {
      shouldHaveRunTask(":kotlin-library:apiDump", SUCCESS)
    }
  }

  @Test
  fun `Given a Kotlin Android Library, when api is checked, then it should match the expected`() {
    assumeHasAndroid()
    test {
      createProjectWithSubModules()
      runner {
        arguments.add(":kotlin-library:apiCheck")
      }
    }.build {
      shouldHaveRunTask(":kotlin-library:apiCheck", SUCCESS)
    }
  }

  //endregion

  //region Java Android Library

  @Test
  fun `Given a Java Android Library, when api is dumped, then task should be successful`() {
    assumeHasAndroid()
    val runner = test {
      createProjectWithSubModules()
      runner {
        arguments.add(":java-library:apiDump")
      }
    }

    runner.build {
      shouldHaveRunTask(":java-library:apiDump", SUCCESS)
    }
  }

  @Test
  fun `Given a Java Android Library, when api is checked, then it should match the expected`() {
    assumeHasAndroid()
    test {
      createProjectWithSubModules()
      runner {
        arguments.add(":java-library:apiCheck")
      }
    }.build {
      shouldHaveRunTask(":java-library:apiCheck", SUCCESS)
    }
  }

  //endregion

  /**
   * Creates a single project with 2 (Kotlin and Java Android Library) modules, applies
   * the plugin on the root project.
   */
  private fun BaseKotlinScope.createProjectWithSubModules() {
    settingsGradleKts {
      resolve("/examples/gradle/settings/settings-android-project.gradle.kts")
    }
    buildGradleKts {
      resolve("/examples/gradle/base/androidProjectRoot.gradle.kts")
    }
    initLocalProperties()

    dir("kotlin-library") {
      buildGradleKts {
        resolve("/examples/gradle/base/androidKotlinLibrary.gradle.kts")
      }
      kotlin("KotlinLib.kt") {
        resolve("/examples/classes/KotlinLib.kt")
      }
      apiFile(projectName = "kotlin-library") {
        resolve("/examples/classes/KotlinLib.dump")
      }
    }
    dir("java-library") {
      buildGradleKts {
        resolve("/examples/gradle/base/androidJavaLibrary.gradle.kts")
      }
      java("JavaLib.java") {
        resolve("/examples/classes/JavaLib.java")
      }
      apiFile(projectName = "java-library") {
        resolve("/examples/classes/JavaLib.dump")
      }
    }
  }

  private fun initLocalProperties() {
    val home = System.getenv("ANDROID_HOME") ?: System.getenv("HOME")
    File(rootProjectDir, "local.properties").apply {
      writeText("sdk.dir=$home/Android/Sdk")
    }
  }

  // We do not have ANDROID_HOME on CI, and this functionality is not critical, so we are disabling these
  // tests on CI
  private fun assumeHasAndroid() {
//        Assume.assumeFalse(System.getenv("ANDROID_HOME").isNullOrEmpty())
  }
}
