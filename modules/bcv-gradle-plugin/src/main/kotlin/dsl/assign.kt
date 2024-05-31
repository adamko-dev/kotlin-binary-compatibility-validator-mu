@file:Suppress("PackageDirectoryMismatch")

package org.gradle.kotlin.dsl

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.targets.KLibSignatureVersion
import org.gradle.api.provider.Property


@OptIn(BCVExperimentalApi::class)
// TODO test Property<KLibSignatureVersion>.assign
fun Property<KLibSignatureVersion>.assign(version: Int) {
  this.set(KLibSignatureVersion.of(version))
}
//
//@OptIn(BCVExperimentalApi::class)
//fun KLibSignatureVersion(version: Int): KLibSignatureVersion =
//  KLibSignatureVersion.of(version)
