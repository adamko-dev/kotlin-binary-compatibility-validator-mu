import buildsrc.utils.configurationNames
import buildsrc.utils.skipTestFixturesPublications
import org.gradle.api.attributes.plugin.GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  buildsrc.conventions.`kotlin-gradle-plugin`
  buildsrc.conventions.`maven-publishing`
  id("dev.adamko.dev-publish")
  `java-test-fixtures`
  //com.github.johnrengelman.shadow
  //buildsrc.conventions.`gradle-plugin-variants`
  dev.adamko.kotlin.`binary-compatibility-validator`
}

dependencies {
  implementation(libs.javaDiffUtils)

  compileOnly(libs.kotlinx.bcv)
//  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.kotlin.gradlePluginApi)

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


publishing {
  publications {
    register<MavenPublication>("relocation") {
      pom {
        val relocationMessage =
          "Relocated artifact. Replaced underscores with dashes in the Group ID, to match BCV-MU's Gradle Plugin ID."
        name = "Binary Compatibility Validator MU [RELOCATION MARKER]"
        description = relocationMessage

        // Old artifact coordinates
        groupId = "dev.adamko.kotlin.binary_compatibility_validator"
        artifactId = "bcv-gradle-plugin"

        distributionManagement {
          relocation {
            // New artifact coordinates
            groupId = project.group.toString()
            artifactId = project.name
            message = relocationMessage
          }
        }
      }
    }
  }
}

val createBCVProperties by tasks.registering {
  val bcvVersion = libs.versions.kotlinx.bcv
  inputs.property("bcvVersion", bcvVersion)

  val generatedSource = layout.buildDirectory.dir("generated-src/main/kotlin/")
  outputs.dir(generatedSource)
    .withPropertyName("generatedSource")

  doLast {
    val bcvMuBuildPropertiesFile = generatedSource.get()
      .file("dev/adamko/kotlin/binary_compatibility_validator/internal/BCVProperties.kt")

    bcvMuBuildPropertiesFile.asFile.apply {
      parentFile.mkdirs()
      writeText(
        """
          |package dev.adamko.kotlin.binary_compatibility_validator.internal
          |
          |internal object BCVProperties {
          |  const val bcvVersion: String = "${bcvVersion.get()}"
          |}
          |
        """.trimMargin()
      )
    }
  }
}

kotlin.sourceSets.main {
  kotlin.srcDir(createBCVProperties)
}
