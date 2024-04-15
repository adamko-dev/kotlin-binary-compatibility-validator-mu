package dev.adamko.kotlin.binary_compatibility_validator.workers

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVKLibTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.KLibSignatureVersion
import java.io.Serializable
import kotlinx.validation.ExperimentalBCVApi
import kotlinx.validation.api.klib.*
import org.gradle.api.file.DirectoryProperty
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
abstract class KLibSignaturesWorker : WorkAction<KLibSignaturesWorker.Parameters> {

  @BCVInternalApi
  interface Parameters : WorkParameters, Serializable {
    val klib: RegularFileProperty
    val targetName: Property<String>

    val supportedByCurrentHost: Property<Boolean>

    val outputApiDir: DirectoryProperty

    val ignoredPackages: SetProperty<String>
    val ignoredMarkers: SetProperty<String>
    val ignoredClasses: SetProperty<String>

    val signatureVersion: Property<KLibSignatureVersion>

//    val targets: ListProperty<BCVKLibTarget>

    /**
     * [Task path][org.gradle.api.Task.getPath] of the task that invoked this worker,
     * for log messages
     */
    val taskPath: Property<String>
  }

  override fun execute() {
//    val targets = parameters.targets.get()
//    val supportedTargets = targets.filter { it.supportedByCurrentHost.get() }
//
//    targets.forEach { target ->
    if (parameters.supportedByCurrentHost.get()) {
      dumpSupportedTarget()
    } else {
//        inferDumpForUnsupportedTarget(target, supportedTargets)
//      }
    }
  }

  private fun dumpSupportedTarget() {
    val outputFile = parameters.outputApiDir.asFile.get()
      .resolve("${parameters.targetName.get()}.klib.api")

    val dump = KlibDump.fromKlib(
      klibFile = parameters.klib.get().asFile,
      configurableTargetName = parameters.targetName.get(),
      filters = KLibDumpFilters {
        ignoredClasses += parameters.ignoredClasses.get()
        ignoredPackages += parameters.ignoredPackages.get()
        nonPublicMarkers += parameters.ignoredMarkers.get()
        signatureVersion = parameters.signatureVersion.get().convert()
      }
    )

    dump.saveTo(outputFile)
  }

  /**
   * Filter out targets that are unsupported by the current machine from the golden image
   */
  private fun filterSupportedTargets() {

  }

  private fun mergeTargetDumps() {

  }


  private fun inferDumpForUnsupportedTarget(
    target: BCVKLibTarget,
//    supportedTargets: List<BCVKLibTarget>,
  ) {
    val unsupportedTarget = KlibTarget(target.targetName, target.targetName)
//    val supportedTargetNames =
//      supportedTargets.map { KlibTarget(target.targetName, target.targetName) }.toSet()

    // Find a set of supported targets that are closer to unsupported target in the hierarchy.
    // Note that dumps are stored using configurable name, but grouped by the canonical target name.
//    val matchingTargets = findMatchingTargets(supportedTargetNames, unsupportedTarget)
    // Load dumps that are a good fit for inference
//    val supportedTargetDumps = matchingTargets.map { target ->
//      val dumpFile =
//        File(outputApiDir).parentFile.resolve(target.configurableName).resolve(dumpFileName)
//      KlibDump.from(dumpFile, target.configurableName).also {
//        check(it.targets.single() == target)
//      }
//    }

//    // Load an old dump, if any
//    var image: KlibDump? = null
//    if (inputImageFile.exists()) {
//      if (inputImageFile.length() > 0L) {
//        image = KlibDump.from(inputImageFile)
//      } else {
//        logger.warn(
//          "Project's ABI file exists, but empty: $inputImageFile. " +
//              "The file will be ignored during ABI dump inference for the unsupported target " +
//              unsupportedTarget
//        )
//      }
//    }

//    inferAbi(unsupportedTarget, supportedTargetDumps, image).saveTo(outputFile)

//    logger.warn(
//      "An ABI dump for target $unsupportedTarget was inferred from the ABI generated for the following targets " +
//          "as the former target is not supported by the host compiler: " +
//          "[${matchingTargets.joinToString(",")}]. " +
//          "Inferred dump may not reflect an actual ABI for the target $unsupportedTarget. " +
//          "It is recommended to regenerate the dump on the host supporting all required compilation target."
//    )
  }

//  private fun findMatchingTargets(
//    supportedTargets: Set<KlibTarget>,
//    unsupportedTarget: KlibTarget,
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
    private val logger: Logger = Logging.getLogger(KLibSignaturesWorker::class.java)

    private fun KLibSignatureVersion.convert(): KlibSignatureVersion =
      when {
        isLatest() -> KlibSignatureVersion.LATEST
        else       -> KlibSignatureVersion.of(version)
      }

    private fun KlibTarget(configName: String, targetName: String): KlibTarget =
      KlibTarget.parse("${configName}.${targetName}")
  }
}
