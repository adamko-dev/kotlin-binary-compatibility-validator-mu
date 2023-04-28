plugins {
  id("com.android.application").version("7.2.2").apply(false)
  id("com.android.library").version("7.2.2").apply(false)
  id("org.jetbrains.kotlin.android").version("1.7.10").apply(false)
  id("dev.adamko.kotlin.binary-compatibility-validator") version "0.0.5" apply false
}

tasks.register("clean", Delete::class) {
  delete(rootProject.buildDir)
}
