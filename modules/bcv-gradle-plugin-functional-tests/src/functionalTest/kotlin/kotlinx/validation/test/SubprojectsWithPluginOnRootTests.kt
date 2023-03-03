package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.invariantNewlines
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveRunTask
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.file.shouldBeEmpty
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("TODO update with 'allprojects {}' replacement")
internal class SubprojectsWithPluginOnRootTests : BaseKotlinGradleTest() {

  /**
   * Sets up a project hierarchy like this:
   * ```
   * build.gradle.kts (with the plugin)
   * settings.gradle.kts (including refs to 4 subprojects)
   * sub1/
   *    build.gradle.kts
   *    subsub1/build.gradle.kts
   *    subsub2/build.gradle.kts
   * sub2/build.gradle.kts
   * ```
   */
  private fun BaseKotlinScope.createProjectHierarchyWithPluginOnRoot() {
    settingsGradleKts {
      resolve("/examples/gradle/settings/settings-with-hierarchy.gradle.kts")
    }
    buildGradleKts {
      resolve("/examples/gradle/base/withPlugin.gradle.kts")
    }
    dir("sub1") {
      buildGradleKts {
        resolve("/examples/gradle/base/withoutPlugin-noKotlinVersion.gradle.kts")
      }
      dir("subsub1") {
        buildGradleKts {
          resolve("/examples/gradle/base/withoutPlugin-noKotlinVersion.gradle.kts")
        }
      }
      dir("subsub2") {
        buildGradleKts {
          resolve("/examples/gradle/base/withoutPlugin-noKotlinVersion.gradle.kts")
        }
      }
    }
    dir("sub2") {
      buildGradleKts {
        resolve("/examples/gradle/base/withoutPlugin-noKotlinVersion.gradle.kts")
      }
    }
  }

  @Test
  fun `apiCheck should be run on all subprojects when running check`() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()

      emptyApiFile(projectName = rootProjectDir.name)

      dir("sub1") {
        emptyApiFile(projectName = "sub1")

        dir("subsub1") {
          emptyApiFile(projectName = "subsub1")
        }

        dir("subsub2") {
          emptyApiFile(projectName = "subsub2")
        }
      }

      dir("sub2") {
        emptyApiFile(projectName = "sub2")
      }

