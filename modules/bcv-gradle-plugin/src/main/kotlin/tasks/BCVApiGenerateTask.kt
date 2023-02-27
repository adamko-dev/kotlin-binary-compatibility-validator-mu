package dev.adamko.kotlin.binary_compatibility_validator.tasks

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVTarget
import dev.adamko.kotlin.binary_compatibility_validator.workers.BCVSignaturesWorker
import java.io.*
import javax.inject.Inject
import kotlinx.validation.api.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class BCVApiGenerateTask @Inject constructor(
  private val workers: WorkerExecutor,
  private val fs: FileSystemOperations,
) : BCVDefaultTask() {

//  @get:InputFiles
//  @get:Optional
//  @get:PathSensitive(PathSensitivity.RELATIVE)
//  abstract val inputClasses: ConfigurableFileCollection
//
//  @get:InputFile
//  @get:Optional
//  @get:PathSensitive(PathSensitivity.RELATIVE)
//  abstract val inputJar: RegularFileProperty

  @get:Nested
  abstract val targets: NamedDomainObjectContainer<BCVTarget>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputDependencies: ConfigurableFileCollection

  @get:Input
  abstract val ignoredPackages: SetProperty<String>

  @get:Input
  abstract val ignoredMarkers: SetProperty<String>

  @get:Input
  abstract val ignoredClasses: SetProperty<String>

  @get:Classpath
  abstract val runtimeClasspath: ConfigurableFileCollection

  @get:Input
  abstract val projectName: Property<String>

  @get:OutputDirectory
  abstract val outputApiBuildDir: DirectoryProperty

  @TaskAction
  fun generate() {
    val groupedTargets = targets.groupBy { it.platformType.orNull }

    val workQueue = prepareWorkQueue()

    val outputApiBuildDir = outputApiBuildDir.get()
    fs.delete { delete(outputApiBuildDir) }
    outputApiBuildDir.asFile.mkdirs()


    groupedTargets.forEach { (type, targets) ->

      val outputDir = if (groupedTargets.size == 1) {
        outputApiBuildDir
      } else {
        outputApiBuildDir.dir(type?.name ?: "common")
      }

      val inputClasses = targets
        .map(BCVTarget::inputClasses)
        .reduce(FileCollection::plus)

      workQueue.submit(
        inputClasses = inputClasses,
        outputDir = outputDir.asFile,
      )
    }

    // The worker queue is asynchronous, so any code here won't wait for the workers to finish.
    // Any follow-up work must be done in another task
  }

  private fun prepareWorkQueue(): WorkQueue {
    fs.delete { delete(temporaryDir) }
    temporaryDir.mkdirs()

    return workers.classLoaderIsolation {
      classpath.from(runtimeClasspath)
    }
  }

  private fun WorkQueue.submit(
    inputClasses: FileCollection,
    outputDir: File,
  ) {
    val task = this@BCVApiGenerateTask

    @OptIn(BCVInternalApi::class)
    submit(BCVSignaturesWorker::class) worker@{
      this@worker.outputApiDir.set(outputDir)
      this@worker.inputClasses.from(inputClasses)
//      this@worker.inputJar.set(task.inputJar)
      this@worker.ignoredPackages.set(task.ignoredPackages)
      this@worker.ignoredMarkers.set(task.ignoredMarkers)
      this@worker.ignoredClasses.set(task.ignoredClasses)
      this@worker.projectName.set(task.projectName)
    }
  }
}
