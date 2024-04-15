package dev.adamko.kotlin.binary_compatibility_validator.tasks

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.isRootProject
import javax.inject.Inject
import kotlin.io.path.invariantSeparatorsPathString
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE

@CacheableTask
abstract class BCVApiDumpTask
@BCVInternalApi
@Inject
constructor(
  private val fs: FileSystemOperations,
  private val layout: ProjectLayout,
) : BCVDefaultTask() {

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val apiDumpFiles: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val apiDirectory: DirectoryProperty

  private val projectGradlePath: String =
    if (project.isRootProject) {
      "project ':' (the root project)"
    } else {
      "subproject '${project.path}'"
    }

  @TaskAction
  fun action() {
    validateApiDir()
    updateDumpDir()
  }

  private fun validateApiDir() {
    val projectDir = layout.projectDirectory.asFile.toPath().toAbsolutePath().normalize()
    val apiDir = projectDir.resolve(apiDirectory.get().asFile.toPath()).normalize()
    require(apiDir.startsWith(projectDir)) {
      /* language=text */ """
        |Error: Invalid output apiDirectory
        |
        |apiDirectory is set to a custom directory, outside of the current project directory.
        |This is not permitted. apiDirectory must be a subdirectory of $projectGradlePath directory.
        |
        |Remove the custom apiDirectory, or update apiDirectory to be a project subdirectory.
        |
        |Project directory: ${projectDir.invariantSeparatorsPathString}
        |apiDirectory:      ${apiDir.invariantSeparatorsPathString}
      """.trimMargin()
    }
  }

  private fun updateDumpDir() {
    fs.sync {
      from(apiDumpFiles) {
        include("**/*.api")
      }
      into(apiDirectory)
    }
  }
}
