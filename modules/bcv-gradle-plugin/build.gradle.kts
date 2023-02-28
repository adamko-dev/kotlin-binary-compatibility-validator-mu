import buildsrc.utils.configurationNames
import buildsrc.utils.skipTestFixturesPublications
import org.gradle.api.attributes.plugin.GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE

plugins {
  buildsrc.conventions.`kotlin-gradle-plugin`
  buildsrc.conventions.`maven-publish-test`
  `java-test-fixtures`
//  buildsrc.conventions.`gradle-plugin-variants`
}

dependencies {
  implementation(libs.javaDiffUtils)

  compileOnly(libs.kotlinx.bcv)
  compileOnly(libs.kotlin.gradlePlugin)

  testFixturesApi(gradleTestKit())

  testFixturesApi(platform(libs.junit.bom))
  testFixturesApi(libs.junit.jupiter)

  testFixturesApi(platform(libs.kotest.bom))
  testFixturesApi(libs.kotest.runnerJUnit5)
  testFixturesApi(libs.kotest.assertionsCore)
  testFixturesApi(libs.kotest.property)
}

@Suppress("UnstableApiUsage")
gradlePlugin {
  website.set("https://github.com/Kotlin/binary-compatibility-validator-alternative")
  vcsUrl.set("https://github.com/adamko-dev/binary-compatibility-validator-alternative")
  isAutomatedPublishing = true

  plugins.configureEach {
    tags.addAll("kotlin", "kotlin/jvm", "api-management", "binary-compatibility")
  }

  fun registerBcvPlugin(name: String, cls: String, config: PluginDeclaration.() -> Unit = {}) {
    plugins.register(cls) {
      id = "dev.adamko.kotlin.$name"
      implementationClass = "dev.adamko.kotlin.binary_compatibility_validator.$cls"
      description = """
          Validates the public JVM binary API to make sure breaking changes are tracked.
        """.trimIndent()
      config()
    }
  }

  registerBcvPlugin("binary-compatibility-validator", "BCVPlugin")
  registerBcvPlugin("binary-compatibility-validator.project", "BCVProjectPlugin") {
    description += """
      |
      |This is a Gradle Project plugin and can be applied directly in a `build.gradle` or `build.gradle.kts` file.
    """.trimMargin()
  }
  registerBcvPlugin("binary-compatibility-validator.settings", "BCVSettingsPlugin") {

    description += """
      |
      |This is a Gradle Settings plugin and can be applied directly in a `settings.gradle` or `settings.gradle.kts` file.
    """.trimMargin()
  }
}

skipTestFixturesPublications()
