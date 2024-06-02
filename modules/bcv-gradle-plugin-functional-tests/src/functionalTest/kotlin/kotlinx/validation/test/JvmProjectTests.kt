package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.build
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.invariantNewlines
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldHaveTaskWithOutcome
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test

class JvmProjectTests : BaseKotlinGradleTest() {

  @Test
  fun `apiDump for a project with generated sources only`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/generatedSources/generatedJvmSources.gradle.kts")
      }
      runner {
        // TODO: enable configuration cache back when we start skipping tasks correctly
        //configurationCache = false
        arguments.add(":apiDump")
      }
    }
    runner.build {
      shouldHaveTaskWithOutcome(":apiDump", SUCCESS, FROM_CACHE)

      val expected = readResourceFile("/examples/classes/GeneratedSources.dump")
      rootProjectApiDump.readText().invariantNewlines() shouldBe expected
    }
  }

  @Test
  fun `apiCheck for a project with generated sources only`() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/generatedSources/generatedJvmSources.gradle.kts")
      }
      apiFile(projectName = rootProjectDir.name) {
        resolve("/examples/classes/GeneratedSources.dump")
      }
      runner {
        // TODO: enable configuration cache back when we start skipping tasks correctly
        //configurationCache = false
        arguments.add(":apiCheck")
      }
    }
    runner.build {
      shouldHaveTaskWithOutcome(":apiCheck", SUCCESS)
    }
  }
}
