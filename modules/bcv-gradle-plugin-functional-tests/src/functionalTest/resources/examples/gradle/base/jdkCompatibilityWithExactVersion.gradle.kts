plugins {
  kotlin("jvm") version "1.9.22"
  id("dev.adamko.kotlin.binary-compatibility-validator") version "+"
}

val selectedJvmTarget = providers.gradleProperty("jvmTarget").get()
val toolchainVersion = selectedJvmTarget.split('.').last().toInt()

kotlin {
    jvmToolchain(toolchainVersion)

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(selectedJvmTarget))
    }
}
