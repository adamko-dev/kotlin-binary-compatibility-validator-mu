package dev.adamko.kotlin.binary_compatibility_validator.internal

import kotlin.annotation.AnnotationTarget.*


@RequiresOptIn(
  "Internal API - may change at any time without notice",
  level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(
  CLASS,
  FUNCTION,
  PROPERTY,
  CONSTRUCTOR,
)
internal annotation class BCVInternalApi
