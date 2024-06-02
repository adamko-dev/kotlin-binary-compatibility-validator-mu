package dev.adamko.kotlin.binary_compatibility_validator.targets

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

sealed class BCVTarget
@BCVInternalApi
@Inject
constructor(
  private val named: String,
) : BCVTargetBaseSpec, Serializable, Named, ExtensionAware, Comparable<BCVTarget> {

  @get:Input
  @get:Optional
  abstract override val enabled: Property<Boolean>

//  @get:InputFiles
//  @get:PathSensitive(RELATIVE)
//  abstract override val inputClasses: ConfigurableFileCollection
//
//  @get:InputFile
//  @get:Optional
//  @get:PathSensitive(RELATIVE)
//  abstract override val inputJar: RegularFileProperty

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget.publicMarkers */
  @get:Input
  @get:Optional
  abstract override val publicMarkers: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget.publicPackages */
  @get:Input
  @get:Optional
  abstract override val publicPackages: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget.publicClasses */
  @get:Input
  @get:Optional
  abstract override val publicClasses: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget.ignoredMarkers */
  @get:Input
  @get:Optional
  abstract override val ignoredMarkers: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget.ignoredPackages */
  @get:Input
  @get:Optional
  abstract override val ignoredPackages: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget.ignoredClasses */
  @get:Input
  @get:Optional
  abstract override val ignoredClasses: SetProperty<String>

  override fun compareTo(other: BCVTarget): Int =
    this.string().compareTo(other.string())

  @Input
  override fun getName(): String = named

  companion object {
    private fun BCVTarget.string(): String = when (this) {
      is BCVJvmTarget  -> "BCVJvmTarget(${named})"
      is BCVKLibTarget -> "BCVKLibTarget(${named})"
    }
  }
}
