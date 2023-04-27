package foo

@Target(AnnotationTarget.CLASS)
annotation class PublicClass

@Target(AnnotationTarget.FIELD)
annotation class PublicField

@Target(AnnotationTarget.PROPERTY)
annotation class PublicProperty

public class ClassWithPublicMarkers {
  @PublicField
  var bar1 = 42

  @PublicProperty
  var bar2 = 42

  @PublicClass
  class MarkedClass {
    val bar1 = 41
  }

  var notMarkedPublic = 42

  class NotMarkedClass
}
