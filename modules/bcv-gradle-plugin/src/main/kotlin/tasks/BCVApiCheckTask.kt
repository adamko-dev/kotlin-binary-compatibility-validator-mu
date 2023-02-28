package dev.adamko.kotlin.binary_compatibility_validator.tasks

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import dev.adamko.kotlin.binary_compatibility_validator.internal.GradlePath
import dev.adamko.kotlin.binary_compatibility_validator.internal.fullPath
import java.io.*
import java.util.TreeSet
import javax.inject.Inject
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*

@CacheableTask
abstract class BCVApiCheckTask @Inject constructor(
  private val objects: ObjectFactory,
  private val providers: ProviderFactory,
) : BCVDefaultTask() {

  @get:InputDirectory
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  val projectApiDir: Provider<File>
    // workaround for https://github.com/gradle/gradle/issues/2016
    get() = expectedApiDirPath.flatMap { providers.provider { File(it).takeIf(File::exists) } }

  @get:Input
  @get:Optional
  abstract val expectedApiDirPath: Property<String>

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val apiBuildDir: DirectoryProperty

  @get:Input
  internal abstract val expectedProjectName: Property<String>

  // Project and tasks paths are used for creating better error messages
  private val projectFullPath = project.fullPath
  private val apiDumpTaskPath = GradlePath(project.path).child("apiDump")

  private val rootDir = project.rootProject.rootDir

  @TaskAction
  fun verify() {
    val projectApiDir = projectApiDir.orNull
      ?: error(
        """
          Expected folder with API declarations '${expectedApiDirPath.get()}' does not exist.
          Please ensure that task '$apiDumpTaskPath' was executed in order to get API dump to compare the build against
        """.trimIndent()
      )

    val checkApiDeclarationPaths = projectApiDir.relativePathsOfContent { !isDirectory }
    logger.info("checkApiDeclarationPaths: $checkApiDeclarationPaths")

    checkApiDeclarationPaths.forEach { checkApiDeclarationPath ->
      logger.info("---------------------------")
      checkTarget(
        checkApiDeclaration = checkApiDeclarationPath.getFile(projectApiDir),
        builtApiDeclaration = checkApiDeclarationPath.getFile(apiBuildDir.get().asFile)
      )
      logger.info("---------------------------")
    }
  }

  private fun checkTarget(
    checkApiDeclaration: File,
    builtApiDeclaration: File,
  ) {
    logger.info("checkApiDeclaration: $checkApiDeclaration")
    logger.info("builtApiDeclaration: $builtApiDeclaration")

    val allBuiltFilePaths = builtApiDeclaration.parentFile.relativePathsOfContent()
    val allCheckFilePaths = checkApiDeclaration.parentFile.relativePathsOfContent()

    logger.info("allBuiltPaths: $allBuiltFilePaths")
    logger.info("allCheckFiles: $allCheckFilePaths")

    val builtFilePath = allBuiltFilePaths.singleOrNull()
      ?: error("Expected a single file ${expectedProjectName.get()}.api, but found ${allBuiltFilePaths.size}: $allBuiltFilePaths")

    if (builtFilePath !in allCheckFilePaths) {
      val relativeDirPath = projectApiDir.get().toRelativeString(rootDir) + File.separator
      error(
        "File ${builtFilePath.lastName} is missing from ${relativeDirPath}, please run '$apiDumpTaskPath' task to generate one"
      )
    }

    val diff = compareFiles(
      checkFile = checkApiDeclaration,
      builtFile = builtApiDeclaration,
    )
    val diffSet = mutableSetOf<String>()
    if (diff != null) diffSet.add(diff)
    if (diffSet.isNotEmpty()) {
      val diffText = diffSet.joinToString("\n\n")
      error(
        """
          |API check failed for project $projectFullPath.
          |$diffText
          |
          |You can run '$apiDumpTaskPath' task to overwrite API declarations
        """.trimMargin()
      )
    }
  }

  /** Get the relative paths of all files and folders inside a directory */
  private fun File?.relativePathsOfContent(
    filter: FileVisitDetails.() -> Boolean = { true },
  ): TreeSet<RelativePath> {
    val contents = setOfRelativePaths()
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

  companion object {
    /*
     * We use case-insensitive comparison to workaround issues with case-insensitive OSes
     * and Gradle behaving slightly different on different platforms.
     * We neither know original sensitivity of existing .api files, not
     * build ones, because projectName that is part of the path can have any sensitivity.
     * To work around that, we replace paths we are looking for the same paths that
     * actually exist on FS.
     */
    private fun setOfRelativePaths() = TreeSet<RelativePath> { rp1, rp2 ->
      rp1.toString().compareTo(rp2.toString(), true)
    }
  }
}
