plugins {
  buildsrc.conventions.`kotlin-gradle-plugin-tests`
  buildsrc.conventions.`maven-publish-test`
  `java-test-fixtures`
  `jvm-test-suite`
}

description = "Functional tests for bcv-gradle-plugin"

dependencies {
  testMavenPublication(projects.modules.bcvGradlePlugin)

  testFixturesApi(gradleTestKit())

  testFixturesApi(platform(libs.junit.bom))
  testFixturesApi(libs.junit.jupiter)

  testFixturesApi(platform(libs.kotest.bom))
  testFixturesApi(libs.kotest.runnerJUnit5)
  testFixturesApi(libs.kotest.assertionsCore)
  testFixturesApi(libs.kotest.property)

  testFixturesApi(testFixtures(projects.modules.bcvGradlePlugin))
}


@Suppress("UnstableApiUsage")
testing.suites {
  withType<JvmTestSuite>().configureEach {
    targets.configureEach {
      testTask.configure {
        val projectTestTempDirPath = "$buildDir/test-temp-dir"
        inputs.property("projectTestTempDir", projectTestTempDirPath)
        systemProperty("projectTestTempDir", projectTestTempDirPath)
        systemProperty("integrationTestProjectsDir", "$projectDir/projects")
        systemProperty("minimumGradleTestVersion", libs.versions.testGradleVersion.get())
      }
    }
  }

  val test by getting(JvmTestSuite::class) {
    description = "Regular ol' unit tests"

    dependencies {
      implementation(projects.modules.bcvGradlePlugin)
    }
  }

  val functionalTest by registering(JvmTestSuite::class) {
    description = "Functional testing, using Gradle TestKit"
    testType.set(TestSuiteType.FUNCTIONAL_TEST)

    targets.configureEach {
      testTask.configure {
        shouldRunAfter(test)
        dependsOn(project.configurations.testMavenPublication)

        systemProperty("testMavenRepoDir", file(mavenPublishTest.testMavenRepo).canonicalPath)
      }
    }
  }

  tasks.check { dependsOn(functionalTest) }
}
