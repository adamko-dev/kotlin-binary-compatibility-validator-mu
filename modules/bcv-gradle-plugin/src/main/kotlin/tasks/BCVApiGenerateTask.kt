package dev.adamko.kotlin.binary_compatibility_validator.tasks

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVExperimentalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.adding
import dev.adamko.kotlin.binary_compatibility_validator.internal.domainObjectContainer
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVKLibTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget
import dev.adamko.kotlin.binary_compatibility_validator.workers.JvmSignaturesWorker
import dev.adamko.kotlin.binary_compatibility_validator.workers.KLibSignaturesWorker
import java.io.File
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
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
//
//  /**
//   * A directory containing a copy of any currently existing API Dump files.
//   * Provided by [BCVApiGeneratePreparationTask].
//   */
//  @get:InputFiles
//  abstract val extantApiDumpDir: DirectoryProperty

  @get:OutputDirectory
  abstract val outputApiBuildDir: DirectoryProperty

  @TaskAction
  fun generate() {
    val workQueue = prepareWorkQueue()

    val outputApiBuildDir = outputApiBuildDir.get()
    fs.delete { delete(outputApiBuildDir) }
    outputApiBuildDir.asFile.mkdirs()

    logger.lifecycle("[$path] got ${targets.size} targets : ${targets.joinToString { it.name }}")

    val enabledTargets = targets.matching { it.enabled.getOrElse(true) }

    logger.lifecycle("[$path] ${enabledTargets.size} enabledTargets : ${enabledTargets.joinToString { it.name }}")

    generateJvmTargets(
      workQueue = workQueue,
      jvmTargets = enabledTargets.withType<BCVJvmTarget>(),
      enabledTargets = enabledTargets.size,
      outputApiBuildDir = outputApiBuildDir,
    )

    generateKLibTargets(
      workQueue = workQueue,
      klibTargets = enabledTargets.withType<BCVKLibTarget>(),
      outputApiBuildDir = outputApiBuildDir,
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

  //region JVM

  private fun generateJvmTargets(
    workQueue: WorkQueue,
    outputApiBuildDir: Directory,
    jvmTargets: Collection<BCVJvmTarget>,
    enabledTargets: Int,
  ) {
    if (jvmTargets.isEmpty()) return
    logger.lifecycle("[$path] generating ${jvmTargets.size} JVM targets : ${jvmTargets.joinToString { it.name }}")

    jvmTargets.forEach { target ->
      val outputDir = if (enabledTargets == 1) {
        outputApiBuildDir
      } else {
        outputApiBuildDir.dir(target.platformType)
      }

      workQueue.submit(
        target = target,
        outputDir = outputDir.asFile,
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
    outputApiBuildDir: Directory,
    klibTargets: Collection<BCVKLibTarget>,
  ) {
    if (klibTargets.isEmpty()) return
    logger.lifecycle("[$path] generating ${klibTargets.size} KLib targets : ${klibTargets.joinToString { it.name }}")

    val (supportedKLibTargets, unsupportedKLibTargets) =
      klibTargets.partition { it.supportedByCurrentHost.get() }
    logger.lifecycle("[$path] generating ${supportedKLibTargets.size} supported KLib targets : ${supportedKLibTargets.joinToString { it.name }}")

    supportedKLibTargets.forEach { target ->
      workQueue.submit(
        target = target,
        outputDir = outputApiBuildDir.asFile,
      )
    }

    workQueue.await()
    logger.lifecycle("[$path] finished generating supported KLib targets.")

    logger.lifecycle("[$path] generating ${unsupportedKLibTargets.size} unsupported KLib targets : ${unsupportedKLibTargets.joinToString { it.name }}")

    supportedKLibTargets.forEach { target ->
      workQueue.submit(
        target = target,
        outputDir = outputApiBuildDir.asFile,
      )
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
      this@worker.supportedByCurrentHost.set(target.supportedByCurrentHost)

//      this@worker.targets.addAll(klibTargets)

      this@worker.ignoredPackages.set(target.ignoredPackages)
      this@worker.ignoredMarkers.set(target.ignoredMarkers)
      this@worker.ignoredClasses.set(target.ignoredClasses)
    }
  }
  //endregion
}
