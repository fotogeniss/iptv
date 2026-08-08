package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterPolicyTest {
    private val movie = Channel(name = "Movie", url = "movie", kind = "vod")
    private val series = Channel(name = "Series", url = "series", kind = "series")
    private val live = Channel(name = "Live", url = "live", kind = "live")

    @Test
    fun unique_keeps_first_occurrence() {
        val result = LibraryPolicy.unique(listOf(movie, movie.copy(name = "Movie duplicate"), series, live))
        assertEquals(3, result.size)
        assertEquals(listOf("Movie", "Series", "Live"), result.map { it.name })
    }
}
