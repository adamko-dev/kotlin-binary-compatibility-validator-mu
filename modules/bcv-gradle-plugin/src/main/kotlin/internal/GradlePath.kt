package dev.adamko.kotlin.binary_compatibility_validator.internal

import org.gradle.api.Project

typealias GradlePath = org.gradle.util.Path

fun GradlePath(path: String): GradlePath = GradlePath.path(path)

internal val Project.isRootProject get() = this == rootProject

internal val Project.fullPath: String
  get() = when {
    isRootProject -> path + rootProject.name
    else          -> name
  }
