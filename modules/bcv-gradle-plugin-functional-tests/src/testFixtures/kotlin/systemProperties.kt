package dev.adamko.kotlin.binary_compatibility_validator.test.utils

import kotlin.properties.ReadOnlyProperty


internal fun <T> systemProperty(
  convert: (String) -> T
): ReadOnlyProperty<Any, T> =
  ReadOnlyProperty { _, property ->
    val value = requireNotNull(System.getProperty(property.name)) {
      "system property ${property.name} is unavailable"
    }
    convert(value)
  }
