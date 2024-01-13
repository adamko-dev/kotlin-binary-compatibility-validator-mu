package dev.adamko.kotlin.binary_compatibility_validator.internal

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*


internal val Project.sourceSets: SourceSetContainer
  get() = extensions.getByType()
