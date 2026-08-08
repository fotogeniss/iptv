package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPolicyTest {
    private fun movie(id: Int, group: String, year: String = "2024") = Channel(
        name = "Movie $id",
        url = "https://example.test/$id.m3u8",
        kind = "vod",
        group = group,
        year = year
    )

    @Test
    fun continueAndMyListComeBeforeTrending() {
        val items = (1..8).map { movie(it, if (it <= 4) "Action" else "Drama") }
        val favorite = PlaybackQueue.favKey(items[3])

        val sections = buildCatalogRailSections(
            channels = items,
            favoriteKeys = setOf(favorite),
            continueWatching = listOf(items[1] to 0.42f)
        )

        assertEquals(listOf("continue", "my-list", "trending"), sections.take(3).map { it.id })
        assertEquals(items[1], sections.first().items.first())
        assertEquals(items[3], sections[1].items.first())
    }

    @Test
    fun duplicateStreamsAppearOnlyOncePerRail() {
        val original = movie(1, "Action")
        val duplicate = original.copy(name = "Same stream, other title")
        val items = listOf(original, duplicate, movie(2, "Action"), movie(3, "Action"))

        val trending = buildCatalogRailSections(items, emptySet(), emptyList())
            .first { it.id == "trending" }

        assertEquals(3, trending.items.size)
        assertTrue(trending.ranked)
    }
    @Test
    fun liveChannelsNeverAppearOnTheHomeRails() {
        // Η αρχική είναι βιβλιοθήκη ταινιών/σειρών. Ένα ζωντανό κανάλι εκεί δεν
        // έχει αφίσα ούτε διάρκεια και εμφανίζεται ως κενό πλακίδιο.
        val live = Channel(name = "SPORT HD", url = "http://x/live", kind = "live", group = "Sports")
        val items = listOf(movie(1, "Action"), live, movie(2, "Action"), movie(3, "Action"))

        val sections = buildCatalogRailSections(items, emptySet(), emptyList())

        assertTrue(sections.isNotEmpty())
        assertTrue(sections.none { section -> section.allItems.any { it.kind == "live" } })
    }

    @Test
    fun liveOnlyCatalogProducesNoHomeRails() {
        // Πηγή μόνο με κανάλια: καμία ράγα, αντί για ράγες γεμάτες κενά πλακίδια.
        val live = (1..5).map {
            Channel(name = "CH $it", url = "http://x/$it", kind = "live", group = "News")
        }
        assertTrue(buildCatalogRailSections(live, emptySet(), emptyList()).isEmpty())
    }

    @Test
    fun railPreviewIsCappedButViewAllKeepsFullSection() {
        val items = (1..34).map { movie(it, "Action") }
        val trending = buildCatalogRailSections(items, emptySet(), emptyList())
            .first { it.id == "trending" }

        assertEquals(20, trending.items.size)
        assertEquals(34, trending.allItems.size)
    }

}
