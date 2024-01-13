plugins {
  kotlin("jvm")
  id("dev.adamko.kotlin.binary-compatibility-validator") version "0.2.0-SNAPSHOT"
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
}
