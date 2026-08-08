package com.prelude.iptv.data

/**
 * Reads a quality hint only when the provider actually included one in its
 * title/category. It intentionally returns an empty value for unknown streams:
 * guessing FHD/4K before the player has inspected the video would mislead users.
 */
object ContentQualityPolicy {
    private val fourK = tokenRegex("4K|UHD|2160P?")
    private val twoK = tokenRegex("2K|QHD|1440P?")
    private val fullHd = tokenRegex("FHD|FULL[ ._-]?HD|1080[PI]?")
    private val hd = tokenRegex("HD|720[PI]?")
    private val sd = tokenRegex("SD|576[PI]?|480[PI]?")

    fun label(vararg hints: String): String {
        val text = hints.filter(String::isNotBlank).joinToString(" ")
        return when {
            fourK.containsMatchIn(text) -> "4K"
            twoK.containsMatchIn(text) -> "2K"
            fullHd.containsMatchIn(text) -> "FHD"
            hd.containsMatchIn(text) -> "HD"
            sd.containsMatchIn(text) -> "SD"
            else -> ""
        }
    }

    private fun tokenRegex(tokens: String) =
        Regex("(?i)(?:^|[^A-Z0-9])(?:$tokens)(?=\$|[^A-Z0-9])")
}
