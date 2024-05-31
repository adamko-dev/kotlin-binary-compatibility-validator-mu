package dev.adamko.kotlin.binary_compatibility_validator.workers

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
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

    val outputApiDir: DirectoryProperty

    val ignoredPackages: SetProperty<String>
    val ignoredMarkers: SetProperty<String>
    val ignoredClasses: SetProperty<String>

    val signatureVersion: Property<KLibSignatureVersion>
    val strictValidation: Property<Boolean>

    /**
     * [Task path][org.gradle.api.Task.getPath] of the task that invoked this worker,
     * for log messages
     */
    val taskPath: Property<String>
  }

  override fun execute() {
    val outputFile = parameters.outputApiDir.asFile.get()
      .resolve("${parameters.targetName.get()}.klib.api")

    val filters = KLibDumpFilters {
      ignoredClasses += parameters.ignoredClasses.get()
      ignoredPackages += parameters.ignoredPackages.get()
      nonPublicMarkers += parameters.ignoredMarkers.get()
      signatureVersion = parameters.signatureVersion.get().convert()
    }

    logger.lifecycle("[${parameters.taskPath.get()}:KLibSignaturesWorker] ${filters.toPrettyString()}}")

    val dump = KlibDump.fromKlib(
      klibFile = parameters.klib.get().asFile,
      configurableTargetName = parameters.targetName.get(),
      filters = filters,
    )

    dump.saveTo(outputFile)
  }

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

    private fun KlibDumpFilters.toPrettyString(): String =
      buildList {
        add("ignoredPackages=$ignoredPackages")
        add("ignoredClasses=$ignoredClasses")
        add("nonPublicMarkers=$nonPublicMarkers")
        add("signatureVersion=$signatureVersion")
      }.joinToString(", ", prefix = "KLibDumpFilters(", postfix = ")")
  }
}
