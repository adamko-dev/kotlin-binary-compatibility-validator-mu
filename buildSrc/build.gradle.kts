import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
  `kotlin-dsl`
//  kotlin("plugin.assignment") version embeddedKotlinVersion
}

dependencies {
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
//  implementation("org.jetbrains.kotlin:kotlin-assignment:$embeddedKotlinVersion")

  implementation("com.gradle.publish:plugin-publish-plugin:1.1.0")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}
