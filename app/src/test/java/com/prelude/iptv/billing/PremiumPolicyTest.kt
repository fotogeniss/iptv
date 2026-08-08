package com.prelude.iptv.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumPolicyTest {

    @Test
    fun `χωρίς επιβεβαιωμένη αγορά το επίπεδο είναι δωρεάν`() {
        assertEquals(PremiumTier.FREE, PremiumPolicy.defaultTier)
        assertFalse(PremiumPolicy.unlocked(PremiumFeature.EDIT_HOME, PremiumPolicy.defaultTier))
    }

    @Test
    fun `το πληρωμένο επίπεδο ξεκλειδώνει τα πάντα`() {
        PremiumFeature.entries.forEach {
            assertTrue(PremiumPolicy.unlocked(it, PremiumTier.PREMIUM))
        }
    }

    @Test
    fun `το δωρεάν κλειδώνει τα premium`() {
        assertFalse(PremiumPolicy.unlocked(PremiumFeature.EDIT_HOME, PremiumTier.FREE))
        assertFalse(PremiumPolicy.unlocked(PremiumFeature.MULTIVIEW, PremiumTier.FREE))
    }

    @Test
    fun `το δωρεάν κρατά τα αντίγραφα ασφαλείας`() {
        // Τα δεδομένα του χρήστη δεν είναι δυνατότητα που πουλάμε: αν σταματήσει
        // να πληρώνει, πρέπει να μπορεί να τα πάρει μαζί του.
        assertTrue(PremiumPolicy.unlocked(PremiumFeature.BACKUP, PremiumTier.FREE))
    }

    @Test
    fun `η ετικέτα περιγράφει το πραγματικό entitlement`() {
        assertEquals("PREMIUM", PremiumPolicy.label(PremiumTier.PREMIUM))
        assertEquals("ΔΩΡΕΑΝ", PremiumPolicy.label(PremiumTier.FREE))
    }

    @Test
    fun `άγνωστο αποθηκευμένο κείμενο πέφτει στην προεπιλογή`() {
        assertEquals(PremiumPolicy.defaultTier, PremiumPolicy.tierOf("ΟΤΙΝΑΝΑΙ"))
        assertEquals(PremiumPolicy.defaultTier, PremiumPolicy.tierOf(""))
    }

    @Test
    fun `αποθηκευμένο επίπεδο διαβάζεται σωστά`() {
        assertEquals(PremiumTier.FREE, PremiumPolicy.tierOf("FREE"))
        assertEquals(PremiumTier.PREMIUM, PremiumPolicy.tierOf("PREMIUM"))
    }
}
