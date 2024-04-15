package dev.adamko.kotlin.binary_compatibility_validator.tasks

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Performs simple file operations, not worth caching")
abstract class BCVApiGeneratePreparationTask
@BCVInternalApi
@Inject
constructor(
  private val fs: FileSystemOperations,
) : BCVDefaultTask() {

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val apiDumpFiles: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val apiDirectory: DirectoryProperty

  @TaskAction
  fun action() {
    fs.sync {
      from(apiDumpFiles)
      into(apiDirectory)
    }
  }
}
