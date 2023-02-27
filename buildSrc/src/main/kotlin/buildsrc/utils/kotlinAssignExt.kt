package org.gradle.kotlin.dsl

import org.gradle.api.provider.HasMultipleValues
import org.gradle.api.provider.Provider

//
//import java.io.File
//import org.gradle.api.file.ConfigurableFileCollection
//import org.gradle.api.file.FileCollection
//import org.gradle.api.file.FileSystemLocation
//import org.gradle.api.file.FileSystemLocationProperty
//import org.gradle.api.provider.HasMultipleValues
//import org.gradle.api.provider.MapProperty
//import org.gradle.api.provider.Property
//import org.gradle.api.provider.Provider
//
//
///**
// * Assign value: T to a property with assign operator
// *
// * @since 8.1
// */
//fun <T> Property<T>.assign(value: T?) {
//  this.set(value)
//}
//
//
///**
// * Assign value: Provider<T> to a property with assign operator
// *
// * @since 8.1
// */
//fun <T> Property<T>.assign(value: Provider<out T?>) {
//  this.set(value)
//}
//
//
///**
// * Assign file to a FileSystemLocationProperty with assign operator
// *
// * @since 8.1
// */
//fun <T : FileSystemLocation> FileSystemLocationProperty<T>.assign(file: File?) {
//  this.set(file)
//}
//
//
///**
// * Assign file provided by a Provider to a FileSystemLocationProperty with assign operator
// *
// * @since 8.1
// */
//fun <T : FileSystemLocation> FileSystemLocationProperty<T>.assign(provider: Provider<File?>) {
//  this.fileProvider(provider)
//}
//
//
///**
// * Sets the value of the property to the elements of the given iterable, and replaces any existing value
// *
// * @since 8.1
// */
//fun <T> HasMultipleValues<T>.assign(elements: Iterable<T?>?) {
//  this.set(elements)
//}
//
//

operator fun <T> HasMultipleValues<T>.plusAssign(elements: Provider<out Iterable<T?>?>) {
  this.addAll(elements)
}

operator fun <T> HasMultipleValues<T>.plusAssign(elements: Iterable<T?>) {
  this.addAll(elements)
}
//
//
///**
// * Sets the value of this property to the entries of the given Map, and replaces any existing value
// *
// * @since 8.1
// */
//fun <K, V> MapProperty<K, V>.assign(entries: Map<out K?, V?>?) {
//  this.set(entries)
//}
//
//
///**
// * Sets the property to have the same value of the given provider, and replaces any existing value
// *
// * @since 8.1
// */
//fun <K, V> MapProperty<K, V>.assign(provider: Provider<out Map<out K?, V?>?>) {
//  this.set(provider)
//}
//
//
///**
// * Sets the ConfigurableFileCollection to contain the source paths of passed collection.
// * This is the same as calling ConfigurableFileCollection.setFrom(fileCollection).
// *
// * @since 8.1
// */
//fun ConfigurableFileCollection.assign(fileCollection: FileCollection) {
//  this.setFrom(fileCollection)
//}
