package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.ui.UiState
import com.prelude.iptv.ui.epg.EpgSourceKind
import com.prelude.iptv.ui.epg.EpgSourceOption
import com.prelude.iptv.ui.epg.EpgStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceSwitchCoordinatorTest {
    private val first = Playlist(
        name = "First",
        type = PlaylistType.M3U,
        source = "https://one.example/list.m3u",
    )
    private val second = Playlist(
        name = "Second",
        type = PlaylistType.XTREAM,
        server = "https://two.example",
        username = "u",
        password = "p",
    )

    @Test
    fun invalidIndexHasNoSideEffectsOrGenerationChange() {
        val gate = SourceGenerationGate()
        val events = mutableListOf<String>()
        val coordinator = coordinator(gate, events, known = true)

        assertFalse(coordinator.switchTo(9))
        assertTrue(events.isEmpty())
        assertTrue(gate.isCurrentLoad(0))
    }

    @Test
    fun validSwitchUsesStrictCancellationAndPublicationOrder() {
        val gate = SourceGenerationGate()
        val events = mutableListOf<String>()
        val coordinator = coordinator(gate, events, known = false)

        assertTrue(coordinator.switchTo(1))

        assertEquals(
            listOf(
                "persist:1",
                "cancel-work",
                "cancel-epg",
                "reset-runtime",
                "invalidate:${second.name}",
                "clear:${second.name}",
                "last-section:${PlaylistIdentity.stableId(second)}",
                "known:${PlaylistIdentity.stableId(second)}:vod",
                "favorites:${PlaylistIdentity.stableId(second)}",
                "publish:1:vod:false",
            ),
            events,
        )
        assertFalse(gate.isCurrentLoad(0))
    }

    @Test
    fun rememberedChoiceAutoLoadsOnlyAfterCleanStatePublication() {
        val gate = SourceGenerationGate()
        val events = mutableListOf<String>()
        val coordinator = coordinator(gate, events, known = true)

        assertTrue(coordinator.switchTo(0))

        assertEquals("publish:0:vod:true", events[events.lastIndex - 1])
        assertEquals("auto-load", events.last())
    }

    @Test
    fun unknownChoiceDoesNotStartNetworkLoad() {
        val events = mutableListOf<String>()
        val coordinator = coordinator(SourceGenerationGate(), events, known = false)

        assertTrue(coordinator.switchTo(0))
        assertFalse("auto-load" in events)
    }

    @Test
    fun switchPlanCarriesOnlyTargetSourceFavorites() {
        val targetId = PlaylistIdentity.stableId(second)
        var plan: SourceSwitchCoordinator.SwitchPlan? = null
        val gate = SourceGenerationGate()
        val coordinator = SourceSwitchCoordinator(
            gate,
            SourceSwitchCoordinator.Callbacks(
                playlists = { listOf(first, second) },
                persistLastPlaylist = {},
                cancelActiveWork = {},
                cancelEpgLoad = {},
                resetSourceRuntime = {},
                invalidateRepository = {},
                clearSourceSession = {},
                lastSection = { "series" },
                hasRememberedChoice = { true },
                favoritesFor = { sourceId -> if (sourceId == targetId) setOf("target") else setOf("wrong") },
                publish = { plan = it },
                autoLoad = {},
            ),
        )

        assertTrue(coordinator.switchTo(1))
        assertEquals(targetId, plan?.sourceId)
        assertEquals(setOf("target"), plan?.favorites)
        assertEquals("series", plan?.contentType)
    }

    @Test
    fun statePolicyClearsSourceBoundUiAndPreservesUserSettings() {
        val current = UiState(
            currentIndex = 0,
            contentType = "live",
            channels = listOf(Channel("Old")),
            groups = listOf(UiState.ALL_GROUP, "Old group"),
            selectedGroup = "Old group",
            search = "old query",
            loading = true,
            loadingAllSections = true,
            loadedSections = setOf("live"),
            status = "old status",
            favorites = setOf("old favorite"),
            epgLoaded = true,
            epgSources = listOf(EpgSourceOption(EpgSourceKind.Current, "old.xml")),
            epgStatus = EpgStatus.Ready(matches = 1),
            pickCategories = true,
            categories = listOf("1" to "Old"),
            askLoadMode = true,
            askRefreshMode = true,
            categoryPickerFromRefresh = true,
            categorySelectionIds = setOf("1"),
            askLoadType = "series",
            fontScale = 1.4f,
            lockedGroups = setOf("Adults"),
            sortMode = "az",
            openSeriesTitle = "Old series",
            seriesSeasons = listOf("Season 1" to listOf(Channel("Episode"))),
            seriesLoading = true,
        )
        val plan = SourceSwitchCoordinator.SwitchPlan(
            index = 1,
            playlist = second,
            sourceId = PlaylistIdentity.stableId(second),
            contentType = "vod",
            hasRememberedChoice = false,
            favorites = setOf("target favorite"),
        )

        val next = SourceSwitchStatePolicy.apply(current, plan)

        assertEquals(1, next.currentIndex)
        assertEquals("vod", next.contentType)
        assertEquals(setOf("target favorite"), next.favorites)
        assertTrue(next.chooseContent)
        assertTrue(next.channels.isEmpty())
        assertTrue(next.groups.isEmpty())
        assertEquals(UiState.ALL_GROUP, next.selectedGroup)
        assertEquals("", next.search)
        assertFalse(next.loading)
        assertFalse(next.loadingAllSections)
        assertTrue(next.loadedSections.isEmpty())
        assertEquals("", next.status)
        assertFalse(next.epgLoaded)
        assertTrue(next.epgSources.isEmpty())
        assertEquals(EpgStatus.Idle, next.epgStatus)
        assertFalse(next.pickCategories)
        assertTrue(next.categories.isEmpty())
        assertFalse(next.askLoadMode)
        assertFalse(next.askRefreshMode)
        assertFalse(next.categoryPickerFromRefresh)
        assertEquals(null, next.categorySelectionIds)
        assertEquals(null, next.askLoadType)
        assertEquals(null, next.openSeriesTitle)
        assertTrue(next.seriesSeasons.isEmpty())
        assertFalse(next.seriesLoading)
        assertEquals(1.4f, next.fontScale)
        assertEquals(setOf("Adults"), next.lockedGroups)
        assertEquals("az", next.sortMode)
    }

    @Test
    fun loadInvalidationRejectsEarlierCatalogToken() {
        val gate = SourceGenerationGate()
        val original = gate.currentLoad()

        gate.invalidateLoad()

        assertFalse(gate.isCurrentLoad(original))
        assertTrue(gate.isCurrentLoad(gate.currentLoad()))
    }

    @Test
    fun newSeriesRequestRejectsEarlierSeriesRequestWithoutChangingLoad() {
        val gate = SourceGenerationGate()
        val firstRequest = gate.beginSeriesRequest()
        val secondRequest = gate.beginSeriesRequest()

        assertFalse(gate.isCurrent(firstRequest))
        assertTrue(gate.isCurrent(secondRequest))
        assertTrue(gate.isCurrentLoad(firstRequest.loadGeneration))
    }

    @Test
    fun sourceSwitchRejectsCatalogAndSeriesTokensTogether() {
        val gate = SourceGenerationGate()
        val load = gate.currentLoad()
        val series = gate.beginSeriesRequest()

        gate.invalidateAll()

        assertFalse(gate.isCurrentLoad(load))
        assertFalse(gate.isCurrent(series))
    }

    @Test
    fun closingSeriesDoesNotInvalidateCatalogGeneration() {
        val gate = SourceGenerationGate()
        val load = gate.currentLoad()
        val series = gate.beginSeriesRequest()

        gate.invalidateSeries()

        assertTrue(gate.isCurrentLoad(load))
        assertFalse(gate.isCurrent(series))
    }

    private fun coordinator(
        gate: SourceGenerationGate,
        events: MutableList<String>,
        known: Boolean,
    ) = SourceSwitchCoordinator(
        gate,
        SourceSwitchCoordinator.Callbacks(
            playlists = { listOf(first, second) },
            persistLastPlaylist = { events += "persist:$it" },
            cancelActiveWork = { events += "cancel-work" },
            cancelEpgLoad = { events += "cancel-epg" },
            resetSourceRuntime = { events += "reset-runtime" },
            invalidateRepository = { events += "invalidate:${it.name}" },
            clearSourceSession = { events += "clear:${it.name}" },
            lastSection = {
                events += "last-section:$it"
                "vod"
            },
            hasRememberedChoice = {
                events += "known:$it"
                known
            },
            favoritesFor = {
                events += "favorites:$it"
                setOf("fav")
            },
            publish = {
                events += "publish:${it.index}:${it.contentType}:${it.hasRememberedChoice}"
            },
            autoLoad = { events += "auto-load" },
        ),
    )
}
