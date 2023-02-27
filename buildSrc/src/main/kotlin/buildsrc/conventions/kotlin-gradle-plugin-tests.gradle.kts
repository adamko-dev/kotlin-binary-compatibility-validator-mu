package buildsrc.conventions

import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME

/**
 * Configure defaults for testing Gradle Plugins.
 */
plugins {
  id("buildsrc.conventions.java-base")
  id("org.gradle.kotlin.kotlin-dsl")
  `java-test-fixtures`
  `jvm-test-suite`
}

sourceSets {
  configureEach {
    java.setSrcDirs(emptyList<File>())
  }

  // remove the 'main' sourceSet - just to make it explicit that this is a test-only plugin
  matching { it.name == MAIN_SOURCE_SET_NAME }.configureEach {
    kotlin.setSrcDirs(emptyList<File>())
    resources.setSrcDirs(emptyList<File>())
  }
}


testing.suites {
  withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()

    dependencies {
      implementation(project.dependencies.testFixtures(project()))
    }
  }
}
