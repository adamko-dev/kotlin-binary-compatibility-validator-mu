package dev.adamko.kotlin.binary_compatibility_validator.test.utils

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.neverNullMatcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

infix fun <T : Any> NamedDomainObjectCollection<out T>?.shouldContainDomainObject(
  name: String
): T {
  this should containDomainObject(name)
  return this?.getByName(name)!!
}

infix fun <T : Any> NamedDomainObjectCollection<out T>?.shouldNotContainDomainObject(
  name: String
): NamedDomainObjectCollection<out T>? {
  this shouldNot containDomainObject(name)
  return this
}

private fun <T> containDomainObject(name: String): Matcher<NamedDomainObjectCollection<T>?> =
  neverNullMatcher { value ->
    MatcherResult(
      name in value.names,
      { "NamedDomainObjectCollection(${value.names}) should contain DomainObject named '$name'" },
      { "NamedDomainObjectCollection(${value.names}) should not contain DomainObject named '$name'" })
  }

/** Assert that a task ran. */
infix fun BuildResult?.shouldHaveRunTask(taskPath: String): BuildTask {
  this should haveTask(taskPath)
  return this?.task(taskPath)!!
}

/** Assert that a task ran, with an [expected outcome][expectedOutcome]. */
fun BuildResult?.shouldHaveRunTask(
  taskPath: String,
  expectedOutcome: TaskOutcome
): BuildTask {
  this should haveTask(taskPath)
  val task = this?.task(taskPath)!!
  task should haveOutcome(expectedOutcome)
  return task
}

/**
 * Assert that a task did not run.
 *
 * A task might not have run if one of its dependencies failed before it could be run.
 */
infix fun BuildResult?.shouldNotHaveRunTask(taskPath: String) {
  this shouldNot haveTask(taskPath)
}

private fun haveTask(taskPath: String): Matcher<BuildResult?> =
  neverNullMatcher { value ->
    MatcherResult(
      value.task(taskPath) != null,
      { "BuildResult should have run task $taskPath. All tasks: ${value.tasks.joinToString { it.path }}" },
      { "BuildResult should not have run task $taskPath. All tasks: ${value.tasks.joinToString { it.path }}" },
    )
  }

infix fun BuildTask?.shouldHaveOutcome(outcome: TaskOutcome) {
  this should haveOutcome(outcome)
}

infix fun BuildTask?.shouldNotHaveOutcome(outcome: TaskOutcome) {
  this shouldNot haveOutcome(outcome)
}

private fun haveOutcome(outcome: TaskOutcome): Matcher<BuildTask?> =
  neverNullMatcher { value ->
    MatcherResult(
      value.outcome == outcome,
      { "Task ${value.path} should have outcome $outcome" },
      { "Task ${value.path} should not have outcome $outcome" },
    )
  }

fun BuildResult.shouldHaveTaskWithOutcome(taskPath: String, outcome: TaskOutcome) {
  this shouldHaveRunTask taskPath shouldHaveOutcome outcome
}
