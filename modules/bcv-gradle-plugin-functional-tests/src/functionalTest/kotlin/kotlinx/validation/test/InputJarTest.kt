package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.api.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("need to update the DSL")
class InputJarTest : BaseKotlinGradleTest() {

  @Test
  fun testOverrideInputJar() {
    val runner = test {
      buildGradleKts {
        resolve("/examples/gradle/base/withPlugin.gradle.kts")
        resolve("/examples/gradle/configuration/jarAsInput/inputJar.gradle.kts")
      }

      kotlin("Properties.kt") {
        resolve("/examples/classes/Properties.kt")
      }

      apiFile(projectName = rootProjectDir.name) {
        resolve("/examples/classes/PropertiesJarTransformed.dump")
      }

      runner {
        arguments.add(":apiCheck")
      }
    }

    runner.build().apply {
      assertTaskSuccess(":jar")
      assertTaskSuccess(":apiCheck")
    }
  }
}
