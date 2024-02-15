package dev.adamko.kotlin.binary_compatibility_validator.tasks

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.adding
import dev.adamko.kotlin.binary_compatibility_validator.internal.domainObjectContainer
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget
import dev.adamko.kotlin.binary_compatibility_validator.workers.BCVSignaturesWorker
import java.io.*
import javax.inject.Inject
import kotlinx.validation.api.*
import org.gradle.api.*
import org.gradle.api.file.*
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

  @get:OutputDirectory
  abstract val outputApiBuildDir: DirectoryProperty

  @TaskAction
  fun generate() {
    val workQueue = prepareWorkQueue()

    val outputApiBuildDir = outputApiBuildDir.get()
    fs.delete { delete(outputApiBuildDir) }
    outputApiBuildDir.asFile.mkdirs()

    val enabledTargets = targets.asMap.values.filter { it.enabled.getOrElse(true) }

    enabledTargets.forEach { target ->

      val outputDir = if (enabledTargets.size == 1) {
        outputApiBuildDir
      } else {
        outputApiBuildDir.dir(target.platformType)
      }

      workQueue.submit(
        target = target,
        outputDir = outputDir.asFile,
      )
    }

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

  private fun WorkQueue.submit(
    target: BCVTarget,
    outputDir: File,
  ) {
    val task = this@BCVApiGenerateTask

    @OptIn(BCVInternalApi::class)
    submit(BCVSignaturesWorker::class) worker@{
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
}
