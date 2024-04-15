package dev.adamko.kotlin.binary_compatibility_validator.internal

import org.gradle.api.Project

internal typealias GradlePath = org.gradle.util.Path

internal fun GradlePath(path: String): GradlePath = GradlePath.path(path)

internal val Project.isRootProject: Boolean
  get() = this == rootProject

internal val Project.fullPath: String
  get() = when {
    isRootProject -> path + rootProject.name
    else          -> name
  }
