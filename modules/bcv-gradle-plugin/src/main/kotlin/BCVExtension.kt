package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetSpec
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ReplacedBy
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface BCVExtension : BCVTargetSpec, ExtensionAware {

  override val enabled: Property<Boolean>

  override val ignoredPackages: SetProperty<String>

  override val ignoredMarkers: SetProperty<String>

  @get:ReplacedBy("ignoredMarkers")
  @Deprecated("renamed to ignoredMarkers", ReplaceWith("ignoredMarkers"))
  val nonPublicMarkers: SetProperty<String>

  override val ignoredClasses: SetProperty<String>

  val outputApiDir: DirectoryProperty

  val projectName: Property<String>

  val kotlinxBinaryCompatibilityValidatorVersion: Property<String>

  val targets: NamedDomainObjectContainer<BCVTarget>
}
