package buildsrc.conventions

plugins {
  id("buildsrc.conventions.base")
  id("buildsrc.conventions.java-base")
  id("org.gradle.kotlin.kotlin-dsl")
  id("com.gradle.plugin-publish")
  `maven-publish`
}

tasks.validatePlugins {
  enableStricterValidation = true
}

sourceSets {
  configureEach {
    java.setSrcDirs(emptyList<File>())
  }
}

//
///** These dependencies will be provided by Gradle, and we should prevent version conflict */
//fun Configuration.excludeGradleCommonDependencies() {
//  dependencies
//    .withType<ModuleDependency>()
//    .configureEach {
//      exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
//      exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
//      exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
//      exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
//      exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
//      exclude(group = "org.jetbrains.kotlin", module = "kotlin-script-runtime")
//    }
//}
//
//// Exclude Gradle runtime from given SourceSet configurations
////configurations[sourceSet.implementationConfigurationName].excludeGradleCommonDependencies()
////configurations[sourceSet.apiConfigurationName].excludeGradleCommonDependencies()
////configurations[sourceSet.runtimeOnlyConfigurationName].excludeGradleCommonDependencies()
//
//dependencies {
//  constraints {
//
//  }
//}
//
//abstract class AsmCapability : ComponentMetadataRule {
//}
//
//@CacheableRule
//abstract class GradleKotlinLibsCapability () : ComponentMetadataRule {
//
//  private val kotlinGroup = "org.jetbrains.kotlin"
//  private val kotlinLibs = setOf(
//    "kotlin-stdlib",
//  "kotlin-stdlib-jdk7",
//  "kotlin-stdlib-jdk8",
//  "kotlin-stdlib-common",
//  "kotlin-reflect",
//  "kotlin-script-runtime",
//  )
//
//  override fun execute(context: ComponentMetadataContext) = context.details.run {
//    if (id.group == kotlinGroup && id.name in kotlinLibs) {
//      allVariants {
//        withCapabilities {
//          addCapability(kotlinGroup, "gradle-kotlin-embedded", id.version)
//        }
//      }
//    }
//  }
//  override fun execute(context: ComponentMetadataContext) { context.details.withVariant("compile") {
//      attributes {
//        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, jvmVersion)
//        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
//      }
//    }
//  }
//}
//dependencies {
//  components {
//    withModule<TargetJvmVersionRule>("commons-io:commons-io") {
//      params(7)
//    }
//    withModule<TargetJvmVersionRule>("commons-collections:commons-collections") {
//      params(8)
//    }
//  }
//  implementation("commons-io:commons-io:2.6")
//  implementation("commons-collections:commons-collections:3.2.2")
//}
