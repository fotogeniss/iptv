package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.ui.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogLoadCoordinatorTest {
    private val playlist = Playlist(
        name = "Local",
        type = PlaylistType.M3U,
        source = "memory://catalog",
    )

    @Test
    fun m3uCategoriesAreSectionScopedUniqueAndSorted() = runTest {
        val coordinator = coordinator(
            listOf(
                Channel("Sports 1", group = "Sports", kind = "live"),
                Channel("News 1", group = "News", kind = "live"),
                Channel("Sports 2", group = "Sports", kind = "live"),
                Channel("Movie", group = "Cinema", kind = "vod"),
                Channel("No group", group = "", kind = "live"),
            )
        )

        assertEquals(
            listOf("News" to "News", "Sports" to "Sports", "Χωρίς ομάδα" to "Χωρίς ομάδα"),
            coordinator.categories(playlist, "live")
        )
    }

    @Test
    fun sectionPublishesNormalizedProgressAndReturnsSameFinalCatalog() = runTest {
        val coordinator = coordinator(
            listOf(
                Channel("One", group = "News", kind = "live", url = "one"),
                Channel("One duplicate", group = "News", kind = "live", url = "one"),
                Channel("Movie", group = "Cinema", kind = "vod", url = "movie"),
            )
        )
        val partials = mutableListOf<List<String>>()

        val loaded = coordinator.section(playlist, "live", listOf("News")) {
            partials += it.items.map(Channel::name)
        }

        assertEquals(listOf("One"), loaded.items.map(Channel::name))
        assertEquals(loaded.items.map(Channel::name), partials.single())
        assertEquals(2, loaded.rawItems.size)
    }

    @Test
    fun providerBoundarySerializesConcurrentCatalogRequests() = runTest {
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        var active = 0
        var maxActive = 0
        var calls = 0
        val coordinator = CatalogLoadCoordinator(
            stalkerFor = { error("not used") },
            m3uPayload = { _, _ ->
                calls++
                active++
                maxActive = maxOf(maxActive, active)
                if (calls == 1) {
                    firstEntered.complete(Unit)
                    releaseFirst.await()
                }
                active--
                CatalogSessionStore.M3uPayload(listOf(Channel("C$calls", kind = "live")), "")
            },
        )

        val first = async { coordinator.section(playlist, "live", null) }
        firstEntered.await()
        val second = async { coordinator.categories(playlist, "live") }
        assertFalse(second.isCompleted)
        releaseFirst.complete(Unit)
        first.await()
        second.await()

        assertEquals(1, maxActive)
    }

    @Test
    fun failedProgressiveRefreshCanRestoreExactVisibleCatalog() {
        val coordinator = coordinator(emptyList())
        val before = UiState(
            contentType = "live",
            channels = listOf(Channel("Old")),
            groups = listOf(UiState.ALL_GROUP, "News"),
            favorites = setOf("old-key"),
            selectedGroup = "News",
        )
        val snapshot = coordinator.captureVisible(before)
        val partial = before.copy(
            channels = listOf(Channel("Partial")),
            groups = listOf(UiState.ALL_GROUP, "Partial"),
            favorites = emptySet(),
            selectedGroup = UiState.ALL_GROUP,
            loading = true,
        )

        val restored = coordinator.restoreAfterRefreshFailure(partial, snapshot, "network failed")

        assertEquals(before.contentType, restored.contentType)
        assertEquals(before.channels, restored.channels)
        assertEquals(before.groups, restored.groups)
        assertEquals(before.favorites, restored.favorites)
        assertEquals(before.selectedGroup, restored.selectedGroup)
        assertFalse(restored.loading)
        assertEquals("network failed", restored.status)
    }

    @Test
    fun normalInitialLoadDoesNotNeedRollbackSnapshotMutation() {
        val coordinator = coordinator(emptyList())
        val state = UiState(channels = listOf(Channel("Stable")))
        val snapshot = coordinator.captureVisible(state)
        assertTrue(snapshot.channels === state.channels)
    }

    private fun coordinator(channels: List<Channel>) = CatalogLoadCoordinator(
        stalkerFor = { error("not used") },
        m3uPayload = { _, _ -> CatalogSessionStore.M3uPayload(channels, "") },
    )
}
