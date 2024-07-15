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

  @get:Input
  abstract val strictKLibTargetValidation: Property<Boolean>

  /**
   * A directory containing any currently existing API Dump files.
   */
//   * Provided by [BCVApiGeneratePreparationTask].
  @get:Internal
  abstract val projectApiDumpDir: DirectoryProperty

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  @get:Optional
  // Gradle sucks and doesn't allow an optional non-existing input dir 🙄
  internal val projectApiDumpDirFiles: List<File>
    get() = projectApiDumpDir.orNull?.asFile?.walk()
      ?.filter { it.isFile }
      ?.toList()?.sorted().orEmpty()

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
    prepareDirectories()

    val workQueue = prepareWorkQueue()

    val enabledTargets = targets.matching { it.enabled.getOrElse(true) }

    logger.lifecycle("[$path] got ${targets.size} targets (${enabledTargets.size} enabled) : ${targets.joinToString { it.name }}")

    val jvmTargets = enabledTargets.withType<BCVJvmTarget>().sorted()
    generateJvmTargets(
      workQueue = workQueue,
      jvmTargets = jvmTargets,
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
    return workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }
  }

  private fun prepareDirectories() {
    fs.delete { delete(outputApiBuildDir) }
    outputApiBuildDir.get().asFile.mkdirs()

    fs.delete { delete(workDir) }
    workDir.mkdirs()
    klibTargetsDir.supported.mkdirs()
    klibTargetsDir.unsupported.mkdirs()
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
    klibTargets: List<BCVKLibTarget>,
  ) {
    if (klibTargets.isEmpty()) {
      logger.info("[$path] No enabled KLib targets")
      return
    }
    logger.lifecycle("[$path] generating ${klibTargets.size} KLib targets : ${klibTargets.joinToString { it.name }}")

    val (supportedKLibTargets, unsupportedKLibTargets) =
      klibTargets.partition { it.supportedByCurrentHost.get() }

    generateSupportedKLibTargets(workQueue, supportedKLibTargets)
    extractSupportedKLibs(workQueue, supportedKLibTargets)
    generateUnsupportedKLibTargets(workQueue, unsupportedKLibTargets)

    val allTargetDumpFiles = buildSet {
      addAll(klibTargetsDir.supported.walk().filter { it.isFile })
      addAll(klibTargetsDir.unsupported.walk().filter { it.isFile })
    }

    mergeDumpFiles(
      workQueue = workQueue,
      allTargetDumpFiles = allTargetDumpFiles,
      outputApiBuildDir = outputApiBuildDir,
      targets = klibTargets,
      strictValidation = strictKLibTargetValidation.get(),
    )

//    workQueue.extract()
  }

  private fun generateSupportedKLibTargets(
    workQueue: WorkQueue,
    supportedTargets: List<BCVKLibTarget>
  ) {
    if (supportedTargets.isEmpty()) {
      logger.info("[$path] No supported enabled KLib targets")
      return
    }
    logger.lifecycle("[$path] generating ${supportedTargets.size} supported KLib targets : ${supportedTargets.joinToString { it.name }}")

    val duration = measureTime {
      supportedTargets.forEach { target ->
        workQueue.submit(
          target = target,
          outputDir = klibTargetsDir.supported,
        )
      }
      workQueue.await()
    }

    logger.lifecycle("[$path] finished generating supported KLib targets in $duration")
  }

  private fun extractSupportedKLibs(
    workQueue: WorkQueue,
    supportedTargets: List<BCVKLibTarget>
  ) {
    if (supportedTargets.isEmpty()) {
      logger.info("[$path] No supported enabled KLib targets for extraction")
      return
    }
    logger.lifecycle("[$path] extracting ${supportedTargets.size} supported KLib targets : ${supportedTargets.joinToString { it.name }}")

    val duration = measureTime {
      workQueue.extract(
        supportedTargets = supportedTargets
      )
      workQueue.await()
    }

    logger.lifecycle("[$path] finished extracting supported KLib targets in $duration")
  }

  private fun generateUnsupportedKLibTargets(
    workQueue: WorkQueue,
    unsupportedTargets: List<BCVKLibTarget>
  ) {
    if (unsupportedTargets.isEmpty()) {
      logger.info("[$path] No unsupported enabled KLib targets")
      return
    }
    logger.lifecycle("[$path] generating ${unsupportedTargets.size} unsupported KLib targets : ${unsupportedTargets.joinToString { it.name }}")

    val duration = measureTime {
      unsupportedTargets.forEach { target ->
        workQueue.inferKLib(
          target = target,
          supportedTargetDumpFiles = klibTargetsDir.supported.walk().filter { it.isFile }.toSet(),
          extantApiDumpFile = projectApiDumpDir.asFile.orNull?.walk()?.filter { it.isFile }
            ?.firstOrNull(),
          outputDir = klibTargetsDir.unsupported,
        )
      }
      workQueue.await()
    }

    logger.lifecycle("[$path] finished generating unsupported KLib targets in $duration")
  }


  private fun mergeDumpFiles(
    workQueue: WorkQueue,
    allTargetDumpFiles: Set<File>,
    outputApiBuildDir: File,
    targets: List<BCVKLibTarget>,
    strictValidation: Boolean,
  ) {
    logger.lifecycle("[$path] merging ${allTargetDumpFiles.size} dump files : ${allTargetDumpFiles.joinToString { it.name }}")

    val duration = measureTime {
      workQueue.merge(
        projectName.get(),
        targetDumpFiles = allTargetDumpFiles,
        outputDir = outputApiBuildDir,
        supportedTargets = targets.filter { it.supportedByCurrentHost.get() }.map { it.targetName },
        strictValidation = strictValidation,
      )
      workQueue.await()
    }

    if (logger.isLifecycleEnabled) {
      val fileNames = outputApiBuildDir.walk().filter { it.isFile }.toList()
      logger.lifecycle("[$path] merged ${allTargetDumpFiles.size} dump files in $duration : $fileNames")
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
//      this@worker.strictValidation.set(target.strictValidation)

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
    supportedTargets: List<String>,
    strictValidation: Boolean,
  ) {
    val task = this@BCVApiGenerateTask

    @OptIn(BCVInternalApi::class)
    submit(KLibMergeWorker::class) worker@{
//      this@worker.projectName.set(projectName)
      this@worker.taskPath.set(task.path)

//      this@worker.outputApiDir.set(outputDir)
      this@worker.outputApiFile.set(
        outputDir.resolve("$projectName.klib.api")
      )

//      this@worker.strictValidation.set(strictValidation)
//      this@worker.supportedTargets.set(supportedTargets)

      this@worker.targetDumpFiles.from(targetDumpFiles)
    }
  }

  @OptIn(BCVExperimentalApi::class)
  private fun WorkQueue.extract(
//    target: BCVKLibTarget,
//    targetDumpFiles: Set<File>,
//    outputDir: File,
    supportedTargets: List<BCVKLibTarget>,
  ) {
    val task = this@BCVApiGenerateTask

    val inputFile = projectApiDumpDir.file(projectName.map { "$it.klib.api" }).get().asFile
    if (!inputFile.exists()) return

    @OptIn(BCVInternalApi::class)
    submit(KLibExtractWorker::class) worker@{
      this@worker.taskPath.set(task.path)
      this@worker.strictValidation.set(strictKLibTargetValidation)

      this@worker.inputAbiFile.set(
        projectApiDumpDir.file(projectName.map { "$it.klib.api" })
      )
//      this@worker.outputAbiFile.set()
      this@worker.supportedTargets.set(
        supportedTargets.map { it.targetName }
      )
      this@worker.outputAbiFile.set(
        klibTargetsDir.extracted.resolve(projectName.map { "$it.klib.api" }.get())
      )
    }
  }
  //endregion
}
