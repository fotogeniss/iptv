package com.prelude.iptv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Αυτά τα tests κλειδώνουν αποφάσεις που ΕΣΠΑΣΑΝ ή θα μπορούσαν να σπάσουν
 * σιωπηλά στο ViewModel. Είναι καθαρή λογική — κανένα Android από κάτω.
 */
class LoadPolicyTest {

    // ---- indexAfterDelete: το ύπουλο off-by-one της διαγραφής ----

    @Test fun `deleting a list BEFORE the current shifts index down`() {
        // λίστες [A,B,C,D], βλέπεις C (index 2), σβήνεις A (index 0)
        // μετά: [B,C,D], το C είναι πλέον index 1
        assertEquals(1, LoadPolicy.indexAfterDelete(sizeAfter = 3, removedIndex = 0, current = 2))
    }

    @Test fun `deleting a list AFTER the current keeps the index`() {
        // βλέπεις A (0), σβήνεις D (3) -> A μένει 0
        assertEquals(0, LoadPolicy.indexAfterDelete(sizeAfter = 3, removedIndex = 3, current = 0))
    }

    @Test fun `deleting the current when it was last clamps to new last`() {
        // [A,B,C], βλέπεις C (2), σβήνεις C -> [A,B], νέο last = 1
        assertEquals(1, LoadPolicy.indexAfterDelete(sizeAfter = 2, removedIndex = 2, current = 2))
    }

    @Test fun `deleting the only list yields zero`() {
        assertEquals(0, LoadPolicy.indexAfterDelete(sizeAfter = 0, removedIndex = 0, current = 0))
    }

    @Test fun `result is always a valid index`() {
        // fuzz-ish: κάθε συνδυασμός πρέπει να δίνει έγκυρο index
        for (size in 1..5) for (removed in 0..5) for (cur in 0..5) {
            val r = LoadPolicy.indexAfterDelete(size, removed, cur)
            assertTrue("size=$size rem=$removed cur=$cur -> $r", r in 0 until size)
        }
    }

    // ---- isStale: stale-while-revalidate ----

    @Test fun `fresh cache is not stale`() {
        val now = 10_000_000L
        assertFalse(LoadPolicy.isStale(savedAtMs = now - 1000, nowMs = now, ttlMs = 60_000, force = false))
    }

    @Test fun `old cache is stale`() {
        val now = 10_000_000L
        assertTrue(LoadPolicy.isStale(savedAtMs = now - 120_000, nowMs = now, ttlMs = 60_000, force = false))
    }

    @Test fun `force always stale even if fresh`() {
        val now = 10_000_000L
        assertTrue(LoadPolicy.isStale(savedAtMs = now, nowMs = now, ttlMs = 60_000, force = true))
    }

    @Test fun `never-saved cache is stale`() {
        assertTrue(LoadPolicy.isStale(savedAtMs = 0L, nowMs = 10_000_000L, ttlMs = 60_000, force = false))
    }

    // ---- isUnlockExpired: γονικό PIN ----

    @Test fun `unlock within ttl is valid`() {
        val now = 10_000_000L
        assertFalse(LoadPolicy.isUnlockExpired(unlockedAtMs = now - 1000, nowMs = now, ttlMs = 30 * 60_000))
    }

    @Test fun `unlock past ttl expires`() {
        val now = 10_000_000L
        assertTrue(LoadPolicy.isUnlockExpired(unlockedAtMs = now - 31 * 60_000, nowMs = now, ttlMs = 30 * 60_000))
    }

    @Test fun `never-unlocked counts as expired`() {
        assertTrue(LoadPolicy.isUnlockExpired(unlockedAtMs = 0L, nowMs = 10_000_000L, ttlMs = 30 * 60_000))
    }

    // ---- orderWithFirst ----

    @Test fun `orderWithFirst puts the requested section first, keeps the rest`() {
        assertEquals(listOf("vod", "live", "series"),
            LoadPolicy.orderWithFirst(listOf("live", "vod", "series"), "vod"))
    }

    @Test fun `orderWithFirst is a no-op when first is already first`() {
        assertEquals(listOf("live", "vod"),
            LoadPolicy.orderWithFirst(listOf("live", "vod"), "live"))
    }

    @Test fun `orderWithFirst is a no-op when requested item is absent`() {
        val sections = listOf("live", "vod")
        assertEquals(sections, LoadPolicy.orderWithFirst(sections, "series"))
    }

    @Test fun `orderWithFirst preserves duplicate entries and their order`() {
        assertEquals(
            listOf("vod", "live", "vod", "series"),
            LoadPolicy.orderWithFirst(listOf("live", "vod", "vod", "series"), "vod")
        )
    }

    // ---- groupAllowed: γονικός έλεγχος (η διαρροή που κλείσαμε) ----

    @Test fun `unlocked session shows everything`() {
        assertTrue(LoadPolicy.groupAllowed("ADULTS", setOf("ADULTS"), unlocked = true))
    }

    @Test fun `no locks means everything allowed`() {
        assertTrue(LoadPolicy.groupAllowed("ADULTS", emptySet(), unlocked = false))
    }

    @Test fun `locked group is hidden when not unlocked`() {
        assertFalse(LoadPolicy.groupAllowed("ADULTS", setOf("ADULTS"), unlocked = false))
    }

    @Test fun `unlocked-named group stays visible`() {
        assertTrue(LoadPolicy.groupAllowed("News", setOf("ADULTS"), unlocked = false))
    }

    @Test fun `empty group maps to the no-group label for locking`() {
        assertFalse(LoadPolicy.groupAllowed("", setOf("Χωρίς ομάδα"), unlocked = false))
    }
}
