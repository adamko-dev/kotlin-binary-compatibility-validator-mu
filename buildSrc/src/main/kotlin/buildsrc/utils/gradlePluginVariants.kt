//package buildsrc.utils
//
//import java.util.*
//import org.gradle.api.Action
//import org.gradle.api.Project
//import org.gradle.api.artifacts.Configuration
//import org.gradle.api.artifacts.type.ArtifactTypeDefinition
//import org.gradle.api.attributes.*
//import org.gradle.api.attributes.java.TargetJvmEnvironment
//import org.gradle.api.attributes.java.TargetJvmVersion
//import org.gradle.api.attributes.plugin.GradlePluginApiVersion
//import org.gradle.api.component.AdhocComponentWithVariants
//import org.gradle.api.file.FileCollection
//import org.gradle.api.plugins.JavaLibraryPlugin
//import org.gradle.api.plugins.JavaPlugin
//import org.gradle.api.plugins.JavaPluginExtension
//import org.gradle.api.tasks.Copy
//import org.gradle.api.tasks.SourceSet
//import org.gradle.api.tasks.SourceSetContainer
//import org.gradle.api.tasks.compile.JavaCompile
//import org.gradle.jvm.tasks.Jar
//import org.gradle.kotlin.dsl.*
//import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin
//import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
//import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
//
///**
// * Gradle plugins common variants.
// */
//enum class GradlePluginVariant(
//  val sourceSetName: String,
//  val gradleVersion: String,
//  val gradleApiJavadocUrl: String = "https://docs.gradle.org/${gradleVersion}/javadoc/"
//) {
//  GRADLE_7("gradle7", "7.6"),
//  GRADLE_8("gradle8", "8.1"),
//  ;
//
//  companion object {
//    val GRADLE_MIN = GRADLE_7
//  }
//}
//
//val commonSourceSetName = "common"
//
/////**
//// * Configures common pom configuration parameters
//// */
////fun Project.configureCommonPublicationSettingsForGradle(
////  signingRequired: Boolean
////) {
////  plugins.withId("maven-publish") {
//////    configureDefaultPublishing(signingRequired)
////
////    extensions.configure<PublishingExtension> {
////      publications
////        .withType<MavenPublication>()
////        .configureEach {
////          configureKotlinPomAttributes(project)
////        }
////    }
////  }
////}
//
//
///**
// * Common sources for all variants.
// * Should contain classes that are independent of Gradle API version or using minimal supported Gradle api.
// */
//fun Project.createGradleCommonSourceSet(): SourceSet {
//  val commonSourceSet = sourceSets.create(commonSourceSetName) {
//    excludeGradleCommonDependencies(this)
//
//    // Adding Gradle API to separate configuration, so version will not leak into variants
//    val commonGradleApiConfiguration = configurations.create("commonGradleApiCompileOnly") {
//      isVisible = false
//      isCanBeConsumed = false
//      isCanBeResolved = true
//    }
//    configurations[compileClasspathConfigurationName].extendsFrom(commonGradleApiConfiguration)
//
//    dependencies {
//      compileOnlyConfigurationName(kotlinStdlib())
//      "commonGradleApiCompileOnly"("dev.gradleplugins:gradle-api:7.6")
//      if (this@createGradleCommonSourceSet.name != "kotlin-gradle-plugin-api" &&
//        this@createGradleCommonSourceSet.name != "android-test-fixes" &&
//        this@createGradleCommonSourceSet.name != "gradle-warnings-detector"
//      ) {
//        compileOnlyConfigurationName(project(":kotlin-gradle-plugin-api")) {
//          capabilities {
//            requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-common")
//          }
//        }
//      }
//    }
//  }
//
//  plugins.withType<JavaLibraryPlugin>().configureEach {
//    this@createGradleCommonSourceSet.extensions.configure<JavaPluginExtension> {
//      registerFeature(commonSourceSet.name) {
//        usingSourceSet(commonSourceSet)
//        disablePublication()
//      }
//    }
//  }
//
//  // Common outputs will also produce '${project.name}.kotlin_module' file, so we need to avoid
//  // files clash
//  tasks.named<KotlinCompile>("compile${commonSourceSet.name.replaceFirstChar { it.uppercase() }}Kotlin") {
//    @Suppress("DEPRECATION")
//    kotlinOptions {
//      moduleName = "${this@createGradleCommonSourceSet.name}_${commonSourceSet.name}"
//    }
//  }
//
//  return commonSourceSet
//}
//
///**
// * Fixes wired SourceSet does not expose compiled common classes and common resources as secondary variant
// * which is used in the Kotlin Project compilation.
// */
//private fun Project.fixWiredSourceSetSecondaryVariants(
//  wireSourceSet: SourceSet,
//  commonSourceSet: SourceSet
//) {
//  configurations
//    .matching {
//      it.name == wireSourceSet.apiElementsConfigurationName ||
//          it.name == wireSourceSet.runtimeElementsConfigurationName
//    }
//    .configureEach {
//      outgoing {
//        variants.maybeCreate("classes").apply {
//          attributes {
//            attribute(
//              LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
//              objects.named(LibraryElements.CLASSES)
//            )
//          }
//          (commonSourceSet.output.classesDirs.files + wireSourceSet.output.classesDirs.files)
//            .toSet()
//            .forEach {
//              if (!artifacts.files.contains(it)) {
//                artifact(it) {
//                  type = ArtifactTypeDefinition.JVM_CLASS_DIRECTORY
//                }
//              }
//            }
//        }
//      }
//    }
//
//  configurations
//    .matching { it.name == wireSourceSet.runtimeElementsConfigurationName }
//    .configureEach {
//      outgoing {
//        val resourcesDirectories = listOfNotNull(
//          commonSourceSet.output.resourcesDir,
//          wireSourceSet.output.resourcesDir
//        )
//
//        if (resourcesDirectories.isNotEmpty()) {
//          variants.maybeCreate("resources").apply {
//            attributes {
//              attribute(
//                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
//                objects.named(LibraryElements.RESOURCES)
//              )
//            }
//            resourcesDirectories.forEach {
//              if (!artifacts.files.contains(it)) {
//                artifact(it) {
//                  type = ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY
//                }
//              }
//            }
//          }
//        }
//      }
//    }
//}
//
///**
// * Make [wireSourceSet] to extend [commonSourceSet].
// */
//fun Project.wireGradleVariantToCommonGradleVariant(
//  wireSourceSet: SourceSet,
//  commonSourceSet: SourceSet
//) {
//  wireSourceSet.compileClasspath += commonSourceSet.output
//  wireSourceSet.runtimeClasspath += commonSourceSet.output
//
//  // Allowing to use 'internal' classes/methods from common source code
//  (extensions.getByName("kotlin") as KotlinSingleJavaTargetExtension).target.compilations.run {
//    getByName(wireSourceSet.name).associateWith(getByName(commonSourceSet.name))
//  }
//
//  configurations[wireSourceSet.apiConfigurationName].extendsFrom(
//    configurations[commonSourceSet.apiConfigurationName]
//  )
//  configurations[wireSourceSet.implementationConfigurationName].extendsFrom(
//    configurations[commonSourceSet.implementationConfigurationName]
//  )
//  configurations[wireSourceSet.runtimeOnlyConfigurationName].extendsFrom(
//    configurations[commonSourceSet.runtimeOnlyConfigurationName]
//  )
//  configurations[wireSourceSet.compileOnlyConfigurationName].extendsFrom(
//    configurations[commonSourceSet.compileOnlyConfigurationName]
//  )
//
//  fixWiredSourceSetSecondaryVariants(wireSourceSet, commonSourceSet)
//
//  tasks.withType<Jar>().configureEach {
//    if (name == wireSourceSet.jarTaskName) {
//      from(wireSourceSet.output, commonSourceSet.output)
//      setupPublicJar(archiveBaseName.get())
//      addEmbeddedRuntime()
//    } else if (name == wireSourceSet.sourcesJarTaskName) {
//      from(wireSourceSet.allSource, commonSourceSet.allSource)
//    }
//  }
//}
//
//private const val FIXED_CONFIGURATION_SUFFIX = "WithFixedAttribute"
//
///**
// * 'main' sources are used for minimal supported Gradle versions (6.7) up to Gradle 7.0.
// */
//fun Project.reconfigureMainSourcesSetForGradlePlugin(
//  commonSourceSet: SourceSet
//) {
//  sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME) {
//    plugins.withType<JavaGradlePluginPlugin>().configureEach {
//      // Removing Gradle api default dependency added by 'java-gradle-plugin'
//      configurations[apiConfigurationName].dependencies.remove(dependencies.gradleApi())
//    }
//
//    dependencies {
//      "compileOnly"(kotlinStdlib())
//      // Decoupling gradle-api artifact from current project Gradle version. Later would be useful for
//      // gradle plugin variants
//      "compileOnly"("dev.gradleplugins:gradle-api:${GradlePluginVariant.GRADLE_MIN.gradleApiVersion}")
//      if (this@reconfigureMainSourcesSetForGradlePlugin.name != "kotlin-gradle-plugin-api" &&
//        this@reconfigureMainSourcesSetForGradlePlugin.name != "android-test-fixes" &&
//        this@reconfigureMainSourcesSetForGradlePlugin.name != "gradle-warnings-detector"
//      ) {
//        "api"(project(":kotlin-gradle-plugin-api"))
//      }
//    }
//
//    excludeGradleCommonDependencies(this)
//    wireGradleVariantToCommonGradleVariant(this, commonSourceSet)
//
//    // https://youtrack.jetbrains.com/issue/KT-51913
//    // Remove workaround after bootstrap update
//    if (configurations["default"].attributes.contains(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE)) {
//      configurations["default"].attributes.attribute(
//        TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
//        objects.named(TargetJvmEnvironment::class, "no-op")
//      )
//    }
//
//    plugins.withType<JavaLibraryPlugin>().configureEach {
//      this@reconfigureMainSourcesSetForGradlePlugin
//        .extensions
//        .configure<JavaPluginExtension> {
//          withSourcesJar()
//          if (kotlinBuildProperties.publishGradlePluginsJavadoc) {
//            withJavadocJar()
//          }
//        }
//    }
//
//    // Workaround for https://youtrack.jetbrains.com/issue/KT-52987
//    val javaComponent = project.components["java"] as AdhocComponentWithVariants
//    listOf(
//      runtimeElementsConfigurationName,
//      apiElementsConfigurationName
//    )
//      .map { configurations[it] }
//      .forEach { originalConfiguration ->
//        configurations.create("${originalConfiguration.name}$FIXED_CONFIGURATION_SUFFIX") {
//          isCanBeResolved = originalConfiguration.isCanBeResolved
//          isCanBeConsumed = originalConfiguration.isCanBeConsumed
//          isVisible = originalConfiguration.isVisible
//          setExtendsFrom(originalConfiguration.extendsFrom)
//
//          artifacts {
//            originalConfiguration.artifacts.forEach {
//              add(name, it)
//            }
//          }
//
//          // Removing 'org.jetbrains.kotlin.platform.type' attribute
//          // as it brings issues with Gradle variant resolve on Gradle 7.6+ versions
//          attributes {
//            originalConfiguration.attributes.keySet()
//              .filter { it.name != KotlinPlatformType.attribute.name }
//              .forEach { originalAttribute ->
//                @Suppress("UNCHECKED_CAST")
//                attribute(
//                  originalAttribute as Attribute<Any>,
//                  originalConfiguration.attributes.getAttribute(originalAttribute)!!
//                )
//              }
//
//            plugins.withType<JavaPlugin> {
//              tasks.named<JavaCompile>(compileJavaTaskName).get().apply {
//                attribute(
//                  TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
//                  targetCompatibility.toInt()
//                )
//              }
//            }
//          }
//
//          val expectedAttributes = setOf(
//            Category.CATEGORY_ATTRIBUTE,
//            Bundling.BUNDLING_ATTRIBUTE,
//            Usage.USAGE_ATTRIBUTE,
//            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
//            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
//            TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE
//          )
//          if (attributes.keySet() != expectedAttributes) {
//            error("Wrong set of attributes:\n" +
//                "  Expected: ${expectedAttributes.joinToString(", ")}\n" +
//                "  Actual: ${
//                  attributes.keySet()
//                    .joinToString(", ") { "${it.name}=${attributes.getAttribute(it)}" }
//                }"
//            )
//          }
//
//          javaComponent.addVariantsFromConfiguration(this) {
//            mapToMavenScope(
//              when (originalConfiguration.name) {
//                runtimeElementsConfigurationName -> "runtime"
//                apiElementsConfigurationName     -> "compile"
//                else                             -> error("Unsupported configuration name")
//              }
//            )
//          }
//
//          // Make original configuration unpublishable and not visible
//          originalConfiguration.isCanBeConsumed = false
//          originalConfiguration.isVisible = false
//          javaComponent.withVariantsFromConfiguration(originalConfiguration) {
//            skip()
//          }
//        }
//      }
//  }
//
//  // Fix common sources visibility for tests
//  sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME) {
//    compileClasspath += commonSourceSet.output
//    runtimeClasspath += commonSourceSet.output
//  }
//
//  // Allowing to use 'internal' classes/methods from common source code
//  (extensions.getByName("kotlin") as KotlinSingleJavaTargetExtension).target.compilations.run {
//    getByName(SourceSet.TEST_SOURCE_SET_NAME).associateWith(getByName(commonSourceSet.name))
//  }
//}
//
///**
// * Adding plugin variants: https://docs.gradle.org/current/userguide/implementing_gradle_plugins.html#plugin-with-variants
// */
//fun Project.createGradlePluginVariant(
//  variant: GradlePluginVariant,
//  commonSourceSet: SourceSet,
//  isGradlePlugin: Boolean = true
//): SourceSet {
//  val variantSourceSet = sourceSets.create(variant.sourceSetName) {
//    excludeGradleCommonDependencies(this)
//    wireGradleVariantToCommonGradleVariant(this, commonSourceSet)
//  }
//
//  plugins.withType<JavaLibraryPlugin>().configureEach {
//    extensions.configure<JavaPluginExtension> {
//      registerFeature(variantSourceSet.name) {
//        usingSourceSet(variantSourceSet)
//        if (isGradlePlugin) {
//          capability(project.group.toString(), project.name, project.version.toString())
//        }
//
//        if (kotlinBuildProperties.publishGradlePluginsJavadoc) {
//          withJavadocJar()
//        }
//        withSourcesJar()
//      }
//
//      configurations.named(variantSourceSet.apiElementsConfigurationName, commonVariantAttributes())
//      configurations.named(
//        variantSourceSet.runtimeElementsConfigurationName,
//        commonVariantAttributes()
//      )
//    }
//
//    tasks.named<Jar>(variantSourceSet.sourcesJarTaskName) {
//      addEmbeddedSources()
//    }
//  }
//
//  plugins.withId("java-gradle-plugin") {
//    tasks.named<Copy>(variantSourceSet.processResourcesTaskName) {
//      val copyPluginDescriptors = rootSpec.addChild()
//      copyPluginDescriptors.into("META-INF/gradle-plugins")
//      copyPluginDescriptors.from(tasks.named("pluginDescriptors"))
//    }
//  }
//
//  configurations.configureEach {
//    if (isCanBeConsumed && this@configureEach.name.startsWith(variantSourceSet.name)) {
//      attributes {
//        attribute(
//          GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
//          objects.named(variant.minimalSupportedGradleVersion)
//        )
//      }
//    }
//  }
//
//  // KT-52138: Make module name the same for all variants, so KSP could access internal methods/properties
//  tasks.named<KotlinCompile>("compile${variantSourceSet.name.replaceFirstChar { it.uppercase() }}Kotlin") {
//    @Suppress("DEPRECATION")
//    kotlinOptions {
//      moduleName = this@createGradlePluginVariant.name
//    }
//  }
//
//  dependencies {
//    variantSourceSet.compileOnlyConfigurationName(kotlinStdlib())
//    variantSourceSet.compileOnlyConfigurationName("dev.gradleplugins:gradle-api:${variant.gradleApiVersion}")
//    if (this@createGradlePluginVariant.name != "kotlin-gradle-plugin-api" &&
//      this@createGradlePluginVariant.name != "android-test-fixes" &&
//      this@createGradlePluginVariant.name != "gradle-warnings-detector"
//    ) {
//      variantSourceSet.apiConfigurationName(project(":kotlin-gradle-plugin-api")) {
//        capabilities {
//          requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-${variant.sourceSetName}")
//        }
//      }
//    }
//  }
//
//  return variantSourceSet
//}
//
///**
// * All additional configuration attributes in plugin variant should be the same as in the 'main' variant.
// * Otherwise, Gradle <7.0 will fail to select plugin variant.
// */
//private fun Project.commonVariantAttributes(): Action<Configuration> = Action<Configuration> {
//  attributes {
//    attribute(
//      TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
//      objects.named(TargetJvmEnvironment.STANDARD_JVM)
//    )
//  }
//}
//
//
//// Will allow combining outputs of multiple SourceSets
//fun Project.publishShadowedJar(
//  sourceSet: SourceSet,
//  commonSourceSet: SourceSet
//) {
//  val jarTask = tasks.named<Jar>(sourceSet.jarTaskName)
//
//  val shadowJarTask = embeddableCompilerDummyForDependenciesRewriting(
//    taskName = "$EMBEDDABLE_COMPILER_TASK_NAME${sourceSet.jarTaskName.replaceFirstChar { it.uppercase() }}"
//  ) {
//    setupPublicJar(
//      jarTask.flatMap { it.archiveBaseName },
//      jarTask.flatMap { it.archiveClassifier }
//    )
//    addEmbeddedRuntime()
//    from(sourceSet.output)
//    from(commonSourceSet.output)
//
//    // When Gradle traverses the inputs, reject the shaded compiler JAR,
//    // which leads to the content of that JAR being excluded as well:
//    val compilerDummyJarConfiguration: FileCollection =
//      project.configurations.getByName("compilerDummyJar")
//    exclude { it.file == compilerDummyJarConfiguration.singleFile }
//  }
//
//  // Removing artifact produced by Jar task
//  if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) {
//    configurations["${sourceSet.runtimeElementsConfigurationName}$FIXED_CONFIGURATION_SUFFIX"]
//      .artifacts.removeAll { true }
//    configurations["${sourceSet.apiElementsConfigurationName}$FIXED_CONFIGURATION_SUFFIX"]
//      .artifacts.removeAll { true }
//  } else {
//    configurations[sourceSet.runtimeElementsConfigurationName]
//      .artifacts.removeAll { true }
//    configurations[sourceSet.apiElementsConfigurationName]
//      .artifacts.removeAll { true }
//  }
//
//  // Adding instead artifact from shadow jar task
//  configurations {
//    artifacts {
//      if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) {
//        add(
//          "${sourceSet.runtimeElementsConfigurationName}$FIXED_CONFIGURATION_SUFFIX",
//          shadowJarTask
//        )
//        add("${sourceSet.apiElementsConfigurationName}$FIXED_CONFIGURATION_SUFFIX", shadowJarTask)
//      } else {
//        add(sourceSet.apiElementsConfigurationName, shadowJarTask)
//        add(sourceSet.runtimeElementsConfigurationName, shadowJarTask)
//      }
//    }
//  }
//}
//
////fun Project.addBomCheckTask() {
////  val checkBomTask = tasks.register("checkGradlePluginsBom") {
////    group = "Validation"
////    description = "Check if project is added into Kotlin Gradle Plugins bom"
////
////    val bomBuildFile = project(":kotlin-gradle-plugins-bom").projectDir.resolve("build.gradle.kts")
////    val exceptions = listOf(
////      project(":gradle:android-test-fixes").path,
////      project(":gradle:gradle-warnings-detector").path,
////      project(":kotlin-gradle-build-metrics").path,
////      project(":kotlin-gradle-statistics").path,
////    )
////    val projectPath = this@addBomCheckTask.path
////
////    doLast {
////      if (projectPath in exceptions) return@doLast
////
////      val constraintsLines = bomBuildFile.readText()
////        .substringAfter("constraints {")
////        .substringBefore("}")
////        .split("\n")
////        .map { it.trim() }
////
////      val isContainingThisProject = constraintsLines.contains(
////        "api(project(\"$projectPath\"))"
////      )
////
////      if (!isContainingThisProject) {
////        throw GradleException(":kotlin-gradle-plugins-bom does not contain $projectPath project constraint!")
////      }
////    }
////  }
////
////  tasks.named("check") {
////    dependsOn(checkBomTask)
////  }
////}
//
//
//private val Project.sourceSets: SourceSetContainer
//  get() = extensions.getByType()
