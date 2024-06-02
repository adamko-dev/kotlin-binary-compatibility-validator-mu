package dev.adamko.kotlin.binary_compatibility_validator.test.utils.api

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.TempTestNameDirFactory
import java.io.File
import org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS
import org.junit.jupiter.api.io.TempDir

open class BaseKotlinGradleTest {
  @TempDir(
    factory = TempTestNameDirFactory::class,
    cleanup = ON_SUCCESS,
  )
  lateinit var testTempDir: File

  val rootProjectDir: File get() = testTempDir.resolve("bcv-test-project")

  val rootProjectApiDump: File get() = rootProjectDir.resolve("$API_DIR/${rootProjectDir.name}.api")

  fun rootProjectAbiDump(
    project: String = rootProjectDir.name
  ): File = rootProjectDir.resolve("$API_DIR/$project.klib.api")
}
