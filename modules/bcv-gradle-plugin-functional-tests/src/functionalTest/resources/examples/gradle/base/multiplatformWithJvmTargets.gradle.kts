plugins {
  kotlin("multiplatform") version "1.5.20"
  id("dev.adamko.kotlin.binary-compatibility-validator") version "0.0.5-SNAPSHOT"
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
    jvm("anotherJvm") {
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
        implementation(kotlin("stdlib"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val jvmMain by getting
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("test-junit"))
      }
    }
    val anotherJvmMain by getting
    val anotherJvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
      }
    }
  }
}
