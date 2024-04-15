plugins {
  kotlin("multiplatform") version embeddedKotlinVersion
  id("dev.adamko.kotlin.binary-compatibility-validator") version "+"
}

kotlin {
  sourceSets {
    commonMain {}
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
