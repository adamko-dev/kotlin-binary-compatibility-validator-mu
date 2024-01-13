package buildsrc.settings

import java.io.File
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*


/**
 * Settings for the [buildsrc.conventions.Maven_publish_test_gradle] convention plugin.
 */
abstract class MavenPublishingSettings @Inject constructor(
  private val project: Project,
  private val providers: ProviderFactory,
) {

  private val isReleaseVersion: Provider<Boolean> =
    providers.provider { !project.version.toString().endsWith("-SNAPSHOT") }

  val sonatypeReleaseUrl: Provider<String> =
    isReleaseVersion.map { isRelease ->
      if (isRelease) {
        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
      } else {
        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
      }
    }

  val mavenCentralUsername: Provider<String> =
    bcvProp("mavenCentralUsername")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_USERNAME"))
  val mavenCentralPassword: Provider<String> =
    bcvProp("mavenCentralPassword")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_PASSWORD"))

  val signingKeyId: Provider<String> =
    bcvProp("signing.keyId")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_KEY_ID"))
  val signingKey: Provider<String> =
    bcvProp("signing.key")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_KEY"))
  val signingPassword: Provider<String> =
    bcvProp("signing.password")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_PASSWORD"))

  val githubPublishDir: Provider<File> =
    providers.environmentVariable("GITHUB_PUBLISH_DIR").map { File(it) }

  private fun bcvProp(name: String): Provider<String> =
    providers.gradleProperty("dev.adamko.bcv-mu.$name")

  private fun <T : Any> bcvProp(name: String, convert: (String) -> T): Provider<T> =
    bcvProp(name).map(convert)

  companion object {
    const val EXTENSION_NAME = "mavenPublishing"

    /** Retrieve the [KayrayBuildProperties] extension. */
    internal val Project.mavenPublishing: MavenPublishingSettings
      get() = extensions.getByType()

    /** Configure the [KayrayBuildProperties] extension. */
    internal fun Project.mavenPublishing(configure: MavenPublishingSettings.() -> Unit) =
      extensions.configure(configure)
  }
}
