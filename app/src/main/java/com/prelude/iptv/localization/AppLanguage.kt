package com.prelude.iptv.localization

/** Stable app-language values; provider and playback languages are separate. */
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    GREEK("el"),
    ENGLISH("en"),
    ;

    companion object {
        fun fromLanguageTag(languageTag: String?): AppLanguage {
            val language = languageTag.orEmpty().substringBefore('-')
            return entries.firstOrNull {
                it.languageTag?.equals(language, ignoreCase = true) == true
            } ?: SYSTEM
        }
    }
}

object LocalizationRolloutPolicy {
    fun pickerVisible(ownerQaBuild: Boolean, translationParityComplete: Boolean): Boolean =
        ownerQaBuild || translationParityComplete
}
