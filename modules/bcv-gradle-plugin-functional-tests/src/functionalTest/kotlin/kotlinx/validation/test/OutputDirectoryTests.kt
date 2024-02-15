package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.buildAndFail
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveTaskWithOutcome
import io.kotest.assertions.withClue
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test

class OutputDirectoryTests : BaseKotlinGradleTest() {

  @Test
  fun dumpIntoCustomDirectory() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/outputDirectory/different.gradle.kts")
      }

      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }
      dir("api") {
        file("letMeBe.txt")
      }

      runner {
        arguments.add(":apiDump")
      }
    }

    runner.build {
      shouldHaveTaskWithOutcome(":apiDump", SUCCESS)

      val dumpFile = rootProjectDir.resolve("custom/${rootProjectDir.name}.api")
      dumpFile.shouldExist()

      val expected = readResourceFile("/examples/classes/AnotherBuildConfig.dump")
      dumpFile.readText() shouldBe expected

      val fileInsideDir = rootProjectDir.resolve("api").resolve("letMeBe.txt")
      withClue("existing api directory should not be overwritten") {
        fileInsideDir.shouldExist()
      }
    }
  }

  @Test
  fun validateDumpFromACustomDirectory() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/outputDirectory/different.gradle.kts")
      }

      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }
      dir("custom") {
        file("${rootProjectDir.name}.api") {
          resolve("/examples/classes/AnotherBuildConfig.dump")
        }
      }

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.build {
      shouldHaveTaskWithOutcome(":apiCheck", SUCCESS)
    }
  }

  @Test
  fun dumpIntoSubdirectory() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/outputDirectory/subdirectory.gradle.kts")
      }

      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }

      runner {
        arguments.add(":apiDump")
      }
    }

    runner.build {
      shouldHaveTaskWithOutcome(":apiDump", SUCCESS)

      val dumpFile = rootProjectDir.resolve("validation/api/${rootProjectDir.name}.api")
      dumpFile.shouldExist()

      val expected = readResourceFile("/examples/classes/AnotherBuildConfig.dump")
      dumpFile.readText() shouldBe expected
    }
  }

  @Test
  fun validateDumpFromASubdirectory() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/outputDirectory/subdirectory.gradle.kts")
      }

      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }
      dir("validation") {
        dir("api") {
          file("${rootProjectDir.name}.api") {
            resolve("/examples/classes/AnotherBuildConfig.dump")
          }
        }
      }

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.build {
      shouldHaveTaskWithOutcome(":apiCheck", SUCCESS)
    }
  }

  @Test
  fun dumpIntoParentDirectory() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/outputDirectory/outer.gradle.kts")
      }

      kotlin("AnotherBuildConfig.kt") {
        resolve("/examples/classes/AnotherBuildConfig.kt")
      }

      runner {
        arguments.add(":apiDump")
      }
    }

    runner.buildAndFail {
      shouldHaveTaskWithOutcome(":apiDump", FAILED)

      output shouldContain /* language=text */ """
          |> Error: Invalid output apiDirectory
          |  
          |  apiDirectory is set to a custom directory, outside of the current project directory.
          |  This is not permitted. apiDirectory must be a subdirectory of project ':' (the root project) directory.
          |  
          |  Remove the custom apiDirectory, or update apiDirectory to be a project subdirectory.
        """.trimMargin()
    }
  }
}
