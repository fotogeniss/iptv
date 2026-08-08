package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueuePolicyTest {
    private fun episode(id: Int) = Channel(
        name = "Episode $id",
        url = "https://example.test/episode-$id.m3u8",
        kind = "series_ep"
    )

    @Test
    fun requestedEpisodeQueueWinsOverCatalogFallback() {
        val episodes = listOf(episode(1), episode(2), episode(3))
        val unrelatedCatalog = listOf(Channel("Series", seriesId = "series-1", kind = "series"))

        val result = PlaybackQueuePolicy.prepare(episodes[1], episodes, unrelatedCatalog)

        assertEquals(episodes, result.items)
        assertEquals(1, result.index)
    }

    @Test
    fun targetIsUsedWhenBothQueuesAreEmpty() {
        val target = episode(7)
        val result = PlaybackQueuePolicy.prepare(target, emptyList(), emptyList())
        assertEquals(listOf(target), result.items)
        assertEquals(0, result.index)
    }

    @Test
    fun targetMissingFromCatalogNeverFallsBackToPositionZero() {
        // Το σενάριο «Συνέχισε να βλέπεις»: το αντικείμενο έρχεται από το ιστορικό
        // και ΔΕΝ υπάρχει στην ορατή, φιλτραρισμένη λίστα. Πριν, ο δείκτης έπεφτε
        // στο 0 και ο player έγραφε το όνομα άσχετης ταινίας πάνω από σωστό βίντεο.
        val target = episode(9)
        val visible = listOf(episode(1), episode(2))

        val result = PlaybackQueuePolicy.prepare(target, requested = null, fallback = visible)

        assertEquals(listOf(target), result.items)
        assertEquals(0, result.index)
        assertEquals("Episode 9", result.items[result.index].name)
    }

    @Test
    fun targetPresentInCatalogKeepsTheCatalogAsQueue() {
        // Έλεγχος ότι η διόρθωση δεν στέρησε το επόμενο/προηγούμενο όταν ΟΝΤΩΣ
        // υπάρχει πλαίσιο.
        val visible = listOf(episode(1), episode(2), episode(3))

        val result = PlaybackQueuePolicy.prepare(visible[2], requested = null, fallback = visible)

        assertEquals(visible, result.items)
        assertEquals(2, result.index)
    }
}
