package buildsrc.utils

import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion


/** The current Gradle version */
internal val CurrentGradleVersion: GradleVersion
  get() = GradleVersion.current()


/** @see GradleVersion.compareTo */
internal operator fun GradleVersion.compareTo(version: String): Int =
  compareTo(GradleVersion.version(version))


/**
 * Mark this [Configuration] as one that should be used to declare dependencies in
 * [Project.dependencies] block.
 *
 * Declarable Configurations should be extended by [resolvable] and [consumable] Configurations.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = false
 * isCanBeDeclared = true
 * ```
 */
internal fun Configuration.declarable(
  visible: Boolean = false,
) {
  isCanBeResolved = false
  isCanBeConsumed = false
  canBeDeclared(true)
  isVisible = visible
}


/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.consumable(
  visible: Boolean = false,
) {
  isCanBeResolved = false
  isCanBeConsumed = true
  canBeDeclared(false)
  isVisible = visible
}


/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.resolvable(
  visible: Boolean = false,
) {
  isCanBeResolved = true
  isCanBeConsumed = false
  canBeDeclared(false)
  isVisible = visible
}


/**
 * Enable/disable [Configuration.isCanBeDeclared] only if it is supported by the
 * [CurrentGradleVersion]
 *
 * This function should be removed when the minimal supported Gradle version is 8.2.
 */
@Suppress("UnstableApiUsage")
private fun Configuration.canBeDeclared(value: Boolean) {
  if (CurrentGradleVersion >= "8.2") {
    isCanBeDeclared = value
  }
}


/** Drop the first [count] directories from the path */
fun RelativePath.dropDirectories(count: Int): RelativePath =
  RelativePath(true, *segments.drop(count).toTypedArray())


/** Drop the first directory from the path */
fun RelativePath.dropDirectory(): RelativePath =
  dropDirectories(1)


/** Drop the first directory from the path */
fun RelativePath.dropDirectoriesWhile(
  segmentPrediate: (segment: String) -> Boolean
): RelativePath =
  RelativePath(
    true,
    *segments.dropWhile(segmentPrediate).toTypedArray(),
  )


/**
 * Don't publish test fixtures (which causes warnings when publishing)
 *
 * https://docs.gradle.org/current/userguide/java_testing.html#publishing_test_fixtures
 */
fun Project.skipTestFixturesPublications() {
  val javaComponent = components["java"] as AdhocComponentWithVariants
  javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
  javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
}

/** Get all [Configuration] names for a [SourceSet] */
fun SourceSet.configurationNames() =
  listOf(
    compileOnlyConfigurationName,
    compileOnlyApiConfigurationName,
    compileClasspathConfigurationName,
    annotationProcessorConfigurationName,
    apiConfigurationName,
    implementationConfigurationName,
    apiElementsConfigurationName,
    runtimeOnlyConfigurationName,
    runtimeClasspathConfigurationName,
    runtimeElementsConfigurationName,
    javadocElementsConfigurationName,
    sourcesElementsConfigurationName,
  )

/** exclude generated Gradle code, so it doesn't clog up search results */
fun ProjectLayout.generatedKotlinDslAccessorDirs(): Set<File> {

  val generatedSrcDirs = listOf(
    "kotlin-dsl-accessors",
    "kotlin-dsl-external-plugin-spec-builders",
    "kotlin-dsl-plugins",
  )

  return projectDirectory.dir("buildSrc/build/generated-sources")
    .asFile
    .walk()
    .filter { it.isDirectory && it.parentFile.name in generatedSrcDirs }
    .flatMap { file ->
      file.walk().maxDepth(1).filter { it.isDirectory }.toList()
    }
    .toSet()
}
