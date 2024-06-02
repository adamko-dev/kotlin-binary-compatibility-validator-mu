package dev.adamko.kotlin.binary_compatibility_validator.targets

import dev.adamko.kotlin.binary_compatibility_validator.internal.BCVInternalApi
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

abstract class BCVJvmTarget
@BCVInternalApi
@Inject
constructor(
  /**
   * The JVM platform being targeted.
   *
   * Targets with the same [platformType] will be grouped together into a single API declaration.
   */
  @get:Internal
  val platformType: String,
  @get:Internal
  internal val objects: ObjectFactory
) : BCVTarget(platformType), Named {

  @get:Classpath
  @get:Optional
//  @get:Internal
  abstract val inputClasses: ConfigurableFileCollection

  @get:Classpath
  @get:Optional
//  @get:Internal
  abstract val inputJar: RegularFileProperty

//  // create a new property to track the inputs, because Gradle sucks and doesn't allow for
//  // @Optional & @Classpath on a property
//  @get:Classpath
//  protected val inputClasspath: FileCollection
//    get() = objects.fileCollection()
//      .from(inputClasses)
//      .from(inputJar)
}
