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

tasks.withType<Test>().configureEach {
  // Help speed up TestKit tests by re-using dependencies cache
  // https://docs.gradle.org/current/userguide/dependency_resolution.html#sub:shared-readonly-cache
  environment("GRADLE_RO_DEP_CACHE", gradle.gradleUserHomeDir.resolve("caches"))
  providers.environmentVariable("GRADLE_TESTKIT_DIR").orNull?.let {
    systemProperty("gradleTestKitDir", it)
  }
}
