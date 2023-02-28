package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetSpec
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ReplacedBy
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface BCVProjectExtension : BCVTargetSpec, ExtensionAware {

  /** Sets the default [BCVTarget.enabled] value for all [targets]. */
  override val enabled: Property<Boolean>

  /** Sets the default [BCVTarget.ignoredPackages] value for all [targets]. */
  override val ignoredPackages: SetProperty<String>

  /** Sets the default [BCVTarget.ignoredMarkers] value for all [targets]. */
  override val ignoredMarkers: SetProperty<String>

  @get:ReplacedBy("ignoredMarkers")
  @Deprecated("renamed to ignoredMarkers", ReplaceWith("ignoredMarkers"))
  val nonPublicMarkers: SetProperty<String>

  /** Sets the default [BCVTarget.ignoredClasses] value for all [targets]. */
  override val ignoredClasses: SetProperty<String>

  /**
   * The directory that contains the API declarations.
   *
   * Defaults to [BCVPlugin.API_DIR].
   */
  val outputApiDir: DirectoryProperty

  val projectName: Property<String>

  val kotlinxBinaryCompatibilityValidatorVersion: Property<String>

  val targets: NamedDomainObjectContainer<BCVTarget>
}
