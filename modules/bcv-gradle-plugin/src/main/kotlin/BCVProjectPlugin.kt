package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_CHECK_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_DIR
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_DUMP_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_GENERATE_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.EXTENSION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.RUNTIME_CLASSPATH_RESOLVER_CONFIGURATION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.internal.*
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiCheckTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiDumpTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiGenerateTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVDefaultTask
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.internal.component.external.model.TestFixturesSupport.TEST_FIXTURE_SOURCESET_NAME
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets


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

    val bcvGenerateClasspath = createBcvMuClasspath(project, extension)

    project.tasks.withType<BCVDefaultTask>().configureEach {
      bcvEnabled.convention(extension.enabled)
      onlyIf("BCV is disabled") { bcvEnabled.get() }
    }

    project.tasks.withType<BCVApiGenerateTask>().configureEach {
      runtimeClasspath.from(bcvGenerateClasspath)
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

    val apiGenerateTask = project.tasks.register(API_GENERATE_TASK_NAME, BCVApiGenerateTask::class)

    project.tasks.register(API_DUMP_TASK_NAME, BCVApiDumpTask::class) {
      apiDumpFiles.from(apiGenerateTask.map { it.outputApiBuildDir })
    }

    val apiCheckTask = project.tasks.register(API_CHECK_TASK_NAME, BCVApiCheckTask::class) {
      apiBuildDir.convention(apiGenerateTask.flatMap { it.outputApiBuildDir })
    }

    project.tasks.named(CHECK_TASK_NAME).configure {
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
      kotlinxBinaryCompatibilityValidatorVersion.convention("0.13.1")
    }

    extension.targets.configureEach {
      enabled.convention(true)

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
        }
      }

    return project.configurations.register(RUNTIME_CLASSPATH_RESOLVER_CONFIGURATION_NAME) {
      description = "Resolve the runtime classpath for running binary-compatibility-validator."
      resolvable()
      isVisible = false
      extendsFrom(bcvGenerateClasspath.get())
    }
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
      val kotlinTargets = findKotlinTargets(project) ?: return@withPlugin

      kotlinTargets
        .filter { target ->
          target.platformType in arrayOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)
        }
        .forEach { target ->
          val targetPlatformType = target.platformType
          val targetCompilations = target.compilations

          extension.targets.register(target.targetName) {
            enabled.convention(true)
            targetCompilations
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

  companion object {
    private val logger: Logger = Logging.getLogger(BCVProjectPlugin::class.java)

    private fun findKotlinTargets(project: Project): Iterable<KotlinTarget>? {

      val kotlinTargets = project.extensions.findKotlinProjectExtension()?.targets
        ?: project.extensions.findKotlinMultiplatformExtension()?.targets
        ?: project.extensions.findKotlinTargetsContainer()?.targets

      if (kotlinTargets != null) {
        return kotlinTargets
      } else {
        if (project.extensions.findByName("kotlin") != null) {
          // uh oh - the Kotlin extension is present but findKotlinTargetsContainer() failed.
          // Is there a class loader issue? https://github.com/gradle/gradle/issues/27218
          logger.warn {
            val allPlugins =
              project.plugins.joinToString { it::class.qualifiedName ?: "${it::class}" }
            val allExtensions =
              project.extensions.extensionsSchema.elements.joinToString { "${it.name} ${it.publicType}" }

            /* language=TEXT */ """
                |BCVProjectPlugin failed to get KotlinProjectExtension in ${project.path}
                |  Applied plugins: $allPlugins
                |  Available extensions: $allExtensions
              """.trimMargin()
          }
        } else {
          logger.warn("Could not apply ${BCVProjectPlugin::class.simpleName} in ${project.path} - could not find Kotlin targets")
        }
        return null
      }
    }

    private fun ExtensionContainer.findKotlinProjectExtension(): org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension? =
      findOrNull()

    private fun ExtensionContainer.findKotlinTargetsContainer(): org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer? =
      findOrNull()

    private fun ExtensionContainer.findKotlinMultiplatformExtension(): org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension? =
      findOrNull()

    private inline fun <reified T : Any> ExtensionContainer.findOrNull(): T? =
      try {
        findByType<T>()
      } catch (e: Throwable) {
        when (e) {
          is TypeNotPresentException,
          is ClassNotFoundException,
          is NoClassDefFoundError -> {
            logger.lifecycle("BCVProjectPlugin failed to find ${T::class.qualifiedName} - ${e::class} ${e.message}")
            null
          }

          else                    -> throw e
        }
      }
  }
}
