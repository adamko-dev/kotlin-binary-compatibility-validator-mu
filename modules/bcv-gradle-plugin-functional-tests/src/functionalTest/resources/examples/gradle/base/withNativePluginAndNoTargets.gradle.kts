plugins {
  kotlin("multiplatform") version "1.9.24"
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
