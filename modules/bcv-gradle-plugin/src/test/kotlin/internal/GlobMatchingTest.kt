package dev.adamko.kotlin.binary_compatibility_validator.test.internal

import dev.adamko.kotlin.binary_compatibility_validator.internal.globToRegex
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.regex.shouldMatch
import io.kotest.matchers.regex.shouldMatchAll
import io.kotest.matchers.regex.shouldNotMatchAny
import org.gradle.api.Project

class GlobMatchingTest : FunSpec({
  val projectPaths = setOf(
    ":",
    ":foo",
    ":foo:alpha",
    ":foo:alpha:beta",
    ":foo:alpha:beta:gamma",
    ":bar",
    ":bar:alpha",
    ":bar:alpha:beta",
    ":bar:alpha:beta:gamma",
  )

  fun String.shouldOnlyMatchPaths(vararg paths: String) {
    val pathsSet = paths.toSet()
    globToRegex(this, Project.PATH_SEPARATOR).run {
      shouldMatchAll(pathsSet)
      shouldNotMatchAny(projectPaths - pathsSet)
    }
  }

  context("** should match all paths") {
    projectPaths.forEach { path ->
      test(path) {
        globToRegex("**", Project.PATH_SEPARATOR) shouldMatch path
      }
    }
  }
  test("partial glob should match some paths") {
    ":*".shouldOnlyMatchPaths(
      ":",
      ":foo",
      ":bar",
    )
    ":*:*alp*:*".shouldOnlyMatchPaths(
      ":foo:alpha:beta",
      ":bar:alpha:beta",
    )
  }
})


private fun Regex.shouldMatchAll(strs: Collection<String>) =
  shouldMatchAll(*strs.toTypedArray())

private fun Regex.shouldNotMatchAny(strs: Collection<String>) =
  shouldNotMatchAny(*strs.toTypedArray())
