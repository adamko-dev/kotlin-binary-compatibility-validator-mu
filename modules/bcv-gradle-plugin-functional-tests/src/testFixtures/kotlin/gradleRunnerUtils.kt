package dev.adamko.kotlin.binary_compatibility_validator.test.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner


fun GradleRunner.withEnvironment(vararg map: Pair<String, String>): GradleRunner =
  withEnvironment(map.toMap())


inline fun GradleRunner.build(block: BuildResult.() -> Unit): Unit = build().block()
inline fun GradleRunner.buildAndFail(block: BuildResult.() -> Unit): Unit = buildAndFail().block()
@Suppress("UnstableApiUsage")
inline fun GradleRunner.run(block: BuildResult.() -> Unit): Unit = run().block()
