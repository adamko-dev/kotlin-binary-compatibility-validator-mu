package buildsrc.conventions

import buildsrc.settings.MavenPublishingSettings

plugins {
  `maven-publish`
  signing
}

val mavenPublishing =
  extensions.create<MavenPublishingSettings>(MavenPublishingSettings.EXTENSION_NAME, project)


//region POM convention
publishing {
  publications.withType<MavenPublication>().configureEach {
    pom {
      name.convention("Binary Compatibility Validator MU")
      description.convention("BCV-MU is a Gradle Plugin that validates the public JVM binary API of libraries, to make sure that breaking changes are tracked.")
      url.convention("https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu")

      scm {
        connection.convention("scm:git:https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu")
        developerConnection.convention("scm:git:https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu")
        url.convention("https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu")
      }

      licenses {
        license {
          name.convention("Apache-2.0")
          url.convention("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
      }

      developers {
        developer {
          email.set("adam@adamko.dev")
        }
      }
    }
  }
}
//endregion


//region Maven Central publishing/signing
publishing {
  repositories {
    val mavenCentralUsername = mavenPublishing.mavenCentralUsername.orNull
    val mavenCentralPassword = mavenPublishing.mavenCentralPassword.orNull
    if (!mavenCentralUsername.isNullOrBlank() && !mavenCentralPassword.isNullOrBlank()) {
      maven(mavenPublishing.sonatypeReleaseUrl) {
        name = "SonatypeRelease"
        credentials {
          username = mavenCentralUsername
          password = mavenCentralPassword
        }
      }
    }
  }

  // com.gradle.plugin-publish automatically adds a Javadoc jar
}

signing {
  logger.info("maven-publishing.gradle.kts enabled signing for ${project.path}")

  val keyId = mavenPublishing.signingKeyId.orNull
  val key = mavenPublishing.signingKey.orNull
  val password = mavenPublishing.signingPassword.orNull

  if (!keyId.isNullOrBlank() && !key.isNullOrBlank() && !password.isNullOrBlank()) {
    useInMemoryPgpKeys(keyId, key, password)
  }

  setRequired({
    gradle.taskGraph.allTasks.filterIsInstance<PublishToMavenRepository>().any {
      it.repository.name == "SonatypeRelease"
    }
  })
}

//afterEvaluate {
//  com.gradle.plugin-publish automatically signs tasks in a weird way, that stops this from working:
//  signing {
//    sign(publishing.publications)
//  }
//}
//endregion


//region Fix Gradle warning about signing tasks using publishing task outputs without explicit dependencies
// https://youtrack.jetbrains.com/issue/KT-46466 https://github.com/gradle/gradle/issues/26091
tasks.withType<AbstractPublishToMaven>().configureEach {
  val signingTasks = tasks.withType<Sign>()
  mustRunAfter(signingTasks)
}
//endregion


//region publishing logging
tasks.withType<AbstractPublishToMaven>().configureEach {
  val publicationGAV = provider { publication?.run { "$group:$artifactId:$version" } }
  doLast("log publication GAV") {
    if (publicationGAV.isPresent) {
      logger.info("[task: ${path}] ${publicationGAV.get()}")
    }
  }
}
//endregion


//region IJ workarounds
// manually define the Kotlin DSL accessors because IntelliJ _still_ doesn't load them properly
fun Project.publishing(configure: PublishingExtension.() -> Unit): Unit =
  extensions.configure(configure)

val Project.publishing: PublishingExtension
  get() = extensions.getByType()

fun Project.signing(configure: SigningExtension.() -> Unit): Unit =
  extensions.configure(configure)

val Project.signing: SigningExtension
  get() = extensions.getByType()
//endregion
