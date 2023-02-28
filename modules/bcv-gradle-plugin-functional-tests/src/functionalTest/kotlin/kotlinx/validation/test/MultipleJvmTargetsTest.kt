package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test

internal class MultipleJvmTargetsTest : BaseKotlinGradleTest() {
  private fun BaseKotlinScope.createProjectHierarchyWithPluginOnRoot() {
    settingsGradleKts {
      resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
    }
    buildGradleKts {
      resolve("/examples/gradle/base/multiplatformWithJvmTargets.gradle.kts")
    }
  }

  @Test
  fun testApiCheckPasses() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()
      runner {
        arguments.add(":apiCheck")
      }

      dir("api/jvm/") {
        file("testproject.api") {
          resolve("/examples/classes/Subsub1Class.dump")
          resolve("/examples/classes/Subsub2Class.dump")
        }
      }

      dir("api/anotherJvm/") {
        file("testproject.api") {
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

      dir("api/jvm/") {
        file("testproject.api") {
          resolve("/examples/classes/Subsub2Class.dump")
          resolve("/examples/classes/Subsub1Class.dump")
        }
      }

      dir("api/anotherJvm/") {
        file("testproject.api") {
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

      System.err.println(output)

      val anotherExpectedApi = readResourceFile("/examples/classes/Subsub1Class.dump")
      anotherApiDump.shouldBeAFile()
      anotherApiDump.readText().invariantNewlines().shouldBe(anotherExpectedApi)

      val mainExpectedApi =
        anotherExpectedApi + readResourceFile("/examples/classes/Subsub2Class.dump")
      jvmApiDump.shouldBeAFile()
      jvmApiDump.readText().invariantNewlines().shouldBe(mainExpectedApi)
    }
  }

  private val jvmApiDump: File get() = rootProjectDir.resolve("api/jvm/testproject.api")
  private val anotherApiDump: File get() = rootProjectDir.resolve("api/anotherJvm/testproject.api")

}
