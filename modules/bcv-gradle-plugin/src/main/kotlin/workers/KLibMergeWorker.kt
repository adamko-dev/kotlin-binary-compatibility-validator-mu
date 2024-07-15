package dev.adamko.kotlin.binary_compatibility_validator.workers

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import java.io.Serializable
import kotlinx.validation.ExperimentalBCVApi
import kotlinx.validation.KlibValidationSettings
import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.saveTo
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@BCVInternalApi
@BCVExperimentalApi
@OptIn(ExperimentalBCVApi::class)
abstract class KLibMergeWorker : WorkAction<KLibMergeWorker.Parameters> {

  @BCVInternalApi
  interface Parameters : WorkParameters, Serializable {
//    val projectName: Property<String>

    val outputApiFile: RegularFileProperty

    val targetDumpFiles: ConfigurableFileCollection

//    /** Provider returning targets supported by the host compiler. */
//    val supportedTargets: SetProperty<String>
//
//    /** Refer to [KlibValidationSettings.strictValidation] for details. */
//    val strictValidation: Property<Boolean>

    /**
     * [Task path][org.gradle.api.Task.getPath] of the task that invoked this worker,
     * for log messages
     */
    val taskPath: Property<String>
  }

  private val taskPath: String get() = parameters.taskPath.get()

  override fun execute() {
    logger.info("[${taskPath}] merging dump files ${parameters.targetDumpFiles} ")

    val sourceFiles = parameters.targetDumpFiles.asFileTree
//    val outputFile = parameters.outputApiDir.get().asFile.resolve(parameters.projectName.get() + ".klib.api")
    val outputApiFile = parameters.outputApiFile.get().asFile

//    val supportedTargets = parameters.supportedTargets.get().map { KlibTarget.parse(it).targetName }

    val dump = KlibDump()

    sourceFiles.forEach { dumpFile ->
      dump.merge(dumpFile, dumpFile.name.substringBefore(".klib.api"))
    }

//    // Filter out only unsupported files.
//    // That ensures that target renaming will be caught and reported as a change.
//    val unsupportedTargets = dump.targets.filter { it.targetName !in supportedTargets }
//
////    if (targetsToRemove.isNotEmpty() && parameters.strictValidation.get()) {
////      error("Validation could not be performed as some targets are not available and strictValidation mode is enabled")
////    }
//    dump.remove(unsupportedTargets)

    dump.saveTo(outputApiFile)
  }

  @BCVInternalApi
  companion object {
    private val logger: Logger = Logging.getLogger(KLibMergeWorker::class.java)
  }
}
