package com.prelude.iptv.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** The only write boundary for Android per-app locales. Call from the main thread. */
object AppLanguageController {
    fun selected(): AppLanguage = AppLanguage.fromLanguageTag(
        AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
    )

    fun select(language: AppLanguage) {
        val target = language.languageTag?.let(LocaleListCompat::forLanguageTags)
            ?: LocaleListCompat.getEmptyLocaleList()
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != target.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(target)
        }
    }
}
