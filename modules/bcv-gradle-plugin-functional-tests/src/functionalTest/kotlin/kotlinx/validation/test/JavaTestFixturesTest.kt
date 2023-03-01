package kotlinx.validation.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.*
import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import kotlin.io.path.readText
import org.gradle.testkit.runner.TaskOutcome.SUCCESS

class JavaTestFixturesTest : FunSpec({

  context("when a project has the java-test-fixtures plugin applied") {

    context("and the testFixtures BCV target is enabled") {
      val project = createTestFixturesProject(bcvTestFixturesTargetEnabled = true)

      test("expect :apiDump task passes") {
        project.runner.withArguments("apiDump").build {
          withClue(output) {
            task(":apiDump") shouldHaveOutcome SUCCESS
          }
        }
      }

      test("expect :apiCheck task passes") {
        project.runner
          .withArguments(
            "apiCheck",
            "--info",
            "--stacktrace",
          )
          .build {
            withClue(output) {
              task(":apiCheck") shouldHaveOutcome SUCCESS
            }
          }
      }

      test("expect correct kotlinJvm api declaration is generated") {
        project.projectDir.resolve("api/kotlinJvm/java-test-fixtures-test.api").asClue {
          it.shouldBeAFile()
          it.readText().invariantNewlines() shouldBe /* language=TEXT */ """
            |public final class Hello {
            |	public fun <init> (Ljava/lang/String;)V
            |	public final fun greeting ()Ljava/lang/String;
            |}
            |
            |
            """.trimMargin()
        }
      }

      test("expect correct testFixtures api declaration is generated") {
        project.projectDir.resolve("api/testFixtures/java-test-fixtures-test.api").asClue {
          it.shouldBeAFile()
          it.readText().invariantNewlines() shouldBe /* language=TEXT */ """
            |public final class HelloHelperKt {
            |	public static final fun standardHello ()LHello;
            |}
            |
            |
          """.trimMargin()
        }
      }
    }
    context("and the testFixtures BCV target is disabled") {
      val project = createTestFixturesProject(bcvTestFixturesTargetEnabled = false)

      test("expect :apiDump task passes") {
        project.runner.withArguments("apiDump").build {
          withClue(output) {
            task(":apiDump") shouldHaveOutcome SUCCESS
          }
        }
      }

      test("expect :apiCheck task passes") {
        project.runner
          .withArguments(
            "apiCheck",
            "--info",
            "--stacktrace",
          )
          .build {
            withClue(output) {
              task(":apiCheck") shouldHaveOutcome SUCCESS
            }
          }
      }

      test("expect correct kotlinJvm api declaration is generated") {
        project.projectDir.resolve("api/java-test-fixtures-test.api").asClue {
          it.shouldBeAFile()
          it.readText().invariantNewlines() shouldBe /* language=TEXT */ """
            |public final class Hello {
            |	public fun <init> (Ljava/lang/String;)V
            |	public final fun greeting ()Ljava/lang/String;
            |}
            |
            |
            """.trimMargin()
        }
      }

      test("expect testFixtures api declaration is not generated") {
        project.projectDir.resolve("api/testFixtures/java-test-fixtures-test.api")
          .shouldNotExist()
      }
    }
  }
})


private fun FunSpec.createTestFixturesProject(
  bcvTestFixturesTargetEnabled: Boolean,
  projectDir: Path = tempdir().toPath()
): GradleProjectTest {
  return gradleKtsProjectTest("java-test-fixtures-test", projectDir) {

    buildGradleKts = """
        |plugins {
        |  kotlin("jvm") version "1.7.10"
        |  id("dev.adamko.kotlin.binary-compatibility-validator") version "0.0.3-SNAPSHOT"
        |  `java-test-fixtures`
        |}
        |
        |binaryCompatibilityValidator {
        |  testFixtures {
        |    enabled.set($bcvTestFixturesTargetEnabled)
        |  }
        |}
        |
      """.trimMargin()

    dir("src/main/kotlin") {
      createKotlinFile(
        "Hello.kt",
        """
            |class Hello(private val response: String) {
            |  fun greeting() = response
            |}
            |
          """.trimMargin()
      )
    }
    dir("src/testFixtures/kotlin") {
      createKotlinFile(
        "HelloHelper.kt",
        """
            |fun standardHello() = Hello("standard")
            |
          """.trimMargin()
      )
    }
  }
}
