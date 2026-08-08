package com.prelude.iptv.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryLayoutPolicyTest {
    private val available = listOf(
        CategoryOption("news", "News"),
        CategoryOption("sports", "Sports"),
        CategoryOption("kids", "Kids"),
    )

    @Test
    fun `saved order is respected and new provider groups are appended`() {
        val layout = CategoryLayout(order = listOf("sports", "news"))

        assertEquals(
            listOf("sports", "news", "kids"),
            CategoryLayoutPolicy.resolve(available, layout).map { it.option.id },
        )
    }

    @Test
    fun `delete is reversible without losing provider category`() {
        val deleted = CategoryLayoutPolicy.delete(CategoryLayout(), "sports")

        assertFalse(CategoryLayoutPolicy.resolve(available, deleted).any { it.option.id == "sports" })
        assertEquals(listOf("sports"), CategoryLayoutPolicy.deleted(available, deleted).map { it.id })

        val restored = CategoryLayoutPolicy.restore(deleted, "sports")
        assertTrue(CategoryLayoutPolicy.resolve(available, restored).any { it.option.id == "sports" })
    }

    @Test
    fun `hidden groups stay in order but are excluded from selected ids`() {
        val layout = CategoryLayout(order = listOf("kids", "sports", "news"), hidden = setOf("sports"))
        val entries = CategoryLayoutPolicy.resolve(available, layout)

        assertEquals(listOf("kids", "sports", "news"), entries.map { it.option.id })
        assertEquals(listOf("kids", "news"), CategoryLayoutPolicy.selectedIds(entries))
    }

    @Test
    fun `screen groups follow saved titles and append new groups`() {
        val actual = listOf("Kids", "News", "Sports", "Documentaries")

        assertEquals(
            listOf("Sports", "News", "Kids", "Documentaries"),
            CategoryLayoutPolicy.orderByTitle(actual, listOf("Sports", "News", "Kids")) { it },
        )
    }
}
