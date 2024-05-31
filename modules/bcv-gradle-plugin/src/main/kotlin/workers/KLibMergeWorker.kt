package dev.adamko.kotlin.binary_compatibility_validator.workers

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import java.io.Serializable
import kotlinx.validation.ExperimentalBCVApi
import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.saveTo
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@BCVInternalApi
@BCVExperimentalApi
@OptIn(ExperimentalBCVApi::class)
abstract class KLibMergeWorker : WorkAction<KLibMergeWorker.Parameters> {

  @BCVInternalApi
  interface Parameters : WorkParameters, Serializable {
    val projectName: Property<String>

    val outputApiDir: DirectoryProperty

    val targetDumpFiles: ConfigurableFileCollection

    /**
     * [Task path][org.gradle.api.Task.getPath] of the task that invoked this worker,
     * for log messages
     */
    val taskPath: Property<String>
  }

  override fun execute() {
    val sourceFiles = parameters.targetDumpFiles.asFileTree
    val outputFile = parameters.outputApiDir.get().asFile.resolve(parameters.projectName.get() + ".klib.api")

    val dump = KlibDump()

    sourceFiles.forEach { dumpFile ->
      dump.merge(dumpFile, dumpFile.name.substringBefore(".klib.api"))
    }

    dump.saveTo(outputFile)
  }

  @BCVInternalApi
  companion object {
    private val logger: Logger = Logging.getLogger(KLibMergeWorker::class.java)
  }
}
