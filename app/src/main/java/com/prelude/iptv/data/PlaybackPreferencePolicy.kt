package com.prelude.iptv.data

object PlaybackPreferencePolicy {
    data class Language(val code: String, val label: String)

    val languages = listOf(
        Language("", "Αυτόματα"),
        Language("el", "Ελληνικά"),
        Language("en", "Αγγλικά"),
        Language("es", "Ισπανικά"),
        Language("fr", "Γαλλικά"),
        Language("de", "Γερμανικά"),
        Language("it", "Ιταλικά"),
        Language("pt", "Πορτογαλικά"),
        Language("ar", "Αραβικά")
    )
    val subtitleSizes = listOf(85, 100, 120)

    fun normalizeLanguage(code: String?): String =
        code?.trim()?.lowercase()?.takeIf { candidate -> languages.any { it.code == candidate } }.orEmpty()

    fun languageLabel(code: String?): String =
        languages.first { it.code == normalizeLanguage(code) }.label

    fun normalizeSubtitleSize(percent: Int): Int =
        subtitleSizes.minBy { kotlin.math.abs(it - percent) }

    fun subtitleSizeLabel(percent: Int): String = when (normalizeSubtitleSize(percent)) {
        85 -> "Μικρό"
        120 -> "Μεγάλο"
        else -> "Μεσαίο"
    }

    fun subtitleSearchLanguages(preferred: String?): List<String> {
        val normalized = normalizeLanguage(preferred)
        return if (normalized.isBlank()) listOf("el", "en")
        else listOf(normalized, "en").distinct()
    }
}
