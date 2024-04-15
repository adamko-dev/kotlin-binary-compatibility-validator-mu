pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "android-project"

include(":kotlin-library")
include(":java-library")
