package com.prelude.iptv.ui.policy

import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogPresentationPolicyTest {
    @Test fun `groups keep preferred order and append new groups deterministically`() {
        val channels = listOf(
            Channel(name = "One", group = "News"),
            Channel(name = "Two", group = "Sports"),
            Channel(name = "Three", group = ""),
            Channel(name = "Duplicate", group = "News"),
        )

        val result = CatalogPresentationPolicy.groups(
            channels = channels,
            hasFavorites = true,
            preferredTitles = listOf("Sports"),
            allGroupLabel = "All",
            favoritesGroupLabel = "Favorites",
        )

        assertEquals(listOf("All", "Favorites", "Sports", "News", "Χωρίς ομάδα"), result)
    }

    @Test fun `locked groups stay hidden from all group and search until unlocked`() {
        val channels = listOf(
            Channel(name = "World News", group = "News", url = "news"),
            Channel(name = "Kids Club", group = "Kids", url = "kids"),
        )

        val locked = visible(
            channels = channels,
            search = "kids",
            lockedGroups = setOf("Kids"),
            parentalUnlocked = false,
        )
        val unlocked = visible(
            channels = channels,
            search = "kids",
            lockedGroups = setOf("Kids"),
            parentalUnlocked = true,
        )

        assertEquals(emptyList<Channel>(), locked)
        assertEquals(listOf("Kids Club"), unlocked.map { it.name })
    }

    @Test fun `favorites filter and requested sort are applied together`() {
        val channels = listOf(
            Channel(name = "Zulu", group = "Movies", url = "z"),
            Channel(name = "Alpha", group = "Movies", url = "a"),
            Channel(name = "Beta", group = "Movies", url = "b"),
        )

        val result = visible(
            channels = channels,
            selectedGroup = "Favorites",
            favorites = setOf("z", "a"),
            sortMode = "az",
        )

        assertEquals(listOf("Alpha", "Zulu"), result.map { it.name })
    }

    @Test fun `default mode preserves provider order`() {
        val channels = listOf(
            Channel(name = "Second", year = "2024"),
            Channel(name = "First", year = "2026"),
        )

        assertEquals(channels, visible(channels = channels))
    }

    private fun visible(
        channels: List<Channel>,
        search: String = "",
        selectedGroup: String = "All",
        favorites: Set<String> = emptySet(),
        lockedGroups: Set<String> = emptySet(),
        parentalUnlocked: Boolean = false,
        sortMode: String = "default",
    ) = CatalogPresentationPolicy.visibleChannels(
        channels = channels,
        search = search,
        selectedGroup = selectedGroup,
        allGroupLabel = "All",
        favoritesGroupLabel = "Favorites",
        favorites = favorites,
        lockedGroups = lockedGroups,
        parentalUnlocked = parentalUnlocked,
        sortMode = sortMode,
        favoriteKey = { it.url },
    )
}
