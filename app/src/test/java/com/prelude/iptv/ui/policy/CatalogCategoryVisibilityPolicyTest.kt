package com.prelude.iptv.ui.policy

import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogCategoryVisibilityPolicyTest {
    private val categories = listOf(
        "10" to "News",
        "20" to "Sports",
        "30" to "Empty",
    )
    private val channels = listOf(
        Channel(name = "News one", group = "News"),
        Channel(name = "News two", group = "News"),
        Channel(name = "Sports one", group = "Sports"),
    )

    @Test
    fun countsIncludeProviderCategoriesWithNoItems() {
        assertEquals(mapOf("10" to 2, "20" to 1, "30" to 0),
            CatalogCategoryVisibilityPolicy.counts(categories, channels))
    }

    @Test
    fun selectedCategoriesFilterOnlyTheAlreadyLoadedSnapshot() {
        assertEquals(listOf("Sports one"),
            CatalogCategoryVisibilityPolicy.visibleChannels(categories, channels, listOf("20")).map(Channel::name))
        assertEquals(emptyList<Channel>(),
            CatalogCategoryVisibilityPolicy.visibleChannels(categories, channels, emptyList()))
        assertEquals(channels,
            CatalogCategoryVisibilityPolicy.visibleChannels(categories, channels, null))
    }

    @Test
    fun invalidLegacyIdsSafelyDefaultToAll() {
        assertNull(CatalogCategoryVisibilityPolicy.initialSelection(categories, true, listOf("old-title")))
        assertEquals(emptySet<String>(),
            CatalogCategoryVisibilityPolicy.initialSelection(categories, true, emptyList()))
    }
}
