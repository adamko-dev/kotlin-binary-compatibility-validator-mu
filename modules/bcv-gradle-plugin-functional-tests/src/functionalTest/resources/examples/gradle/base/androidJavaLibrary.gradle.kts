plugins {
  id("com.android.library")
  id("dev.adamko.kotlin.binary-compatibility-validator") version "0.0.5"
}

android {
  namespace = "org.jetbrains.kotlinx.android.java.library"

  compileSdk = 32

  defaultConfig {
    minSdk = 31
    targetSdk = 32

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
}

dependencies {
  // no dependencies required
}
