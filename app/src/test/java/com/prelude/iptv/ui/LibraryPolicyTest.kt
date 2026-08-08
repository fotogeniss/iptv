package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryPolicyTest {
    private val dune = Channel(name = "Dune Part Two", kind = "vod", url = "https://x/dune", genre = "Science Fiction")
    private val duneDuplicate = dune.copy(group = "Movies")
    private val office = Channel(name = "The Office", kind = "series", seriesId = "42", genre = "Comedy")

    @Test
    fun unique_preservesFirstOccurrence() {
        assertEquals(listOf(dune, office), LibraryPolicy.unique(listOf(dune, duneDuplicate, office)))
    }

    @Test
    fun search_matchesAllTermsAcrossMetadata() {
        val result = LibraryPolicy.search(listOf(dune, office), "dune science")
        assertEquals(listOf(dune), result)
    }

    @Test
    fun favorites_returnsOnlyKnownFavoriteItems() {
        val keys = setOf(PlaybackQueue.favKey(office))
        val result = LibraryPolicy.favorites(listOf(dune, office), keys)
        assertEquals(listOf(office), result)
        assertTrue(result.none { it == dune })
    }
}
