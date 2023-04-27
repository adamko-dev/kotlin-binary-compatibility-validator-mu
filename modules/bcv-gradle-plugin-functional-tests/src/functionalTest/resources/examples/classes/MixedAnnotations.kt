package mixed

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.FIELD,
  AnnotationTarget.FUNCTION
)
annotation class PublicApi

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.FIELD,
  AnnotationTarget.FUNCTION
)
annotation class PrivateApi

@PublicApi
class MarkedPublicWithPrivateMembers {
  @PrivateApi
  var private1 = 42

  @field:PrivateApi
  var private2 = 15

  @PrivateApi
  fun privateFun() = Unit

  @PublicApi
  @PrivateApi
  fun privateFun2() = Unit

  fun otherFun() = Unit
}

// Member annotations should be ignored in explicitly private classes
@PrivateApi
class MarkedPrivateWithPublicMembers {
  @PublicApi
  var public1 = 42

  @field:PublicApi
  var public2 = 15

  @PublicApi
  fun publicFun() = Unit

  fun otherFun() = Unit
}

@PrivateApi
@PublicApi
class PublicAndPrivateFilteredOut
