package dev.adamko.kotlin.binary_compatibility_validator.test.utils.api

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.GradleProjectTest
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.GradleProjectTest.Companion.minimumGradleTestVersion
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.devMavenRepoKotlinDsl
import dev.adamko.kotlin.binary_compatibility_validator.test.utils.invariantNewlines
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language

const val API_DIR = "api"

fun BaseKotlinGradleTest.test(fn: BaseKotlinScope.() -> Unit): GradleRunner {
  val baseKotlinScope = BaseKotlinScope()

  baseKotlinScope.settingsGradleKts {
    addText(/*language=kts*/ """
        |pluginManagement {
        |  repositories {
        |${devMavenRepoKotlinDsl().prependIndent("    ")}
        |    gradlePluginPortal()
        |    mavenCentral()
        |  }
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
    )
  }

  fn(baseKotlinScope)

  baseKotlinScope.files.forEach { scope ->
    val fileWriteTo = rootProjectDir.resolve(scope.filePath)
      .apply {
        parentFile?.mkdirs()
        createNewFile()
      }

    scope.content.forEach {
      val content = when (it) {
        is AppendableScope.AppendFile -> readResourceFile(it.path)
        is AppendableScope.AppendText -> it.text
      }
      fileWriteTo.appendText(content)
    }
  }

  baseKotlinScope.gradleProperties {
    addLine("org.gradle.jvmargs=" + baseKotlinScope.runner.gradleJvmArgs.joinToString(" "))
  }

  return GradleRunner.create()
    .withProjectDir(rootProjectDir)
    .withGradleVersion(baseKotlinScope.runner.gradleVersion)
    .withArguments(baseKotlinScope.runner.buildArgs())
    .apply {
      GradleProjectTest.gradleTestKitDir?.let {
        println("Using Gradle TestKit dir $it")
        withTestKitDir(it.toFile())
      }
    }
}

/**
 * same as [file][FileContainer.file], but prepends `src/${sourceSet}/kotlin` before given `classFileName`
 */
fun FileContainer.kotlin(
  classFileName: String,
  sourceSet: String = "main",
  fn: AppendableScope.() -> Unit
) {
  require(classFileName.endsWith(".kt")) {
    "ClassFileName must end with '.kt'"
  }

  val fileName = "src/${sourceSet}/kotlin/$classFileName"
  file(fileName, fn)
}

/**
 * same as [file][FileContainer.file], but prepends `src/${sourceSet}/java` before given `classFileName`
 */
fun FileContainer.java(
  classFileName: String,
  sourceSet: String = "main",
  fn: AppendableScope.() -> Unit
) {
  require(classFileName.endsWith(".java")) {
    "ClassFileName must end with '.java'"
  }

  val fileName = "src/${sourceSet}/java/$classFileName"
  file(fileName, fn)
}

/**
 * Shortcut for creating a `build.gradle.kts` by using [file][FileContainer.file]
 */
fun FileContainer.buildGradleKts(fn: AppendableScope.() -> Unit) {
  val fileName = "build.gradle.kts"
  file(fileName, fn)
}

/**
 * Shortcut for creating a `settings.gradle.kts` by using [file][FileContainer.file]
 */
fun FileContainer.settingsGradleKts(fn: AppendableScope.() -> Unit) {
  val fileName = "settings.gradle.kts"
  file(fileName, fn)
}

/**
 * Shortcut for creating a `settings.gradle.kts` by using [file][FileContainer.file]
 */
fun FileContainer.gradleProperties(fn: AppendableScope.() -> Unit) {
  val fileName = "gradle.properties"
  file(fileName, fn)
}

/**
 * Declares a directory with the given [dirName] inside the current container.
 * All calls creating files within this scope will create the files nested in this directory.
 *
 * Note that it is valid to call this method multiple times at the same level with the same [dirName].
 * Files declared within 2 independent calls to [dir] will be added to the same directory.
 */
fun FileContainer.dir(dirName: String, fn: DirectoryScope.() -> Unit) {
  DirectoryScope(dirName, this).fn()
}

/**
 * Shortcut for creating a `api/<project>.api` descriptor by using [file][FileContainer.file]
 */
fun FileContainer.apiFile(projectName: String, fn: AppendableScope.() -> Unit) {
  dir(API_DIR) {
    file("$projectName.api", fn)
  }
}

/**
 * Shortcut for creating a `api/<target>/<project>.klib.api` descriptor using [file][FileContainer.file]
 */
fun FileContainer.abiFile(projectName: String, target: String, fn: AppendableScope.() -> Unit) {
  dir(API_DIR) {
    dir(target) {
      file("$projectName.klib.api", fn)
    }
  }
}

fun FileContainer.abiFile(projectName: String, fn: AppendableScope.() -> Unit) {
  dir(API_DIR) {
    file("$projectName.klib.api", fn)
  }
}

// not using default argument in apiFile for clarity in tests (explicit "empty" in the name)
/**
 * Shortcut for creating an empty `api/<project>.api` descriptor by using [file][FileContainer.file]
 */
fun FileContainer.emptyApiFile(projectName: String) {
  apiFile(projectName) {}
}

fun BaseKotlinScope.runner(fn: Runner.() -> Unit) {
  fn(runner)
}

fun AppendableScope.resolve(@Language("file-reference") path: String) {
  this.content.add(AppendableScope.AppendFile(path))
}

fun AppendableScope.addText(text: String) {
  this.content.add(AppendableScope.AppendText(text))
}

fun AppendableScope.addLine(text: String): Unit = addText("$text\n")

interface FileContainer {
  fun file(fileName: String, fn: AppendableScope.() -> Unit = {})
}

class BaseKotlinScope : FileContainer {

  var files: MutableList<AppendableScope> = mutableListOf()
  var runner: Runner = Runner()

  override fun file(fileName: String, fn: AppendableScope.() -> Unit) {
    val appendableScope = AppendableScope(fileName)
    fn(appendableScope)
    files.add(appendableScope)
  }
}

class DirectoryScope(
  val dirPath: String,
  val parent: FileContainer
) : FileContainer {

  override fun file(fileName: String, fn: AppendableScope.() -> Unit) {
    parent.file("$dirPath/$fileName", fn)
  }
}

class AppendableScope(val filePath: String) {
  internal val content: MutableList<AppendableContent> = mutableListOf()

  sealed interface AppendableContent
  @JvmInline
  value class AppendFile(val path: String) : AppendableContent
  @JvmInline
  value class AppendText(val text: String) : AppendableContent
}

class Runner {
  var gradleVersion: String = minimumGradleTestVersion
  /** JVM args used by Gradle. Set as `org.gradle.jvmargs` in `gradle.properties`. */
  val gradleJvmArgs: MutableSet<String> = mutableSetOf(
    "-Dfile.encoding=UTF-8",
    "org.gradle.welcome=never",
  )
  var configurationCache: Boolean = true
  var rerunTasks: Boolean = false
  var rerunTask: Boolean = false
  var buildCache: Boolean = true
  var stacktrace: Boolean = true
  var continues: Boolean = true
  var parallel: Boolean = true

  val arguments: MutableList<String> = mutableListOf()

  internal fun buildArgs(): List<String> = buildList {
    addAll(arguments)
    if (configurationCache) add("--configuration-cache") else add("--no-configuration-cache")
    if (buildCache) add("--build-cache") else add("--no-build-cache")
    if (rerunTasks) add("--rerun-tasks")
    if (rerunTask) add("--rerun-task")
    if (stacktrace) add("--stacktrace")
    if (continues) add("--continue")
    if (parallel) add("--parallel")
  }.distinct()
}

/** Read a resources file as a [String]. */
fun readResourceFile(@Language("file-reference") path: String): String {
  val resource = BaseKotlinGradleTest::class.java.getResource(path)
    ?: error("Could not find resource '$path'")
  return File(resource.toURI()).readText().invariantNewlines()
}
