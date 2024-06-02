package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_CHECK_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_DIR
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_DUMP_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_GENERATE_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.EXTENSION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.PREPARE_API_GENERATE_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.RUNTIME_CLASSPATH_RESOLVER_CONFIGURATION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.adapters.createJavaTestFixtureTargets
import dev.adamko.kotlin.binary_compatibility_validator.adapters.createKotlinAndroidTargets
import dev.adamko.kotlin.binary_compatibility_validator.adapters.createKotlinJvmTargets
import dev.adamko.kotlin.binary_compatibility_validator.adapters.createKotlinMultiplatformTargets
import dev.adamko.kotlin.binary_compatibility_validator.internal.*
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVKLibTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.KLibSignatureVersion
import dev.adamko.kotlin.binary_compatibility_validator.tasks.*
import java.io.File
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME


abstract class BCVProjectPlugin
@BCVInternalApi
@Inject
constructor(
  private val providers: ProviderFactory,
  private val layout: ProjectLayout,
  private val objects: ObjectFactory,
) : Plugin<Project> {

  override fun apply(project: Project) {
    // apply the base plugin so the 'check' task is available
    project.pluginManager.apply(LifecycleBasePlugin::class)

    val extension = createExtension(project)

    val bcvGenerateClasspath = createBcvMuClasspath(project, extension)

    configureBcvTaskConventions(project, extension, bcvGenerateClasspath)

    registerBcvTasks(project)

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
      kotlinxBinaryCompatibilityValidatorVersion.convention(BCVProperties.bcvVersion)
      kotlinCompilerEmbeddableVersion.convention(BCVProperties.kotlinVersion)

      // have to set conventions because otherwise .add("...") doesn't work
      ignoredMarkers.convention(emptyList())
      publicPackages.convention(emptyList())
      publicClasses.convention(emptyList())
      publicMarkers.convention(emptyList())
      ignoredClasses.convention(emptyList())
      @Suppress("DEPRECATION")
      nonPublicMarkers.convention(null)

      @OptIn(BCVExperimentalApi::class)
      klib.apply {
        enabled.convention(false)
        signatureVersion.convention(KLibSignatureVersion.Latest)
        strictValidation.convention(false)
      }
    }

    extension.targets.apply {
      registerBinding(BCVJvmTarget::class, BCVJvmTarget::class)
      registerBinding(BCVKLibTarget::class, BCVKLibTarget::class)

      withType<BCVJvmTarget>().configureEach {
        enabled.convention(true)
        inputClasses.setFrom(emptyList<File>())
        inputJar.convention(null)
      }

      @OptIn(BCVExperimentalApi::class)
      withType<BCVKLibTarget>().configureEach {
        enabled.convention(extension.klib.enabled)

        signatureVersion.convention(extension.klib.signatureVersion)
        strictValidation.convention(extension.klib.strictValidation)
        supportedByCurrentHost.convention(false)
      }

      configureEach {
        publicMarkers.convention(extension.publicMarkers)
        publicPackages.convention(extension.publicPackages)
        publicClasses.convention(extension.publicClasses)

        ignoredClasses.convention(extension.ignoredClasses)
        ignoredMarkers.convention(
          @Suppress("DEPRECATION")
          extension.ignoredMarkers.orElse(extension.nonPublicMarkers)
        )
        ignoredPackages.convention(extension.ignoredPackages)
      }
    }

    return extension
  }


  private fun createBcvMuClasspath(
    project: Project,
    extension: BCVProjectExtension,
  ): NamedDomainObjectProvider<Configuration> {

    val bcvGenerateClasspath =
      project.configurations.register(RUNTIME_CLASSPATH_CONFIGURATION_NAME) {
        description = "Runtime classpath for running binary-compatibility-validator."
        declarable()
        defaultDependencies {
          addLater(
            extension.kotlinxBinaryCompatibilityValidatorVersion.map { version ->
              project.dependencies.create(
                "org.jetbrains.kotlinx:binary-compatibility-validator:$version"
              )
            }
          )
          addLater(
            extension.kotlinCompilerEmbeddableVersion.map { version ->
              project.dependencies.create(
                "org.jetbrains.kotlin:kotlin-compiler-embeddable:$version"
              )
            }
          )
        }
      }

    return project.configurations.register(RUNTIME_CLASSPATH_RESOLVER_CONFIGURATION_NAME) {
      description = "Resolve the runtime classpath for running binary-compatibility-validator."
      resolvable()
      extendsFrom(bcvGenerateClasspath.get())
    }
  }


  private fun configureBcvTaskConventions(
    project: Project,
    extension: BCVProjectExtension,
    bcvGenerateClasspath: NamedDomainObjectProvider<Configuration>
  ) {
    project.tasks.withType<BCVDefaultTask>().configureEach {
      bcvEnabled.convention(extension.enabled)

      onlyIf("BCV is enabled") { bcvEnabled.get() }
    }

    project.tasks.withType<BCVApiGeneratePreparationTask>().configureEach {
      apiDumpFiles.from(extension.outputApiDir)
      apiDirectory.convention(objects.directoryProperty().fileValue(temporaryDir))
    }

    project.tasks.withType<BCVApiGenerateTask>().configureEach {
      runtimeClasspath.from(bcvGenerateClasspath)
      targets.addAllLater(providers.provider { extension.targets })
      outputApiBuildDir.convention(layout.buildDirectory.dir("bcv-api"))
      projectName.convention(extension.projectName)

      onlyIf("Must have at least one target") { targets.isNotEmpty() }
    }

    project.tasks.withType<BCVApiCheckTask>().configureEach {
      outputs.dir(temporaryDir) // dummy output, so up-to-date checks work
      expectedProjectName.convention(extension.projectName)
      expectedApiDirPath.convention(
        extension.outputApiDir.map { it.asFile.canonicalFile.invariantSeparatorsPath }
      )
    }

    project.tasks.withType<BCVApiDumpTask>().configureEach {
      apiDirectory.convention(extension.outputApiDir)
    }

    project.tasks.named(CHECK_TASK_NAME).configure {
      dependsOn(project.tasks.withType<BCVApiCheckTask>())
    }
  }


  private fun registerBcvTasks(project: Project) {
    val prepareApiGenerateTask =
      project.tasks.register(PREPARE_API_GENERATE_TASK_NAME, BCVApiGeneratePreparationTask::class)

    val apiGenerateTask =
      project.tasks.register(API_GENERATE_TASK_NAME, BCVApiGenerateTask::class) {
        extantApiDumpDir.convention(prepareApiGenerateTask.flatMap { it.apiDirectory })
      }

    project.tasks.register(API_DUMP_TASK_NAME, BCVApiDumpTask::class) {
      apiDumpFiles.from(apiGenerateTask.map { it.outputApiBuildDir })
    }

    project.tasks.register(API_CHECK_TASK_NAME, BCVApiCheckTask::class) {
      apiBuildDir.convention(apiGenerateTask.flatMap { it.outputApiBuildDir })
    }
  }

  companion object {
    private val logger = Logging.getLogger(BCVProjectPlugin::class.java)
  }
}