      runner {
        arguments.add("check")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiCheck", SUCCESS)
      shouldHaveRunTask(":sub1:apiCheck", SUCCESS)
      shouldHaveRunTask(":sub1:subsub1:apiCheck", SUCCESS)
      shouldHaveRunTask(":sub1:subsub2:apiCheck", SUCCESS)
      shouldHaveRunTask(":sub2:apiCheck", SUCCESS)
    }
  }

  @Test
  fun `apiCheck should succeed on all subprojects when api files are empty but there are no Kotlin sources`() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()

      emptyApiFile(projectName = rootProjectDir.name)

      dir("sub1") {
        emptyApiFile(projectName = "sub1")

        dir("subsub1") {
          emptyApiFile(projectName = "subsub1")
        }

        dir("subsub2") {
          emptyApiFile(projectName = "subsub2")
        }
      }

      dir("sub2") {
        emptyApiFile(projectName = "sub2")
      }

      runner {
        arguments.add("apiCheck")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiCheck", SUCCESS)
      shouldHaveRunTask(":sub1:apiCheck", SUCCESS)
      shouldHaveRunTask(":sub1:subsub1:apiCheck", SUCCESS)
      shouldHaveRunTask(":sub1:subsub2:apiCheck", SUCCESS)
      shouldHaveRunTask(":sub2:apiCheck", SUCCESS)
    }
  }

  @Test
  fun `apiCheck should succeed on subproject, when api file is empty but there are no sources`() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()

      dir("sub1") {
        emptyApiFile(projectName = "sub1")
      }

      runner {
        arguments.add(":sub1:apiCheck")
      }
    }

    runner.build {
      shouldHaveRunTask(":sub1:apiCheck", SUCCESS)
    }
  }

  @Test
  fun `apiCheck should succeed on sub-subproject, when api file is empty but there are no sources`() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()

      dir("sub1") {
        dir("subsub2") {
          emptyApiFile(projectName = "subsub2")
        }
      }

      runner {
        arguments.add(":sub1:subsub2:apiCheck")
      }
    }

    runner.build {
      shouldHaveRunTask(":sub1:subsub2:apiCheck", SUCCESS)
    }
  }

  @Test
  fun `apiCheck should succeed on sub-subproject, when public classes match api file`() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()

      dir("sub1") {
        dir("subsub2") {
          kotlin("Subsub2Class.kt") {
            resolve("/examples/classes/Subsub2Class.kt")
          }
          apiFile(projectName = "subsub2") {
            resolve("/examples/classes/Subsub2Class.dump")
          }
        }
      }

      runner {
        arguments.add(":sub1:subsub2:apiCheck")
      }
    }

    runner.build {
      shouldHaveRunTask(":sub1:subsub2:apiCheck", SUCCESS)
    }
  }

  @Test
  fun `apiCheck should succeed on subprojects, when public classes match api files`() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()

      emptyApiFile(projectName = rootProjectDir.name)

      dir("sub1") {
        emptyApiFile(projectName = "sub1")

        dir("subsub1") {
          kotlin("Subsub1Class.kt") {
            resolve("/examples/classes/Subsub1Class.kt")
          }
          apiFile(projectName = "subsub1") {
            resolve("/examples/classes/Subsub1Class.dump")
          }
        }
        dir("subsub2") {
          kotlin("Subsub2Class.kt") {
            resolve("/examples/classes/Subsub2Class.kt")
          }
          apiFile(projectName = "subsub2") {
            resolve("/examples/classes/Subsub2Class.dump")
          }
        }
      }

      dir("sub2") {
        emptyApiFile(projectName = "sub2")
      }

      runner {
        arguments.add("apiCheck")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiCheck", SUCCESS)
      shouldHaveRunTask(":sub1:apiCheck", SUCCESS)
      shouldHaveRunTask(":sub1:subsub1:apiCheck", SUCCESS)
      shouldHaveRunTask(":sub1:subsub2:apiCheck", SUCCESS)
      shouldHaveRunTask(":sub2:apiCheck", SUCCESS)
    }
  }

  @Test
  fun `apiDump should succeed and create empty api on subproject, when no kotlin files are included in SourceSet`() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()

      runner {
        arguments.add(":sub1:apiDump")
      }
    }

    runner.build {
      shouldHaveRunTask(":sub1:apiDump", SUCCESS)

      val apiDumpFile = rootProjectDir.resolve("sub1/api/sub1.api")
      assertTrue(apiDumpFile.exists(), "api dump file ${apiDumpFile.path} should exist")

      apiDumpFile.shouldBeEmpty()
    }
  }

  @Test
  fun `apiDump should succeed and create correct api dumps on subprojects`() {
    val runner = test {
      createProjectHierarchyWithPluginOnRoot()

      dir("sub1") {
        dir("subsub1") {
          kotlin("Subsub1Class.kt") {
            resolve("/examples/classes/Subsub1Class.kt")
          }
        }
        dir("subsub2") {
          kotlin("Subsub2Class.kt") {
            resolve("/examples/classes/Subsub2Class.kt")
          }
        }
      }

      runner {
        arguments.add("apiDump")
      }
    }

    runner.build {
      shouldHaveRunTask(":apiDump", SUCCESS)
      shouldHaveRunTask(":sub1:apiDump", SUCCESS)
      shouldHaveRunTask(":sub1:subsub1:apiDump", SUCCESS)
      shouldHaveRunTask(":sub1:subsub2:apiDump", SUCCESS)
      shouldHaveRunTask(":sub2:apiDump", SUCCESS)

      assertTrue(
        rootProjectApiDump.exists(),
        "api dump file ${rootProjectApiDump.path} should exist"
      )
      rootProjectApiDump.shouldBeEmpty()

      val apiSub1 = rootProjectDir.resolve("sub1/api/sub1.api")
      assertTrue(apiSub1.exists(), "api dump file ${apiSub1.path} should exist")
      apiSub1.shouldBeEmpty()

      val apiSubsub1 = rootProjectDir.resolve("sub1/subsub1/api/subsub1.api")
      assertTrue(apiSubsub1.exists(), "api dump file ${apiSubsub1.path} should exist")
      val apiSubsub1Expected = readResourceFile("/examples/classes/Subsub1Class.dump")
      apiSubsub1.readText().invariantNewlines().shouldBeEqualComparingTo(apiSubsub1Expected)

      val apiSubsub2 = rootProjectDir.resolve("sub1/subsub2/api/subsub2.api")
      assertTrue(apiSubsub2.exists(), "api dump file ${apiSubsub2.path} should exist")
      val apiSubsub2Expected = readResourceFile("/examples/classes/Subsub2Class.dump")
      apiSubsub2.readText().invariantNewlines().shouldBeEqualComparingTo(apiSubsub2Expected)

      val apiSub2 = rootProjectDir.resolve("sub2/api/sub2.api")
      assertTrue(apiSub2.exists(), "api dump file ${apiSub2.path} should exist")
      apiSub2.shouldBeEmpty()
    }
  }
}
