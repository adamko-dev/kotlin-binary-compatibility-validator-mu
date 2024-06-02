package dev.adamko.kotlin.binary_compatibility_validator.internal

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

// can't use kotlin.time.measureTime {} because Gradle forces the language level to be low.
internal fun measureTime(block: () -> Unit): Duration =
  System.nanoTime().let { startTime ->
    block()
    (System.nanoTime() - startTime).nanoseconds
  }
