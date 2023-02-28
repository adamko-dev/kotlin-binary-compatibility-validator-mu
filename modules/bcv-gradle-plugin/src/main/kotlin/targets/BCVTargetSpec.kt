package dev.adamko.kotlin.binary_compatibility_validator.targets

import java.io.Serializable
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface BCVTargetSpec : Serializable {

  /** Enables or disables API generation and validation for this target */
  val enabled: Property<Boolean>

  /**
   * The classes to generate signatures for.
   *
   * Note that if [inputJar] has a value, the contents of [inputClasses] will be ignored
   */
  val inputClasses: ConfigurableFileCollection

  /**
   * A JAR that contains the classes to generate signatures for.
   *
   * Note that if [inputJar] has a value, the contents of [inputClasses] will be ignored
   */
  val inputJar: RegularFileProperty

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
