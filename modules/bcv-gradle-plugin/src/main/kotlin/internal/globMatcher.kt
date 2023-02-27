package dev.adamko.kotlin.binary_compatibility_validator.internal


/** Converts a glob-string to a [Regex] */
internal fun globToRegex(glob: String, separatorChars: String): Regex {

  val separatorsEscaped = separatorChars.escapeRegexChars()

  return glob
    .escapeRegexChars()
    .replace(Regex("""(?<doubleStar>\\\*\\\*)|(?<singleStar>\\\*)|(?<singleChar>\\\?)""")) {
      when {
        it.groups["doubleStar"] != null -> ".*?"
        it.groups["singleStar"] != null -> "[^${separatorsEscaped}]*?"
        it.groups["singleChar"] != null -> "[^${separatorsEscaped}]?"
        else                            -> error("could not convert '$glob' to regex")
      }
    }.toRegex(RegexOption.IGNORE_CASE)
}

private fun String.escapeRegexChars() = replace(Regex("""\W"""), """\\$0""")
