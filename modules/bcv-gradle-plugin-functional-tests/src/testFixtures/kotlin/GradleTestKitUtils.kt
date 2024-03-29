@file:OptIn(io.kotest.common.KotestInternal::class)

package dev.adamko.kotlin.binary_compatibility_validator.test.utils

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.GradleProjectTest.Companion.devMavenRepoPathString
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language


// utils for testing using Gradle TestKit


class GradleProjectTest(
  override val projectDir: Path,
) : ProjectDirectoryScope {

  constructor(
    testProjectName: String,
    baseDir: Path = funcTestTempDir,
  ) : this(projectDir = baseDir.resolve(testProjectName))

  val runner: GradleRunner = GradleRunner.create()
    .withProjectDir(projectDir.toFile())
    .apply {
      gradleTestKitDir?.let {
        println("Using Gradle TestKit dir $it")
        withTestKitDir(it.toFile())
      }
    }

  val projectName by projectDir::name

  companion object {

    val gradleTestKitDir: Path? by optionalSystemProperty(Paths::get)

    /** file-based Maven Repo that contains the published plugin */
    private val devMavenRepoDir: Path by systemProperty(Paths::get)
    val devMavenRepoPathString
      get() = devMavenRepoDir
        .toFile()
        .canonicalFile
        .absoluteFile
        .invariantSeparatorsPath

    private val projectTestTempDir: Path by systemProperty(Paths::get)

    /** Temporary directory for the functional tests */
    val funcTestTempDir: Path by lazy {
      projectTestTempDir.resolve("functional-tests")
    }

    val minimumGradleTestVersion: String by systemProperty()

    private fun systemProperty() = systemProperty { it }

    private fun <T> optionalSystemProperty(
      convert: (String) -> T?
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, T?>> =
      PropertyDelegateProvider { _: Any?, property ->
        val systemProp = System.getProperty(property.name)
        val value = if (systemProp != null) convert(systemProp) else null
        ReadOnlyProperty { _, _ -> value }
      }

    private fun <T> systemProperty(
      convert: (String) -> T & Any
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, T>> =
      PropertyDelegateProvider { _: Any?, property ->
        val systemProp = requireNotNull(System.getProperty(property.name)) {
          "system property ${property.name} is unavailable"
        }
        val converted = convert(systemProp)
        ReadOnlyProperty { _, _ -> converted }
      }
  }
}


/**
 * Builder for testing a Gradle project that uses Kotlin script DSL and creates default
 * `settings.gradle.kts` and `gradle.properties` files.
 *
 * @param[testProjectName] the path of the project directory
 */
fun gradleKtsProjectTest(
  testProjectName: String,
  baseDir: Path = GradleProjectTest.funcTestTempDir,
  build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
  return GradleProjectTest(baseDir = baseDir, testProjectName = testProjectName).apply {

    settingsGradleKts = """
      |rootProject.name = "$projectName"
      |
      |pluginManagement {
      |  repositories {
      |${devMavenRepoKotlinDsl().prependIndent("    ")}
      |    mavenCentral()
      |    gradlePluginPortal()
      | }
      |}
      |
      |@Suppress("UnstableApiUsage")
      |dependencyResolutionManagement {
      |  repositories {
      |${devMavenRepoKotlinDsl().prependIndent("    ")}
      |    mavenCentral()
      |  }
      |}
      |
    """.trimMargin()

    buildGradleKts = """"""

    gradleProperties = """
      |kotlin.mpp.stability.nowarn=true
      |
    """.trimMargin()

    build()
  }
}

/**
 * Builder for testing a Gradle project that uses Groovy script and creates default,
 * `settings.gradle`, and `gradle.properties` files.
 *
 * @param[testProjectName] the name of the test, which should be distinct across the project
 */
fun gradleGroovyProjectTest(
  testProjectName: String,
  baseDir: Path = GradleProjectTest.funcTestTempDir,
  build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
  return GradleProjectTest(baseDir = baseDir, testProjectName = testProjectName).apply {

    settingsGradle = """
      |rootProject.name = "test"
      |
      |dependencyResolutionManagement {
      |  repositories {
      |    mavenCentral()
      |${devMavenRepoGroovyDsl().prependIndent("    ")}
      |  }
      |}
      |
      |pluginManagement {
      |  repositories {
      |${devMavenRepoGroovyDsl().prependIndent("    ")}
      |  }
      |}
      |
    """.trimMargin()

    gradleProperties = """
      |kotlin.mpp.stability.nowarn=true
      |org.gradle.cache=true
    """.trimMargin()

    build()
  }
}

@Language("kts")
internal fun devMavenRepoKotlinDsl(): String {
  return """
    |exclusiveContent {
    |  forRepository {
    |    maven(file("$devMavenRepoPathString")) {
    |      name = "Dev Maven Repo"
    |    }
    |  }
    |  filter {
    |        includeGroup("dev.adamko.kotlin.binary_compatibility_validator")
    |        includeGroup("dev.adamko.kotlin.binary-compatibility-validator")
    |        includeGroup("dev.adamko.kotlin.binary-compatibility-validator.project")
    |        includeGroup("dev.adamko.kotlin.binary-compatibility-validator.settings")
    |  }
    |}
  """.trimMargin()
}

@Language("groovy")
private fun devMavenRepoGroovyDsl(): String {
  return """
    |exclusiveContent {
    |  forRepository {
    |    maven {
    |      url = file("$devMavenRepoPathString") 
    |      name = "Dev Maven Repo"
    |    }
    |  }
    |  filter {
    |        includeGroup("dev.adamko.kotlin.binary_compatibility_validator")
    |        includeGroup("dev.adamko.kotlin.binary-compatibility-validator")
    |        includeGroup("dev.adamko.kotlin.binary-compatibility-validator.project")
    |        includeGroup("dev.adamko.kotlin.binary-compatibility-validator.settings")
    |  }
    |}
  """.trimMargin()
}


fun GradleProjectTest.projectFile(
  @Language("TEXT")
  filePath: String
): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, String>> =
  PropertyDelegateProvider { _, _ ->
    TestProjectFileProvidedDelegate(this, filePath)
  }


/** Delegate for reading and writing a [GradleProjectTest] file. */
private class TestProjectFileProvidedDelegate(
  private val project: GradleProjectTest,
  private val filePath: String,
) : ReadWriteProperty<Any?, String> {
  override fun getValue(thisRef: Any?, property: KProperty<*>): String =
    project.projectDir.resolve(filePath).toFile().readText()

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    project.createFile(filePath, value)
  }
}

/** Delegate for reading and writing a [GradleProjectTest] file. */
class TestProjectFileDelegate(
  private val filePath: String,
) : ReadWriteProperty<ProjectDirectoryScope, String> {
  override fun getValue(thisRef: ProjectDirectoryScope, property: KProperty<*>): String =
    thisRef.projectDir.resolve(filePath).toFile().readText()

  override fun setValue(thisRef: ProjectDirectoryScope, property: KProperty<*>, value: String) {
    thisRef.createFile(filePath, value)
  }
}


@DslMarker
annotation class ProjectDirectoryDsl

@ProjectDirectoryDsl
interface ProjectDirectoryScope {
  val projectDir: Path
}

private data class ProjectDirectoryScopeImpl(
  override val projectDir: Path
) : ProjectDirectoryScope


fun ProjectDirectoryScope.createFile(filePath: String, contents: String): File =
  projectDir.resolve(filePath).toFile().apply {
    if (!exists()) {
      parentFile.mkdirs()
      createNewFile()
    }
    writeText(contents)
  }

@ProjectDirectoryDsl
fun ProjectDirectoryScope.dir(
  path: String,
  block: ProjectDirectoryScope.() -> Unit = {},
): ProjectDirectoryScope =
  ProjectDirectoryScopeImpl(projectDir.resolve(path)).apply(block)


@ProjectDirectoryDsl
fun ProjectDirectoryScope.file(
  path: String
): Path = projectDir.resolve(path)


fun ProjectDirectoryScope.findFiles(matcher: (File) -> Boolean): Sequence<File> =
  projectDir.toFile().walk().filter(matcher)


/** Get or set the content of `settings.gradle.kts` */
@delegate:Language("kts")
var ProjectDirectoryScope.settingsGradleKts: String by TestProjectFileDelegate("settings.gradle.kts")


/** Get or set the content of `build.gradle.kts` */
@delegate:Language("kts")
var ProjectDirectoryScope.buildGradleKts: String by TestProjectFileDelegate("build.gradle.kts")


/** Get or set the content of `settings.gradle` */
@delegate:Language("groovy")
var ProjectDirectoryScope.settingsGradle: String by TestProjectFileDelegate("settings.gradle")


/** Get or set the content of `build.gradle` */
@delegate:Language("groovy")
var ProjectDirectoryScope.buildGradle: String by TestProjectFileDelegate("build.gradle")


/** Get or set the content of `gradle.properties` */
@delegate:Language("properties")
var ProjectDirectoryScope.gradleProperties: String by TestProjectFileDelegate("gradle.properties")

fun ProjectDirectoryScope.createKotlinFile(filePath: String, @Language("kotlin") contents: String) =
  createFile(filePath, contents)

fun ProjectDirectoryScope.createKtsFile(filePath: String, @Language("kts") contents: String) =
  createFile(filePath, contents)
