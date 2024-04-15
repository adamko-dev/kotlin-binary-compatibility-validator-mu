package dev.adamko.kotlin.binary_compatibility_validator.targets

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Settings affecting KLib ABI validation.
 */
@BCVExperimentalApi
interface KLibValidationSpec {

  /** Enables KLib ABI validation checks. */
  @get:Input
  @get:Optional
  val enabled: Property<Boolean>

  /** Enables KLib ABI validation checks. */
  fun enable(): Unit = enabled.set(true)

  /**
   * Specifies which version of signature KLib ABI dump should contain.
   * By default, or when explicitly set to null, the latest supported version will be used.
   *
   * This option covers some advanced scenarios and does not require any configuration by default.
   *
   * A linker uses signatures to look up symbols, thus signature changes brake binary compatibility and
   * should be tracked. Signature format itself is not stabilized yet and may change in the future. In that case,
   * a new version of a signature will be introduced. Change of a signature version will be reflected in a dump
   * causing a validation failure even if declarations itself remained unchanged.
   * However, if a KLib supports multiple signature versions simultaneously, one my explicitly specify the version
   * that will be dumped to prevent changes in a dump file.
   */
  @get:Input
  @get:Optional
  val signatureVersion: Property<KLibSignatureVersion>

  /**
   * Fail validation if some build targets are not supported by the host compiler.
   * By default, ABI dumped only for supported files will be validated. This option makes validation behavior
   * stricter and treats having unsupported targets as an error.
   */
  @get:Input
  @get:Optional
  val strictValidation: Property<Boolean>
}
