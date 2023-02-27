package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface BCVExtension {

  /** Enables or disables API generation and validation */
  val enabled: Property<Boolean>

  /**
   * Fully qualified package names that are not consider public API.
   * For example, it could be `kotlinx.coroutines.internal` or `kotlinx.serialization.implementation`.
   */
  val ignoredPackages: SetProperty<String>

  /**
   * Fully qualified names of annotations that effectively exclude declarations from being public.
   * Example of such annotation could be `kotlinx.coroutines.InternalCoroutinesApi`.
   */
  val ignoredMarkers: SetProperty<String>

  @get:ReplacedBy("ignoredMarkers")
  @Deprecated("renamed to ignoredMarkers", ReplaceWith("ignoredMarkers"))
  val nonPublicMarkers: SetProperty<String>

  /**
   * Fully qualified names of classes that are ignored by the API check.
   * Example of such a class could be `com.package.android.BuildConfig`.
   */
  val ignoredClasses: SetProperty<String>

  val outputApiDir: DirectoryProperty

  val projectName: Property<String>

  val kotlinxBinaryCompatibilityValidatorVersion: Property<String>

  val targets: NamedDomainObjectContainer<BCVTarget>
}
