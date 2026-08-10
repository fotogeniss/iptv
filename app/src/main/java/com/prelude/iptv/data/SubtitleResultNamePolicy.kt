package com.prelude.iptv.data

/** Παράγει πάντα χρήσιμο, μη κενό όνομα για αποτέλεσμα υποτίτλων. */
object SubtitleResultNamePolicy {
    private val genericNames = setOf(
        "subtitle", "subtitles", "sub", "unknown", "unnamed", "n/a", "none"
    )

    fun displayName(
        fileName: String,
        release: String,
        featureTitle: String,
        request: SubtitleSearchRequest,
        language: String,
        ordinal: Int,
        noTitleFallback: String,
    ): String {
        listOf(fileName, release, featureTitle)
            .map(::cleanCandidate)
            .firstOrNull(::isMeaningful)
            ?.let { return it }

        val mediaName = request.displayQuery().trim().ifBlank { noTitleFallback }
        return "$mediaName · ${language.uppercase().ifBlank { "SUB" }} ${ordinal + 1}"
    }

    private fun cleanCandidate(value: String): String = value
        .trim()
        .trim('.', '·', '-', '_', '|', ' ')
        .replace(Regex("""\s+"""), " ")

    private fun isMeaningful(value: String): Boolean =
        value.length >= 3 &&
            value.lowercase() !in genericNames &&
            value.any { it.isLetterOrDigit() }
}
