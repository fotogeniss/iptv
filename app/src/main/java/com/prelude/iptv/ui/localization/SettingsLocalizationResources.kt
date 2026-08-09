package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.data.PlaybackPreferencePolicy
import com.prelude.iptv.player.BufferProfile
import com.prelude.iptv.ui.components.settings.AutoFrameRateOption
import com.prelude.iptv.ui.components.settings.PlayerModeOption

@Composable
fun localizedPlayerMode(mode: String): String =
    stringResource(PlayerModeOption.fromStorage(mode).labelRes())

@Composable
fun localizedAutoFrameRate(mode: String): String =
    stringResource(AutoFrameRateOption.fromStorage(mode).shortLabelRes())

@StringRes
fun PlayerModeOption.labelRes(): Int = when (this) {
    PlayerModeOption.Automatic -> R.string.settings_player_mode_auto_description
    PlayerModeOption.ExoPlayer -> R.string.settings_player_mode_exo
    PlayerModeOption.Vlc -> R.string.settings_player_mode_vlc
}

@StringRes
fun AutoFrameRateOption.labelRes(): Int = when (this) {
    AutoFrameRateOption.Off -> R.string.settings_afr_off
    AutoFrameRateOption.Seamless -> R.string.settings_afr_seamless
    AutoFrameRateOption.Always -> R.string.settings_afr_always
}

@StringRes
fun AutoFrameRateOption.shortLabelRes(): Int = when (this) {
    AutoFrameRateOption.Off -> R.string.settings_afr_off
    AutoFrameRateOption.Seamless -> R.string.settings_afr_seamless_short
    AutoFrameRateOption.Always -> R.string.settings_afr_always_short
}

@StringRes
fun AutoFrameRateOption.descriptionRes(): Int = when (this) {
    AutoFrameRateOption.Off -> R.string.settings_afr_off_description
    AutoFrameRateOption.Seamless -> R.string.settings_afr_seamless_description
    AutoFrameRateOption.Always -> R.string.settings_afr_always_description
}

@StringRes
fun BufferProfile.labelRes(): Int = when (this) {
    BufferProfile.LOW -> R.string.settings_buffer_low_label
    BufferProfile.NORMAL -> R.string.settings_buffer_normal_label
    BufferProfile.HIGH -> R.string.settings_buffer_high_label
}

@StringRes
fun BufferProfile.titleRes(): Int = when (this) {
    BufferProfile.LOW -> R.string.settings_buffer_low_title
    BufferProfile.NORMAL -> R.string.settings_buffer_normal_title
    BufferProfile.HIGH -> R.string.settings_buffer_high_title
}

@StringRes
fun BufferProfile.descriptionRes(): Int = when (this) {
    BufferProfile.LOW -> R.string.settings_buffer_low_description
    BufferProfile.NORMAL -> R.string.settings_buffer_normal_description
    BufferProfile.HIGH -> R.string.settings_buffer_high_description
}

@StringRes
fun preferenceLanguageLabelRes(code: String?): Int = when (PlaybackPreferencePolicy.normalizeLanguage(code)) {
    "el" -> R.string.settings_language_greek
    "en" -> R.string.settings_language_english
    "es" -> R.string.settings_language_spanish
    "fr" -> R.string.settings_language_french
    "de" -> R.string.settings_language_german
    "it" -> R.string.settings_language_italian
    "pt" -> R.string.settings_language_portuguese
    "ar" -> R.string.settings_language_arabic
    else -> R.string.settings_language_automatic
}

@StringRes
fun subtitleSizeLabelRes(percent: Int): Int = when (PlaybackPreferencePolicy.normalizeSubtitleSize(percent)) {
    85 -> R.string.settings_subtitle_size_small
    120 -> R.string.settings_subtitle_size_large
    else -> R.string.settings_subtitle_size_medium
}
