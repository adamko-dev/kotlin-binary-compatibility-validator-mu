tasks.jar {
  exclude("foo/HiddenField.class")
  exclude("**/HiddenProperty.class")
}

binaryCompatibilityValidator {
  kotlinJvm {
    inputJar.set(tasks.jar.flatMap { it.archiveFile })
  }
}
