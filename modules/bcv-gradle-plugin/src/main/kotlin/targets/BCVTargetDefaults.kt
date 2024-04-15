package dev.adamko.kotlin.binary_compatibility_validator.targets

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.adding
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.*

abstract class BCVTargetDefaults
@BCVInternalApi
@Inject
constructor(
  private val objects: ObjectFactory,
) : BCVTargetBaseSpec, Serializable, ExtensionAware {

  abstract override val enabled: Property<Boolean>

//  abstract override val inputClasses: ConfigurableFileCollection

//  abstract override val inputJar: RegularFileProperty

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetDefaults.publicMarkers */
  abstract override val publicMarkers: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetDefaults.publicPackages */
  abstract override val publicPackages: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetDefaults.publicClasses */
  abstract override val publicClasses: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetDefaults.ignoredMarkers */
  abstract override val ignoredMarkers: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetDefaults.ignoredPackages */
  abstract override val ignoredPackages: SetProperty<String>

  /** @see dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTargetDefaults.ignoredClasses */
  abstract override val ignoredClasses: SetProperty<String>

  @BCVExperimentalApi
  val klib: KLibValidationSpec =
    extensions.adding("klib") {
      objects.newInstance(KLibValidationSpec::class)
    }
}
