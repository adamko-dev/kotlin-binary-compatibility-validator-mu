package dev.adamko.kotlin.binary_compatibility_validator.workers

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import java.io.Serializable
import kotlinx.validation.ExperimentalBCVApi
import kotlinx.validation.KlibValidationSettings
import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.saveTo
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
abstract class KLibExtractWorker : WorkAction<KLibExtractWorker.Parameters> {

  @BCVInternalApi
  interface Parameters : WorkParameters, Serializable {
//    val projectName: Property<String>
    /** Merged KLib dump that should be filtered by this task. */
    val inputAbiFile: RegularFileProperty
    /** A path to the resulting dump file. */
    val outputAbiFile: RegularFileProperty
    /** Provider returning targets supported by the host compiler. */
    val supportedTargets: SetProperty<String>
    /** Refer to [KlibValidationSettings.strictValidation] for details. */
    val strictValidation: Property<Boolean>
    /**
     * [Task path][org.gradle.api.Task.getPath] of the task that invoked this worker,
     * for log messages
     */
    val taskPath: Property<String>
  }

  override fun execute() {
    val inputAbiFile = parameters.inputAbiFile.get().asFile
    val supportedTargets = parameters.supportedTargets.get()
    val strictValidation = parameters.strictValidation.getOrElse(false)
    val outputAbiFile = parameters.outputAbiFile.get().asFile

    if (inputAbiFile.length() == 0L) {
      error("Project ABI file $inputAbiFile is empty")
    }
    val dump = KlibDump.from(inputAbiFile)
    val enabledTargets = supportedTargets.map { KlibTarget.parse(it).targetName }
    // Filter out only unsupported files.
    // That ensures that target renaming will be caught and reported as a change.
    val targetsToRemove = dump.targets.filter { it.targetName !in enabledTargets }
    if (targetsToRemove.isNotEmpty() && strictValidation) {
      error("Validation could not be performed as some targets are not available and strictValidation mode is enabled")
    }
    dump.remove(targetsToRemove)
    dump.saveTo(outputAbiFile)
  }

  @BCVInternalApi
  companion object {
    private val logger: Logger = Logging.getLogger(KLibExtractWorker::class.java)
  }
}
