[![GitHub license](https://img.shields.io/github/license/adamko-dev/kotlin-binary-compatibility-validator-mu?style=for-the-badge)](https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu/blob/main/LICENSE)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.adamko.kotlin.binary-compatibility-validator?style=for-the-badge)](https://plugins.gradle.org/plugin/dev.adamko.kotlin.binary-compatibility-validator)

# Kotlin Binary Compatibility Validator (Mirror Universe)

[BCV-MU](https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu) is a
re-imagined [Gradle](https://gradle.org/) Plugin for
[Kotlin/binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator).

This plugin validates the public JVM binary API of libraries to make sure that breaking changes are
tracked.

Read more in the BCV project:

* [What constitutes the public API?](https://github.com/Kotlin/binary-compatibility-validator/#what-constitutes-the-public-api)
* [What makes an incompatible change to the public binary API?](https://github.com/Kotlin/binary-compatibility-validator/#what-makes-an-incompatible-change-to-the-public-binary-api)

(The [Mirror Universe](https://en.wikipedia.org/wiki/Mirror_Universe) tag was chosen because I hope
to banish this plugin as soon the improvements here are merged upstream.)

### Description

Under-the-hood BCV-MU still uses the signature-generation code from BCV, but the Gradle plugin has
been refactored to better follow the Gradle API, and generate API signatures more flexibly.

BCV-MU plugin can either be [applied as to a Project](#build-plugin) in any `build.gradle(.kts)`,
or (**experimentally**) [as a Settings plugin](#settings-plugin) in `settings.gradle(.kts)`.

#### Requirements

The minimal supported Gradle version is 7.6.

By default, BCV-MU uses BCV version `0.13.0`, which can be overridden, but may introduce runtime
errors.

### Build plugin

BCV-MU can be applied to each subproject as a standard Gradle plugin.

```kotlin
// build.gradle.kts

plugins {
  id("dev.adamko.kotlin.binary-compatibility-validator") version "$bcvMuVersion"
}
```

To initialise the API declarations, run the Gradle task

```shell
./gradlew apiDump
```

This will produce API files into the `./api` directory of subprojects with the BCV-MU plugin.
These API declarations files must be committed to the repository.

To verify that the API declarations is up-to-date, run the Gradle task

```shell
./gradlew apiCheck
```

The `apiCheck` task will also be run whenever the `check` task is run.

##### Configuration

BCV-MU can be configured in a similar manner to BCV:

```kotlin
// build.gradle.kts

plugins {
  kotlin("jvm") version "1.8.10"
  id("dev.adamko.kotlin.binary-compatibility-validator") version "$bcvMuVersion"
}

binaryCompatibilityValidator {
  // Packages that are excluded from public API dumps even if they contain public API.
  ignoredPackages.add("kotlinx.coroutines.internal")
  // Classes (fully qualified) that are excluded from public API dumps even if they contain public API.
  ignoredClasses.add("com.company.BuildConfig")
  // Set of annotations that exclude API from being public.
  // Typically, it is all kinds of `@InternalApi` annotations that mark
  // effectively private API that cannot be actually private for technical reasons.
  ignoredMarkers.add("my.package.MyInternalApiAnnotation")

  // Disable or enable all BCV-MU tasks for this project
  bcvEnabled.set(true)

  // Override the default BCV version
  kotlinxBinaryCompatibilityValidatorVersion.set("0.13.0")
}
```

##### Advanced configuration

BCV automatically generates 'targets' for each Kotlin/JVM target that it finds.
These targets can be specifically modified, or manually defined, for fine-grained control.

```kotlin
// build.gradle.kts

plugins {
  kotlin("jvm") version "1.8.10"
  id("dev.adamko.kotlin.binary-compatibility-validator") version "$bcvMuVersion"
  `java-test-fixtures`
}

binaryCompatibilityValidator {
  // these are the default values that will be used in all Targets
  ignoredMarkers.add("my.package.MyFirstInternalApiAnnotation")
  ignoredClasses.add("com.company.BuildConfig")

  // BCV will automatically generate targets for each Kotlin/JVM target,
  // and each can be configured manually for fine-grained control. 
  targets.configureEach {
    // values can be appended to the default values
    ignoredMarkers.add("my.package.MySecondInternalApiAnnotation")
    // Or overridden
    ignoredClasses.set(listOf())
  }

  // BCV will automatically register a target for testFixtures, but it must be enabled manually
  targets.named("testFixtures") {
    enabled.set(true)
  }

  // BCV Targets can also be manually defined
  targets.register("customTarget") {
    inputJar.set(tasks.customTargetJar.flatMap { it.archiveFile })
  }
}
```

##### Shared configuration with a convention plugin

To share common configuration it is best to set up a convention-plugin.

If you don't have any convention plugins already, then here's a quick guide.

First, set-up
[buildSrc](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources)
by creating a file `./buildSrc/build.gradle.kts`

In it, add the
[`kotlin-dsl` plugin](https://docs.gradle.org/current/userguide/kotlin_dsl.html#sec:kotlin-dsl_plugin),
and add the BCV-MU plugin as a dependency.

```kotlin
// ./buildSrc/build.gradle.kts

plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  // add the *Maven coordinates* of the bcv-MU plugin, not the plugin ID, as a dependency
  implementation("dev.adamko.kotlin.binary-compatibility-validator:bcv-gradle-plugin:$bcvMuVersion")
}
```

Next, create a convention plugin. This is where any shared configuration can be defined.

```kotlin
// buildSrc/src/main/kotlin/binary-compatibility-validator-convention.gradle.kts

plugins {
  id("dev.adamko.kotlin.binary-compatibility-validator") // no version needed - it's defined in buildSrc/build.gradle.kts
}

binaryCompatibilityValidator {
  ignoredClasses.add("com.company.BuildConfig")
}
```

Finally, apply the convention plugin to subprojects. This will automatically re-use the same
configuration.

```kotlin
// ./some/subproject/build.gradle.kts
plugins {
  id("binary-compatibility-validator-convention")
  kotlin("jvm")
}
```

#### Settings plugin

There is experimental support for applying BCV-MU as a Settings plugin.

This allows for applying BCV-MU to all subprojects in a Gradle-friendly way.
All subprojects are included by default, and can be excluded using BCV-MU config.

```kotlin
// settings.gradle.kts

buildscript {
  dependencies {
    // BCV-MU requires the Kotlin Gradle Plugin classes are present
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.8.10")
  }
}

plugins {
  id("dev.adamko.kotlin.binary-compatibility-validator") version "$bcvMuVersion"
}

binaryCompatibilityValidator {
  ignoredProjects.addAll(

    ":",                // ignore root project
    ":some-subproject", // ignore subprojects explicitly

    // or ignore using a glob pattern
    ":internal-dependencies:*",
    ":*-tasks:**",
  )

  // set the default values for all targets in all enabled-subprojects 
  defaultTargetValues {
    enabled.convention(true)
    ignoredClasses.set(listOf("com.package.MyIgnoredClass"))
    ignoredMarkers.set(listOf("com.package.MyInternalApiAnnotationMarker"))
    ignoredPackages.set(listOf("com.package.my_ignored_package"))
  }
}

include(
  // these projects will have BCV-MU automatically applied
  ":common",
  ":internal-dependencies:alpha:nested",
  ":internal-dependencies:alpha:nested",
  ":a-task",

  // this subproject is explicitly excluded from BCV
  ":some-subproject",

  // these subprojects will be excluded by glob pattern
  ":internal-dependencies",
  ":internal-dependencies:alpha",
  ":internal-dependencies:beta",
  ":x-tasks",
  ":x-tasks:sub1",
  ":x-tasks:sub1:sub2",
  ":z-tasks",
  ":z-tasks:sub1",
  ":z-tasks:sub1:sub2",
)
```

## License

The code in this project is based on the code in the
[Kotlin/binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator)
project.
