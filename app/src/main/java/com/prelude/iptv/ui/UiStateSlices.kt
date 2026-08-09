package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.SourceLoadProgress
import com.prelude.iptv.ui.epg.EpgSourceOption
import com.prelude.iptv.ui.epg.EpgStatus

/**
 * Small read models exposed to Compose route boundaries.
 *
 * The mutable application state remains centralized for now so behavior is not
 * changed, but screens no longer subscribe to unrelated fields. Data-class
 * equality plus distinctUntilChanged prevents emissions when only another
 * feature's state changes.
 */
data class AppShellUiState(
    val playlists: List<Playlist> = emptyList(),
    val currentIndex: Int = 0,
    val fontScale: Float = 1.0f
)

data class CatalogUiState(
    val contentType: String = "live",
    val channels: List<Channel> = emptyList(),
    val groups: List<String> = emptyList(),
    val selectedGroup: String = UiState.ALL_GROUP,
    val search: String = "",
    val loading: Boolean = false,
    val loadingAllSections: Boolean = false,
    val loadedSections: Set<String> = emptySet(),
    val recentsVersion: Int = 0,
    val lockedGroups: Set<String> = emptySet(),
    val parentalUnlocked: Boolean = false,
    val sortMode: String = "default",
    val status: String = "",
    val favorites: Set<String> = emptySet(),
    val chooseContent: Boolean = false,
    val epgLoaded: Boolean = false,
    val pickCategories: Boolean = false,
    val categories: List<Pair<String, String>> = emptyList(),
    val askLoadMode: Boolean = false,
    val askRefreshMode: Boolean = false,
    val categoryPickerFromRefresh: Boolean = false,
    val categorySelectionIds: Set<String>? = null,
    val seriesSeasons: List<Pair<String, List<Channel>>> = emptyList(),
    val seriesLoading: Boolean = false
)

data class CatalogProgressUiState(
    val sourceProgress: Map<String, SourceLoadProgress> = emptyMap()
)

data class SettingsUiState(
    val playlists: List<Playlist> = emptyList(),
    val currentIndex: Int = 0,
    val currentChannelCount: Int = 0,
    val sourceProgress: Map<String, SourceLoadProgress> = emptyMap(),
    val fontScale: Float = 1.0f
)

data class EpgUiState(
    val loaded: Boolean = false,
    val sources: List<EpgSourceOption> = emptyList(),
    val status: EpgStatus = EpgStatus.Idle,
)

data class ExportUiState(
    val favorites: Set<String> = emptySet(),
    val relayRunning: Boolean = false,
    val relayUrl: String = ""
)

internal fun UiState.toAppShellUiState() = AppShellUiState(
    playlists = playlists,
    currentIndex = currentIndex,
    fontScale = fontScale
)

internal fun UiState.toCatalogUiState() = CatalogUiState(
    contentType = contentType,
    channels = channels,
    groups = groups,
    selectedGroup = selectedGroup,
    search = search,
    loading = loading,
    loadingAllSections = loadingAllSections,
    loadedSections = loadedSections,
    recentsVersion = recentsVersion,
    lockedGroups = lockedGroups,
    parentalUnlocked = parentalUnlocked,
    sortMode = sortMode,
    status = status,
    favorites = favorites,
    chooseContent = chooseContent,
    epgLoaded = epgLoaded,
    pickCategories = pickCategories,
    categories = categories,
    askLoadMode = askLoadMode,
    askRefreshMode = askRefreshMode,
    categoryPickerFromRefresh = categoryPickerFromRefresh,
    categorySelectionIds = categorySelectionIds,
    seriesSeasons = seriesSeasons,
    seriesLoading = seriesLoading
)

internal fun UiState.toCatalogProgressUiState() = CatalogProgressUiState(
    sourceProgress = sourceProgress
)

internal fun UiState.toSettingsUiState() = SettingsUiState(
    playlists = playlists,
    currentIndex = currentIndex,
    currentChannelCount = channels.size,
    sourceProgress = sourceProgress,
    fontScale = fontScale
)

internal fun UiState.toEpgUiState() = EpgUiState(
    loaded = epgLoaded,
    sources = epgSources,
    status = epgStatus
)

internal fun UiState.toExportUiState() = ExportUiState(
    favorites = favorites,
    relayRunning = relayRunning,
    relayUrl = relayUrl
)
