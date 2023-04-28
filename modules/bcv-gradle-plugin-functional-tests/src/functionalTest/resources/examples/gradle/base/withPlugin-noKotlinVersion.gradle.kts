plugins {
  kotlin("jvm")
  id("dev.adamko.kotlin.binary-compatibility-validator") version "0.0.5"
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
}
