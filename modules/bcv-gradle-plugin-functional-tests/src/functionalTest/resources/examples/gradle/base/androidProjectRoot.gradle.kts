plugins {
    id("com.android.application").version("7.2.2").apply(false)
    id("com.android.library").version("7.2.2").apply(false)
    id("org.jetbrains.kotlin.android").version("1.7.10").apply(false)
    //id("org.jetbrains.kotlinx.binary-compatibility-validator").apply(false)
    id("dev.adamko.kotlin.binary-compatibility-validator") version "0.0.3-SNAPSHOT" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
