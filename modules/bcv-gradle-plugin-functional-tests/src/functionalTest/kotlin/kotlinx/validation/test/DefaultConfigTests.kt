package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.file.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DefaultConfigTests : BaseKotlinGradleTest() {

  @Test
  fun `apiCheck should fail, when there is no api directory, even if there are no Kotlin sources`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }
      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.buildAndFail {
      output shouldContain "Please ensure that task ':apiDump' was executed"
      shouldHaveRunTask(":apiCheck", FAILED)
    }
  }

  @Test
  fun `check should fail, when there is no api directory, even if there are no Kotlin sources`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }

      runner {
        arguments.add(":check")
      }
    }

    runner.buildAndFail {
      shouldHaveRunTask(":apiCheck", FAILED)
      shouldNotHaveRunTask(":check") // apiCheck fails before we can run check
    }
  }

  @Test
  fun `apiCheck should succeed, when api-File is empty, but no kotlin files are included in SourceSet`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }

      emptyApiFile(projectName = rootProjectDir.name)

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiCheck", SUCCESS)
    }
  }

  @Test
  fun `apiCheck should succeed when public classes match api file`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }
      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }
      apiFile(projectName = rootProjectDir.name) {
        resolve("/examples/classes/AnotherBuildConfig.dump")
      }

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiCheck", SUCCESS)
    }
  }

  @Test
  fun `apiCheck should succeed when public classes match api file ignoring case`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }
      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }
      apiFile(projectName = rootProjectDir.name.uppercase()) {
        resolve("/examples/classes/AnotherBuildConfig.dump")
      }

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiCheck", SUCCESS)
    }
  }

  @Test
  fun `apiCheck should fail, when a public class is not in api-File`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }

      kotlin("BuildConfig.kt") {
        resolve("/examples/classes/BuildConfig.kt")
      }

      emptyApiFile(projectName = rootProjectDir.name)

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.buildAndFail {
      shouldHaveRunTask(":apiCheck", FAILED)

      // note that tabs are used as function indents!
      val dumpOutput = /*language=TEXT*/ """
        |  @@ -1,1 +1,7 @@
        |  +public final class com/company/BuildConfig {
        |  +	public fun <init> ()V
        |  +	public final fun function ()I
        |  +	public final fun getProperty ()I
        |  +}
      """.trimMargin()
      output shouldContain dumpOutput
    }
  }

  @Test
  fun `apiDump should create empty api file when there are no Kotlin sources`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }

      runner {
        arguments.add(":apiDump")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiDump", SUCCESS)

      assertTrue(
        rootProjectApiDump.exists(),
        "api dump file ${rootProjectApiDump.path} should exist"
      )

      rootProjectApiDump.shouldBeEmpty()
    }
  }

  @Test
  fun `apiDump should create api file with the name of the project, respecting settings file`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }
      settingsGradleKts {
        resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
      }

      runner {
        arguments.add(":apiDump")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiDump", SUCCESS)

      val apiDumpFile = rootProjectDir.resolve("api/testproject.api")
      assertTrue(apiDumpFile.exists(), "api dump file ${apiDumpFile.path} should exist")

      assertFalse(
        rootProjectApiDump.exists(), "api dump file ${rootProjectApiDump.path} should NOT exist " +
            "(based on project dir instead of custom name from settings)"
      )

      rootProjectApiDump.shouldBeEmpty()
    }
  }

  @Test
  fun `apiDump should dump public classes`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }
      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }

      runner {
        arguments.add(":apiDump")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiDump", SUCCESS)

      assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

      val expected = readResourceFile("/examples/classes/AnotherBuildConfig.dump")
      rootProjectApiDump.readText().invariantNewlines().shouldBeEqualComparingTo(expected)
      //Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
    }
  }

  @Test
  fun `apiCheck should be run when we run check`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
      }

      emptyApiFile(projectName = rootProjectDir.name)

      runner {
        arguments.add(":check")
      }
    }

    runner.build {
      shouldHaveRunTask(":check", SUCCESS)
      shouldHaveRunTask(":apiCheck", SUCCESS)
    }
  }
}
