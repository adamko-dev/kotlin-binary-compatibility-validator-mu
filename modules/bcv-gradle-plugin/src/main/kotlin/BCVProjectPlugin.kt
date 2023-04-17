package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_DIR
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.EXTENSION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.sourceSets
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiCheckTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiDumpTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiGenerateTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVDefaultTask
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.internal.component.external.model.TestFixturesSupport.TEST_FIXTURE_SOURCESET_NAME
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer


abstract class BCVProjectPlugin
@BCVInternalApi
@Inject
constructor(
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
    createJavaTestFixtureTargets(project, extension)
  }

  private fun createExtension(project: Project): BCVProjectExtension {
    val extension = project.extensions.create(EXTENSION_NAME, BCVProjectExtension::class).apply {
      enabled.convention(true)
      outputApiDir.convention(layout.projectDirectory.dir(API_DIR))
      projectName.convention(providers.provider { project.name })
      kotlinxBinaryCompatibilityValidatorVersion.convention("0.13.0")
    }

    extension.targets.configureEach {
      enabled.convention(true)
      ignoredClasses.convention(extension.ignoredClasses)
      ignoredMarkers.convention(
        @Suppress("DEPRECATION")
        extension.ignoredMarkers.orElse(extension.nonPublicMarkers)
      )
      ignoredPackages.convention(extension.ignoredPackages)
    }

    extension.targets.all {
      extension.extensions.add(platformType, this)
    }

    return extension
  }

  private fun createKotlinJvmTargets(
    project: Project,
    extension: BCVProjectExtension,
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
    extension: BCVProjectExtension,
  ) {
    project.pluginManager.withPlugin("kotlin-android") {
      val kotlinSourceSetsContainer = project.extensions.getByType<KotlinSourceSetContainer>()
      kotlinSourceSetsContainer.sourceSets.all {
        extension.targets.create(name) {
          inputClasses.from(kotlin.classesDirectory)
        }
      }
    }
  }

  private fun createKotlinMultiplatformTargets(
    project: Project,
    extension: BCVProjectExtension,
  ) {
    project.pluginManager.withPlugin("kotlin-multiplatform") {
      val kotlinTargetsContainer = project.extensions.getByType<KotlinTargetsContainer>()

      kotlinTargetsContainer.targets
        .matching {
          it.platformType in arrayOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)
        }.all {
          val targetPlatformType = platformType

          extension.targets.register(targetName) {
            enabled.convention(true)
            compilations
              .matching {
                when (targetPlatformType) {
                  KotlinPlatformType.jvm -> it.name == "main"
                  KotlinPlatformType.androidJvm -> it.name == "release"
                  else -> false
                }
              }.all {
                inputClasses.from(output.classesDirs)
              }
          }
        }
    }
  }

  private fun createJavaTestFixtureTargets(
    project: Project,
    extension: BCVProjectExtension,
  ) {
    project.pluginManager.withPlugin("java-test-fixtures") {
      extension.targets.register(TEST_FIXTURE_SOURCESET_NAME) {
        // don't enable by default - requiring an API spec for test-fixtures is pretty unusual
        enabled.convention(false)
        project
          .sourceSets
          .matching { it.name == TEST_FIXTURE_SOURCESET_NAME }
          .all {
            inputClasses.from(output.classesDirs)
          }
      }
    }
  }
}
