plugins {
  kotlin("multiplatform") version embeddedKotlinVersion
  id("dev.adamko.kotlin.binary-compatibility-validator") version "+"
}

kotlin {
  linuxX64()
  linuxArm64()
  mingwX64()
  androidNativeArm32()
  androidNativeArm64()
  androidNativeX64()
  androidNativeX86()

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
