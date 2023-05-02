tasks.jar {
  exclude("foo/HiddenField.class")
  exclude("**/HiddenProperty.class")
}

binaryCompatibilityValidator {
  targets.kotlinJvm {
    inputJar.set(tasks.jar.flatMap { it.archiveFile })
  }
}
