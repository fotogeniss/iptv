package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackBackendPolicyTest {

    private fun backend(url: String) = PlaybackBackendPolicy.backendFor(url)

    // ---- Πρωτόκολλα που ΜΟΝΟ το LibVLC ανοίγει ----

    @Test
    fun `RTSP παει στο LibVLC`() {
        assertEquals(PlaybackBackend.VLC, backend("rtsp://10.0.0.1:554/stream1"))
        assertEquals(PlaybackBackend.VLC, backend("rtsps://host/stream"))
    }

    @Test
    fun `RTMP παει στο LibVLC`() {
        assertEquals(PlaybackBackend.VLC, backend("rtmp://server/live/key"))
        assertEquals(PlaybackBackend.VLC, backend("rtmpe://server/live/key"))
        assertEquals(PlaybackBackend.VLC, backend("rtmps://server/live/key"))
    }

    @Test
    fun `UDP και RTP πανε στο LibVLC`() {
        assertEquals(PlaybackBackend.VLC, backend("udp://@239.255.0.1:1234"))
        assertEquals(PlaybackBackend.VLC, backend("rtp://@239.0.0.1:5004"))
    }

    @Test
    fun `MMS παει στο LibVLC`() {
        assertEquals(PlaybackBackend.VLC, backend("mms://host/stream"))
        assertEquals(PlaybackBackend.VLC, backend("mmsh://host/stream"))
        assertEquals(PlaybackBackend.VLC, backend("mmst://host/stream"))
    }

    @Test
    fun `το σχημα δεν εξαρταται απο πεζα-κεφαλαια`() {
        // Οι πάροχοι γράφουν ό,τι θέλουν. «RTSP://» είναι το ίδιο πρωτόκολλο.
        assertEquals(PlaybackBackend.VLC, backend("RTSP://host/path"))
        assertEquals(PlaybackBackend.VLC, backend("Rtmp://host/path"))
    }

    // ---- Ό,τι χειρίζεται το Media3 ----

    @Test
    fun `HLS μενει στο Media3`() {
        assertEquals(PlaybackBackend.EXO, backend("https://host/live/stream.m3u8"))
        assertEquals(PlaybackBackend.EXO, backend("http://host/a/b.m3u8?token=abc"))
    }

    @Test
    fun `MP4 μενει στο Media3`() {
        assertEquals(PlaybackBackend.EXO, backend("https://host/movies/film.mp4"))
    }

    @Test
    fun `αγνωστη καταληξη σε HTTP μενει στο Media3`() {
        // Οι περισσότερες Xtream ταινίες δεν έχουν κατάληξη καθόλου. Το Media3
        // αναγνωρίζει τον container από τα πρώτα bytes και τα παίζει.
        assertEquals(PlaybackBackend.EXO, backend("http://host/movie/12345"))
    }

    // ---- Το «γυμνό» MPEG-TS ----

    @Test
    fun `γυμνο TS παει στο LibVLC`() {
        assertEquals(PlaybackBackend.VLC, backend("http://host/live/1001.ts"))
        assertEquals(PlaybackBackend.VLC, backend("http://host/x.m2ts"))
    }

    @Test
    fun `TS με query παραμετρους αναγνωριζεται`() {
        // Το κλασικό Xtream: η κατάληξη κρύβεται πίσω από token.
        assertTrue(PlaybackBackendPolicy.isBareTransportStream("http://host/live/u/p/1001.ts?token=xyz"))
        assertEquals(PlaybackBackend.VLC, backend("http://host/live/u/p/1001.ts?token=xyz"))
    }

    @Test
    fun `TS μεσα σε HLS ΔΕΝ μετραει ως γυμνο`() {
        // Το manifest είναι .m3u8· τα κομμάτια .ts τα κατεβάζει το Media3 μόνο του.
        assertFalse(PlaybackBackendPolicy.isBareTransportStream("http://host/live.m3u8"))
        assertEquals(PlaybackBackend.EXO, backend("http://host/live.m3u8"))
    }

    @Test
    fun `τελεια σε φακελο δεν μπερδευει την καταληξη`() {
        assertEquals("", PlaybackBackendPolicy.extensionOf("http://host/v1.2/stream"))
        assertEquals(PlaybackBackend.EXO, backend("http://host/v1.2/stream"))
    }

    @Test
    fun `fragment δεν μπερδευει την καταληξη`() {
        assertEquals("mp4", PlaybackBackendPolicy.extensionOf("http://host/a.mp4#t=10"))
    }

    // ---- Ρύθμιση χρήστη ----

    @Test
    fun `η επιλογη παντα LibVLC υπερισχυει`() {
        assertEquals(
            PlaybackBackend.VLC,
            PlaybackBackendPolicy.backendFor("https://host/a.m3u8", forceVlc = true)
        )
    }

    // ---- Ανθεκτικότητα ----

    @Test
    fun `κενη διευθυνση δεν σκαει`() {
        assertEquals(PlaybackBackend.EXO, backend(""))
        assertEquals(PlaybackBackend.EXO, backend("   "))
        assertEquals("", PlaybackBackendPolicy.schemeOf(""))
        assertEquals("", PlaybackBackendPolicy.extensionOf(""))
    }

    @Test
    fun `διευθυνση χωρις σχημα δεν σκαει`() {
        assertEquals("", PlaybackBackendPolicy.schemeOf("/sdcard/movie.mp4"))
        assertEquals("mp4", PlaybackBackendPolicy.extensionOf("/sdcard/movie.mp4"))
    }

    // ---- Διάγνωση ----

    @Test
    fun `η διαγνωση ξεχωριζει τι ξερει σιγουρα το Media3`() {
        assertTrue(PlaybackBackendPolicy.exoHandlesConfidently("http://host/a.m3u8"))
        assertTrue(PlaybackBackendPolicy.exoHandlesConfidently("http://host/a.mp4"))
        assertFalse(PlaybackBackendPolicy.exoHandlesConfidently("rtsp://host/a"))
        // Χωρίς κατάληξη: παίζει συνήθως, αλλά δεν το εγγυόμαστε.
        assertFalse(PlaybackBackendPolicy.exoHandlesConfidently("http://host/movie/12345"))
    }

    @Test
    fun `καθε σχημα της λιστας εχει αποφαση`() {
        // Η λίστα που ζητήθηκε ρητά: HLS, RTMP, UDP, RTSP, MMS.
        val requested = listOf(
            "https://h/s.m3u8" to PlaybackBackend.EXO,
            "rtmp://h/s" to PlaybackBackend.VLC,
            "udp://@239.0.0.1:1234" to PlaybackBackend.VLC,
            "rtsp://h/s" to PlaybackBackend.VLC,
            "mms://h/s" to PlaybackBackend.VLC,
        )
        requested.forEach { (url, expected) ->
            assertEquals("Λάθος backend για $url", expected, backend(url))
        }
    }
}
