# Kotlin Binary Compatibility Validator (Mirror Universe)

BCV-MU is a re-imagined [Gradle](https://gradle.org/) Plugin for
[Kotlin/binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator).

This plugin validates the public JVM binary API of libraries to make sure that breaking changes are
tracked.

(The [Mirror Universe](https://en.wikipedia.org/wiki/Mirror_Universe) tag was chosen because I hope
to banish this plugin as soon the improvements here are merged upstream.)

### Description

Under-the-hood BCV-MU still uses the signature-generation code from BCV, but the Gradle plugin has
been refactored to better follow the Gradle API, and generate API signatures more flexibly.

### Quick start

BCV-MU plugin can either be [applied as to a Project](#build-plugin) in any `build.gradle(.kts)`,
or (**experimentally**) [as a Settings plugin](#settings-plugin) in `settings.gradle(.kts)`.

#### Requirements

The minimal supported Gradle version is 7.6.

By default, BCV-MU uses BCV version `0.13.0`, which can be overridden, but may introduce runtime
errors.

#### Build plugin

BCV-MU can be applied to each subproject.

```kotlin
// build.gradle.kts

plugins {
  id("dev.adamko.kotlin.binary-compatibility-validator") version "$bcvMuVersion"
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
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
  }
}

plugins {
  id("dev.adamko.kotlin.binary-compatibility-validator") version "$bcvMuVersion"
}

extensions
  .getByType<dev.adamko.kotlin.binary_compatibility_validator.BCVSettingsPlugin.Extension>()
  .apply {
    ignoredProjects.addAll(

      // ignore subprojects explicitly
      ":some-subproject",

      // or ignore using a glob pattern
      ":internal-dependencies:**",
      ":*-tasks:**",
    )
  }
```

## License

The code in this project is based on the code in the
[Kotlin/binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator)
project.
