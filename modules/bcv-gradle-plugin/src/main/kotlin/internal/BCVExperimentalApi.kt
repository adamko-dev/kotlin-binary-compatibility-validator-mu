package dev.adamko.kotlin.binary_compatibility_validator.internal

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*

/**
 * Marks an API that is still experimental in BCV and may change in the future.
 * There are no guarantees on preserving the behavior of the API until its stabilization.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(BINARY)
@Target(
  CLASS,
  FUNCTION,
  PROPERTY,
  CONSTRUCTOR,
)
@MustBeDocumented
annotation class BCVExperimentalApi
