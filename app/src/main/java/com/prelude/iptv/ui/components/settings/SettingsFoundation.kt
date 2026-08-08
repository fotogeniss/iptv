package com.prelude.iptv.ui.components.settings

import androidx.compose.runtime.Immutable
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.data.SourceLoadProgress
import com.prelude.iptv.data.PlaylistType

@Immutable
data class SettingsSourceUi(
    val index: Int,
    val name: String,
    val type: PlaylistType,
    val endpoint: String,
    val channelCount: Int?,
    val current: Boolean,
    val loading: Boolean = false,
    val progressPercent: Int? = null,
    val progressStage: String = ""
) {
    val typeLabel: String
        get() = when (type) {
            PlaylistType.M3U -> "M3U"
            PlaylistType.XTREAM -> "XTREAM"
            PlaylistType.STALKER -> "PORTAL"
        }

    val statusLabel: String
        get() = when {
            loading -> progressPercent?.let { "$it%" } ?: "Φόρτωση"
            current && channelCount != null && channelCount > 0 -> "Ενεργή"
            current -> "Επιλεγμένη"
            else -> "Αποθηκευμένη"
        }
}

enum class SettingsPage(val label: String) {
    Sources("Πηγές"),
    Playback("Αναπαραγωγή"),
    Appearance("Εμφάνιση"),
    Account("Λογαριασμός"),
    About("Σχετικά")
}

fun buildSettingsSources(
    playlists: List<Playlist>,
    currentIndex: Int,
    currentChannelCount: Int,
    sourceProgress: Map<String, SourceLoadProgress> = emptyMap()
): List<SettingsSourceUi> = playlists.mapIndexed { index, playlist ->
    val progress = sourceProgress[PlaylistIdentity.stableId(playlist)]
    SettingsSourceUi(
        index = index,
        name = playlist.name.ifBlank { "Πηγή ${index + 1}" },
        type = playlist.type,
        endpoint = maskedEndpoint(playlist),
        channelCount = if (index == currentIndex && currentChannelCount > 0) currentChannelCount else null,
        current = index == currentIndex,
        loading = progress?.active == true,
        progressPercent = progress?.percent,
        progressStage = progress?.stage.orEmpty()
    )
}

fun playerModeLabel(mode: String): String = when (mode) {
    "exo" -> "ExoPlayer"
    "vlc" -> "VLC"
    else -> "Αυτόματο"
}

fun autoFrameRateLabel(mode: String): String = when (mode) {
    "seamless" -> "Ομαλό"
    "always" -> "Πλήρες"
    else -> "OFF"
}

private fun maskedEndpoint(playlist: Playlist): String {
    val raw = when (playlist.type) {
        PlaylistType.M3U -> playlist.source
        PlaylistType.XTREAM -> playlist.server
        PlaylistType.STALKER -> playlist.portal
    }.trim()
    if (raw.isBlank()) return "Τοπική πηγή"
    if (!playlist.isUrl && playlist.type == PlaylistType.M3U) return "Τοπικό αρχείο"

    return runCatching {
        val normalized = if (raw.contains("://")) raw else "https://$raw"
        val uri = java.net.URI(normalized)
        val host = uri.host?.removePrefix("www.") ?: raw.substringBefore('/').substringBefore(':')
        val port = if (uri.port > 0) ":${uri.port}" else ""
        "$host$port"
    }.getOrDefault(raw.take(42))
}
