package dev.adamko.kotlin.binary_compatibility_validator.test.utils.api

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * Helper `fun` for asserting a [TaskOutcome] to be equal to [TaskOutcome.SUCCESS]
 */
fun BuildResult.assertTaskSuccess(task: String) {
  assertTaskOutcome(TaskOutcome.SUCCESS, task)
}

/**
 * Helper `fun` for asserting a [TaskOutcome] to be equal to [TaskOutcome.FAILED]
 */
fun BuildResult.assertTaskFailure(task: String) {
  assertTaskOutcome(TaskOutcome.FAILED, task)
}

private fun BuildResult.assertTaskOutcome(taskOutcome: TaskOutcome, taskName: String) {
  task(taskName)?.outcome shouldBe taskOutcome
}

/**
 * Helper `fun` for asserting that a task was not run, which also happens if one of its dependencies failed before it
 * could be run.
 */
fun BuildResult.assertTaskNotRun(taskName: String) {
  withClue("task $taskName was not expected to be run") {
    task(taskName) shouldBe null
  }
}
