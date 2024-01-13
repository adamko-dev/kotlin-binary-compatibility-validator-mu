package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.*


abstract class BCVPlugin
@BCVInternalApi
constructor() : Plugin<PluginAware> {
  override fun apply(target: PluginAware) {
    when (target) {
      is Project  -> target.pluginManager.apply(BCV_PROJECT_PLUGIN_ID)
      is Settings -> target.pluginManager.apply(BCV_SETTINGS_PLUGIN_ID)
      else        -> error("cannot apply BCVPlugin to ${target::class}")
    }
  }

  companion object {
    const val API_DIR = "api"
    const val EXTENSION_NAME = "binaryCompatibilityValidator"
    const val RUNTIME_CLASSPATH_CONFIGURATION_NAME = "bcvMuRuntime"
    const val RUNTIME_CLASSPATH_RESOLVER_CONFIGURATION_NAME = "bcvMuRuntimeResolver"

    const val TASK_GROUP = "bcv mu"
    const val API_CHECK_TASK_NAME = "apiCheck"
    const val API_DUMP_TASK_NAME = "apiDump"
    const val API_GENERATE_TASK_NAME = "apiGenerate"

    const val BCV_PROJECT_PLUGIN_ID = "dev.adamko.kotlin.binary-compatibility-validator.project"
    const val BCV_SETTINGS_PLUGIN_ID = "dev.adamko.kotlin.binary-compatibility-validator.settings"
  }
}
