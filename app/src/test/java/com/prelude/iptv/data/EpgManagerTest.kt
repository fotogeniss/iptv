package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ο EpgManager είναι νέος κώδικας που αντικατέστησε το προηγούμενο αρχείο —
 * ό,τι αφορά ΧΡΟΝΟΥΣ και ΔΥΑΔΙΚΗ ΑΝΑΖΗΤΗΣΗ πρέπει να είναι απόλυτα σωστό,
 * αλλιώς το «τι παίζει τώρα» και το grid δείχνουν λάθος πρόγραμμα.
 */
class EpgManagerTest {

    // ---- parseXmltvTime: το πιο εύθραυστο κομμάτι (timezones) ----

    @Test fun `xmltv time with positive offset converts to correct UTC`() {
        // 12:00 +0300 = 09:00 UTC
        val ms = EpgManager.parseXmltvTime("20260718120000 +0300")
        val utcNoon = EpgManager.parseXmltvTime("20260718090000 +0000")
        assertEquals(utcNoon, ms)
    }

    @Test fun `xmltv time without offset is treated as UTC`() {
        val a = EpgManager.parseXmltvTime("20260718090000")
        val b = EpgManager.parseXmltvTime("20260718090000 +0000")
        assertEquals(b, a)
    }

    @Test fun `negative offset shifts the other way`() {
        // 09:00 -0500 = 14:00 UTC
        val ms = EpgManager.parseXmltvTime("20260718090000 -0500")
        val utc = EpgManager.parseXmltvTime("20260718140000 +0000")
        assertEquals(utc, ms)
    }

    @Test fun `malformed time returns zero, never crashes`() {
        assertEquals(0L, EpgManager.parseXmltvTime(null))
        assertEquals(0L, EpgManager.parseXmltvTime(""))
        assertEquals(0L, EpgManager.parseXmltvTime("garbage"))
        assertEquals(0L, EpgManager.parseXmltvTime("2026"))
    }

    // ---- indexAt: δυαδική αναζήτηση «τελευταίο πρόγραμμα που ξεκινά <= t» ----

    private fun prog(startMin: Int, durMin: Int = 30) =
        EpgManager.Prog("p$startMin", "", startMin * 60_000L, (startMin + durMin) * 60_000L)

    @Test fun `indexAt finds the last programme starting at or before t`() {
        val list = listOf(prog(0), prog(30), prog(60), prog(90))
        assertEquals(0, EpgManager.indexAt(list, 10 * 60_000L))   // μέσα στο 1ο
        assertEquals(1, EpgManager.indexAt(list, 30 * 60_000L))   // ακριβώς στην αρχή του 2ου
        assertEquals(3, EpgManager.indexAt(list, 200 * 60_000L))  // μετά το τελευταίο
    }

    @Test fun `indexAt returns minus one before the first programme`() {
        val list = listOf(prog(100))
        assertEquals(-1, EpgManager.indexAt(list, 10 * 60_000L))
    }

    @Test fun `nowNext and programmes on unknown channel are empty, not crash`() {
        EpgManager.clear()
        val (now, next) = EpgManager.nowNext("does.not.exist")
        assertNull(now); assertNull(next)
        assertTrue(EpgManager.programmes("nope", 0, Long.MAX_VALUE).isEmpty())
        assertTrue(EpgManager.upcoming("nope", 5).isEmpty())
    }

    @Test fun `isLoaded reflects clear`() {
        EpgManager.clear()
        assertEquals(false, EpgManager.isLoaded())
        assertEquals(0, EpgManager.channelCount())
        assertNull(EpgManager.currentSource())
    }
}
