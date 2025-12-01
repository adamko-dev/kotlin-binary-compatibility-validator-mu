package dev.adamko.kotlin.binary_compatibility_validator

import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_CHECK_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_DIR
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_DUMP_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.API_GENERATE_TASK_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.EXTENSION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.BCVPlugin.Companion.RUNTIME_CLASSPATH_RESOLVER_CONFIGURATION_NAME
import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import dev.adamko.kotlin.binary_compatibility_validator.internal.declarable
import dev.adamko.kotlin.binary_compatibility_validator.internal.resolvable
import dev.adamko.kotlin.binary_compatibility_validator.internal.sourceSets
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiCheckTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiDumpTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVApiGenerateTask
import dev.adamko.kotlin.binary_compatibility_validator.tasks.BCVDefaultTask
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.internal.component.external.model.TestFixturesSupport.TEST_FIXTURE_SOURCESET_NAME
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
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
      try {
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
                    KotlinPlatformType.jvm        -> it.name == "main"
                    KotlinPlatformType.androidJvm -> it.name == "release"
                    else                          -> false
                  }
                }.all {
                  inputClasses.from(output.classesDirs)
                }
            }
          }
      } catch (e: Throwable) {
        when (e) {
          is NoClassDefFoundError,
          is TypeNotPresentException -> {
            logger.info("Failed to apply BCVProjectPlugin to project ${project.path} with plugin $id using KGP classes $e")
            createKotlinMultiplatformTargetsHack(project, extension)
          }

          else                       -> throw e
        }
      }
    }
  }

  private fun createKotlinMultiplatformTargetsHack(
    project: Project,
    extension: BCVProjectExtension,
  ) {
    logger.info("Falling back to Groovy metaprogramming to access to KGP classes ${project.path} https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu/issues/1")
    val kotlinExtension = project.extensions.findByName("kotlin")
      ?: return

    val targets = kotlinExtension.withGroovyBuilder { "getTargets"() }
        as NamedDomainObjectContainer<*>

    targets
      .matching { target ->
        val platformType = target.withGroovyBuilder { "getPlatformType"() }
          ?: return@matching false
        val platformName = platformType.withGroovyBuilder { "getName"() }
        platformName in listOf("jvm", "androidJvm")
      }
      .all action@{
        val platformType = withGroovyBuilder { "getPlatformType"() } ?: return@action
        val platformName = platformType.withGroovyBuilder { "getName"() }
        val targetName = withGroovyBuilder { "getTargetName"() }
        val compilations = withGroovyBuilder { "getCompilations"() }
            as NamedDomainObjectContainer<*>

        extension.targets.register(targetName.toString()) {
          enabled.convention(true)

          compilations
            .matching { comp ->
              val compName = comp.withGroovyBuilder { "getName"() }

              when (platformName) {
                "jvm"        -> compName == "main"
                "androidJvm" -> compName == "release"
                else         -> false
              }
            }
            .all {
              val output = withGroovyBuilder { "getOutput"() } ?: return@all
              val classesDirs = output.withGroovyBuilder { "getClassesDirs"() }
              inputClasses.from(classesDirs)
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
    private val logger = Logging.getLogger(BCVProjectPlugin::class.java)
  }
}
