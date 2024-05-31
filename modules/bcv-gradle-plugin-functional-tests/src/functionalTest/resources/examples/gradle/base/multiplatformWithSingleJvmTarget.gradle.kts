plugins {
  kotlin("multiplatform") version "1.9.24"
  id("dev.adamko.kotlin.binary-compatibility-validator") version "+"
}

kotlin {
  targets {
    jvm {
      compilations.all {
        kotlinOptions.jvmTarget = "1.8"
      }
      testRuns["test"].executionTask.configure {
        useJUnit()
      }
    }
  }
  sourceSets {
    val commonMain by getting
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val jvmMain by getting
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
      }
    }
  }
}
