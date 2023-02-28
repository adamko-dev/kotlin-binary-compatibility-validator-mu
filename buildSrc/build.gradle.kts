import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
  `kotlin-dsl`
}

dependencies {
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")

  implementation(libs.gradlePlugin.pluginPublishing)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}
