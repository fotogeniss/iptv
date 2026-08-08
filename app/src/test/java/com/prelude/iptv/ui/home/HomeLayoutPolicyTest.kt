package com.prelude.iptv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutPolicyTest {

    private fun ids(entries: List<HomeEntry>) = entries.map { it.section.id }

    @Test
    fun `χωρίς αποθηκευμένα δίνει την προεπιλογή`() {
        assertEquals(HomeLayoutPolicy.DEFAULT.map { it.id }, ids(HomeLayoutPolicy.resolve()))
    }

    @Test
    fun `όλα ορατά όταν δεν έχει κρυφτεί τίποτα`() {
        assertTrue(HomeLayoutPolicy.resolve().all { it.visible })
    }

    @Test
    fun `κρυμμένη ενότητα μένει στη λίστα αλλά σημαδεμένη`() {
        val out = HomeLayoutPolicy.resolve(hidden = setOf(HomeLayoutPolicy.NEW_LIVE))
        val entry = out.first { it.section.id == HomeLayoutPolicy.NEW_LIVE }
        assertFalse(entry.visible)
        // Δεν φεύγει: αλλιώς δεν θα υπήρχε τρόπος να ξαναεμφανιστεί.
        assertTrue(out.size == HomeLayoutPolicy.DEFAULT.size)
    }

    @Test
    fun `σταθερή ενότητα δεν κρύβεται ποτέ`() {
        val hidden = HomeLayoutPolicy.toggle(emptySet(), HomeLayoutPolicy.HEADER)
        assertTrue(hidden.isEmpty())
        val out = HomeLayoutPolicy.resolve(hidden = setOf(HomeLayoutPolicy.HEADER))
        assertTrue(out.first { it.section.id == HomeLayoutPolicy.HEADER }.visible)
    }

    @Test
    fun `το μάτι ανάβει και σβήνει`() {
        val once = HomeLayoutPolicy.toggle(emptySet(), HomeLayoutPolicy.CONTINUE)
        assertEquals(setOf(HomeLayoutPolicy.CONTINUE), once)
        assertTrue(HomeLayoutPolicy.toggle(once, HomeLayoutPolicy.CONTINUE).isEmpty())
    }

    @Test
    fun `αποθηκευμένη σειρά τηρείται`() {
        val saved = listOf(
            HomeLayoutPolicy.HEADER,
            HomeLayoutPolicy.HEADER,
            HomeLayoutPolicy.SERIES,
            HomeLayoutPolicy.MOVIES,
        )
        val out = ids(HomeLayoutPolicy.resolve(saved))
        assertEquals(HomeLayoutPolicy.HEADER, out[0])
        assertEquals(HomeLayoutPolicy.SERIES, out[1])
        assertEquals(HomeLayoutPolicy.MOVIES, out[2])
        assertEquals(1, out.count { it == HomeLayoutPolicy.HEADER })
    }

    @Test
    fun `ενότητα που λείπει από τα αποθηκευμένα μπαίνει στο τέλος`() {
        // Παλιά αποθήκευση, πριν προστεθεί η «Νέα επεισόδια».
        val saved = HomeLayoutPolicy.DEFAULT.map { it.id } - HomeLayoutPolicy.NEW_EPISODES
        val out = ids(HomeLayoutPolicy.resolve(saved))
        assertEquals(HomeLayoutPolicy.DEFAULT.size, out.size)
        assertEquals(HomeLayoutPolicy.NEW_EPISODES, out.last())
    }

    @Test
    fun `άγνωστο id αγνοείται`() {
        val out = ids(HomeLayoutPolicy.resolve(listOf("κάτι-που-καταργήθηκε", HomeLayoutPolicy.SERIES)))
        assertFalse(out.contains("κάτι-που-καταργήθηκε"))
        assertEquals(HomeLayoutPolicy.DEFAULT.size, out.size)
    }

    @Test
    fun `διπλότυπο id δεν διπλασιάζει τη γραμμή`() {
        val saved = listOf(HomeLayoutPolicy.SERIES, HomeLayoutPolicy.SERIES)
        val out = ids(HomeLayoutPolicy.resolve(saved))
        assertEquals(HomeLayoutPolicy.DEFAULT.size, out.size)
        assertEquals(1, out.count { it == HomeLayoutPolicy.SERIES })
    }

    @Test
    fun `η σταθερή ανεβαίνει στην κορυφή ακόμη κι αν το αρχείο λέει αλλιώς`() {
        val saved = listOf(HomeLayoutPolicy.SERIES, HomeLayoutPolicy.MOVIES, HomeLayoutPolicy.HEADER)
        val out = ids(HomeLayoutPolicy.resolve(saved))
        assertEquals(HomeLayoutPolicy.HEADER, out[0])
        assertEquals(HomeLayoutPolicy.SERIES, out[1])
    }

    @Test
    fun `μετακίνηση αλλάζει θέση`() {
        val order = HomeLayoutPolicy.DEFAULT.map { it.id }
        val moved = HomeLayoutPolicy.move(order, from = order.lastIndex, to = HomeLayoutPolicy.FIXED_COUNT)
        assertEquals(HomeLayoutPolicy.SERIES, moved[HomeLayoutPolicy.FIXED_COUNT])
        assertEquals(order.size, moved.size)
    }

    @Test
    fun `μετακίνηση δεν περνά πάνω από τις σταθερές`() {
        val order = HomeLayoutPolicy.DEFAULT.map { it.id }
        val moved = HomeLayoutPolicy.move(order, from = order.lastIndex, to = 0)
        assertEquals(HomeLayoutPolicy.HEADER, moved[0])
        // Σταματά στο πρώτο επιτρεπτό σημείο αντί να αγνοηθεί.
        assertEquals(HomeLayoutPolicy.SERIES, moved[HomeLayoutPolicy.FIXED_COUNT])
        assertEquals(1, moved.count { it == HomeLayoutPolicy.HEADER })
    }

    @Test
    fun `σταθερή γραμμή δεν σύρεται`() {
        val order = HomeLayoutPolicy.DEFAULT.map { it.id }
        assertEquals(order, HomeLayoutPolicy.move(order, from = 0, to = 5))
    }

    @Test
    fun `δείκτης εκτός ορίων δεν χαλά τη λίστα`() {
        val order = HomeLayoutPolicy.DEFAULT.map { it.id }
        assertEquals(order, HomeLayoutPolicy.move(order, from = 3, to = 99))
        assertEquals(order, HomeLayoutPolicy.move(order, from = -1, to = 3))
        assertEquals(order, HomeLayoutPolicy.move(order, from = 3, to = 3))
    }

    @Test
    fun `σταθερή είναι μόνο η κεφαλίδα`() {
        assertEquals(1, HomeLayoutPolicy.FIXED_COUNT)
        assertEquals(HomeLayoutPolicy.HEADER, HomeLayoutPolicy.DEFAULT[0].id)
        assertEquals(HomeLayoutPolicy.HERO, HomeLayoutPolicy.DEFAULT[1].id)
    }

    @Test
    fun `τα καθαριζόμενα είναι μόνο τα ιστορικά`() {
        val clearable = HomeLayoutPolicy.DEFAULT.filter { it.clearable }.map { it.id }
        assertEquals(listOf(HomeLayoutPolicy.CONTINUE, HomeLayoutPolicy.RECENT_LIVE), clearable)
    }

    @Test
    fun `κατηγορία διαλέγεται μόνο στα τρία μεγάλα rails`() {
        val categorised = HomeLayoutPolicy.DEFAULT.filter { it.categorised }.map { it.id }
        assertEquals(
            listOf(HomeLayoutPolicy.LIVE, HomeLayoutPolicy.MOVIES, HomeLayoutPolicy.SERIES),
            categorised
        )
    }

    @Test
    fun `η σειρά επιβιώνει σε αποθήκευση και επαναφόρτωση`() {
        var order = HomeLayoutPolicy.DEFAULT.map { it.id }
        order = HomeLayoutPolicy.move(order, order.lastIndex, HomeLayoutPolicy.FIXED_COUNT)
        val hidden = HomeLayoutPolicy.toggle(emptySet(), HomeLayoutPolicy.NEW_LIVE)
        val reloaded = HomeLayoutPolicy.resolve(order, hidden)
        assertEquals(order, HomeLayoutPolicy.idsOf(reloaded))
        assertFalse(reloaded.first { it.section.id == HomeLayoutPolicy.NEW_LIVE }.visible)
    }
}
