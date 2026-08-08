package com.prelude.iptv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturedGroupsPolicyTest {

    private val available = listOf("A", "B", "C", "D", "E", "F", "G", "H")

    @Test
    fun resolveFallsBackToFirstSixWhenNothingSaved() {
        assertEquals(listOf("A", "B", "C", "D", "E", "F"), FeaturedGroupsPolicy.resolve(emptyList(), available))
    }

    @Test
    fun resolveKeepsUserOrderAndDropsStaleTitles() {
        val saved = listOf("C", "ZZZ", "A")
        assertEquals(listOf("C", "A"), FeaturedGroupsPolicy.resolve(saved, available))
    }

    @Test
    fun resolveCapsAtMax() {
        val saved = listOf("H", "G", "F", "E", "D", "C", "B", "A")
        assertEquals(6, FeaturedGroupsPolicy.resolve(saved, available).size)
        assertEquals(listOf("H", "G", "F", "E", "D", "C"), FeaturedGroupsPolicy.resolve(saved, available))
    }

    @Test
    fun resolveFallsBackWhenAllSavedAreStale() {
        assertEquals(listOf("A", "B", "C", "D", "E", "F"), FeaturedGroupsPolicy.resolve(listOf("X", "Y"), available))
    }

    @Test
    fun toggleAddsWhenAbsent() {
        assertEquals(listOf("A", "B"), FeaturedGroupsPolicy.toggle(listOf("A"), "B"))
    }

    @Test
    fun toggleRemovesWhenPresent() {
        assertEquals(listOf("A", "C"), FeaturedGroupsPolicy.toggle(listOf("A", "B", "C"), "B"))
    }

    @Test
    fun toggleIgnoresAddPastMax() {
        val full = listOf("A", "B", "C", "D", "E", "F")
        assertEquals(full, FeaturedGroupsPolicy.toggle(full, "G"))
        assertTrue(FeaturedGroupsPolicy.isFull(full))
        assertFalse(FeaturedGroupsPolicy.isFull(full.dropLast(1)))
    }
}
