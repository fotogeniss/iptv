package com.prelude.iptv.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ο ταξινομητής live/vod/series είναι καθαρή λογική και ΕΣΠΑΣΕ στο παρελθόν:
 * live κανάλια σε groups «CINEMA»/«MOVIES» κατέληγαν στις Ταινίες, χάνοντας
 * EPG/zapping. Αυτά τα tests κλειδώνουν τη σωστή συμπεριφορά ώστε να μην
 * ξανασυμβεί σε μελλοντικό refactor.
 */
class M3uParserClassifyTest {

    @Test fun `live channel keeps live even in a CINEMA group`() {
        // #EXTINF:-1 (live) + group "CINEMA" -> ΠΡΕΠΕΙ να μείνει live
        assertEquals("live", M3uParser.classify("-1", "http://x/stream.ts", "CINEMA HD", "HBO"))
        assertEquals("live", M3uParser.classify("-1", "http://x/1", "MOVIES 24/7", "Action Channel"))
    }

    @Test fun `provider path wins over everything`() {
        assertEquals("series", M3uParser.classify("-1", "http://x/series/1.mkv", "whatever", "n"))
        assertEquals("vod", M3uParser.classify("-1", "http://x/movie/9.mp4", "Live TV", "n"))
        assertEquals("vod", M3uParser.classify("-1", "http://x/vod/9", "g", "n"))
    }

    @Test fun `positive duration means a file`() {
        assertEquals("vod", M3uParser.classify("7200", "http://x/a", "Movies", "Inception"))
    }

    @Test fun `file extension implies vod even with negative duration`() {
        assertEquals("vod", M3uParser.classify("-1", "http://x/film.mkv", "g", "Dune"))
    }

    @Test fun `series tag in the name is detected on files`() {
        assertEquals("series", M3uParser.classify("1800", "http://x/a.mp4", "TV", "Show S01E02"))
    }

    @Test fun `plain live stream with no hints stays live`() {
        assertEquals("live", M3uParser.classify("-1", "http://x/live/5", "News", "BBC"))
        assertEquals("live", M3uParser.classify("-1", "http://x/8.m3u8", "Sports", "ESPN"))
    }

    @Test fun `series group only applies to actual files`() {
        // group λέει "series" ΑΛΛΑ είναι live stream -> μένει live (ασφαλέστερο)
        assertEquals("live", M3uParser.classify("-1", "http://x/z.ts", "Series Zone", "n"))
        // ίδιο group αλλά αρχείο -> series
        assertEquals("series", M3uParser.classify("-1", "http://x/z.mkv", "Series Zone", "n"))
    }
    @Test fun `parse reports real line progress and completes at 100 percent`() {
        val text = """#EXTM3U
#EXTINF:-1 group-title="Live",News
http://example/live.ts
#EXTINF:7200 group-title="Movies",Film
http://example/movie/1.mp4
""".trimIndent()
        val events = mutableListOf<Pair<Int, Int>>()

        val channels = M3uParser.parse(text) { processed, total ->
            events += processed to total
        }

        assertEquals(2, channels.size)
        assertTrue(events.isNotEmpty())
        assertEquals(events.last().second, events.last().first)
    }

}
