import buildsrc.utils.configurationNames
import buildsrc.utils.skipTestFixturesPublications
import org.gradle.api.attributes.plugin.GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  buildsrc.conventions.`kotlin-gradle-plugin`
  buildsrc.conventions.`gradle-plugin-bcv-features`
  id("dev.adamko.dev-publish")
  `java-test-fixtures`
  //com.github.johnrengelman.shadow
  //buildsrc.conventions.`gradle-plugin-variants`
  dev.adamko.kotlin.`binary-compatibility-validator`
}

dependencies {
  implementation(libs.javaDiffUtils)

  runtimeOnly(projects.modules.bcvGradlePlugin) {
    capabilities {
      requireCapability("${project.group}:${project.name.replace("-plugin", "-build-plugin")}")
    }
  }
  runtimeOnly(projects.modules.bcvGradlePlugin) {
    capabilities {
      requireCapability("${project.group}:${project.name.replace("-plugin", "-settings-plugin")}")
    }
  }

  compileOnly(libs.kotlinx.bcv)
  compileOnly(libs.kotlin.gradlePluginApi)

  mainBuildPluginCompileOnly(gradleApi())
  mainBuildPluginCompileOnly(libs.kotlin.gradlePluginApi) {
    version {
      prefer(embeddedKotlinVersion)
//      prefer(libs.versions.kotlinGradle.get())
    }
  }
  mainBuildPluginCompileOnly(gradleKotlinDsl())
  mainBuildPluginImplementation(project)

  mainSettingsPluginCompileOnly(gradleApi())
  mainSettingsPluginCompileOnly(gradleKotlinDsl())
  mainSettingsPluginImplementation(project)
  mainSettingsPluginImplementation(libs.kotlin.gradlePluginApi) {
    version {
      prefer(embeddedKotlinVersion)
//      prefer(libs.versions.kotlinGradle.get())
    }
  }

  testFixturesApi(gradleTestKit())

  testFixturesApi(platform(libs.junit.bom))
  testFixturesApi(libs.junit.jupiter)

  testFixturesApi(platform(libs.kotest.bom))
  testFixturesApi(libs.kotest.runnerJUnit5)
  testFixturesApi(libs.kotest.assertionsCore)
  testFixturesApi(libs.kotest.property)
}

//configurations.mainBuildPluginRuntimeClasspath {
//  extendsFrom(configurations.runtimeClasspath.get())
//}
//
//configurations.mainSettingsPluginRuntimeClasspath {
//  extendsFrom(configurations.runtimeClasspath.get())
//}

kotlin {
  target {
    val mainCompilation = compilations.named("main")
    compilations
      .matching {
        it.name == "mainBuildPlugin" || it.name == "mainSettingsPlugin"
      }
      .configureEach {
        associateWith(mainCompilation.get())
      }
  }
}

@Suppress("UnstableApiUsage")
gradlePlugin {
  website = "https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu"
  vcsUrl = "https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu"
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

  registerBcvPlugin("binary-compatibility-validator", "BCVPlugin") {
    displayName = "Binary Compatibility Validator MU"
  }
  registerBcvPlugin("binary-compatibility-validator.project", "BCVProjectPlugin") {
    displayName = "Binary Compatibility Validator MU (Project Plugin)"
    description += """
      |
      |This is a Gradle Project plugin and can be applied in a `build.gradle` or `build.gradle.kts` file.
    """.trimMargin()
  }
  registerBcvPlugin("binary-compatibility-validator.settings", "BCVSettingsPlugin") {
    displayName = "Binary Compatibility Validator MU (Settings Plugin)"
    description += """
      |
      |This is a Gradle Settings plugin and can be applied in a `settings.gradle` or `settings.gradle.kts` file.
    """.trimMargin()
  }
}

// Only consume Gradle dependencies that match the Gradle version we support
configurations
  .matching { it.isCanBeConsumed && it.name in sourceSets.main.get().configurationNames() }
  .configureEach {
    attributes {
      attribute(
        GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
        objects.named(libs.versions.supportedGradleVersion.get())
      )
    }
  }

skipTestFixturesPublications()

// Shadow plugin doesn't seem to help with https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu/issues/1
//tasks.shadowJar {
//  minimize()
//  isEnableRelocation = false
//  archiveClassifier.set("")
//}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-opt-in=dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi"
    )
  }
}

binaryCompatibilityValidator {
  ignoredMarkers.add("dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi")
}
