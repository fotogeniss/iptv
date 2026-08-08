package com.prelude.iptv.ui.mobile.home

import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.CatalogRailSection
import com.prelude.iptv.ui.home.HomeLayoutPolicy
import com.prelude.iptv.ui.home.HomeRail
import com.prelude.iptv.ui.home.HomeRailContentPolicy

internal data class MobileHomeRailResolver(
    val canSuggest: Boolean,
    val suggestions: List<Channel>,
    val sections: List<CatalogRailSection>,
    val recentLive: List<Channel>,
    val live: List<Channel>,
    val movies: List<Channel>,
    val series: List<Channel>,
    val displayedMovies: List<Channel>,
    val displayedSeries: List<Channel>,
    val selectedDestination: String,
    val selectedCatalogGroup: String?,
    val categoryOf: (String) -> String,
) {
    fun railFor(id: String): HomeRail? = when (id) {
        HomeLayoutPolicy.SUGGESTIONS -> suggestions.takeIf { canSuggest }
            ?.let { HomeRailContentPolicy.rail(id, "Προτάσεις για σένα", it) }

        HomeLayoutPolicy.CONTINUE -> sections.firstOrNull { it.id == "continue" }
            ?.let {
                HomeRailContentPolicy.rail(
                    id, "Συνέχισε να βλέπεις", it.allItems, progress = it.progress
                )
            }

        HomeLayoutPolicy.RECENT_LIVE -> HomeRailContentPolicy.rail(
            id, "Κανάλια που είδες", recentLive, live = true, removable = true
        )

        HomeLayoutPolicy.NEW_LIVE -> HomeRailContentPolicy.rail(
            id, "Νέα ζωντανά", HomeRailContentPolicy.newest(live), live = true
        )

        HomeLayoutPolicy.NEW_MOVIES -> HomeRailContentPolicy.rail(
            id, "Νέες ταινίες", HomeRailContentPolicy.newest(displayedMovies)
        )

        HomeLayoutPolicy.NEW_EPISODES -> HomeRailContentPolicy.rail(
            id, "Νέα επεισόδια", HomeRailContentPolicy.newest(displayedSeries)
        )

        HomeLayoutPolicy.LIVE -> categoryRail(id, "Ζωντανά", live, categoryOf(id), live = true)
        HomeLayoutPolicy.MOVIES -> if (selectedDestination == "movies") {
            HomeRailContentPolicy.rail(id, selectedCatalogGroup ?: "Ταινίες", displayedMovies)
        } else {
            categoryRail(id, "Ταινίες", movies, categoryOf(id))
        }
        HomeLayoutPolicy.SERIES -> if (selectedDestination == "series") {
            HomeRailContentPolicy.rail(id, selectedCatalogGroup ?: "Σειρές", displayedSeries)
        } else {
            categoryRail(id, "Σειρές", series, categoryOf(id))
        }
        else -> null
    }
}

private fun categoryRail(
    id: String,
    label: String,
    pool: List<Channel>,
    category: String,
    live: Boolean = false,
): HomeRail? {
    if (category.isBlank()) return null
    val items = pool.filter { it.group.trim() == category }
    return HomeRailContentPolicy.rail(id, "$label · $category", items, live = live)
}

internal fun HomeRail.toCatalogSection() = CatalogRailSection(
    id = id,
    title = title,
    items = items,
    progress = progress,
    allItems = allItems
)
