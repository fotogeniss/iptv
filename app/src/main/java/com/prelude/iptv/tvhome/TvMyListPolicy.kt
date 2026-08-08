package com.prelude.iptv.tvhome

import com.prelude.iptv.data.SourceFavorite
import com.prelude.iptv.data.SourceFavoritePolicy

object TvMyListPolicy {
    const val MAX_ITEMS = 20

    fun select(
        favorites: List<SourceFavorite>,
        availableSourceIds: Set<String>,
        lockedGroups: Set<String>,
        maxItems: Int = MAX_ITEMS
    ): List<SourceFavorite> {
        if (maxItems <= 0 || availableSourceIds.isEmpty()) return emptyList()
        val locked = lockedGroups.mapTo(HashSet()) { it.trim().lowercase() }
        val seen = HashSet<String>()
        return favorites.asSequence()
            .filter { it.sourceId in availableSourceIds }
            .filter { SourceFavoritePolicy.playable(it.channel) }
            .filter { it.channel.group.trim().lowercase() !in locked }
            .sortedByDescending { it.addedAtMs }
            .filter { seen.add(it.identity) }
            .take(maxItems)
            .toList()
    }
}
