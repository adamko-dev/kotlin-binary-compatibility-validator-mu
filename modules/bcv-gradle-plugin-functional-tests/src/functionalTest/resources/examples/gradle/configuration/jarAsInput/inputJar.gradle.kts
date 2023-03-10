//tasks {
//    jar {
//        exclude("foo/HiddenField.class")
//        exclude("foo/HiddenProperty.class")
//    }
//    apiBuild {
//        inputJar.value(jar.flatMap { it.archiveFile })
//    }
//}

tasks.jar {
  exclude("foo/HiddenField.class")
  exclude("**/HiddenProperty.class")
}

binaryCompatibilityValidator {
  kotlinJvm {
    inputJar.set(tasks.jar.flatMap { it.archiveFile })
  }
}
