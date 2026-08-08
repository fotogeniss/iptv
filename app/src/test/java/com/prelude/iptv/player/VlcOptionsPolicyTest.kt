package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Τα ορίσματα του LibVLC είναι κείμενα προς native βιβλιοθήκη: ένα ορθογραφικό
 * λάθος αγνοείται σιωπηλά. Αυτά τα tests είναι το μόνο δίχτυ.
 */
class VlcOptionsPolicyTest {

    private fun live() = VlcOptionsPolicy.startupOptions(live = true)
    private fun vod() = VlcOptionsPolicy.startupOptions(live = false)

    @Test
    fun `καθε ορισμα ξεκινα με παυλες`() {
        (live() + vod()).forEach {
            assertTrue("Ύποπτο όρισμα: $it", it.startsWith("-"))
        }
    }

    @Test
    fun `κανενα ορισμα δεν εχει κενα`() {
        // Ένα κενό μέσα σε όρισμα το σπάει σε δύο και το LibVLC τα αγνοεί και τα δύο.
        (live() + vod()).forEach {
            assertFalse("Κενό μέσα στο όρισμα: $it", it.contains(' '))
        }
    }

    @Test
    fun `καμια επαναληψη ορισματος`() {
        // Δύο φορές το ίδιο κλειδί με διαφορετική τιμή = απρόβλεπτο αποτέλεσμα.
        val keys = live().map { it.substringBefore('=') }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `η ζωντανη ροη εχει μικροτερη αποθηκευση απο την ταινια`() {
        assertTrue(VlcOptionsPolicy.LIVE_CACHING_MS < VlcOptionsPolicy.VOD_CACHING_MS)
        assertTrue(live().contains("--network-caching=${VlcOptionsPolicy.LIVE_CACHING_MS}"))
        assertTrue(vod().contains("--network-caching=${VlcOptionsPolicy.VOD_CACHING_MS}"))
    }

    @Test
    fun `και οι τεσσερις αποθηκευσεις οριζονται μαζι`() {
        // Ορίζοντας μόνο το network-caching, το RTSP κρατά τη δική του προεπιλογή
        // και συμπεριφέρεται διαφορετικά από τα υπόλοιπα.
        val options = live()
        listOf("network-caching", "live-caching", "file-caching", "rtsp-caching").forEach { key ->
            assertTrue(
                "Λείπει το --$key",
                options.any { it.startsWith("--$key=") }
            )
        }
    }

    @Test
    fun `το RTSP περναει πανω απο TCP`() {
        assertTrue(live().contains("--rtsp-tcp"))
    }

    @Test
    fun `η επανασυνδεση HTTP ειναι ανοιχτη`() {
        assertTrue(live().contains("--http-reconnect"))
    }

    @Test
    fun `το ρολοι δεν κυνηγα χαλασμενες χρονοσημανσεις`() {
        assertTrue(live().contains("--clock-jitter=0"))
        assertTrue(live().contains("--clock-synchro=0"))
    }

    @Test
    fun `ο τονος μενει σταθερος σε αλλαγη ταχυτητας`() {
        assertTrue(vod().contains("--audio-time-stretch"))
    }

    @Test
    fun `η κωδικοποιηση υποτιτλων ειναι αυτοματη`() {
        // Ελληνικοί υπότιτλοι σε MPEG-TS είναι συχνά windows-1253 χωρίς δήλωση.
        assertTrue(live().contains("--subsdec-encoding=auto"))
    }

    @Test
    fun `η καταγραφη ειναι κλειστη απο προεπιλογη`() {
        assertTrue(live().contains("--quiet"))
        assertFalse(live().contains("-vv"))
        assertTrue(VlcOptionsPolicy.startupOptions(live = true, verbose = true).contains("-vv"))
    }

    @Test
    fun `το skip loop filter εχει εγκυρο ευρος`() {
        assertEquals("--avcodec-skiploopfilter=0", VlcOptionsPolicy.skipLoopFilterOption(false))
        assertEquals("--avcodec-skiploopfilter=4", VlcOptionsPolicy.skipLoopFilterOption(true))
    }
}
