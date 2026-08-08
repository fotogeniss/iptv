package com.prelude.iptv.tvhome

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.SourceFavorite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvMyListPolicyTest {
    private fun favorite(
        source: String = "source",
        key: String,
        kind: String = "vod",
        group: String = "Movies",
        addedAt: Long
    ) = SourceFavorite(
        sourceId = source,
        itemKey = key,
        channel = Channel(name = key, group = group, url = key, kind = kind),
        addedAtMs = addedAt
    )

    @Test fun filtersMissingSourcesLockedGroupsAndSeriesContainers() {
        val result = TvMyListPolicy.select(
            favorites = listOf(
                favorite(source = "gone", key = "gone", addedAt = 5),
                favorite(key = "locked", group = " Adults ", addedAt = 4),
                favorite(key = "series", kind = "series", addedAt = 3),
                favorite(key = "ok", addedAt = 2)
            ),
            availableSourceIds = setOf("source"),
            lockedGroups = setOf("adults")
        )
        assertEquals(listOf("ok"), result.map { it.itemKey })
    }

    @Test fun newestItemsComeFirstAndResultIsCapped() {
        val input = (1L..25L).map { favorite(key = "k$it", addedAt = it) }
        val result = TvMyListPolicy.select(input, setOf("source"), emptySet())
        assertEquals(TvMyListPolicy.MAX_ITEMS, result.size)
        assertEquals("k25", result.first().itemKey)
        assertEquals("k6", result.last().itemKey)
    }

    @Test fun duplicateIdentityIsPublishedOnce() {
        val result = TvMyListPolicy.select(
            listOf(favorite(key = "same", addedAt = 1), favorite(key = "same", addedAt = 2)),
            setOf("source"),
            emptySet()
        )
        assertEquals(1, result.size)
        assertEquals(2L, result.single().addedAtMs)
    }

    @Test fun zeroLimitPublishesNothing() {
        assertTrue(
            TvMyListPolicy.select(
                listOf(favorite(key = "x", addedAt = 1)),
                setOf("source"),
                emptySet(),
                maxItems = 0
            ).isEmpty()
        )
    }
}
