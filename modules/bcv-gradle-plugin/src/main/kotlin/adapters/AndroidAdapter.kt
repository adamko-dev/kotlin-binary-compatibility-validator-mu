package dev.adamko.kotlin.binary_compatibility_validator.adapters

import dev.adamko.kotlin.binary_compatibility_validator.BCVProjectExtension
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer


internal fun createKotlinAndroidTargets(
  project: Project,
  extension: BCVProjectExtension,
) {
  project.pluginManager.withPlugin("kotlin-android") {
    val kotlinSourceSetsContainer = project.extensions.getByType<KotlinSourceSetContainer>()
    kotlinSourceSetsContainer.sourceSets.all {
      extension.targets.register<BCVJvmTarget>(name) {
        inputClasses.from(kotlin.classesDirectory)
      }
    }
  }
}
