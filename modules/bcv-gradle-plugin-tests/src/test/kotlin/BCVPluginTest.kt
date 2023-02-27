package dev.adamko.kotlin.binary_compatibility_validator.test

import dev.adamko.kotlin.binary_compatibility_validator.test.utils.shouldContainDomainObject
import io.kotest.core.spec.style.FunSpec
import org.gradle.testfixtures.ProjectBuilder

/**
 * A simple unit test for the 'dev.adamko.kotlin.binary-compatibility-validator' plugin.
 */
class BCVPluginTest : FunSpec({
  context("plugin registers tasks") {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("dev.adamko.kotlin.binary-compatibility-validator")

    test("apiCheck") {
      project.tasks shouldContainDomainObject "apiCheck"
    }
    test("apiDump") {
      project.tasks shouldContainDomainObject "apiDump"
    }
    test("apiGenerate") {
      project.tasks shouldContainDomainObject "apiGenerate"
    }
  }
})
