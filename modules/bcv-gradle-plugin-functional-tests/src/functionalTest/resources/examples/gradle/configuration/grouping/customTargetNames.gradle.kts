kotlin {
  linuxX64("linuxA") {
    attributes {
      attribute(Attribute.of("variant", String::class.java), "a")
    }
  }
  linuxX64("linuxB") {
    attributes {
      attribute(Attribute.of("variant", String::class.java), "b")
    }
  }
}
