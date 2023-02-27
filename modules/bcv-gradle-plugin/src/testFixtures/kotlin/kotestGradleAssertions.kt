package dev.adamko.kotlin.binary_compatibility_validator.test.utils

import io.kotest.matchers.*
import org.gradle.api.NamedDomainObjectCollection


infix fun <T : Any> NamedDomainObjectCollection<out T>?.shouldContainDomainObject(
  name: String
): T {
  this should containDomainObject(name)
  return this?.getByName(name)!!
}

infix fun <T : Any> NamedDomainObjectCollection<out T>?.shouldNotContainDomainObject(
  name: String
): NamedDomainObjectCollection<out T>? {
  this shouldNot containDomainObject(name)
  return this
}

private fun <T> containDomainObject(name: String): Matcher<NamedDomainObjectCollection<T>?> =
  neverNullMatcher { value ->
    MatcherResult(
      name in value.names,
      { "NamedDomainObjectCollection(${value.names}) should contain DomainObject named '$name'" },
      { "NamedDomainObjectCollection(${value.names}) should not contain DomainObject named '$name'" })
  }
