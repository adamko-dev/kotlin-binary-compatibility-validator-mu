package dev.adamko.kotlin.binary_compatibility_validator.targets

import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*

abstract class BCVTarget
@Inject
constructor(
  /**
   * The JVM platform being targeted.
   *
   * Targets with the same [platformType] will be grouped together into a single API declaration.
   */
  @get:Input
  val platformType: String
) : BCVTargetSpec, Serializable, Named {

  @get:Input
  @get:Optional
  abstract override val enabled: Property<Boolean>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract override val inputClasses: ConfigurableFileCollection

  @get:InputFile
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract override val inputJar: RegularFileProperty

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

  @Internal
  override fun getName(): String = platformType
}
