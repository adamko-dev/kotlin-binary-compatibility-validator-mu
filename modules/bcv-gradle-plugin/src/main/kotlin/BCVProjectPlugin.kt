package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.internal.sourceSets
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiCheckTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiDumpTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiGenerateTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVDefaultTask
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType


abstract class BCVProjectPlugin @Inject constructor(
  private val providers: ProviderFactory,
  private val layout: ProjectLayout,
) : Plugin<Project> {

  override fun apply(project: Project) {
    // apply the base plugin so the 'check' task is available
    project.pluginManager.apply(LifecycleBasePlugin::class)

    val extension = createExtension(project)

    val bcvGenerateClasspath =
      project.configurations.register(RUNTIME_CLASSPATH_CONFIGURATION_NAME) {
        isCanBeConsumed = false
        isCanBeResolved = true
        isVisible = false
        defaultDependencies {
          addLater(
            extension.kotlinxBinaryCompatibilityValidatorVersion.map { version ->
              project.dependencies.create(
                "org.jetbrains.kotlinx:binary-compatibility-validator:$version"
              )
            }
          )
        }
      }

    project.tasks.withType<BCVDefaultTask>().configureEach {
      bcvEnabled.convention(extension.enabled)
      onlyIf("BCV is disabled") { bcvEnabled.get() }
    }

    project.tasks.withType<BCVApiGenerateTask>().configureEach {
      runtimeClasspath.from(bcvGenerateClasspath.map { it.incoming.files })
      targets.addAllLater(providers.provider { extension.targets })
      onlyIf("Must have at least one target") { targets.isNotEmpty() }
      outputApiBuildDir.convention(layout.buildDirectory.dir("bcv-api"))
      projectName.convention(extension.projectName)
    }

    project.tasks.withType<BCVApiCheckTask>().configureEach {
      outputs.dir(temporaryDir) // dummy output, so up-to-date checks work
      expectedProjectName.convention(extension.projectName)
      expectedApiDirPath.convention(extension.outputApiDir.map { it.asFile.canonicalFile.absolutePath })
    }

    project.tasks.withType<BCVApiDumpTask>().configureEach {
      apiDirectory.convention(extension.outputApiDir)
    }

    val apiGenerateTask = project.tasks.register("apiGenerate", BCVApiGenerateTask::class)

    project.tasks.register("apiDump", BCVApiDumpTask::class) {
      apiDumpFiles.from(apiGenerateTask.map { it.outputApiBuildDir })
    }

    val apiCheckTask = project.tasks.register("apiCheck", BCVApiCheckTask::class) {
      apiBuildDir.convention(apiGenerateTask.flatMap { it.outputApiBuildDir })
    }

    project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
      dependsOn(apiCheckTask)
    }

    createKotlinJvmTargets(project, extension)
    createKotlinAndroidTargets(project, extension)
    createKotlinMultiplatformTargets(project, extension)
  }

  private fun createExtension(project: Project): BCVExtension {
    val extension = project.extensions.create(EXTENSION_NAME, BCVExtension::class).apply {
      enabled.convention(true)
      ignoredPackages.convention(emptySet())
      ignoredMarkers.convention(emptySet())
      ignoredClasses.convention(emptySet())
      outputApiDir.convention(layout.projectDirectory.dir(API_DIR))
      projectName.convention(providers.provider { project.name })
      kotlinxBinaryCompatibilityValidatorVersion.convention("0.13.0")
    }

    extension.targets.configureEach {
      enabled.convention(true)
      ignoredClasses.convention(extension.ignoredClasses)
      ignoredMarkers.convention(extension.ignoredMarkers.apply {
        @Suppress("DEPRECATION")
        addAll(extension.nonPublicMarkers)
      })
      ignoredPackages.convention(extension.ignoredPackages)
    }

    extension.targets.all {
      extension.extensions.add(platformType, this)
    }

    return extension
  }

  private fun createKotlinJvmTargets(
    project: Project,
    extension: BCVExtension,
  ) {
    project.pluginManager.withPlugin("kotlin") {
      extension.targets.create("kotlinJvm") {
        project
          .sourceSets
          .matching { it.name == SourceSet.MAIN_SOURCE_SET_NAME }
          .all {
            inputClasses.from(output.classesDirs)
          }
      }
    }
  }

  private fun createKotlinAndroidTargets(
    project: Project,
    extension: BCVExtension,
  ) {
    project.pluginManager.withPlugin("kotlin-android") {
      val androidExtension = project.extensions.getByType<KotlinAndroidProjectExtension>()

      extension.targets.create(androidExtension.target.name) {
        androidExtension.target.compilations.all {
          inputClasses.from(this)
        }
      }
    }
  }

  private fun createKotlinMultiplatformTargets(
    project: Project,
    extension: BCVExtension,
  ) {
    project.pluginManager.withPlugin("kotlin-multiplatform") {
      val kotlinExtension = project.extensions.getByType<KotlinMultiplatformExtension>()
      val kotlinJvmTargets = kotlinExtension.targets.matching {
        it.platformType in arrayOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)
      }

      kotlinJvmTargets.all {
        extension.targets.create(targetName) {
          enabled.convention(true)
          compilations.all {
            inputClasses.from(output.classesDirs)
          }
        }
      }
    }
  }

