import buildsrc.utils.excludeProjectConfigurationDirs
import buildsrc.utils.filterContains

plugins {
  buildsrc.conventions.base
  idea
}

group = "dev.adamko.kotlin.binary-compatibility-validator"
project.version = object {
  private val gitVersion = project.gitVersion
  override fun toString(): String = gitVersion.get()
}

idea {
  module {
    excludeProjectConfigurationDirs(layout, providers)
  }
}


val readmeCheck by tasks.registering {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  val readme = providers.fileContents(layout.projectDirectory.file("README.md")).asText
  val supportedGradleVersion = libs.versions.supportedGradleVersion
  val kotlinBcvVersion = libs.versions.kotlinx.bcv
  val kgpVersion = embeddedKotlinVersion

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
      readme.lines()
        .filterContains("kotlin(\"jvm\") version ")
        .forEach { line ->
          require("kotlin(\"jvm\") version \"$kgpVersion\"" in line) {
            "Incorrect Kotlin JVM plugin (expected $kgpVersion) version in README\n  $line"
          }
        }
      readme.lines()
        .filterContains("""org.jetbrains.kotlin:kotlin-gradle-plugin-api:""")
        .forEach { line ->
          require("""classpath("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kgpVersion")""" in line) {
            "Incorrect kotlin-gradle-plugin-api version (expected $kgpVersion) version in README\n  $line"
          }
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
  val version = providers.provider { project.version.toString() }
  inputs.property("version", version)
  outputs.cacheIf("logging task, it should always run") { false }
  doLast {
    logger.quiet("${version.orNull}")
  }
}
