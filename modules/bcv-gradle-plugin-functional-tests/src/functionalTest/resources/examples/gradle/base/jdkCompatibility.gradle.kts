import org.jetbrains.kotlin.config.JvmTarget

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

repositories {
    mavenCentral()
}

val minTarget = JvmTarget.supportedValues().minBy { it.majorVersion }
val maxTarget = JvmTarget.supportedValues().maxBy { it.majorVersion }

val useMax = (project.properties["useMaxVersion"]?.toString() ?: "false").toBoolean()
val target = (if (useMax) maxTarget else minTarget).toString()

val toolchainVersion = target.split('.').last().toInt()

kotlin {
    jvmToolchain(toolchainVersion)
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(target))
    }
}
