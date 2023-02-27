package buildsrc.conventions

import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  base
//  id("org.jetbrains.kotlin.plugin.assignment")
}

// common config for all projects

if (project != rootProject) {
  project.version = rootProject.version
  project.group = rootProject.group
}

tasks.withType<AbstractArchiveTask>().configureEach {
  // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

tasks.withType<AbstractTestTask>().configureEach {
  timeout = Duration.ofMinutes(60)

  testLogging {
    showCauses = true
    showExceptions = true
    showStackTraces = true
    showStandardStreams = true
    events(
      PASSED,
      FAILED,
      SKIPPED,
      STARTED,
      STANDARD_ERROR,
      STANDARD_OUT,
    )
  }
}

tasks.withType<AbstractCopyTask>().configureEach {
  includeEmptyDirs = false
}
