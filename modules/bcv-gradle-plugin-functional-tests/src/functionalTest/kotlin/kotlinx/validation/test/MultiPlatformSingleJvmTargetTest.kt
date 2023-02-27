package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test

internal class MultiPlatformSingleJvmTargetTest : BaseKotlinGradleTest() {
  private fun BaseKotlinScope.createProjectHierarchyWithPluginOnRoot() {
    settingsGradleKts {
      resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
    }
    buildGradleKts {
      resolve("/examples/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
    }
  }

  @Test
  fun testApiCheckPasses() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()
      runner {
        arguments.add(":apiCheck")
        arguments.add("--stacktrace")
      }

      dir("api/") {
        file("testproject.api") {
          resolve("/examples/classes/Subsub1Class.dump")
          resolve("/examples/classes/Subsub2Class.dump")
        }
      }

      dir("src/jvmMain/kotlin") {}
      kotlin("Subsub1Class.kt", "commonMain") {
        resolve("/examples/classes/Subsub1Class.kt")
      }
      kotlin("Subsub2Class.kt", "jvmMain") {
        resolve("/examples/classes/Subsub2Class.kt")
      }

    }

    runner.build {
      task(":apiCheck") shouldHaveOutcome SUCCESS
    }
  }

  @Test
  fun testApiCheckFails() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()
      runner {
        arguments.add("--continue")
        arguments.add(":check")
        arguments.add("--stacktrace")
      }

      dir("api/") {
        file("testproject.api") {
          resolve("/examples/classes/Subsub2Class.dump")
          resolve("/examples/classes/Subsub1Class.dump")
        }
      }

      dir("src/jvmMain/kotlin") {}
      kotlin("Subsub1Class.kt", "commonMain") {
        resolve("/examples/classes/Subsub1Class.kt")
      }
      kotlin("Subsub2Class.kt", "jvmMain") {
        resolve("/examples/classes/Subsub2Class.kt")
      }

    }

    runner.buildAndFail {
      task(":apiCheck") shouldHaveOutcome FAILED
      output shouldContain "API check failed for project :testproject"
      shouldNotHaveRunTask(":check")
    }
  }

  @Test
  fun testApiDumpPasses() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()

      runner {
        arguments.add(":apiDump")
        arguments.add("--stacktrace")
      }

      dir("src/jvmMain/kotlin") {}
      kotlin("Subsub1Class.kt", "commonMain") {
        resolve("/examples/classes/Subsub1Class.kt")
      }
      kotlin("Subsub2Class.kt", "jvmMain") {
        resolve("/examples/classes/Subsub2Class.kt")
      }

    }

    runner.build {
      task(":apiDump") shouldHaveOutcome SUCCESS

      val mainExpectedApi = """
        |${readResourceFile("/examples/classes/Subsub1Class.dump").trim()}
        |
        |${readResourceFile("/examples/classes/Subsub2Class.dump").trim()}
        |
        |
      """.trimMargin()

      val actual = jvmApiDump.readText().invariantNewlines()

      actual.shouldBeEqualComparingTo(mainExpectedApi)
    }
  }

  private val jvmApiDump: File get() = rootProjectDir.resolve("api/testproject.api")
}
