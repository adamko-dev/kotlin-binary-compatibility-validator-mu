plugins {
  kotlin("jvm")
  id("dev.adamko.kotlin.binary-compatibility-validator") version "0.0.6-SNAPSHOT"
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
}
