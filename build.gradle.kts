import buildsrc.utils.generatedKotlinDslAccessorDirs

plugins {
  buildsrc.conventions.base
  idea
}

group = "dev.adamko.kotlin.binary_compatibility_validator"
project.version = object {
  private val gitVersion = project.gitVersion
  override fun toString(): String = gitVersion.get()
}

idea {
  module {
    excludeDirs = excludeDirs +
        layout.generatedKotlinDslAccessorDirs() +
        layout.files(
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

val projectVersion by tasks.registering {
  description = "prints the project version"
  group = "help"
  val version = providers.provider { project.version }
  inputs.property("version", version)
  outputs.cacheIf("logging task, it should always run") { false }
  doLast {
    logger.quiet("${version.orNull}")
  }
}
