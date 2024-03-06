package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveRunTask
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class IgnoredClassesTests : BaseKotlinGradleTest() {

  @Test
  fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredClasses`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
      }

      kotlin("BuildConfig.kt") {
        resolve("/examples/classes/BuildConfig.kt")
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
  fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredPackages`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/ignoredPackages/oneValidPackage.gradle.kts")
      }

      kotlin("BuildConfig.kt") {
        resolve("/examples/classes/BuildConfig.kt")
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
  fun `apiDump should not dump ignoredClasses, when class is excluded via ignoredClasses`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
      }
      kotlin("BuildConfig.kt") {
        resolve("/examples/classes/BuildConfig.kt")
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
      rootProjectApiDump.readText().shouldBeEqualComparingTo(expected)
    }
  }

  @Test
  fun `apiDump should dump class whose name is a subsset of another class that is excluded via ignoredClasses`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
      }
      kotlin("BuildConfig.kt") {
        resolve("/examples/classes/BuildConfig.kt")
      }
      kotlin("BuildCon.kt") {
        resolve("/examples/classes/BuildCon.kt")
      }

      runner {
        arguments.add(":apiDump")
      }
    }

    runner.build().apply {
      shouldHaveRunTask(":apiDump", SUCCESS)

      assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

      val expected = readResourceFile("/examples/classes/BuildCon.dump")
      rootProjectApiDump.readText().shouldBeEqualComparingTo(expected)
    }
  }
}
