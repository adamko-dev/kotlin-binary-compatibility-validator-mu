package buildsrc.conventions

plugins {
  id("org.gradle.kotlin.kotlin-dsl")
}

val java = extensions.getByType<JavaPluginExtension>()

extensions.configure<SourceSetContainer> {
//sourceSets {
  val mainBuildPlugin by creating {
    kotlin { srcDir("src/mainBuildPlugin/kotlin") }
    java { srcDirs(emptyList<File>()) }
  }
  java.registerFeature(mainBuildPlugin.name) {
    usingSourceSet(mainBuildPlugin)
    withJavadocJar()
    withSourcesJar()
    project.run {
      capability(group.toString(), name.replace("-plugin", "-build-plugin"), version.toString())
    }
  }
  val mainSettingsPlugin by creating {
    kotlin { srcDir("src/mainSettingsPlugin/kotlin") }
    java { srcDirs(emptyList<File>()) }
  }
  java.registerFeature(mainSettingsPlugin.name) {
    usingSourceSet(mainSettingsPlugin)
    withJavadocJar()
    withSourcesJar()
    project.run {
      capability(group.toString(), name.replace("-plugin", "-settings-plugin"), version.toString())
    }
  }
}

////extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
//kotlin {
//  target {
//    val mainCompilation = compilations.named("main")
//    compilations
//      .matching {
//        it.name == "mainBuildPlugin" || it.name == "mainSettingsPlugin"
//      }
//      .configureEach {
//        associateWith(mainCompilation.get())
//      }
//  }
//}

plugins.withType<MavenPublishPlugin>().configureEach {
  extensions.configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
      // disable warning because this project doesn't need to be resolvable by Maven
      // > Maven publication 'pluginMaven' pom metadata warnings (silence with 'suppressPomMetadataWarningsFor(variant)'):
      // >  - Variant mainBuildPluginApiElements:
      // >     - Declares capability dev.adamko.kotlin.binary_compatibility_validator:bcv-gradle-build-plugin:main-SNAPSHOT which cannot be mapped to Maven
      suppressPomMetadataWarningsFor("pluginMaven")
      suppressPomMetadataWarningsFor("mainBuildPlugin")
      suppressPomMetadataWarningsFor("mainSettingsPlugin")

      suppressPomMetadataWarningsFor("mainBuildPluginApiElements")
      suppressPomMetadataWarningsFor("mainBuildPluginJavadocElements")
      suppressPomMetadataWarningsFor("mainBuildPluginRuntimeElements")
      suppressPomMetadataWarningsFor("mainBuildPluginSourcesElements")
      suppressPomMetadataWarningsFor("mainSettingsPluginApiElements")
      suppressPomMetadataWarningsFor("mainSettingsPluginJavadocElements")
      suppressPomMetadataWarningsFor("mainSettingsPluginRuntimeElements")
      suppressPomMetadataWarningsFor("mainSettingsPluginSourcesElements")
    }
  }
}
