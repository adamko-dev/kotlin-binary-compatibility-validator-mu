package dev.adamko.kotlin.binary_compatibility_validator.internal

import kotlin.RequiresOptIn.Level.WARNING
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*


@RequiresOptIn(
  "Internal API - may change at any time without notice",
  level = WARNING
)
@Retention(BINARY)
@Target(
  CLASS,
  FUNCTION,
  PROPERTY,
  CONSTRUCTOR,
)
@MustBeDocumented
internal annotation class BCVInternalApi
