package buildsrc.utils

import java.io.File
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModule

/**
 * Exclude directories containing
 *
 * - generated Gradle code,
 * - IDE files,
 * - Gradle config,
 *
 * so they don't clog up search results.
 */
fun IdeaModule.excludeProjectConfigurationDirs(
  layout: ProjectLayout,
  providers: ProviderFactory,
) {
  val excludedDirs = providers.of(IdeaExcludedDirectoriesSource::class) {
    parameters.projectDir.set(layout.projectDirectory)
  }

  excludeDirs.addAll(excludedDirs.get())
}

// We have to use a ValueSource to find the files, otherwise Gradle
// considers _all files_ an input for configuration cache 🙄
internal abstract class IdeaExcludedDirectoriesSource
  : ValueSource<Set<File>, IdeaExcludedDirectoriesSource.Parameters> {

  interface Parameters : ValueSourceParameters {
    val projectDir: DirectoryProperty
  }

  override fun obtain(): Set<File> {
    val projectDir = parameters.projectDir.get().asFile

    val excludedDirs = setOf(
      ".git",
      ".gradle",
      ".idea",
      ".kotlin",
    )

    val generatedSrcDirs = listOf(
      "kotlin-dsl-accessors",
      "kotlin-dsl-external-plugin-spec-builders",
      "kotlin-dsl-plugins",
    )

    val generatedDirs = projectDir
      .walk()
      .onEnter { it.name !in excludedDirs && it.parentFile.name !in generatedSrcDirs }
      .filter { it.isDirectory }
      .filter { it.parentFile.name in generatedSrcDirs }
      .flatMap { file ->
        file.walk().maxDepth(1).filter { it.isDirectory }.toList()
      }
      .toSet()

    // can't use buildSet {} https://github.com/gradle/gradle/issues/28325
    return mutableSetOf<File>().apply {
      addAll(generatedDirs)
      add(projectDir.resolve(".idea"))
      add(projectDir.resolve("gradle/wrapper"))
    }
  }
}
