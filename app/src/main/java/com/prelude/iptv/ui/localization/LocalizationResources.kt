package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import com.prelude.iptv.R
import com.prelude.iptv.localization.AppLanguage
import com.prelude.iptv.ui.components.settings.SettingsPage
import com.prelude.iptv.ui.navigation.PrimaryContentDestination

@StringRes
fun PrimaryContentDestination.labelRes(): Int = when (this) {
    PrimaryContentDestination.HOME -> R.string.nav_home
    PrimaryContentDestination.LIVE -> R.string.nav_live
    PrimaryContentDestination.MOVIES -> R.string.nav_movies
    PrimaryContentDestination.SERIES -> R.string.nav_series
    PrimaryContentDestination.SEARCH -> R.string.nav_search
}

@StringRes
fun SettingsPage.labelRes(): Int = when (this) {
    SettingsPage.Sources -> R.string.settings_nav_sources
    SettingsPage.Playback -> R.string.settings_nav_playback
    SettingsPage.Appearance -> R.string.settings_nav_appearance
    SettingsPage.Account -> R.string.settings_nav_account
    SettingsPage.About -> R.string.settings_nav_about
}

@StringRes
fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.GREEK -> R.string.language_greek
    AppLanguage.ENGLISH -> R.string.language_english
}

@StringRes
fun AppLanguage.summaryRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.language_system_summary
    AppLanguage.GREEK -> R.string.language_greek_summary
    AppLanguage.ENGLISH -> R.string.language_english_summary
}
