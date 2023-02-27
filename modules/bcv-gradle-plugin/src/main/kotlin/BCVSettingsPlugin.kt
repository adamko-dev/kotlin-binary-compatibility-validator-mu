package dev.adamko.kotlin.binary_compatibility_validator

import org.gradle.api.Plugin
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
        extension.ignoredProjects.get().none { globToRegex(it).matches(project.path) }
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

  companion object {

    /** Converts a glob-string to a [Regex] */
    private fun globToRegex(glob: String): Regex {
      return glob
        .replace(Regex("""\W"""), """\\$0""")
        .replace(Regex("""(?<doubleStar>\\\*\\\*)|(?<singleStar>\\\*)|(?<singleChar>\\\?)""")) {
          when {
            it.groups["doubleStar"] != null -> ".*?"
            it.groups["singleStar"] != null -> "[^:]*?"
            it.groups["singleChar"] != null -> "[^:]?"
            else                            -> error("could not convert '$glob' to regex")
          }
        }.toRegex(RegexOption.IGNORE_CASE)
    }
  }
}
