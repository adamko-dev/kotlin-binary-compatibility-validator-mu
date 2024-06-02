plugins {
  buildsrc.conventions.`kotlin-gradle-plugin-tests`
  id("dev.adamko.dev-publish")
  `java-test-fixtures`
  `jvm-test-suite`
}

description = "Functional tests for bcv-gradle-plugin"

dependencies {
  devPublication(projects.modules.bcvGradlePlugin)

  testFixturesApi(gradleTestKit())

  testFixturesApi(platform(libs.junit.bom))
  testFixturesApi(libs.junit.jupiter)

  testFixturesApi(platform(libs.kotest.bom))
  testFixturesApi(libs.kotest.runnerJUnit5)
  testFixturesApi(libs.kotest.assertionsCore)
  testFixturesApi(libs.kotest.property)

  testFixturesApi("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")

  testFixturesApi(testFixtures(projects.modules.bcvGradlePlugin))
}

@Suppress("UnstableApiUsage")
testing.suites {
  withType<JvmTestSuite>().configureEach {
    targets.configureEach {
      testTask.configure {
        val projectTestTempDirPath = layout.buildDirectory.dir("test-temp-dir").get().asFile
        inputs.property("projectTestTempDir", projectTestTempDirPath)
        systemProperty("projectTestTempDir", projectTestTempDirPath)
        systemProperty("integrationTestProjectsDir", "$projectDir/projects")
        systemProperty("minimumGradleTestVersion", libs.versions.supportedGradleVersion.get())
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
        dependsOn(tasks.updateDevRepo)
        systemProperty(
          "devMavenRepoDir",
          devPublish.devMavenRepo.asFile.get().invariantSeparatorsPath,
        )
        val testTempDir = layout.buildDirectory.dir("test-temp").get().asFile
        systemProperty(
          "testTempDir",
          testTempDir.invariantSeparatorsPath
        )
        doFirst {
          testTempDir.mkdirs()
        }
      }
    }
  }

  tasks.check { dependsOn(functionalTest) }
}
