package com.prelude.iptv.data

import java.util.Locale

object PlaybackPreferencePolicy {
    data class Language(val code: String)

    val languages = listOf(
        Language(""),
        Language("el"),
        Language("en"),
        Language("es"),
        Language("fr"),
        Language("de"),
        Language("it"),
        Language("pt"),
        Language("ar")
    )
    val subtitleSizes = listOf(85, 100, 120)

    fun normalizeLanguage(code: String?): String =
        code?.trim()?.lowercase(Locale.ROOT)?.takeIf { candidate -> languages.any { it.code == candidate } }.orEmpty()

    fun normalizeSubtitleSize(percent: Int): Int =
        subtitleSizes.minBy { kotlin.math.abs(it - percent) }

    fun subtitleSearchLanguages(preferred: String?): List<String> {
        val normalized = normalizeLanguage(preferred)
        return if (normalized.isBlank()) listOf("el", "en")
        else listOf(normalized, "en").distinct()
    }
}
