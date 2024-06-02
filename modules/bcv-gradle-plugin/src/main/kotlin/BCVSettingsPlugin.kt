package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.globToRegex
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetDefaults
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetBaseSpec
import dev.adamko.kotlin.binary_compatibility_validator.targets.KLibSignatureVersion
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.*

abstract class BCVSettingsPlugin
@BCVInternalApi
@Inject
constructor(
  private val objects: ObjectFactory
) : Plugin<Settings> {

  override fun apply(settings: Settings) {
    val extension = settings.extensions.create<Extension>(
      "bcvSettings",
      objects.newInstance<BCVTargetDefaults>(),
    ).apply {
      ignoredProjects.convention(emptySet())

      defaultTargetValues {
        enabled.convention(true)
        ignoredClasses.convention(emptySet())
        ignoredMarkers.convention(emptySet())
        ignoredPackages.convention(emptySet())

        @OptIn(BCVExperimentalApi::class)
        klib.apply {
          enabled.convention(false)
          signatureVersion.convention(KLibSignatureVersion.Latest)
          strictValidation.convention(false)
        }
      }
    }

    settings.gradle.beforeProject {
      if (
        extension.ignoredProjects.get().none {
          globToRegex(it, Project.PATH_SEPARATOR).matches(project.path)
        }
      ) {
        project.pluginManager.apply(BCVProjectPlugin::class)
        project.extensions.configure<BCVProjectExtension> {
          enabled.convention(extension.defaultTargetValues.enabled)
          ignoredClasses.convention(extension.defaultTargetValues.ignoredClasses)
          ignoredMarkers.convention(extension.defaultTargetValues.ignoredMarkers)
          ignoredPackages.convention(extension.defaultTargetValues.ignoredPackages)
        }
      }
    }
  }

  abstract class Extension
  @BCVInternalApi
  @Inject
  constructor(

    /**
     * Set [BCVTargetBaseSpec] values that will be used as defaults for all
     * [BCVProjectExtension.targets] in subprojects.
     */
    val defaultTargetValues: BCVTargetDefaults
  ) {

    /**
     * Paths of projects.
     *
     * Uses a glob matcher.
     *
     * - `?` will match nothing, or a single character, excluding `:`
     * - `*` will match zero, or many characters, excluding `:`
     * - `**` will match 0 to many characters, including `:`
     */
    abstract val ignoredProjects: SetProperty<String>

    fun defaultTargetValues(configure: Action<BCVTargetDefaults>): Unit =
      configure.execute(defaultTargetValues)
  }
}
