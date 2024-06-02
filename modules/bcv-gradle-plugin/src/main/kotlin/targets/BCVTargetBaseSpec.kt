package dev.adamko.kotlin.binary_compatibility_validator.targets

import java.io.Serializable
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface BCVTargetBaseSpec : Serializable {

  /** Enables or disables API generation and validation for this target */
  val enabled: Property<Boolean>

  /**
   * Fully qualified names of annotations that can be used to explicitly mark public declarations.
   *
   * If at least one of [publicMarkers], [publicPackages] or [publicClasses] is defined,
   * all declarations not covered by any of them will be considered non-public.
   * [ignoredPackages], [ignoredClasses] and [ignoredMarkers] can be used for additional filtering.
   */
  val publicMarkers: SetProperty<String>

  /**
   * Fully qualified package names that contain public declarations.
   *
   * If at least one of [publicMarkers], [publicPackages] or [publicClasses] is defined,
   * all declarations not covered by any of them will be considered non-public.
   *
   * [ignoredPackages], [ignoredClasses] and [ignoredMarkers] can be used for additional filtering.
   */
  val publicPackages: SetProperty<String>

  /**
   * Fully qualified names of public classes.
   *
   * If at least one of [publicMarkers], [publicPackages] or [publicClasses] is defined,
   * all declarations not covered by any of them will be considered non-public.
   *
   * [ignoredPackages], [ignoredClasses] and [ignoredMarkers] can be used for additional filtering.
   */
  val publicClasses: SetProperty<String>

  /**
   * Fully qualified names of annotations that effectively exclude declarations from being public.
   * Example of such annotation could be `kotlinx.coroutines.InternalCoroutinesApi`.
   */
  val ignoredMarkers: SetProperty<String>

  /**
   * Fully qualified package names that are not consider public API.
   * For example, it could be `kotlinx.coroutines.internal` or `kotlinx.serialization.implementation`.
   */
  val ignoredPackages: SetProperty<String>

  /**
   * Fully qualified names of classes that are ignored by the API check.
   * Example of such a class could be `com.package.android.BuildConfig`.
   */
  val ignoredClasses: SetProperty<String>
}
