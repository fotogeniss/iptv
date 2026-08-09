package com.prelude.iptv.ui.epg

/** App-owned EPG status identity. Display copy belongs to the Android resource boundary. */
sealed interface EpgStatus {
    data object Idle : EpgStatus
    data object Loading : EpgStatus
    data object LoadingWithExistingGuide : EpgStatus
    data class LoadFailed(val keptExistingGuide: Boolean) : EpgStatus
    data object LoadedWithoutMatches : EpgStatus
    data class Ready(val matches: Int, val fromDisk: Boolean = false) : EpgStatus
    data object Discovering : EpgStatus
    data class SourcesFound(val count: Int) : EpgStatus
    data object DiscoveryNeedsChannelIds : EpgStatus
    data object DiscoveryNoMatch : EpgStatus
    data object InvalidUrl : EpgStatus
    data object Downloading : EpgStatus
    data object DownloadingWithExistingGuide : EpgStatus
    data class DownloadFailed(val keptExistingGuide: Boolean) : EpgStatus
    data class Saved(val matches: Int) : EpgStatus
    data object SavedWithoutMatches : EpgStatus
}

enum class EpgFilter { Now, Later, All, Movies, Sports }

enum class EpgSourceKind {
    PlaylistSettings,
    EmbeddedM3u,
    XtreamProvider,
    Current,
    PublicDirectory,
}

/** URL and directory metadata remain data; the UI localizes only the source label. */
data class EpgSourceOption(
    val kind: EpgSourceKind,
    val url: String,
    val host: String = "",
    val matchedChannels: Int = 0,
)
