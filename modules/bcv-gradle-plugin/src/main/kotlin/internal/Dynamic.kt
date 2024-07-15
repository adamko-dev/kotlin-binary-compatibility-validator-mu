package dev.adamko.kotlin.binary_compatibility_validator.internal

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection


/**
 * Wrap a [target] instance, allowing dynamic calls.
 */
internal class Dynamic<out T : Any> private constructor(
  private val cls: KClass<T>,
  private val target: Any,
) : InvocationHandler, ReadOnlyProperty<Any?, T> {

  private val targetName: String = target.javaClass.name
  private val targetMethods: List<Method> = target.javaClass.methods.asList()

  private val proxy: T by lazy {
    val proxy = Proxy.newProxyInstance(
      target.javaClass.classLoader,
      arrayOf(cls.java),
      this,
    )
    @Suppress("UNCHECKED_CAST")
    proxy as T
  }

  override fun invoke(
    proxy: Any,
    method: Method,
    args: Array<out Any?>?
  ): Any? {
    for (delegateMethod in targetMethods) {
      if (method matches delegateMethod) {
        return if (args == null)
          delegateMethod.invoke(target)
        else
          delegateMethod.invoke(target, *args)
      }
    }
    throw UnsupportedOperationException("$targetName : $method args:[${args?.joinToString()}]")
  }

  /** Delegated value provider */
  override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = proxy


  companion object {
    private infix fun Method.matches(other: Method): Boolean =
      this.name == other.name && this.parameterTypes.contentEquals(other.parameterTypes)

    internal inline fun <reified T : Any> Dynamic(target: Any): Dynamic<T> =
      Dynamic(T::class, target)
  }
}


//private class A {
//  val name: String = "Team A"
//  fun shout() = println("go team A!")
//  fun echo(input: String) = input.repeat(5)
//}
//
//private class B {
//  val name: String = "Team B"
//  fun shout() = println("go team B!")
//  fun echo(call: String) = call.repeat(2)
//}
//
//private interface Shoutable {
//  val name: String
//  fun shout()
//  fun echo(call: String): String
//}
//
//
//private fun main() {
//  val a = A()
//  val b = B()
//
//  val sa by DuckTyper<Shoutable>(a)
//  val sb: Shoutable? by DuckTyper(b)
//
//  sa?.shout()
//  sb?.shout()
//  println(sa?.echo("hello..."))
//  println(sb?.echo("hello..."))
//  println(sa?.name)
//  println(sb?.name)
//}

/** Wrap [org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer] */
internal interface KotlinTargetsContainerWrapped {
  val targets: NamedDomainObjectCollection<Any>
}

/** Wrap [org.jetbrains.kotlin.gradle.plugin.KotlinTarget] */
internal interface KotlinTargetWrapped {
  val platformType: Any
  val targetName: String
  val compilations: NamedDomainObjectContainer<Named>
}

/** Wrap [org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType] */
internal interface KotlinPlatformTypeWrapped : Named

/** Wrap [org.jetbrains.kotlin.gradle.plugin.KotlinCompilation] */
internal interface KotlinCompilationWrapped {
  val output: Any
}

/** Wrap [org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput] */
internal interface KotlinCompilationOutputWrapped {
  val classesDirs: ConfigurableFileCollection
}
