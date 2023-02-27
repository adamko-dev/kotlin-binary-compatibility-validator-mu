package dev.adamko.kotlin.binary_compatibility_validator.test.utils.api

import java.io.File
import org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS
import org.junit.jupiter.api.io.TempDir

open class BaseKotlinGradleTest {
//  @Rule
//  @JvmField
//  internal val testProjectDir: TemporaryFolder = TemporaryFolder()

  @TempDir(cleanup = ON_SUCCESS)
  lateinit var testProjectDir: File

  val rootProjectDir: File get() = testProjectDir

  val rootProjectApiDump: File get() = rootProjectDir.resolve("api/${rootProjectDir.name}.api")
}
