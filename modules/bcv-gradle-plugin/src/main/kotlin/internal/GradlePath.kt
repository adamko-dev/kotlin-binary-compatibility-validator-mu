package dev.adamko.kotlin.binary_compatibility_validator.internal

import org.gradle.api.Project


internal val Project.isRootProject get() = this == rootProject

internal val Project.fullPath: String
  get() = when {
    isRootProject -> path + rootProject.name
    else          -> name
  }
