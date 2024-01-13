@file:Suppress("PackageDirectoryMismatch")

/**
 * Gradle does not generate Kotlin DSL accessors for Settings plugins, so they are defined manually.
 *
 * For ease of use, the package is set to `org.gradle.kotlin.dsl`, which is a default import
 * package in `settings.gradle.kts` files.
 */
package org.gradle.kotlin.dsl

import dev.adamko.kotlin.binary_compatibility_validator.BCVSettingsPlugin
import org.gradle.api.initialization.Settings

/** Retrieves the [binaryCompatibilityValidator][BCVSettingsPlugin.Extension] extension. */
val Settings.binaryCompatibilityValidator: BCVSettingsPlugin.Extension
  get() = extensions.getByType()


/** Configures the [binaryCompatibilityValidator][BCVSettingsPlugin.Extension] extension. */
fun Settings.binaryCompatibilityValidator(configure: BCVSettingsPlugin.Extension.() -> Unit) =
  binaryCompatibilityValidator.configure()
