package com.prelude.iptv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceDeletionPolicyTest {
    @Test fun `deleting a source after active preserves active source`() {
        val decision = SourceDeletionPolicy.decide(sizeAfter = 2, removedIndex = 2, activeIndex = 0)
        assertEquals(0, decision.newActiveIndex)
        assertFalse(decision.removedActiveSource)
        assertFalse(decision.hasReplacementSource)
    }

    @Test fun `deleting a source before active shifts index but preserves source`() {
        val decision = SourceDeletionPolicy.decide(sizeAfter = 2, removedIndex = 0, activeIndex = 2)
        assertEquals(1, decision.newActiveIndex)
        assertFalse(decision.removedActiveSource)
    }

    @Test fun `deleting active source activates bounded replacement`() {
        val decision = SourceDeletionPolicy.decide(sizeAfter = 2, removedIndex = 1, activeIndex = 1)
        assertEquals(1, decision.newActiveIndex)
        assertTrue(decision.removedActiveSource)
        assertTrue(decision.hasReplacementSource)
    }

    @Test fun `deleting only active source yields empty state`() {
        val decision = SourceDeletionPolicy.decide(sizeAfter = 0, removedIndex = 0, activeIndex = 0)
        assertEquals(0, decision.newActiveIndex)
        assertTrue(decision.removedActiveSource)
        assertFalse(decision.hasReplacementSource)
    }

    @Test fun `duplicate source keeps source scoped data`() {
        assertFalse(SourceDeletionPolicy.isLastReference("source-a", listOf("source-a", "source-b")))
    }

    @Test fun `last source reference permits cleanup`() {
        assertTrue(SourceDeletionPolicy.isLastReference("source-a", listOf("source-b")))
    }
    @Test fun `all valid deletion combinations preserve a bounded index`() {
        for (sizeBefore in 1..100) {
            for (activeIndex in 0 until sizeBefore) {
                for (removedIndex in 0 until sizeBefore) {
                    val sizeAfter = sizeBefore - 1
                    val decision = SourceDeletionPolicy.decide(sizeAfter, removedIndex, activeIndex)
                    if (sizeAfter == 0) {
                        assertEquals(0, decision.newActiveIndex)
                    } else {
                        assertTrue(decision.newActiveIndex in 0 until sizeAfter)
                    }
                    assertEquals(removedIndex == activeIndex, decision.removedActiveSource)
                    assertEquals(removedIndex == activeIndex && sizeAfter > 0, decision.hasReplacementSource)
                }
            }
        }
    }

}
