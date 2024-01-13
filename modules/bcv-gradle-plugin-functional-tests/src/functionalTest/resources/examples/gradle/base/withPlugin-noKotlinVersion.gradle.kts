plugins {
  kotlin("jvm")
  id("dev.adamko.kotlin.binary-compatibility-validator") version "+"
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
}
