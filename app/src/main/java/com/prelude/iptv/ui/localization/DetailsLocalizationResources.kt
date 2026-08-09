package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import com.prelude.iptv.R
import com.prelude.iptv.ui.WatchProgress
import com.prelude.iptv.ui.WatchProgressPolicy
import com.prelude.iptv.ui.components.details.DetailSection
import java.text.NumberFormat

@StringRes
fun DetailSection.labelRes(): Int = when (this) {
    DetailSection.Episodes -> R.string.details_episodes
    DetailSection.About -> R.string.details_about
    DetailSection.Cast -> R.string.details_cast
    DetailSection.Similar -> R.string.details_similar
}

@Composable
fun localizedSeasonCount(count: Int): String =
    pluralStringResource(R.plurals.details_season_count, count, count)

@Composable
fun localizedEpisodeCount(count: Int): String =
    pluralStringResource(R.plurals.details_episode_count, count, count)

@Composable
fun localizedWatchRemaining(progress: WatchProgress): String {
    val remaining = WatchProgressPolicy.remaining(progress)
    return when {
        remaining.hours > 0 && remaining.minutes > 0 -> pluralStringResource(
            R.plurals.details_remaining_hours_minutes,
            remaining.hours,
            remaining.hours,
            remaining.minutes,
        )
        remaining.hours > 0 -> pluralStringResource(
            R.plurals.details_remaining_hours,
            remaining.hours,
            remaining.hours,
        )
        else -> pluralStringResource(
            R.plurals.details_remaining_minutes,
            remaining.minutes,
            remaining.minutes,
        )
    }
}

@Composable
fun localizedProgressPercent(progress: WatchProgress): String =
    NumberFormat.getPercentInstance(LocalConfiguration.current.locales[0]).format(progress.fraction)
