package buildsrc.utils

fun String.titlecaseFirstChar(): String =
  replaceFirstChar {
    when {
      it.isLowerCase() -> it.titlecase()
      else             -> it.toString()
    }
  }

fun List<String>.filterContains(substring: String): List<String> =
  filter { substring in it }
