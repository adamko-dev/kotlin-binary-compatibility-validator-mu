package dev.adamko.kotlin.binary_compatibility_validator.targets

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import java.io.Serializable
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

@OptIn(BCVExperimentalApi::class)
abstract class BCVKLibTarget
@BCVInternalApi
constructor(
  @get:Internal
  val targetName: String,
) : BCVTarget(targetName), KLibValidationSpec, Named, Serializable {

  @get:Classpath
  abstract val klibFile: ConfigurableFileCollection

  @get:Classpath
  abstract val compilationDependencies: ConfigurableFileCollection

  @get:Input
  abstract val currentPlatform: Property<String> // for up-to-date checks?

  @get:Input
  abstract val supportedByCurrentHost: Property<Boolean>
  @get:Input
  abstract val hasKotlinSources: Property<Boolean>

//  @get:Internal // TODO
//  abstract val inputAbiFile: RegularFileProperty
//  @get:Internal // TODO
//  abstract val outputAbiFile: RegularFileProperty
//  abstract val supportedTargets: SetProperty<String>

//  @Internal
//  override fun getName(): String = targetName
}
