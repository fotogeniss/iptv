package com.prelude.iptv.ui.policy

import com.prelude.iptv.data.Channel

/** Pure group ordering, visibility and sorting rules for a loaded catalog. */
object CatalogPresentationPolicy {
    private const val UNGROUPED = "Χωρίς ομάδα"

    fun groups(
        channels: List<Channel>,
        hasFavorites: Boolean,
        preferredTitles: List<String>,
        allGroupLabel: String,
        favoritesGroupLabel: String,
    ): List<String> {
        val seen = LinkedHashSet<String>()
        channels.forEach { seen.add(it.group.ifEmpty { UNGROUPED }) }
        val rank = preferredTitles.withIndex().associate { it.value to it.index }
        val sorted = seen.sortedWith(
            compareBy<String> { rank[it] ?: Int.MAX_VALUE }
                .thenBy { if (it in rank) "" else it.lowercase() },
        )
        return buildList {
            add(allGroupLabel)
            if (hasFavorites) add(favoritesGroupLabel)
            addAll(sorted)
        }
    }

    fun visibleChannels(
        channels: List<Channel>,
        search: String,
        selectedGroup: String,
        allGroupLabel: String,
        favoritesGroupLabel: String,
        favorites: Set<String>,
        lockedGroups: Set<String>,
        parentalUnlocked: Boolean,
        sortMode: String,
        favoriteKey: (Channel) -> String,
    ): List<Channel> {
        val query = search.trim().lowercase()
        val visible = channels.filter { channel ->
            val groupName = channel.group.ifEmpty { UNGROUPED }
            if (!parentalUnlocked && groupName in lockedGroups) return@filter false
            val groupMatches = when (selectedGroup) {
                allGroupLabel -> true
                favoritesGroupLabel -> favoriteKey(channel) in favorites
                else -> groupName == selectedGroup
            }
            val searchMatches = query.isEmpty() || channel.name.lowercase().contains(query)
            groupMatches && searchMatches
        }
        return when (sortMode) {
            "az" -> visible.sortedBy { it.name.lowercase() }
            "za" -> visible.sortedByDescending { it.name.lowercase() }
            "year" -> visible.sortedByDescending { it.year }
            else -> visible
        }
    }
}
