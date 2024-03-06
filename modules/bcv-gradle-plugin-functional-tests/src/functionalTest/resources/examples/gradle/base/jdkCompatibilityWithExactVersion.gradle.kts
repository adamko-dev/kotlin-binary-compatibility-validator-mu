import org.jetbrains.kotlin.config.JvmTarget

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

repositories {
    mavenCentral()
}

val target = project.properties["jdkVersion"]!!.toString()
val toolchainVersion = target.split('.').last().toInt()

kotlin {
    jvmToolchain(toolchainVersion)
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(target))
    }
}
