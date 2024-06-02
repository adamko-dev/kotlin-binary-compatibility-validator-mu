package dev.adamko.kotlin.binary_compatibility_validator.tasks

import dev.adamko.kotlin.binary_compatibility_validator.internal.*
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVKLibTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget
import dev.adamko.kotlin.binary_compatibility_validator.workers.*
import java.io.File
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.*
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class BCVApiGenerateTask
@BCVInternalApi
@Inject
constructor(
  private val workers: WorkerExecutor,
  private val fs: FileSystemOperations,
  private val objects: ObjectFactory,
) : BCVDefaultTask() {

  @get:Nested
  val targets: NamedDomainObjectContainer<BCVTarget> =
    extensions.adding("targets") { objects.domainObjectContainer() }

  @get:Internal
  @Deprecated("inputDependencies was unused and can be removed without impact")
  @Suppress("unused")
  abstract val inputDependencies: ConfigurableFileCollection

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Input
  abstract val projectName: Property<String>

  /**
   * A directory containing a copy of any currently existing API Dump files.
   * Provided by [BCVApiGeneratePreparationTask].
   */
  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val extantApiDumpDir: DirectoryProperty

  @get:OutputDirectory
  abstract val outputApiBuildDir: DirectoryProperty

  @get:LocalState
  internal val workDir: File get() = temporaryDir

  private val klibTargetsDir = object {
    private val klibDir: File get() = workDir.resolve("klib")
    val supported: File get() = klibDir.resolve("supported")
    val unsupported: File get() = klibDir.resolve("unsupported")
    val extracted: File get() = klibDir.resolve("extracted")
  }

  @TaskAction
  fun generate() {
    val workQueue = prepareWorkQueue()

    prepareDirectories()

    logger.lifecycle("[$path] got ${targets.size} targets : ${targets.joinToString { it.name }}")

    val enabledTargets = targets.matching { it.enabled.getOrElse(true) }

    generateJvmTargets(
      workQueue = workQueue,
      jvmTargets = enabledTargets.withType<BCVJvmTarget>().sorted(),
      outputApiBuildDir = outputApiBuildDir.get().asFile,
    )

    // TODO log when klib file doesn't exist
    // TODO log warning when klibFile has >1 file
    val klibTargets = enabledTargets.withType<BCVKLibTarget>()
      .filter { it.klibFile.singleOrNull()?.exists() == true }
      .sorted()

    generateKLibTargets(
      workQueue = workQueue,
      klibTargets = klibTargets,
      outputApiBuildDir = outputApiBuildDir.get().asFile,
    )

    // The worker queue is asynchronous, so any code here won't wait for the workers to finish.
    // Any follow-up work must be done in another task.
  }


  private fun prepareWorkQueue(): WorkQueue {
    fs.delete { delete(temporaryDir) }
    temporaryDir.mkdirs()

    return workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }
  }

  private fun prepareDirectories() {
    val outputApiBuildDir = outputApiBuildDir.get()
    fs.delete { delete(outputApiBuildDir) }
    outputApiBuildDir.asFile.mkdirs()

    fs.delete { delete(klibTargetsDir.supported) }
    klibTargetsDir.supported.mkdirs()

    fs.delete { delete(klibTargetsDir.unsupported) }
    klibTargetsDir.unsupported.mkdirs()

    fs.delete { delete(klibTargetsDir.extracted) }
    klibTargetsDir.extracted.mkdirs()
  }

  //region JVM

  private fun generateJvmTargets(
    workQueue: WorkQueue,
    outputApiBuildDir: File,
    jvmTargets: Collection<BCVJvmTarget>,
  ) {
    if (jvmTargets.isEmpty()) {
      logger.info("[$path] No enabled JVM targets")
      return
    }

    logger.lifecycle("[$path] generating ${jvmTargets.size} JVM targets : ${jvmTargets.joinToString { it.name }}")

    jvmTargets.forEach { target ->
      val outputDir = if (jvmTargets.size == 1) {
        outputApiBuildDir
      } else {
        outputApiBuildDir.resolve(target.platformType)
      }

      workQueue.submit(
        target = target,
        outputDir = outputDir,
      )
    }
  }

  private fun WorkQueue.submit(
    target: BCVJvmTarget,
    outputDir: File,
  ) {
    val task = this@BCVApiGenerateTask

    @OptIn(BCVInternalApi::class)
    submit(JvmSignaturesWorker::class) worker@{
      this@worker.projectName.set(task.projectName)
      this@worker.taskPath.set(task.path)

      this@worker.outputApiDir.set(outputDir)

      this@worker.inputClasses.from(target.inputClasses)
      this@worker.inputJar.set(target.inputJar)

      this@worker.publicMarkers.set(target.publicMarkers)
      this@worker.publicPackages.set(target.publicPackages)
      this@worker.publicClasses.set(target.publicClasses)

      this@worker.ignoredPackages.set(target.ignoredPackages)
      this@worker.ignoredMarkers.set(target.ignoredMarkers)
      this@worker.ignoredClasses.set(target.ignoredClasses)
    }
  }
  //endregion


  //region KLib

  private fun generateKLibTargets(
    workQueue: WorkQueue,
    outputApiBuildDir: File,
    klibTargets: Collection<BCVKLibTarget>,
  ) {
    if (klibTargets.isEmpty()) {
      logger.info("[$path] No enabled KLib targets")
      return
    }
    logger.lifecycle("[$path] generating ${klibTargets.size} KLib targets : ${klibTargets.joinToString { it.name }}")

    val (supportedKLibTargets, unsupportedKLibTargets) =
      klibTargets.partition { it.supportedByCurrentHost.get() }

    generateSupportedKLibTargets(workQueue, supportedKLibTargets)
    generateUnsupportedKLibTargets(workQueue, unsupportedKLibTargets)

    workQueue.await()

    val allTargetDumpFiles =
      klibTargetsDir.supported.walk().filter { it.isFile }.toSet() union
          klibTargetsDir.unsupported.walk().filter { it.isFile }.toSet()

    mergeDumpFiles(workQueue, allTargetDumpFiles, outputApiBuildDir)
  }

  private fun generateSupportedKLibTargets(
    workQueue: WorkQueue,
    supportedKLibTargets: List<BCVKLibTarget>
  ) {
    logger.lifecycle("[$path] generating ${supportedKLibTargets.size} supported KLib targets : ${supportedKLibTargets.joinToString { it.name }}")

    val duration = measureTime {
      supportedKLibTargets.forEach { target ->
        workQueue.submit(
          target = target,
          outputDir = klibTargetsDir.supported,
        )
      }
      workQueue.await()
    }

    logger.lifecycle("[$path] finished generating supported KLib targets in $duration")
  }

  private fun generateUnsupportedKLibTargets(
    workQueue: WorkQueue,
    unsupportedKLibTargets: List<BCVKLibTarget>
  ) {
    logger.lifecycle("[$path] generating ${unsupportedKLibTargets.size} unsupported KLib targets : ${unsupportedKLibTargets.joinToString { it.name }}")

    val duration = measureTime {
      unsupportedKLibTargets.forEach { target ->
        workQueue.inferKLib(
          target = target,
          supportedTargetDumpFiles = klibTargetsDir.supported.walk().filter { it.isFile }.toSet(),
          extantApiDumpFile = extantApiDumpDir.asFile.orNull?.walk()?.filter { it.isFile }
            ?.firstOrNull(),
          outputDir = klibTargetsDir.unsupported,
        )
      }
    }

    logger.lifecycle("[$path] finished generating unsupported KLib targets in $duration")
  }


  private fun mergeDumpFiles(
    workQueue: WorkQueue,
    allTargetDumpFiles: Set<File>,
    outputApiBuildDir: File
  ) {
    logger.lifecycle("[$path] merging ${allTargetDumpFiles.size} dump files : ${allTargetDumpFiles.joinToString { it.name }}")

    workQueue.merge(
      projectName.get(),
      targetDumpFiles = allTargetDumpFiles,
      outputDir = outputApiBuildDir,
    )
    workQueue.await()

    if (logger.isLifecycleEnabled) {
      val fileNames = outputApiBuildDir.walk().filter { it.isFile }.toList()
      logger.lifecycle("[$path] merged ${allTargetDumpFiles.size} dump files : $fileNames")
    }
  }


  @OptIn(BCVExperimentalApi::class)
  private fun WorkQueue.submit(
    target: BCVKLibTarget,
    outputDir: File,
  ) {
    val task = this@BCVApiGenerateTask

    @OptIn(BCVInternalApi::class)
    submit(KLibSignaturesWorker::class) worker@{
      this@worker.targetName.set(target.targetName)
      this@worker.taskPath.set(task.path)

      this@worker.outputApiDir.set(outputDir)

      this@worker.klib.set(target.klibFile.singleFile)
      this@worker.signatureVersion.set(target.signatureVersion)
      this@worker.strictValidation.set(target.strictValidation)
//      this@worker.supportedByCurrentHost.set(target.supportedByCurrentHost)

//      this@worker.targets.addAll(klibTargets)

      this@worker.ignoredPackages.set(target.ignoredPackages)
      this@worker.ignoredMarkers.set(target.ignoredMarkers)
      this@worker.ignoredClasses.set(target.ignoredClasses)
    }
  }

  @OptIn(BCVExperimentalApi::class)
  private fun WorkQueue.inferKLib(
    target: BCVKLibTarget,
    supportedTargetDumpFiles: Set<File>,
    extantApiDumpFile: File?,
    outputDir: File,
  ) {
    val task = this@BCVApiGenerateTask

    @OptIn(BCVInternalApi::class)
    submit(KLibInferSignaturesWorker::class) worker@{
      this@worker.targetName.set(target.name)
      this@worker.taskPath.set(task.path)

      this@worker.outputApiDir.set(outputDir)

      this@worker.supportedTargetDumpFiles.from(supportedTargetDumpFiles)
      this@worker.extantApiDumpFile.set(extantApiDumpFile)
    }
  }

  @OptIn(BCVExperimentalApi::class)
  private fun WorkQueue.merge(
    projectName: String,
    targetDumpFiles: Set<File>,
    outputDir: File,
  ) {
    val task = this@BCVApiGenerateTask

    @OptIn(BCVInternalApi::class)
    submit(KLibMergeWorker::class) worker@{
      this@worker.projectName.set(projectName)
      this@worker.taskPath.set(task.path)

      this@worker.outputApiDir.set(outputDir)

      this@worker.targetDumpFiles.from(targetDumpFiles)
    }
  }

  @OptIn(BCVExperimentalApi::class)
  private fun WorkQueue.extract(
    target: BCVKLibTarget,
    targetDumpFiles: Set<File>,
    outputDir: File,
  ) {
    val task = this@BCVApiGenerateTask

    @OptIn(BCVInternalApi::class)
    submit(KLibExtractWorker::class) worker@{
      this@worker.taskPath.set(task.path)
      this@worker.strictValidation.set(target.strictValidation)

//      this@worker.inputAbiFile.set()
//      this@worker.outputAbiFile.set()
//      this@worker.supportedTargets.set()
    }
  }
  //endregion
}
