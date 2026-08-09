package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.ui.epg.EpgFilter
import com.prelude.iptv.ui.epg.EpgSourceKind
import com.prelude.iptv.ui.epg.EpgSourceOption
import com.prelude.iptv.ui.epg.EpgStatus

@StringRes
fun EpgFilter.labelRes(): Int = when (this) {
    EpgFilter.Now -> R.string.epg_tab_now
    EpgFilter.Later -> R.string.epg_tab_later
    EpgFilter.All -> R.string.epg_tab_all
    EpgFilter.Movies -> R.string.epg_tab_movies
    EpgFilter.Sports -> R.string.epg_tab_sports
}

@Composable
fun EpgStatus.localizedText(): String = when (this) {
    EpgStatus.Idle -> ""
    EpgStatus.Loading -> stringResource(R.string.epg_status_loading)
    EpgStatus.LoadingWithExistingGuide -> stringResource(R.string.epg_status_loading_keep)
    is EpgStatus.LoadFailed -> stringResource(
        if (keptExistingGuide) R.string.epg_status_load_failed_keep else R.string.epg_status_load_failed
    )
    EpgStatus.LoadedWithoutMatches -> stringResource(R.string.epg_status_no_matches)
    is EpgStatus.Ready -> pluralStringResource(
        if (fromDisk) R.plurals.epg_status_ready_disk else R.plurals.epg_status_ready,
        matches,
        matches,
    )
    EpgStatus.Discovering -> stringResource(R.string.epg_status_discovering)
    is EpgStatus.SourcesFound -> pluralStringResource(R.plurals.epg_status_sources_found, count, count)
    EpgStatus.DiscoveryNeedsChannelIds -> stringResource(R.string.epg_status_missing_channel_ids)
    EpgStatus.DiscoveryNoMatch -> stringResource(R.string.epg_status_no_compatible_source)
    EpgStatus.InvalidUrl -> stringResource(R.string.epg_status_invalid_url)
    EpgStatus.Downloading -> stringResource(R.string.epg_status_downloading)
    EpgStatus.DownloadingWithExistingGuide -> stringResource(R.string.epg_status_downloading_keep)
    is EpgStatus.DownloadFailed -> stringResource(
        if (keptExistingGuide) R.string.epg_status_download_failed_keep else R.string.epg_status_download_failed
    )
    is EpgStatus.Saved -> pluralStringResource(R.plurals.epg_status_saved, matches, matches)
    EpgStatus.SavedWithoutMatches -> stringResource(R.string.epg_status_saved_no_matches)
}

@Composable
fun EpgSourceOption.localizedLabel(): String = when (kind) {
    EpgSourceKind.PlaylistSettings -> stringResource(R.string.epg_source_playlist_settings)
    EpgSourceKind.EmbeddedM3u -> stringResource(R.string.epg_source_embedded_m3u)
    EpgSourceKind.XtreamProvider -> stringResource(R.string.epg_source_xtream)
    EpgSourceKind.Current -> stringResource(R.string.epg_source_current)
    EpgSourceKind.PublicDirectory -> {
        val displayHost = host.ifBlank { stringResource(R.string.epg_source_public) }
        pluralStringResource(
            R.plurals.epg_source_public_matches,
            matchedChannels,
            displayHost,
            matchedChannels,
        )
    }
}

@Composable
fun localizedEpgRuntime(totalMinutes: Long): String {
    val minutes = totalMinutes.coerceAtLeast(1L)
    return if (minutes >= 60L) {
        stringResource(R.string.epg_duration_hours_minutes, minutes / 60L, minutes % 60L)
    } else {
        stringResource(R.string.epg_duration_minutes, minutes)
    }
}
