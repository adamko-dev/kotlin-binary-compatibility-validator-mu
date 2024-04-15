package dev.adamko.kotlin.binary_compatibility_validator.adapters

import dev.adamko.kotlin.binary_compatibility_validator.BCVProjectExtension
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVJvmTarget
import dev.adamko.kotlin.binary_compatibility_validator.targets.BCVKLibTarget
import java.io.File
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.*
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer
import org.jetbrains.kotlin.konan.target.HostManager

private val logger: Logger = Logging.getLogger("dev.adamko.kotlin.binary_compatibility_validator.adapters.KotlinMultiplatformAdapter")

internal fun createKotlinMultiplatformTargets(
  project: Project,
  extension: BCVProjectExtension,
) {
  project.pluginManager.withPlugin("kotlin-multiplatform") {
    val kotlinTargetsContainer = project.extensions.getByType<KotlinTargetsContainer>()

    kotlinTargetsContainer.targets
      .matching { it.platformType != common }
      .configureEach target@{
        when (platformType) {
          common,
          -> {
            // no-op
          }

          js,
          native,
          wasm,
          -> registerKotlinKLibCompilation(extension, this@target, project.providers)

          androidJvm,
          jvm,
          -> registerKotlinJvmCompilations(extension, this@target)
        }
      }
  }
}


private fun registerKotlinJvmCompilations(
  extension: BCVProjectExtension,
  target: KotlinTarget,
) {
  val targetPlatformType = target.platformType

  extension.targets.register<BCVJvmTarget>(target.targetName) {
    logger.lifecycle("registering JVM target ${target.targetName}")
//    enabled.convention(true)

    target.compilations
      .matching {
        when (targetPlatformType) {
          jvm -> it.name == KotlinCompilation.MAIN_COMPILATION_NAME
          androidJvm -> it.name == "release"
          else -> false
        }
      }.all {
        inputClasses.from(output.classesDirs)
      }
  }
}


private fun registerKotlinKLibCompilation(
  extension: BCVProjectExtension,
  target: KotlinTarget,
  providers: ProviderFactory,
) {
  extension.targets.register<BCVKLibTarget>(target.targetName) {
    logger.lifecycle("registering KLib target ${target.targetName}")
//    enabled.convention(false)

    target.compilations
      .matching { it.name == KotlinCompilation.MAIN_COMPILATION_NAME }
      .all {
        klibFile.from(output.classesDirs)
        compilationDependencies.from(providers.provider { compileDependencyFiles })
        currentPlatform.convention(HostManager.platformName())
        supportedByCurrentHost.set(target.isSupportedByCurrentHost())
        hasKotlinSources.convention(
          providers.provider {
            allKotlinSourceSets.any { it.kotlin.srcDirs.any(File::exists) }
          }
        )
      }
  }
}

private fun KotlinTarget.isSupportedByCurrentHost(): Boolean {
  return when (this) {
    is org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget ->
      HostManager().isEnabled(konanTarget)

    else                                                         -> true
  }
}

//private fun extractUnderlyingTarget(target: KotlinTarget): String {
//  if (target is KotlinNativeTarget) {
//    return konanTargetNameMapping[target.konanTarget.name]!!
//  }
//  return when (target.platformType) {
//    KotlinPlatformType.js   -> "js"
//    KotlinPlatformType.wasm -> when ((target as KotlinJsIrTarget).wasmTargetType) {
//      KotlinWasmTargetType.WASI -> "wasmWasi"
//      KotlinWasmTargetType.JS   -> "wasmJs"
//      else                      -> throw IllegalStateException("Unreachable")
//    }
//
//    else                    -> throw IllegalArgumentException("Unsupported platform type: ${target.platformType}")
//  }
//}
//
//
//internal fun KotlinCompilation<KotlinCommonOptions>.hasKotlinSources(): Provider<Boolean> =
//  project.provider {
//    allKotlinSourceSets.any { it.kotlin.srcDirs.any(File::exists) }
//  }
