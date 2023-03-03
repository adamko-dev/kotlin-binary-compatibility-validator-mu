package dev.adamko.kotlin.binary_compatibility_validator

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.*


abstract class BCVPlugin : Plugin<PluginAware> {
  override fun apply(target: PluginAware) {
    when (target) {
      is Project  -> target.pluginManager.apply(BCVProjectPlugin::class)
      is Settings -> target.pluginManager.apply(BCVSettingsPlugin::class)
      else        -> error("cannot apply BCVPlugin to ${target::class}")
    }
  }

  companion object {
    const val API_DIR = "api"
    const val EXTENSION_NAME = "binaryCompatibilityValidator"
    const val TASK_GROUP = "bcv mu"
    const val RUNTIME_CLASSPATH_CONFIGURATION_NAME = "bcvMuRuntime"
  }
}