//  private fun configureMultiplatformPlugin(
//    project: Project,
//    extension: BCVExtension
//  ) = project.pluginManager.withPlugin("kotlin-multiplatform") {
//    val kotlinExtension = project.extensions.getByType<KotlinMultiplatformExtension>()
//
//    // Create common tasks for multiplatform
//    val commonApiDump = project.tasks.register("apiDump") {
//      group = "other"
//      description = "Task that collects all target specific dump tasks"
//    }
//
//    val commonApiCheck: TaskProvider<Task> = project.tasks.register("apiCheck") {
//      group = LifecycleBasePlugin.VERIFICATION_GROUP
//      description = "Shortcut task that depends on all specific check tasks"
//    }
//
//    project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
//      dependsOn(commonApiCheck)
//    }
//
//    val kotlinJvmTargets = kotlinExtension.targets.matching {
//      it.platformType in arrayOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)
//    }
//
//    kotlinJvmTargets.all {
//      extension.targets.create(name) {
//        compilations.all {
//          inputClasses.from(this)
//        }
//      }
//    }
//
//    val jvmTargetCountProvider = project.provider { kotlinJvmTargets.count() }
//
//    val dirConfig = jvmTargetCountProvider.map {
//      if (it == 1) DirConfig.COMMON else DirConfig.TARGET_DIR
//    }
//
//    kotlinJvmTargets.all {
//      val target = this
//      val targetConfig = TargetConfig(project, extension, target.name, dirConfig)
//      when (target.platformType) {
//        KotlinPlatformType.jvm ->
//          target.compilations
//            .matching { target.name == KotlinCompilation.MAIN_COMPILATION_NAME }
//            .all {
//              project.configureKotlinCompilation(
//                this,
//                extension,
//                targetConfig,
//                commonApiDump,
//                commonApiCheck
//              )
//            }
//
//        KotlinPlatformType.androidJvm ->
//          target.compilations
//            .matching { target.name == "release" }
//            .all {
//              project.configureKotlinCompilation(
//                this,
//                extension,
//                targetConfig,
//                commonApiDump,
//                commonApiCheck,
//                useOutput = true
//              )
//            }
//
//        else -> error("only expected JVM platforms, but got ${target.platformType}")
//      }
//    }
//  }
//
//  private fun configureAndroidPlugin(
//    project: Project,
//    extension: BCVExtension
//  ) = project.pluginManager.withPlugin("kotlin-android") {
//    val androidExtension = project.extensions
//      .getByName("kotlin") as KotlinAndroidProjectExtension
//    androidExtension.target.compilations
//      .matching { it.compilationName == "release" }
//      .all {
//        project.configureKotlinCompilation(this, extension, useOutput = true)
//      }
//  }
//
//  private fun configureKotlinPlugin(
//    project: Project,
//    extension: BCVExtension
//  ) = project.pluginManager.withPlugin("kotlin") {
//    project.sourceSets
//      .matching { it.name != SourceSet.MAIN_SOURCE_SET_NAME }
//      .all {
//        project.configureApiTasks(
//          this,
//          extension,
//          TargetConfig(project, extension)
//        )
//      }
//  }

  companion object {
    const val API_DIR = "api"
    const val EXTENSION_NAME = "bcvMu"
    const val TASK_GROUP = "bcv mu"
    const val RUNTIME_CLASSPATH_CONFIGURATION_NAME = "bcvMuRuntime"
  }
}


