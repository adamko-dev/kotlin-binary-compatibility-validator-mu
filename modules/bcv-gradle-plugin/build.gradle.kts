plugins {
  buildsrc.conventions.`kotlin-gradle-plugin`
  buildsrc.conventions.`maven-publish-test`
//  buildsrc.conventions.`gradle-plugin-variants`
}

dependencies {
//  implementation("com.googlecode.java-diff-utils:diffutils:1.3.0")
  implementation("io.github.java-diff-utils:java-diff-utils:4.12")

  compileOnly("org.jetbrains.kotlinx:binary-compatibility-validator:0.13.0")

  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
}

@Suppress("UnstableApiUsage")
gradlePlugin {
  website = "https://github.com/Kotlin/binary-compatibility-validator-alternative"
  vcsUrl = "https://github.com/adamko-dev/binary-compatibility-validator-alternative"
  isAutomatedPublishing = true

  plugins.configureEach {
    tags += listOf("kotlin", "kotlin/jvm", "api-management", "binary-compatibility")
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
