package buildsrc.utils

fun String.titlecaseFirstChar(): String =
  replaceFirstChar {
    when {
      it.isLowerCase() -> it.titlecase()
      else             -> it.toString()
    }
  }
