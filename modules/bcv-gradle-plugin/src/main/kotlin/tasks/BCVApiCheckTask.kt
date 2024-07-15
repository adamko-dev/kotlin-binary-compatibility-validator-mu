package dev.adamko.kotlin.binary_compatibility_validator.tasks

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_DUMP_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.internal.*
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVKLibTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget
import dev.adamko.kotlin.binary_compatibility_validator.workers.KLibExtractWorker
import java.io.File
import java.util.TreeMap
import javax.inject.Inject
import kotlin.io.path.listDirectoryEntries
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.*
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class BCVApiCheckTask
@BCVInternalApi
@Inject
constructor(
  private val workers: WorkerExecutor,
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
) : BCVDefaultTask() {

  @get:Nested
  val targets: NamedDomainObjectContainer<BCVTarget> =
    extensions.adding("targets") { objects.domainObjectContainer() }

  @get:InputDirectory
  @get:Optional
  @get:PathSensitive(RELATIVE)
  val projectApiDir: Provider<File>
    // workaround for https://github.com/gradle/gradle/issues/2016
    get() = expectedApiDirPath.flatMap { providers.provider { File(it).takeIf(File::exists) } }

  @get:Input
  @get:Optional
  abstract val expectedApiDirPath: Property<String>

  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  abstract val apiBuildDir: DirectoryProperty
//
//  @get:LocalState
//  internal val tempDir: File get() = temporaryDir

  @get:Input
  internal abstract val expectedProjectName: Property<String>
//
//  @get:Input
//  abstract val strictKLibTargetValidation: Property<Boolean>

  // Project and tasks paths are used for creating better error messages
  private val projectFullPath: String = project.fullPath
  private val apiDumpTaskPath: GradlePath = GradlePath(project.path).child(API_DUMP_TASK_NAME)

  private val rootDir: File = project.rootProject.rootDir

//  @get:Classpath
//  abstract val runtimeClasspath: ConfigurableFileCollection

  init {
    super.onlyIf { task ->
      require(task is BCVApiCheckTask)
      task.apiBuildDir.orNull?.asFile
        ?.takeIf(File::exists)
        ?.toPath()
        ?.listDirectoryEntries()
        ?.isNotEmpty() == true
    }
  }

  @TaskAction
  fun verify() {
    val projectApiDir = projectApiDir.orNull
      ?: error(
        """
        Expected folder with API declarations '${expectedApiDirPath.get()}' does not exist.
        Please ensure that task '$apiDumpTaskPath' was executed in order to get API dump to compare the build against.
        """.trimIndent()
      )

    val apiBuildDir = apiBuildDir.get().asFile

    verifyJvm(projectApiDir, apiBuildDir)

//    val klibTargets = targets.withType<BCVKLibTarget>().filter { it.enabled.get() }
//    verifyKLib(projectApiDir, apiBuildDir, klibTargets)

    // TODO need to verify that all .api files in projectApiDir have a match
  }

  private fun verifyJvm(
    projectApiDir: File,
    apiBuildDir: File,
  ) {


//    val jvmTargets = targets.withType<BCVJvmTarget>().filter { it.enabled.get() }
//
//
//    jvmTargets.forEach { target ->
//      if (jvmTargets.size > 1) {
//        val expectedApiFile = projectApiDir
//          .resolve(target.name)
//          .resolve(target.platformType)
//        val actualApiFile = apiBuildDir
//          .resolve(target.name)
//          .resolve(target.platformType)
//        checkTarget(
//          expectedApiDeclaration = expectedApiFile,
//          actualApiDeclaration = actualApiFile,
//        )
//      } else {
//        val expectedApiFile = projectApiDir
//          .resolve(target.platformType)
//        val actualApiFile = apiBuildDir
//          .resolve(target.platformType)
//        checkTarget(
//          expectedApiDeclaration = expectedApiFile,
//          actualApiDeclaration = actualApiFile,
//        )
//      }
//    }

    val expectedApiFiles = projectApiDir.relativePathsOfContent {
      file.name.substringAfter(".") == "api"
    }
    val actualApiFiles = apiBuildDir.relativePathsOfContent {
      file.name.substringAfter(".") == "api"
    }
    logger.info("[$path] expectedApiFiles: $expectedApiFiles")

    expectedApiFiles.forEach { expectedApiFile ->
      checkTarget(
        expectedApiDeclaration = expectedApiFile.getFile(projectApiDir),
        // fetch the builtFile, using the case-insensitive map
        actualApiDeclaration = actualApiFiles[expectedApiFile]?.getFile(apiBuildDir)
      )
    }
  }

  private fun checkTarget(
    expectedApiDeclaration: File,
    actualApiDeclaration: File?,
  ) {
    logger.info("[$path] expectedApiDeclaration: $expectedApiDeclaration")
    logger.info("[$path] actualApiDeclaration: $actualApiDeclaration")

    val allBuiltFilePaths = actualApiDeclaration?.parentFile.relativePathsOfContent()
    val allCheckFilePaths = expectedApiDeclaration.parentFile.relativePathsOfContent()

    logger.info("[$path] allBuiltPaths: $allBuiltFilePaths")
    logger.info("[$path] allCheckFiles: $allCheckFilePaths")

    val builtFilePath = allBuiltFilePaths.singleOrNull()
      ?: error("[$path] Expected a single file ${expectedProjectName.get()}.api, but found ${allBuiltFilePaths.size}: $allBuiltFilePaths")

    if (actualApiDeclaration == null || builtFilePath !in allCheckFilePaths) {
      val relativeDirPath = projectApiDir.get().toRelativeString(rootDir) + File.separator
      error(
        "[$path] File ${builtFilePath.lastName} is missing from ${relativeDirPath}, please run '$apiDumpTaskPath' task to generate one"
      )
    }

    val diffText = compareFiles(
      checkFile = expectedApiDeclaration,
      builtFile = actualApiDeclaration,
    )?.trim()

    if (!diffText.isNullOrBlank()) {
      error(
        """
          |API check failed for project $projectFullPath.
          |
          |$diffText
          |
          |You can run '$apiDumpTaskPath' task to overwrite API declarations.
        """.trimMargin()
      )
    }
  }

  /** Get the relative paths of all files inside a directory. */
  private fun File?.relativePathsOfContent(
    filter: FileVisitDetails.() -> Boolean = { true },
  ): RelativePaths {
    val contents = RelativePaths()
    if (this != null) {
      objects.fileTree().from(this).visit {
        if (filter()) contents += relativePath
      }
    }
    return contents
  }

  private fun compareFiles(checkFile: File, builtFile: File): String? {
    val checkText = checkFile.readText()
    val builtText = builtFile.readText()

    // We don't compare full text because newlines on Windows & Linux/macOS are different
    val checkLines = checkText.lines()
    val builtLines = builtText.lines()
    if (checkLines == builtLines)
      return null

    val patch = DiffUtils.diff(checkLines, builtLines)
    val diff = UnifiedDiffUtils.generateUnifiedDiff(
      checkFile.toString(),
      builtFile.toString(),
      checkLines,
      patch,
      3,
    )
    return diff.joinToString("\n")
  }


//  private fun verifyKLib(
//    projectApiDir: File,
//    apiBuildDir: File,
//    targets: List<BCVKLibTarget>
//  ) {
//    if (targets.isEmpty()) return
//    val klibFile = projectApiDir.resolve(expectedProjectName.get())
//
//  }
//  private fun prepareWorkQueue(): WorkQueue {
//    return workers.classLoaderIsolation {
//      classpath.from(runtimeClasspath)
//    }
//  }


//  @OptIn(BCVExperimentalApi::class)
//  private fun WorkQueue.extract(
//    target: BCVKLibTarget,
//    targetDumpFiles: Set<File>,
//    outputDir: File,
//  ) {
//    val task = this@BCVApiCheckTask
//
//    @OptIn(BCVInternalApi::class)
//    submit(KLibExtractWorker::class) worker@{
//      this@worker.taskPath.set(task.path)
//      this@worker.strictValidation.set(task.strictKLibTargetValidation)
//
////      this@worker.inputAbiFile.set()
////      this@worker.outputAbiFile.set()
////      this@worker.supportedTargets.set()
//    }
//  }
}

/**
 * We use case-insensitive comparison to workaround issues with case-insensitive OSes and Gradle
 * behaving slightly different on different platforms. We neither know original sensitivity of
 * existing `.api` files, not build ones, because `projectName` that is part of the path can have
 * any sensitivity. To work around that, we replace paths we are looking for the same paths that
 * actually exist on the FS.
 */
private class RelativePaths(
  private val map: TreeMap<RelativePath, RelativePath> = caseInsensitiveMap()
) : Set<RelativePath> by map.keys {

  operator fun plusAssign(path: RelativePath): Unit = map.set(path, path)

  operator fun get(path: RelativePath): RelativePath? = map[path]

  override fun toString(): String =
    map.keys.joinToString(
      prefix = "RelativePaths(",
      separator = "/",
      postfix = ")",
      transform = RelativePath::getPathString,
    )

  companion object {
    private fun caseInsensitiveMap() =
      TreeMap<RelativePath, RelativePath> { path1, path2 ->
        path1.toString().compareTo(path2.toString(), true)
      }
  }
}
