package dev.adamko.kotlin.binary_compatibility_validator.workers

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import java.io.File
import java.util.jar.JarFile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.validation.api.*
import org.gradle.api.file.*
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@BCVInternalApi
abstract class BCVSignaturesWorker : WorkAction<BCVSignaturesWorker.Parameters> {

  private val logger = Logging.getLogger(this::class.java)

  interface Parameters : WorkParameters {
    val outputApiDir: DirectoryProperty

    val inputClasses: ConfigurableFileCollection
    val inputJar: RegularFileProperty

    val ignoredPackages: SetProperty<String>
    val ignoredMarkers: SetProperty<String>
    val ignoredClasses: SetProperty<String>

    val projectName: Property<String>
  }

  override fun execute() {
    val projectName = parameters.projectName.get()

    val (signaturesCount, duration) = measureTimedValue {
      val signatures = generateSignatures(
        inputJar = parameters.inputJar.orNull,
        inputClasses = parameters.inputClasses,
        ignoredPackages = parameters.ignoredPackages.get(),
        ignoredMarkers = parameters.ignoredMarkers.get(),
        ignoredClasses = parameters.ignoredClasses.get(),
      )

      writeSignatures(
        outputApiDir = parameters.outputApiDir.get().asFile,
        projectName = parameters.projectName.get(),
        signatures = signatures
      )

      signatures.count()
    }

    logger.info("BCVSignaturesWorker generated $signaturesCount signatures for $projectName in $duration")
  }


  private fun generateSignatures(
    inputJar: RegularFile?,
    inputClasses: FileCollection,
    ignoredClasses: Set<String>,
    ignoredMarkers: Set<String>,
    ignoredPackages: Set<String>,
  ): List<ClassBinarySignature> {
    val signatures = when {
      // inputJar takes precedence if specified
      inputJar != null      ->
        JarFile(inputJar.asFile).use { it.loadApiFromJvmClasses() }

      !inputClasses.isEmpty -> {
        logger.info("inputClasses: ${inputClasses.files}")

        val filteredInputClasses = inputClasses.asFileTree.matching {
          exclude("META-INF/**")
          include("**/*.class")
        }

        logger.info("filteredInputClasses: ${filteredInputClasses.files}")

        filteredInputClasses.asSequence()
          .map(File::inputStream)
          .loadApiFromJvmClasses()
      }

      else                  ->
        error("BCVSignaturesWorker should have either inputClassesDirs, or inputJar property set")
    }

    return signatures
      .filterOutNonPublic(ignoredPackages, ignoredClasses)
      .filterOutAnnotated(ignoredMarkers.map { it.replace(".", "/") }.toSet())
  }

  private fun writeSignatures(
    outputApiDir: File,
    projectName: String?,
    signatures: List<ClassBinarySignature>,
  ) {
    outputApiDir.mkdirs()

    outputApiDir
      .resolve("$projectName.api")
      .bufferedWriter().use { writer ->
        signatures
          .sortedBy { it.name }
          .forEach { api ->
            writer.append(api.signature).appendLine(" {")
            api.memberSignatures
              .sortedWith(MEMBER_SORT_ORDER)
              .forEach { writer.append("\t").appendLine(it.signature) }
            writer.appendLine("}\n")
          }
      }
  }

  companion object {
    // can't use kotlin.time.measureTime {} because the implementation isn't stable across Kotlin versions
    private fun <T> measureTimedValue(block: () -> T): Pair<T, Duration> {
      val start = System.nanoTime()
      val value = block()
      val end = System.nanoTime()
      return value to (end - start).nanoseconds
    }
  }
}
