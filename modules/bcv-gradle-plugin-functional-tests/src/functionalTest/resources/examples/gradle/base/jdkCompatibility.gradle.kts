plugins {
  kotlin("jvm") version "1.9.24"
  id("dev.adamko.kotlin.binary-compatibility-validator") version "+"
}

val minJvmTarget = org.jetbrains.kotlin.config.JvmTarget.supportedValues().minBy { it.majorVersion }
val maxJvmTarget = org.jetbrains.kotlin.config.JvmTarget.supportedValues().maxBy { it.majorVersion }

val useMaxJdkVersion = providers.gradleProperty("useMaxJdkVersion").orNull.toBoolean()
val selectedJvmTarget = (if (useMaxJdkVersion) maxJvmTarget else minJvmTarget).toString()

val toolchainVersion = selectedJvmTarget.split('.').last().toInt()

kotlin {
  jvmToolchain(toolchainVersion)

  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(selectedJvmTarget))
  }
}
