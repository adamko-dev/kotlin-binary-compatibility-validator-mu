package foo

@Target(AnnotationTarget.FIELD)
annotation class HiddenField

@Target(AnnotationTarget.PROPERTY)
annotation class HiddenProperty

public class ClassWithProperties {
  @HiddenField
  var bar1 = 42

  @HiddenProperty
  var bar2 = 42
}
