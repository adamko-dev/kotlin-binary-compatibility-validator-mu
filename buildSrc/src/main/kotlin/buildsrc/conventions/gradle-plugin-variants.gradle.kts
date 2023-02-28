package buildsrc.conventions

import org.gradle.api.attributes.plugin.GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE
import buildsrc.utils.configurationNames

plugins {
  id("buildsrc.conventions.base")
  `java-gradle-plugin`
}

configurations
  .matching { it.isCanBeConsumed && it.name in sourceSets.main.get().configurationNames() }
  .configureEach {
    attributes {
      attribute(GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named("7.6"))
    }
  }

fun createGradleVariantSourceSet(name: String, gradleVersion: String) {
  val variantSources = sourceSets.create(name)

  java {
    registerFeature(variantSources.name) {
      usingSourceSet(variantSources)
      capability("${project.group}", "${project.name}", "${project.version}")

      withJavadocJar()
      withSourcesJar()
    }
  }

  configurations
    .matching { it.isCanBeConsumed && it.name in variantSources.configurationNames() }
    .configureEach {
      attributes {
        attribute(GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named(gradleVersion))
      }
    }

  tasks.named<Copy>(variantSources.processResourcesTaskName) {
    val copyPluginDescriptors = rootSpec.addChild()
//    copyPluginDescriptors.into("META-INF/gradle-plugins")
    copyPluginDescriptors.into(tasks.pluginDescriptors.map { it.outputDirectory.asFile.get().invariantSeparatorsPath })
    copyPluginDescriptors.from(tasks.pluginDescriptors)
  }

  dependencies {
    variantSources.compileOnlyConfigurationName(gradleApi())
  }

  project.tasks.named<AbstractCompile>(variantSources.compileJavaTaskName).configure {
    classpath += sourceSets.main.get().compileClasspath
  }
}

//val mainGradle = registerGradleVariant("mainGradle", "7.6")
createGradleVariantSourceSet("mainGradle8", "8.1")
