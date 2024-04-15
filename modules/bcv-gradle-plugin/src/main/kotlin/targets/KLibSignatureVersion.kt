package dev.adamko.kotlin.binary_compatibility_validator.targets

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import java.io.Serializable

@BCVExperimentalApi
sealed interface KLibSignatureVersion {

  val version: Int

  fun isLatest(): Boolean = version == Latest.version

  @BCVExperimentalApi
  companion object {
    fun of(value: Int): KLibSignatureVersion {
      require(value >= 1) {
        "Invalid version value, expected positive value: $value"
      }
      return KLibSignatureVersionImpl(value)
    }

    val Latest: KLibSignatureVersion = KLibSignatureVersionImpl(Int.MIN_VALUE)
  }
}


@OptIn(BCVExperimentalApi::class)
private data class KLibSignatureVersionImpl(
  override val version: Int,
) : Serializable, KLibSignatureVersion {

  override fun toString(): String {
    val version = if (isLatest()) "Latest" else "$version"
    return "KLibSignatureVersion($version)"
  }
}
