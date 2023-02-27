package dev.adamko.kotlin.binary_compatibility_validator.internal


@RequiresOptIn(
  "Internal API - may change at any time without notice",
  level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class BCVInternalApi
