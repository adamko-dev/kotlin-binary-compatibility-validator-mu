plugins {
  kotlin("multiplatform") version "1.9.24"
  id("dev.adamko.kotlin.binary-compatibility-validator") version "+"
}

kotlin {
  linuxArm64()

  sourceSets {
    commonTest {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }
}

binaryCompatibilityValidator {
  klib.enable()
}
