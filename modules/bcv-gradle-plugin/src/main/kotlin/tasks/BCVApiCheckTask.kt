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

    val expectedApiDeclarations = setOfRelativePaths()
    objects.fileTree().from(projectApiDir)
      .visit {
        if (!isDirectory) expectedApiDeclarations += relativePath
      }
    logger.info("expectedApiDeclarations: $expectedApiDeclarations")

    expectedApiDeclarations.forEach { expectedApiDeclaration ->
      logger.info("---------------------------")
      checkTarget(
        projectApiDir.resolve(expectedApiDeclaration.pathString),
        apiBuildDir.get().asFile.resolve(expectedApiDeclaration.pathString),
      )
      logger.info("---------------------------")
    }
  }

  private fun checkTarget(
    projectApiDeclaration: File,
    buildApiDeclaration: File,
  ) {
    logger.info("projectApiDeclaration: $projectApiDeclaration")
    logger.info("buildApiDeclaration: $buildApiDeclaration")

    val apiBuildDirFiles = setOfRelativePaths()
    objects.fileTree().from(buildApiDeclaration.parent)
      .visit {
        if (!isDirectory) apiBuildDirFiles += relativePath
      }
    val expectedApiFiles = setOfRelativePaths()
    objects.fileTree().from(projectApiDeclaration.parent)
      .visit {
        if (!isDirectory) expectedApiFiles += relativePath
      }

    logger.info("apiBuildDirFiles: $apiBuildDirFiles")
    logger.info("expectedApiFiles: $expectedApiFiles")

    val expectedApiDeclaration = apiBuildDirFiles.singleOrNull()
      ?: error("Expected a single file ${expectedProjectName.get()}.api, but found ${apiBuildDirFiles.size}: $apiBuildDirFiles")

    if (expectedApiDeclaration !in expectedApiFiles) {
      val relativeDirPath = projectApiDir.get().toRelativeString(rootDir) + File.separator
      error(
        "File ${expectedApiDeclaration.lastName} is missing from ${relativeDirPath}, please run '$apiDumpTaskPath' task to generate one"
      )
    }

    val diff = compareFiles(projectApiDeclaration, buildApiDeclaration)
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
