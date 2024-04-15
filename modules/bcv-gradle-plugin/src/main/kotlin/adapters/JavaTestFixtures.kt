package dev.adamko.kotlin.binary_compatibility_validator.adapters

import dev.adamko.kotlin.binary_compatibility_validator.BCVProjectExtension
import dev.adamko.kotlin.binary_compatibility_validator.internal.sourceSets
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget
import org.gradle.api.Project
import org.gradle.internal.component.external.model.TestFixturesSupport.TEST_FIXTURE_SOURCESET_NAME
import org.gradle.kotlin.dsl.*


internal fun createJavaTestFixtureTargets(
  project: Project,
  extension: BCVProjectExtension,
) {
  project.pluginManager.withPlugin("java-test-fixtures") {
    extension.targets.register<BCVJvmTarget>(TEST_FIXTURE_SOURCESET_NAME) {
      // don't enable by default - requiring an API spec for test-fixtures is a little unusual, and might be surprising
      enabled.convention(false)
      project
        .sourceSets
        .matching { it.name == TEST_FIXTURE_SOURCESET_NAME }
        .all {
          inputClasses.from(output.classesDirs)
        }
    }
  }
}
