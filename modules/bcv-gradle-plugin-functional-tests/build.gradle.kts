plugins {
  buildsrc.conventions.`kotlin-gradle-plugin-tests`
  buildsrc.conventions.`maven-publish-test`
  `java-test-fixtures`
  `jvm-test-suite`
}

description = "Functional tests for bcv-gradle-plugin"

dependencies {
  testMavenPublication(projects.modules.bcvGradlePlugin)

//  runtimeOnly("org.jetbrains.kotlinx:binary-compatibility-validator:0.13.0")
//  runtimeOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")

  testFixturesApi(gradleTestKit())

  testFixturesApi(platform("org.junit:junit-bom:5.9.2"))
  testFixturesApi("org.junit.jupiter:junit-jupiter")

  testFixturesApi(platform("io.kotest:kotest-bom:5.5.5"))
  testFixturesApi("io.kotest:kotest-runner-junit5")
  testFixturesApi("io.kotest:kotest-assertions-core")

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
