package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogNormalizerTest {
    @Test
    fun `series episodes collapse into one parent and ordered seasons`() {
        val raw = listOf(
            Channel("Chernobyl S01 E02", group = "SERIES | HBO", url = "u2", kind = "series"),
            Channel("Chernobyl S01 E01", group = "SERIES | HBO", url = "u1", kind = "series"),
            Channel("Chernobyl S02 E01", group = "SERIES | HBO", url = "u3", kind = "series")
        )

        val normalized = CatalogNormalizer.normalize("series", raw)

        assertEquals(1, normalized.items.size)
        assertEquals("Chernobyl", normalized.items.single().name)
        val seasons = normalized.seriesEpisodes.getValue(normalized.items.single().seriesId)
        assertEquals(listOf("Season 1", "Season 2"), seasons.map { it.first })
        assertEquals(listOf("u1", "u2"), seasons.first().second.map { it.url })
    }

    @Test
    fun `xtream series containers stay as containers`() {
        val raw = listOf(Channel("Dark", kind = "series", seriesId = "42", logo = "cover"))
        val normalized = CatalogNormalizer.normalize("series", raw)
        assertEquals(raw, normalized.items)
        assertTrue(normalized.seriesEpisodes.isEmpty())
    }

    @Test
    fun `search hides episode rows when parent exists`() {
        val items = listOf(
            Channel("Chernobyl", kind = "series", seriesId = "42"),
            Channel("Chernobyl S01 E01", kind = "series_ep", seriesId = "42", url = "ep1"),
            Channel("Movie", kind = "vod", streamId = "7", url = "movie"),
            Channel("News", kind = "live", streamId = "8", url = "live")
        )
        val search = CatalogNormalizer.searchEntries(items)
        assertEquals(3, search.size)
        assertEquals(1, search.count { it.kind == "series" })
        assertEquals(0, search.count { it.kind == "series_ep" })
    }

    @Test
    fun `search reclassifies legacy vod episode rows as one series`() {
        val search = CatalogNormalizer.searchEntries(listOf(
            Channel("Chernobyl S01 E01", kind = "vod", url = "one"),
            Channel("Chernobyl S01 E02", kind = "vod", url = "two")
        ))
        assertEquals(1, search.size)
        assertEquals("series", search.single().kind)
        assertEquals("Chernobyl", search.single().name)
    }

    @Test
    fun `live and movie duplicates collapse by provider identity`() {
        val live = CatalogNormalizer.normalize("live", listOf(
            Channel("News", streamId = "1", url = "a"),
            Channel("News duplicate", streamId = "1", url = "b")
        ))
        val vod = CatalogNormalizer.normalize("vod", listOf(
            Channel("Film", streamId = "2", url = "a"),
            Channel("Film", streamId = "2", url = "b")
        ))
        assertEquals(1, live.items.size)
        assertEquals(1, vod.items.size)
    }
}
