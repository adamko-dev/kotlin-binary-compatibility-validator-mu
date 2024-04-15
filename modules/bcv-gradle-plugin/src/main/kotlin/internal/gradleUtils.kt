package dev.adamko.kotlin.binary_compatibility_validator.internal

import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion


/** The current Gradle version */
internal val CurrentGradleVersion: GradleVersion
  get() = GradleVersion.current()


/** @see GradleVersion.compareTo */
internal operator fun GradleVersion.compareTo(version: String): Int =
  compareTo(GradleVersion.version(version))


/**
 * Mark this [Configuration] as one that should be used to declare dependencies in
 * [Project.dependencies] block.
 *
 * Declarable Configurations should be extended by [resolvable] and [consumable] Configurations.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = false
 * isCanBeDeclared = true
 * ```
 */
internal fun Configuration.declarable(
  visible: Boolean = false,
) {
  isCanBeResolved = false
  isCanBeConsumed = false
  canBeDeclared = true
  isVisible = visible
}


/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.consumable(
  visible: Boolean = false,
) {
  isCanBeResolved = false
  isCanBeConsumed = true
  canBeDeclared = false
  isVisible = visible
}


/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.resolvable(
  visible: Boolean = false,
) {
  isCanBeResolved = true
  isCanBeConsumed = false
  canBeDeclared = false
  isVisible = visible
}


/**
 * Enable/disable [Configuration.isCanBeDeclared] only if it is supported by the
 * [CurrentGradleVersion]
 *
 * This function should be removed when the minimal supported Gradle version is 8.2.
 */
@Suppress("UnstableApiUsage")
private var Configuration.canBeDeclared: Boolean
  get() = isCanBeDeclaredSupported && isCanBeDeclared
  set(value) {
    if (isCanBeDeclaredSupported) isCanBeDeclared = value
  }

private val isCanBeDeclaredSupported = CurrentGradleVersion >= "8.2"

/**
 * Create a new [NamedDomainObjectContainer], using
 * [org.gradle.kotlin.dsl.domainObjectContainer]
 * (but [T] is `reified`).
 *
 * @param[factory] an optional factory for creating elements
 * @see org.gradle.kotlin.dsl.domainObjectContainer
 */
internal inline fun <reified T : Any> ObjectFactory.domainObjectContainer(
  factory: NamedDomainObjectFactory<T>? = null
): NamedDomainObjectContainer<T> =
  if (factory == null) {
    domainObjectContainer(T::class)
  } else {
    domainObjectContainer(T::class, factory)
  }


/**
 * Create a new [ExtensiblePolymorphicDomainObjectContainer], using
 * [org.gradle.kotlin.dsl.polymorphicDomainObjectContainer]
 * (but [T] is `reified`).
 *
 * @see org.gradle.kotlin.dsl.polymorphicDomainObjectContainer
 */
internal inline fun <reified T : Any> ObjectFactory.polymorphicDomainObjectContainer()
    : ExtensiblePolymorphicDomainObjectContainer<T> =
  polymorphicDomainObjectContainer(T::class)



/** Create a new [BCVTargetsContainer] instance. */
internal fun ObjectFactory.bcvTargetsContainer(): BCVTargetsContainer {
  val container = polymorphicDomainObjectContainer<BCVTarget>()
  container.whenObjectAdded {
    // workaround for https://github.com/gradle/gradle/issues/24972
    (container as ExtensionAware).extensions.add(name, this)
  }
  return container
}


typealias BCVTargetsContainer =
    ExtensiblePolymorphicDomainObjectContainer<BCVTarget>



/**
 * [Add][ExtensionContainer.add] a value (from [valueProvider]) with [name], and return the value.
 *
 * Adding an extension is especially useful for improving the DSL in build scripts when [T] is a
 * [NamedDomainObjectContainer].
 * Using an extension will allow Gradle to generate
 * [type-safe model accessors](https://docs.gradle.org/current/userguide/kotlin_dsl.html#kotdsl:accessor_applicability)
 * for added types.
 *
 * ([name] should match the property name. This has to be done manually because using a
 * delegated-property provider means Gradle can't introspect the types properly, so it fails to
 * create accessors).
 */
internal inline fun <reified T : Any> ExtensionContainer.adding(
  name: String,
  crossinline valueProvider: () -> T,
): T {
  val value: T = valueProvider()
  add<T>(name, value)
  return value
}
