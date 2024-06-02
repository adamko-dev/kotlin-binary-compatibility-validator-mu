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
      attributes {
        attribute(Attribute.of("variant", String::class.java), "a")
      }
    }
    jvm("anotherJvm") {
      compilations.all {
        kotlinOptions.jvmTarget = "1.8"
      }
      testRuns["test"].executionTask.configure {
        useJUnit()
      }
      attributes {
        attribute(Attribute.of("variant", String::class.java), "b")
      }
    }
  }
  sourceSets {
    commonMain
    commonTest {
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
