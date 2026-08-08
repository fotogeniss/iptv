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
    val sectionTitle: (String) -> String,
    val categoryTitle: (String, String) -> String,
) {
    fun railFor(id: String): HomeRail? = when (id) {
        HomeLayoutPolicy.SUGGESTIONS -> suggestions.takeIf { canSuggest }
            ?.let { HomeRailContentPolicy.rail(id, sectionTitle(id), it) }

        HomeLayoutPolicy.CONTINUE -> sections.firstOrNull { it.id == "continue" }
            ?.let {
                HomeRailContentPolicy.rail(
                    id, sectionTitle(id), it.allItems, progress = it.progress
                )
            }

        HomeLayoutPolicy.RECENT_LIVE -> HomeRailContentPolicy.rail(
            id, sectionTitle(id), recentLive, live = true, removable = true
        )

        HomeLayoutPolicy.NEW_LIVE -> HomeRailContentPolicy.rail(
            id, sectionTitle(id), HomeRailContentPolicy.newest(live), live = true
        )

        HomeLayoutPolicy.NEW_MOVIES -> HomeRailContentPolicy.rail(
            id, sectionTitle(id), HomeRailContentPolicy.newest(displayedMovies)
        )

        HomeLayoutPolicy.NEW_EPISODES -> HomeRailContentPolicy.rail(
            id, sectionTitle(id), HomeRailContentPolicy.newest(displayedSeries)
        )

        HomeLayoutPolicy.LIVE -> categoryRail(id, sectionTitle(id), live, categoryOf(id), categoryTitle, live = true)
        HomeLayoutPolicy.MOVIES -> if (selectedDestination == "movies") {
            HomeRailContentPolicy.rail(id, selectedCatalogGroup ?: sectionTitle(id), displayedMovies)
        } else {
            categoryRail(id, sectionTitle(id), movies, categoryOf(id), categoryTitle)
        }
        HomeLayoutPolicy.SERIES -> if (selectedDestination == "series") {
            HomeRailContentPolicy.rail(id, selectedCatalogGroup ?: sectionTitle(id), displayedSeries)
        } else {
            categoryRail(id, sectionTitle(id), series, categoryOf(id), categoryTitle)
        }
        else -> null
    }
}

private fun categoryRail(
    id: String,
    label: String,
    pool: List<Channel>,
    category: String,
    categoryTitle: (String, String) -> String,
    live: Boolean = false,
): HomeRail? {
    if (category.isBlank()) return null
    val items = pool.filter { it.group.trim() == category }
    return HomeRailContentPolicy.rail(id, categoryTitle(label, category), items, live = live)
}

internal fun HomeRail.toCatalogSection() = CatalogRailSection(
    id = id,
    title = title,
    items = items,
    progress = progress,
    allItems = allItems
)
