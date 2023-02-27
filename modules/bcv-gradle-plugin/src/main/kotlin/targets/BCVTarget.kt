package dev.adamko.kotlin.binary_compatibility_validator.targets

import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

abstract class BCVTarget @Inject constructor(
  private val targetName: String
) : Serializable, Named {

  @get:Input
  @get:Optional
  abstract val enabled: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val platformType: Property<BCVPlatformType>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputClasses: ConfigurableFileCollection

  @Input
  override fun getName() = targetName
}
