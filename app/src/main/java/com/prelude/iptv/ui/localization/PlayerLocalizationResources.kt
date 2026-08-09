package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.ui.player.AspectMode
import java.text.NumberFormat

@StringRes
fun AspectMode.labelRes(): Int = when (this) {
    AspectMode.FIT -> R.string.player_aspect_fit_short
    AspectMode.FILL -> R.string.player_aspect_fill_short
    AspectMode.FORCE_4_3 -> R.string.player_aspect_4_3
    AspectMode.FORCE_16_9 -> R.string.player_aspect_16_9
}

@StringRes
fun AspectMode.badgeLabelRes(): Int = when (this) {
    AspectMode.FIT -> R.string.player_aspect_badge_fit
    AspectMode.FILL -> R.string.player_aspect_badge_fill
    AspectMode.FORCE_4_3 -> R.string.player_aspect_4_3
    AspectMode.FORCE_16_9 -> R.string.player_aspect_16_9
}

@Composable
fun localizedSubtitleBackground(value: String): String = stringResource(
    when (value) {
        "shadow" -> R.string.player_subtitle_background_shadow
        "box" -> R.string.player_subtitle_background_box
        else -> R.string.player_subtitle_background_none
    }
)

@Composable
fun localizedPlaybackSpeed(speed: Float): String =
    NumberFormat.getNumberInstance(LocalConfiguration.current.locales[0]).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }.format(speed) + "×"
