val generateSources by tasks.registering {
  val outputDir = layout.buildDirectory.dir("generated/kotlin")

  outputs.dir(outputDir).withPropertyName("outputDir")

  doLast {
    outputDir.get().asFile.apply {
      mkdirs()
      resolve("Generated.kt").writeText(
        """
          |public class Generated {
          |  public fun helloCreator(): Int = 42
          |}
          |
        """.trimMargin()
      )
    }
  }
}

kotlin.sourceSets.main {
  kotlin.srcDir(generateSources)
}
