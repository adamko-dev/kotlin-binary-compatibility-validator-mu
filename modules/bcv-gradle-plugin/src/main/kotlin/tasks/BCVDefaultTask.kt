package dev.adamko.kotlin.binary_compatibility_validator.tasks

import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input

@CacheableTask
abstract class BCVDefaultTask @BCVInternalApi constructor() : DefaultTask() {

  @get:Input
  abstract val bcvEnabled: Property<Boolean>

  init {
    group = BCVPlugin.TASK_GROUP
  }
}
