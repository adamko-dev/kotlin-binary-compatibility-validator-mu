package dev.adamko.kotlin.binary_compatibility_validator.test.utils

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.jvm.optionals.getOrNull
import org.junit.jupiter.api.extension.AnnotatedElementContext
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDirFactory

/**
 * Create temp dir with a stable name based on the current test name.
 *
 * @see TempDirFactory.Standard
 */
class TempTestNameDirFactory : TempDirFactory {

  @OptIn(ExperimentalPathApi::class)
  override fun createTempDirectory(
    elementContext: AnnotatedElementContext,
    extensionContext: ExtensionContext,
  ): Path {

    // convert `${TestClass.fqn}.${testDisplayName}`
    // to file path, e.g. `f/q/n/TestClass/test-display-name`
    val path = listOfNotNull(
      extensionContext.testClass.getOrNull()?.canonicalName,
      extensionContext.displayName,
    ).joinToString("/") { segment ->
      segment
        .split(".")
        .joinToString(separator = "/") {
          it
            .map { c -> if (c.isLetterOrDigit()) c else '-' }
            .dropLastWhile { c -> !c.isLetterOrDigit() }
            .dropWhile { c -> !c.isLetterOrDigit() }
            .joinToString("")
        }
    }

    val dir = testTempDir.resolve(path)
    dir.deleteRecursively()
    dir.createDirectories()
    return dir
  }

  companion object {
    private val testTempDir: Path by systemProperty(::Path)
  }
}
