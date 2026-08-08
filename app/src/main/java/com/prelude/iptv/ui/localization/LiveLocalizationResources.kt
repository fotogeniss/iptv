package com.prelude.iptv.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.ui.UiState
import com.prelude.iptv.ui.components.live.LiveFilterOption
import com.prelude.iptv.ui.components.live.LiveRemaining
import java.text.NumberFormat

/** Reserved filter IDs are app copy; provider group labels pass through unchanged. */
@Composable
fun LiveFilterOption.localizedLabel(): String = providerLabel ?: when (id) {
    "all" -> stringResource(R.string.live_all)
    "favorites" -> stringResource(R.string.live_favorites)
    "recent" -> stringResource(R.string.live_recent)
    else -> id.removePrefix("group:")
}

/** Only app-owned synthetic groups are localized; provider group names are data. */
@Composable
fun localizedLiveGroupLabel(group: String): String = when (group) {
    UiState.ALL_GROUP -> stringResource(R.string.live_all_channels)
    UiState.FAV_GROUP -> stringResource(R.string.live_favorites)
    else -> group
}

@Composable
fun localizedLiveRemaining(remaining: LiveRemaining?): String {
    if (remaining == null) return stringResource(R.string.live_broadcast)
    return if (remaining.hours > 0) {
        pluralStringResource(
            R.plurals.live_hours_minutes_remaining,
            remaining.hours,
            remaining.hours,
            remaining.minutes,
        )
    } else {
        pluralStringResource(
            R.plurals.live_minutes_remaining,
            remaining.minutes,
            remaining.minutes,
        )
    }
}

@Composable
fun localizedLiveProgress(progress: Float): String =
    NumberFormat.getPercentInstance(LocalConfiguration.current.locales[0]).format(progress)
