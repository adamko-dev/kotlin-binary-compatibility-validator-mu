package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.internal.globToRegex
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create

abstract class BCVSettingsPlugin : Plugin<Settings> {

  override fun apply(settings: Settings) {
    val extension = settings.extensions.create("bcvSettings", Extension::class).apply {
      ignoredProjects.convention(emptySet())
    }

    settings.gradle.beforeProject {
      if (
        extension.ignoredProjects.get().none {
          globToRegex(it, Project.PATH_SEPARATOR).matches(project.path)
        }
      ) {
        project.pluginManager.apply(BCVProjectPlugin::class)
      }
    }
  }

  interface Extension {

    /**
     * Paths of projects.
     *
     * Uses a glob matcher.
     *
     * - `?` will match nothing, or a single character, excluding `:`
     * - `*` will match zero, or many characters, excluding `:`
     * - `**` will match 0 to many characters, including `:`
     */
    val ignoredProjects: SetProperty<String>
  }
}
