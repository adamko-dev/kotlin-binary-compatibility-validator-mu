package dev.adamko.kotlin.binary_compatibility_validator.tasks

import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.*

@CacheableTask
abstract class BCVApiDumpTask @Inject constructor(
  private val fs: FileSystemOperations
) : BCVDefaultTask() {

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val apiDumpFiles: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val apiDirectory: DirectoryProperty

  @TaskAction
  fun action() {
    fs.sync {
      from(apiDumpFiles) {
        include("**/*.api")
      }
      into(apiDirectory)
    }
  }
}
