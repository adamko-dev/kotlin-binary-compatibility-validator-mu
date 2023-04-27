configure<dev.adamko.kotlin.binary_compatibility_validator.BCVProjectExtension> {
  publicMarkers.add("foo.PublicClass")
  publicMarkers.add("foo.PublicField")
  publicMarkers.add("foo.PublicProperty")

  publicPackages.add("foo.api")
  publicClasses.add("foo.PublicClass")
}
