package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.targets.KLibValidationSpec
import org.gradle.api.provider.Property

@OptIn(BCVExperimentalApi::class)
abstract class BCVKLibConventions : KLibValidationSpec {

  /**
   * Fail validation if some build targets are not supported by the host compiler.
   *
   * By default, ABI dumped only for supported files will be validated. This option makes
   * validation behavior stricter and treats having unsupported targets as an error.
   */
  abstract val strictValidation: Property<Boolean>
}
