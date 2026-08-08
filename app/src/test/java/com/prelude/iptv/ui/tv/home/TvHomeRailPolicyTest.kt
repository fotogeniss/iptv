package com.prelude.iptv.ui.tv.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvHomeRailPolicyTest {

    /* ---------------- κάθετη κίνηση ---------------- */

    @Test
    fun downMovesToNextRail() {
        val move = TvHomeRailPolicy.moveDown(current = 0, railCount = 3)
        assertEquals(1, move.index)
        assertTrue(move.consumed)
    }

    @Test
    fun downOnLastRailStaysAndDoesNotConsume() {
        // Δεν καταναλώνουμε το πάτημα στο τέλος: αλλιώς το D-pad «κολλάει»
        // χωρίς καμία ένδειξη προς τον χρήστη.
        val move = TvHomeRailPolicy.moveDown(current = 2, railCount = 3)
        assertEquals(2, move.index)
        assertFalse(move.consumed)
    }

    @Test
    fun upMovesToPreviousRail() {
        val move = TvHomeRailPolicy.moveUp(current = 2)
        assertEquals(1, move.index)
        assertTrue(move.consumed)
    }

    @Test
    fun upOnFirstRailDoesNotConsumeSoFocusCanLeave() {
        val move = TvHomeRailPolicy.moveUp(current = 0)
        assertEquals(0, move.index)
        assertFalse(move.consumed)
    }

    @Test
    fun singleRailConsumesNothing() {
        assertFalse(TvHomeRailPolicy.moveDown(current = 0, railCount = 1).consumed)
        assertFalse(TvHomeRailPolicy.moveUp(current = 0).consumed)
    }

    /* ---------------- ασφάλεια δείκτη ---------------- */

    @Test
    fun coerceClampsWhenRailsShrink() {
        // Ήσουν στη σειρά 7 και ο κατάλογος ξαναχτίστηκε με 3 σειρές.
        assertEquals(2, TvHomeRailPolicy.coerce(index = 7, railCount = 3))
    }

    @Test
    fun coerceHandlesEmptyCatalog() {
        assertEquals(0, TvHomeRailPolicy.coerce(index = 4, railCount = 0))
    }

    @Test
    fun coerceKeepsValidIndex() {
        assertEquals(1, TvHomeRailPolicy.coerce(index = 1, railCount = 3))
    }

    /* ---------------- πότε επιστρέφουμε στην πρώτη σειρά ---------------- */

    @Test
    fun signatureChangesWhenGroupsChange() {
        val before = TvHomeRailPolicy.signature(listOf("continue", "trending", "group:δράση"))
        val after = TvHomeRailPolicy.signature(listOf("continue", "trending", "group:κωμωδία"))
        assertTrue(TvHomeRailPolicy.shouldResetToFirst(before, after))
    }

    @Test
    fun signatureStableWhenOnlyContentGrows() {
        // Partial publish: ίδιες σειρές, περισσότερα στοιχεία μέσα τους.
        // ΔΕΝ πρέπει να πεταχτεί ο χρήστης στην πρώτη σειρά ενώ περιηγείται.
        val before = TvHomeRailPolicy.signature(listOf("continue", "trending", "new"))
        val after = TvHomeRailPolicy.signature(listOf("continue", "trending", "new"))
        assertFalse(TvHomeRailPolicy.shouldResetToFirst(before, after))
    }
}
