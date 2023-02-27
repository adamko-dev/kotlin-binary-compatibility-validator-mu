package dev.adamko.kotlin.binary_compatibility_validator.targets

import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.tasks.Input

abstract class BCVPlatformType @Inject constructor(
  private val name: String
) : Serializable, Named {
  @Input
  override fun getName(): String = name
}
