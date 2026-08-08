package com.prelude.iptv.ui

enum class SearchKeyboardMode { GREEK, LATIN, NUMERIC }

enum class SearchKeyboardAction { CHARACTER, SPACE, BACKSPACE, CLEAR, GREEK, LATIN, NUMERIC }

data class SearchKeyboardKey(
    val action: SearchKeyboardAction,
    val character: String = "",
)

/** Keyboard identity and input stay locale-independent; the UI owns key labels. */
object SearchKeyboardPolicy {
    private val greekLetters = listOf(
        "Α", "Ά", "Β", "Γ", "Δ", "Ε", "Έ", "Ζ", "Η", "Ή", "Θ", "Ι", "Ί",
        "Κ", "Λ", "Μ", "Ν", "Ξ", "Ο", "Ό", "Π", "Ρ", "Σ", "Τ", "Υ", "Ύ",
        "Φ", "Χ", "Ψ", "Ω", "Ώ",
    )

    fun initialMode(language: String): SearchKeyboardMode =
        if (language == "el") SearchKeyboardMode.GREEK else SearchKeyboardMode.LATIN

    fun keys(mode: SearchKeyboardMode): List<SearchKeyboardKey> {
        val characters = when (mode) {
            SearchKeyboardMode.GREEK -> greekLetters
            SearchKeyboardMode.LATIN -> ('A'..'Z').map(Char::toString)
            SearchKeyboardMode.NUMERIC -> ('0'..'9').map(Char::toString)
        }.map { SearchKeyboardKey(SearchKeyboardAction.CHARACTER, it) }
        val modes = when (mode) {
            SearchKeyboardMode.GREEK -> listOf(SearchKeyboardAction.LATIN, SearchKeyboardAction.NUMERIC)
            SearchKeyboardMode.LATIN -> listOf(SearchKeyboardAction.GREEK, SearchKeyboardAction.NUMERIC)
            SearchKeyboardMode.NUMERIC -> listOf(SearchKeyboardAction.GREEK, SearchKeyboardAction.LATIN)
        }.map(::SearchKeyboardKey)
        return characters + listOf(
            SearchKeyboardKey(SearchKeyboardAction.SPACE),
            SearchKeyboardKey(SearchKeyboardAction.BACKSPACE),
        ) + modes + SearchKeyboardKey(SearchKeyboardAction.CLEAR)
    }
}
