package dev.adamko.kotlin.binary_compatibility_validator.adapters

import dev.adamko.kotlin.binary_compatibility_validator.BCVProjectExtension
import dev.adamko.kotlin.binary_compatibility_validator.internal.sourceSets
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.*


internal fun createKotlinJvmTargets(
  project: Project,
  extension: BCVProjectExtension,
) {
  project.pluginManager.withPlugin("kotlin") {
    extension.targets.register<BCVJvmTarget>("kotlinJvm") {
      project
        .sourceSets
        .matching { it.name == SourceSet.MAIN_SOURCE_SET_NAME }
        .all {
          inputClasses.from(output.classesDirs)
        }
    }
  }
}
