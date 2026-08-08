package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BufferPolicyTest {

    @Test
    fun everyProfileIsInternallyConsistent() {
        // Ο ExoPlayer δεν ελέγχει αυτές τις σχέσεις για εμάς. Μια ανεστραμμένη
        // τετράδα δίνει player που ξαναφορτώνει συνεχώς — σφάλμα που φαίνεται
        // μόνο πάνω στη συσκευή, όχι στη μεταγλώττιση.
        BufferProfile.entries.forEach { profile ->
            val d = BufferPolicy.durationsFor(profile)
            assertTrue("$profile: min <= max", d.minMs <= d.maxMs)
            assertTrue("$profile: εκκίνηση <= ελάχιστο", d.forPlaybackMs <= d.minMs)
            assertTrue(
                "$profile: μετά από διακοπή περιμένουμε περισσότερο",
                d.afterRebufferMs >= d.forPlaybackMs
            )
            assertTrue("$profile: θετικές τιμές", d.forPlaybackMs > 0)
        }
    }

    @Test
    fun profilesAreOrderedFromLeastToMostBuffering() {
        val low = BufferPolicy.durationsFor(BufferProfile.LOW)
        val normal = BufferPolicy.durationsFor(BufferProfile.NORMAL)
        val high = BufferPolicy.durationsFor(BufferProfile.HIGH)
        assertTrue(low.minMs < normal.minMs)
        assertTrue(normal.minMs < high.minMs)
        assertTrue(low.forPlaybackMs < high.forPlaybackMs)
    }

    @Test
    fun storageValuesRoundTrip() {
        BufferProfile.entries.forEach { profile ->
            assertEquals(profile, BufferPolicy.fromStorage(profile.storageValue))
        }
    }

    @Test
    fun unknownOrMissingValueFallsBackToNormal() {
        // Παλιά εγκατάσταση χωρίς αποθηκευμένη τιμή, ή χαλασμένη τιμή: ο player
        // πρέπει να παίξει, όχι να σκάσει.
        assertEquals(BufferProfile.NORMAL, BufferPolicy.fromStorage(null))
        assertEquals(BufferProfile.NORMAL, BufferPolicy.fromStorage(""))
        assertEquals(BufferProfile.NORMAL, BufferPolicy.fromStorage("κάτι άσχετο"))
    }

    @Test
    fun storageParsingToleratesCaseAndWhitespace() {
        assertEquals(BufferProfile.HIGH, BufferPolicy.fromStorage(" HIGH "))
    }
}
