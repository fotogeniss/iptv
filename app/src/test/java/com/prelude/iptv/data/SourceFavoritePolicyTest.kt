package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceFavoritePolicyTest {
    @Test fun migrationRequiresExactLegacyItemKey() {
        val sourceItems = listOf(
            Channel(name = "A", url = "u:a", kind = "vod"),
            Channel(name = "B", url = "u:b", kind = "vod")
        )
        val result = SourceFavoritePolicy.selectLegacyMatches(
            sourceId = "source-1",
            legacyKeys = setOf("u:b", "missing"),
            sourceItems = sourceItems,
            nowMs = 100L
        )
        assertEquals(listOf("u:b"), result.map { it.itemKey })
        assertEquals("source-1", result.single().sourceId)
    }

    @Test fun migrationDeduplicatesProviderRows() {
        val item = Channel(name = "A", url = "same", kind = "vod")
        val result = SourceFavoritePolicy.selectLegacyMatches("source", setOf("same"), listOf(item, item), 10L)
        assertEquals(1, result.size)
    }

    @Test fun snapshotReconciliationNeverCrossesSources() {
        val oldA = SourceFavorite("a", "key", Channel(name = "Old A", url = "key"), 1L)
        val oldB = SourceFavorite("b", "key", Channel(name = "Old B", url = "key"), 2L)
        val updated = SourceFavoritePolicy.reconcileSnapshots(
            entries = listOf(oldA, oldB),
            sourceId = "a",
            sourceItems = listOf(Channel(name = "Fresh A", url = "key"))
        )
        assertEquals("Fresh A", updated[0].channel.name)
        assertEquals("Old B", updated[1].channel.name)
        assertEquals(1L, updated[0].addedAtMs)
    }

    @Test fun migrationDoesNotGuessSeriesIdOnlyFavorites() {
        val result = SourceFavoritePolicy.selectLegacyMatches(
            sourceId = "source",
            legacyKeys = setOf("series-1"),
            sourceItems = listOf(Channel(name = "Show", seriesId = "series-1", kind = "series")),
            nowMs = 1L
        )
        assertTrue(result.isEmpty())
    }

    @Test fun seriesContainerIsNotLauncherPlayable() {
        assertFalse(SourceFavoritePolicy.playable(Channel(name = "Show", seriesId = "1", kind = "series")))
        assertTrue(SourceFavoritePolicy.playable(Channel(name = "Episode", url = "ep", kind = "series_ep")))
        assertTrue(SourceFavoritePolicy.playable(Channel(name = "Live", url = "live", kind = "live")))
    }
}
