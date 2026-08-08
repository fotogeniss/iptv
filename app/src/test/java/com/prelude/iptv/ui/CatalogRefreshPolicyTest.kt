package com.prelude.iptv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogRefreshPolicyTest {
    private val fresh = listOf("news" to "News", "sports" to "Sports", "kids" to "Kids")

    @Test fun allGroupsRemainsAll() {
        assertNull(CatalogRefreshPolicy.initialSelection(fresh, null))
    }

    @Test fun existingGroupsArePreselected() {
        assertEquals(
            linkedSetOf("sports", "kids"),
            CatalogRefreshPolicy.initialSelection(fresh, listOf("sports", "kids"))
        )
    }

    @Test fun removedGroupsAreDroppedButNewGroupsStayAvailable() {
        assertEquals(
            linkedSetOf("news"),
            CatalogRefreshPolicy.initialSelection(fresh, listOf("removed", "news"))
        )
    }

    @Test
    fun `refresh keeps the visible group when it still exists`() {
        val groups = listOf("Όλα τα κανάλια", "News", "Sports")
        assertEquals(
            "Sports",
            CatalogRefreshPolicy.restoredVisibleGroup("Sports", groups, "Όλα τα κανάλια")
        )
    }

    @Test
    fun `refresh falls back to all when the visible group disappeared`() {
        val groups = listOf("Όλα τα κανάλια", "News")
        assertEquals(
            "Όλα τα κανάλια",
            CatalogRefreshPolicy.restoredVisibleGroup("Removed", groups, "Όλα τα κανάλια")
        )
    }


    @Test
    fun `empty-category refresh fallback remains transactional`() {
        assertEquals(
            true,
            CatalogRefreshPolicy.usesTransactionalSelectionCommit(
                pickerFromRefresh = false,
                directRefreshFallback = true
            )
        )
    }

    @Test
    fun `normal initial load does not defer selection persistence`() {
        assertEquals(
            false,
            CatalogRefreshPolicy.usesTransactionalSelectionCommit(
                pickerFromRefresh = false,
                directRefreshFallback = false
            )
        )
    }
}
