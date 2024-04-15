package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import io.kotest.assertions.withClue
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.*
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
      shouldHaveRunTask(":apiCheck", SUCCESS)
    }
  }

  @Test
  fun testApiCheckFails() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()
      runner {
        arguments.add(":check")
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
      dir("src/jvmTest/kotlin") {}
      kotlin("Subsub2ClassTest.kt", "jvmTest") {
        addText(/*language=kotlin*/ """
            |package com.company.test
            |
            |class SubSub2Test {
            |  fun blah() {
            |    println("test")
            |  }
            |}
            |
          """.trimMargin()
        )
      }
    }

    runner.buildAndFail {
      withClue(output) {
        shouldHaveRunTask(":apiCheck", FAILED)
        output shouldContain "API check failed for project :testproject"
        shouldNotHaveRunTask(":check")
      }
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
      shouldHaveRunTask(":apiDump", SUCCESS)

      val mainExpectedApi = """
        |${readResourceFile("/examples/classes/Subsub1Class.dump").trim()}
        |
        |${readResourceFile("/examples/classes/Subsub2Class.dump").trim()}
        |
        |
      """.trimMargin()

      jvmApiDump.shouldBeAFile()
      jvmApiDump.readText().invariantNewlines() shouldBe mainExpectedApi
    }
  }

  @Test
  fun testApiDumpPassesForEmptyProject() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
      }

      runner {
        arguments.add(":apiDump")
      }
    }

    runner.build {
//      shouldHaveTaskWithOutcome(":jvmApiDump", SKIPPED)
//      shouldHaveTaskWithOutcome(":apiDump", UP_TO_DATE)
      shouldHaveTaskWithOutcome(":apiDump", SUCCESS)
    }
  }

  @Test
  fun testApiCheckPassesForEmptyProject() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
      }

      emptyApiFile(projectName = rootProjectDir.name)

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.build {
//      shouldHaveTaskWithOutcome(":jvmApiCheck", SKIPPED)
//      shouldHaveTaskWithOutcome(":apiCheck", UP_TO_DATE)
      shouldHaveTaskWithOutcome(":apiCheck", SUCCESS)
    }
  }

  private val jvmApiDump: File get() = rootProjectDir.resolve("api/testproject.api")
}
