package dev.adamko.kotlin.binary_compatibility_validator.workers

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import java.io.Serializable
import kotlinx.validation.ExperimentalBCVApi
import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.inferAbi
import kotlinx.validation.api.klib.saveTo
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

/**
 * Infers a possible KLib ABI dump for an unsupported target.
 *
 * To infer a dump, walk up the default targets hierarchy tree starting from the unsupported
 * target until it finds a node corresponding to a group containing least one supported target.
 *
 * After that, dumps generated for such supported targets are merged and declarations that are
 * common to all of them are considered as a common ABI that most likely will be shared by the
 * unsupported target.
 *
 * At the next step, if a project contains an old dump, declarations specific to the unsupported
 * target are copied from it and merged into the common ABI extracted previously.
 *
 * The resulting dump is then used as an inferred dump for the unsupported target.
 */
@BCVInternalApi
@BCVExperimentalApi
@OptIn(ExperimentalBCVApi::class)
abstract class KLibInferSignaturesWorker : WorkAction<KLibInferSignaturesWorker.Parameters> {

  @BCVInternalApi
  interface Parameters : WorkParameters, Serializable {
    val targetName: Property<String>

    val outputApiDir: DirectoryProperty

    val supportedTargetDumpFiles: ConfigurableFileCollection
    val extantApiDumpFile: RegularFileProperty

    /**
     * [Task path][org.gradle.api.Task.getPath] of the task that invoked this worker,
     * for log messages
     */
    val taskPath: Property<String>
  }

  private val taskPath: String = parameters.taskPath.get()

  override fun execute() {
    // Find a set of supported targets that are closer to unsupported target in the hierarchy.
    // Note that dumps are stored using configurable name, but grouped by the canonical target name.
//    val matchingTargets = findMatchingTargets(availableDumps.keys, target.get())
    // Load dumps that are a good fit for inference
    val supportedTargetDumps =
      parameters.supportedTargetDumpFiles.asFileTree.map { dumpFile ->
        KlibDump.from(dumpFile, dumpFile.name.substringBefore(".klib.api"))
//          .also {
//            check(it.targets.single() == target)
//          }
      }

    val extantApiDumpFile = parameters.extantApiDumpFile.orNull?.asFile

    // Load an old dump, if any
    val extantImage: KlibDump? =
      extantApiDumpFile?.let { extantApiDump ->
        KlibDump.from(extantApiDump)
      }
    if (extantImage == null) {
      logger.warn(
        "[$taskPath] Project's ABI file exists, but empty: ${extantApiDumpFile}. " +
            "The file will be ignored during ABI dump inference for the unsupported target "
//            +  target.get()
      )
    }

    val target = KlibTarget.parse(parameters.targetName.get())
    inferAbi(target, supportedTargetDumps, extantImage)
      .saveTo(parameters.outputApiDir.get().asFile.resolve(parameters.targetName.get()))

//    logger.warn(
//      "An ABI dump for target ${target.get()} was inferred from the ABI generated for the following targets " +
//          "as the former target is not supported by the host compiler: " +
//          "[${matchingTargets.joinToString(",")}]. " +
//          "Inferred dump may not reflect an actual ABI for the target ${target.get()}. " +
//          "It is recommended to regenerate the dump on the host supporting all required compilation target."
//    )
  }


//  private fun findMatchingTargets(
//    supportedTargets: Set<KlibTarget>,
//    unsupportedTarget: KlibTarget
//  ): Collection<KlibTarget> {
//    var currentGroup: String? = unsupportedTarget.targetName
//    while (currentGroup != null) {
//      // If a current group has some supported targets, use them.
//      val groupTargets = TargetHierarchy.targets(currentGroup)
//      val matchingTargets = supportedTargets.filter { groupTargets.contains(it.targetName) }
//      if (matchingTargets.isNotEmpty()) {
//        return matchingTargets
//      }
//      // Otherwise, walk up the target hierarchy.
//      currentGroup = TargetHierarchy.parent(currentGroup)
//    }
//    throw IllegalStateException(
//      "The target $unsupportedTarget is not supported by the host compiler " +
//          "and there are no targets similar to $unsupportedTarget to infer a dump from it."
//    )
//  }

  @BCVInternalApi
  companion object {
    private val logger: Logger = Logging.getLogger(KLibInferSignaturesWorker::class.java)
  }
}
