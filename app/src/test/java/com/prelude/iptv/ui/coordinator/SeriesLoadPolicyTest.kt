package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.CatalogNormalization
import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesLoadPolicyTest {
    private val episodes = listOf(
        "Season 1" to listOf(Channel(name = "Episode 1", kind = "series_ep", url = "https://test/1"))
    )

    @Test
    fun `matches a fresh provider result by stable series id`() {
        val requested = Channel(name = "Localized name", kind = "series", seriesId = "42")
        val parent = Channel(name = "Provider name", kind = "series", seriesId = "42")

        val result = SeriesLoadPolicy.resolve(
            requested,
            CatalogNormalization(listOf(parent), mapOf("42" to episodes)),
        )

        assertEquals(episodes, result.seasons)
        assertFalse(result.synthetic)
    }

    @Test
    fun `falls back to a case-insensitive title when provider ids changed`() {
        val requested = Channel(name = "The Series", kind = "series", seriesId = "old")
        val parent = Channel(name = "the series", kind = "series", seriesId = "new")

        val result = SeriesLoadPolicy.resolve(
            requested,
            CatalogNormalization(listOf(parent), mapOf("new" to episodes)),
        )

        assertEquals(episodes, result.seasons)
        assertFalse(result.synthetic)
    }

    @Test
    fun `playable provider row becomes a non-persistent synthetic episode`() {
        val requested = Channel(
            name = "Standalone episode",
            kind = "series",
            seriesId = "local:standalone",
            url = "https://test/standalone",
        )

        val result = SeriesLoadPolicy.resolve(requested, CatalogNormalization(emptyList()))

        assertTrue(result.synthetic)
        assertEquals(1, result.seasons.single().second.size)
        assertEquals("series_ep", result.seasons.single().second.single().kind)
    }

    @Test
    fun `non-playable row without provider episodes stays empty`() {
        val requested = Channel(name = "Missing", kind = "series", seriesId = "missing")

        val result = SeriesLoadPolicy.resolve(requested, CatalogNormalization(emptyList()))

        assertTrue(result.seasons.isEmpty())
        assertFalse(result.synthetic)
    }
}
