package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.ui.UiState
import com.prelude.iptv.ui.epg.EpgStatus

/**
 * Monotonic generation boundary for source-scoped asynchronous work.
 *
 * Catalog callbacks are accepted only while their load generation is current.
 * Series callbacks carry their own series generation, so opening or closing
 * one details flow cannot publish into a newer one. Completion is gated on
 * the series generation alone: a background reload of the browsing list
 * (e.g. re-picking categories for the active section) bumps only the load
 * generation and must not silently discard episodes for a details flow the
 * user still has open. Switching or removing the source itself invalidates
 * both generations together via [invalidateAll], which still cancels any
 * pending series request.
 */
internal class SourceGenerationGate {
    data class SeriesRequest(
        val loadGeneration: Int,
        val seriesGeneration: Int,
    )

    private var loadGeneration: Int = 0
    private var seriesGeneration: Int = 0

    fun currentLoad(): Int = loadGeneration

    fun invalidateLoad(): Int {
        loadGeneration++
        return loadGeneration
    }

    fun invalidateSeries(): Int {
        seriesGeneration++
        return seriesGeneration
    }

    fun invalidateAll() {
        loadGeneration++
        seriesGeneration++
    }

    fun beginSeriesRequest(): SeriesRequest {
        seriesGeneration++
        return SeriesRequest(loadGeneration, seriesGeneration)
    }

    fun isCurrentLoad(generation: Int): Boolean = generation == loadGeneration

    fun isCurrent(request: SeriesRequest): Boolean =
        request.seriesGeneration == seriesGeneration
}


/** Clears every source-bound UI field while preserving profile/settings state. */
internal object SourceSwitchStatePolicy {
    fun apply(
        current: UiState,
        plan: SourceSwitchCoordinator.SwitchPlan,
    ): UiState = current.copy(
        currentIndex = plan.index,
        contentType = plan.contentType,
        favorites = plan.favorites,
        chooseContent = false,
        channels = emptyList(),
        groups = emptyList(),
        selectedGroup = UiState.ALL_GROUP,
        search = "",
        loading = false,
        loadingAllSections = false,
        loadedSections = emptySet(),
        status = "",
        epgLoaded = false,
        epgSources = emptyList(),
        epgStatus = EpgStatus.Idle,
        askRefreshMode = false,
        askLoadMode = false,
        pickCategories = false,
        categories = emptyList(),
        categoryPickerFromRefresh = false,
        categorySelectionIds = null,
        askLoadType = null,
        openSeriesTitle = null,
        seriesSeasons = emptyList(),
        seriesLoading = false,
    )
}

/**
 * Transaction boundary for an explicit playlist switch.
 *
 * The coordinator owns the ordering contract:
 *  1. persist the valid target index,
 *  2. invalidate every source generation,
 *  3. cancel old jobs and EPG work,
 *  4. release source-bound runtime objects,
 *  5. invalidate caches for the requested source,
 *  6. publish one clean source state,
 *  7. start one complete three-section load.
 *
 * Android lifecycle objects remain in MainViewModel through narrow callbacks.
 */
internal class SourceSwitchCoordinator(
    private val generationGate: SourceGenerationGate,
    private val callbacks: Callbacks,
) {
    data class SwitchPlan(
        val index: Int,
        val playlist: Playlist,
        val sourceId: String,
        val contentType: String,
        val hasRememberedChoice: Boolean,
        val favorites: Set<String>,
    )

    data class Callbacks(
        val playlists: () -> List<Playlist>,
        val persistLastPlaylist: (Int) -> Unit,
        val cancelActiveWork: () -> Unit,
        val cancelEpgLoad: () -> Unit,
        val resetSourceRuntime: () -> Unit,
        val invalidateRepository: (Playlist) -> Unit,
        val clearSourceSession: (Playlist) -> Unit,
        val lastSection: (String) -> String,
        val hasRememberedChoice: (String) -> Boolean,
        val favoritesFor: (String) -> Set<String>,
        val publish: (SwitchPlan) -> Unit,
        val autoLoad: () -> Unit,
    )

    fun switchTo(index: Int): Boolean {
        val playlist = callbacks.playlists().getOrNull(index) ?: return false

        callbacks.persistLastPlaylist(index)
        generationGate.invalidateAll()
        callbacks.cancelActiveWork()
        callbacks.cancelEpgLoad()
        callbacks.resetSourceRuntime()
        callbacks.invalidateRepository(playlist)
        callbacks.clearSourceSession(playlist)

        val sourceId = PlaylistIdentity.stableId(playlist)
        val contentType = callbacks.lastSection(sourceId)
        val hasRememberedChoice = callbacks.hasRememberedChoice("$sourceId:$contentType")
        val plan = SwitchPlan(
            index = index,
            playlist = playlist,
            sourceId = sourceId,
            contentType = contentType,
            hasRememberedChoice = hasRememberedChoice,
            favorites = callbacks.favoritesFor(sourceId),
        )
        callbacks.publish(plan)
        callbacks.autoLoad()
        return true
    }
}
