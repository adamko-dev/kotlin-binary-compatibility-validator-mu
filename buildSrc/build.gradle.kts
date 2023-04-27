import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
  `kotlin-dsl`
}

dependencies {
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")

  implementation(libs.gradlePlugin.bcvMu)
  implementation(libs.gradlePlugin.pluginPublishing)
  implementation(libs.gradlePlugin.shadow)
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(11)
  }
}
