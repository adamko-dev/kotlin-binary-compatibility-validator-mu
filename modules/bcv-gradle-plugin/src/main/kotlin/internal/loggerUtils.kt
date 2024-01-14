package dev.adamko.kotlin.binary_compatibility_validator.internal

import org.gradle.api.logging.Logger

/** Only evaluate and log [msg] when [Logger.isWarnEnabled] is `true`. */
internal fun Logger.warn(msg: () -> String) {
  if (isWarnEnabled) warn(msg())
}
