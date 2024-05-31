plugins {
  id("com.android.application").version("7.2.2").apply(false)
  id("com.android.library").version("7.2.2").apply(false)
  id("org.jetbrains.kotlin.android").version("1.9.24").apply(false)
  id("dev.adamko.kotlin.binary-compatibility-validator") version "+" apply false
}

tasks.register("clean", Delete::class) {
  delete(rootProject.buildDir)
}
