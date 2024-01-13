package buildsrc.conventions

plugins {
  id("buildsrc.conventions.base")
  id("buildsrc.conventions.java-base")
  id("org.gradle.kotlin.kotlin-dsl")
  id("com.gradle.plugin-publish")
}

tasks.validatePlugins {
  enableStricterValidation = true
}

val createJavadocJarReadme by tasks.registering(Sync::class) {
  description = "generate a readme.txt for the Javadoc JAR"
  val projectCoords = provider { project.run { "$group:$name:$version" } }
  inputs.property("projectGAV", projectCoords)
  val projectCoordsToken = "%{projectGAV}"
  from(
    resources.text.fromString(
      """
      |This Javadoc JAR for $projectCoordsToken is intentionally empty.
      |
      |For documentation, see the sources JAR or https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu/
      |
      """.trimMargin()
    )
  ) {
    rename { "readme.txt" }
  }
  into(temporaryDir)
  doLast {
    temporaryDir.walk()
      .filter { it.isFile }
      .forEach { file ->
        file.writeText(
          file.readText().replace(projectCoordsToken, projectCoords.get())
        )
      }
  }
}


// The Gradle Publish Plugin enables the Javadoc JAR in afterEvaluate, so find it lazily
tasks.withType<Jar>()
  .matching { it.name == "javadocJar" }
  .configureEach {
    from(createJavadocJarReadme)
  }