//private class TargetConfig(
//  project: Project,
//  extension: BCVExtension,
//  val targetName: String? = null,
//  dirConfig: Provider<DirConfig> = project.provider { DirConfig.COMMON }
//) {
//
////    private val API_DIR_PROVIDER = project.provider { API_DIR }
//
//  fun apiTaskName(suffix: String) = when (targetName) {
//    null, "" -> "api$suffix"
//    else     -> "${targetName}Api$suffix"
//  }
//
//  val buildTaskName = apiTaskName("Build")
//  val dumpTaskName = apiTaskName("Dump")
//  val checkTaskName = apiTaskName("Check")
//
//  val targetApiDir: Provider<Directory> =
//    extension.outputApiDir.zip(dirConfig.orElse(DirConfig.COMMON)) { apiDir, dirConfig ->
//      when {
//        dirConfig == DirConfig.TARGET_DIR && !targetName.isNullOrBlank() ->
//          apiDir.dir(targetName)
//
//        dirConfig == DirConfig.COMMON                                    -> apiDir
//        else                                                             -> apiDir
//      }
//    }
//}
//
//private enum class DirConfig {
//  /**
//   * `api` directory for .api files.
//   * Used in single target projects
//   */
//  COMMON,
//
//  /**
//   * Target-based directory, used in multitarget setups.
//   * E.g. for the project with targets jvm and android,
//   * the resulting paths will be
//   * `/api/jvm/project.api` and `/api/android/project.api`
//   */
//  TARGET_DIR,
//}
//
//private fun Project.configureKotlinCompilation(
//  compilation: KotlinCompilation<KotlinCommonOptions>,
//  extension: BCVExtension,
//  targetConfig: TargetConfig = TargetConfig(this, extension),
//  commonApiDump: TaskProvider<Task>? = null,
//  commonApiCheck: TaskProvider<Task>? = null,
//  useOutput: Boolean = false,
//) {
//  val projectName = project.name
////    val apiDirProvider = targetConfig.apiDir
////    val apiBuildDir = apiDirProvider.map { buildDir.resolve(it) }
//
//  val apiBuild = tasks.register<BCVApiGenerateTask>(targetConfig.buildTaskName) {
//    // Do not enable task for empty umbrella modules
////        isEnabled =
////            apiCheckEnabled(projectName, extension)
////                && compilation.allKotlinSourceSets.any { it.kotlin.srcDirs.any { it.exists() } }
//    // 'group' is not specified deliberately, so it will be hidden from ./gradlew tasks
//    description =
//      "Builds Kotlin API for 'main' compilations of $projectName. Complementary task and shouldn't be called manually"
//    inputs.files(compilation.output.classesDirs)
////        if (useOutput) {
////            // Workaround for #4
////            inputClassesDirs =
////                files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
////            inputDependencies =
////                files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
////        } else {
////            inputClassesDirs =
////                files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
////            inputDependencies =
////                files(provider<Any> { if (isEnabled) compilation.compileDependencyFiles else emptyList<Any>() })
////        }
//    outputApiBuildDir.set(targetConfig.targetApiDir)
//  }
//  configureCheckTasks(
//    apiBuild,
//    extension,
//    targetConfig,
//    commonApiDump,
//    commonApiCheck
//  )
//}
//
//private fun Project.configureApiTasks(
//  sourceSet: SourceSet,
//  extension: BCVExtension,
//  targetConfig: TargetConfig = TargetConfig(this, extension),
//) {
////    val apiBuildDir = targetConfig.apiDir.map { buildDir.resolve(it) }
//  val apiBuild =
//    // 'group' is not specified deliberately, so it will be hidden from ./gradlew tasks
//    tasks.register<BCVApiGenerateTask>(targetConfig.buildTaskName) {
////            isEnabled = apiCheckEnabled(projectName, extension)
//      // 'group' is not specified deliberately, so it will be hidden from ./gradlew tasks
//      description =
//        "Builds Kotlin API for 'main' compilations. Complementary task and shouldn't be called manually"
//      inputs.files(sourceSet.output.classesDirs)
//
////            inputClassesDirs =
////                files(provider<Any> { if (isEnabled) sourceSet.output.classesDirs else emptyList<Any>() })
////            inputDependencies =
////                files(provider<Any> { if (isEnabled) sourceSet.output.classesDirs else emptyList<Any>() })
////            outputApiDir = apiBuildDir.get()
//
//      outputApiBuildDir.set(temporaryDir)
//    }
//
//  configureCheckTasks(apiBuild, extension, targetConfig)
//}
//
//private fun Project.configureCheckTasks(
//  apiBuild: TaskProvider<BCVApiGenerateTask>,
//  extension: BCVExtension,
//  targetConfig: TargetConfig,
//  commonApiDump: TaskProvider<Task>? = null,
//  commonApiCheck: TaskProvider<Task>? = null,
//) {
//  val projectName = project.name
//
//  val apiCheck = tasks.register<BCVApiCheckTask>(targetConfig.checkTaskName) {
////        isEnabled =
////            apiCheckEnabled(projectName, extension) && apiBuild.map { it.enabled }.getOrElse(true)
//    group = LifecycleBasePlugin.VERIFICATION_GROUP
//    description =
//      "Checks signatures of public API against the golden value in API folder for $projectName"
//    projectApiDir = targetConfig.targetApiDir
//    apiBuildDir = apiBuild.flatMap { it.outputApiBuildDir }
////        compareApiDumps(apiReferenceDir = apiCheckDir.get(), apiBuildDir = apiBuildDir.get())
//    dependsOn(apiBuild)
//  }
//
//  val apiDump = tasks.register<BCVApiDumpTask>(targetConfig.dumpTaskName) {
//    setGroup(null)
//    description = "Syncs API from build dir to API dir for $projectName"
//    apiDumpFiles.from(apiBuild.flatMap { it.outputApiBuildDir })
//    apiDirectory = (targetConfig.targetApiDir)
//  }
//
//  commonApiDump?.configure { dependsOn(apiDump) }
//
//  when (commonApiCheck) {
//    null -> project.tasks.named("check").configure { dependsOn(apiCheck) }
//    else -> commonApiCheck.configure { dependsOn(apiCheck) }
//  }
//}
//
////internal val Project.apiValidationExtensionOrNull: ApiValidationExtension?
////  get() =
////    generateSequence(this) { it.parent }
////      .map { it.extensions.findByType(ApiValidationExtension::class.java) }
////      .firstOrNull { it != null }
