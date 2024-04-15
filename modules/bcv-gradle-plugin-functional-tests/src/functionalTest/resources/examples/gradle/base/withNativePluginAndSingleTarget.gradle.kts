plugins {
  kotlin("multiplatform") version embeddedKotlinVersion
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
