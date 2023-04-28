import buildsrc.utils.excludeGeneratedGradleDsl

plugins {
  buildsrc.conventions.base
  idea
}

group = "dev.adamko.kotlin.binary_compatibility_validator"
version = "0.0.6-SNAPSHOT"

idea {
  module {
    excludeGeneratedGradleDsl(layout)
    excludeDirs = excludeDirs + layout.files(
      ".idea",
      "gradle/wrapper",
    )
  }
}

val readmeCheck by tasks.registering {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  val readme = providers.fileContents(layout.projectDirectory.file("README.md")).asText
  val supportedGradleVersion = libs.versions.supportedGradleVersion
  val kotlinBcvVersion = libs.versions.kotlinx.bcv

  doLast {
    readme.get().let { readme ->
      require("BCV version `${kotlinBcvVersion.get()}`" in readme) {
        "Incorrect BCV version in README"
      }
      require("kotlinxBinaryCompatibilityValidatorVersion.set(\"${kotlinBcvVersion.get()}\")" in readme) {
        "Incorrect BCV version in README"
      }
      require("The minimal supported Gradle version is ${supportedGradleVersion.get()}" in readme) {
        "Incorrect Gradle version in README"
      }
    }
  }
}

tasks.check {
  dependsOn(readmeCheck)
}
