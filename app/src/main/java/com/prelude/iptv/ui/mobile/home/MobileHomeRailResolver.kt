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
    /** Οι ΕΠΙΛΕΓΜΕΝΕΣ κατηγορίες μιας ενότητας, με τη σειρά που τις θέλει ο χρήστης. */
    val selectedCategoriesOf: (String) -> List<String> = { emptyList() },
    val sectionTitle: (String) -> String,
    val categoryTitle: (String, String) -> String,
) {
    private fun railFor(id: String): HomeRail? = when (id) {
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

        // Χωρίς βαθμολογίες από τον πάροχο, το topRated γυρίζει κενό και το
        // rail() γυρίζει null — η ενότητα δεν ζωγραφίζεται καθόλου.
        HomeLayoutPolicy.TOP_MOVIES -> HomeRailContentPolicy.rail(
            id, sectionTitle(id), HomeRailContentPolicy.topRated(displayedMovies)
        )

        HomeLayoutPolicy.TOP_SERIES -> HomeRailContentPolicy.rail(
            id, sectionTitle(id), HomeRailContentPolicy.topRated(displayedSeries)
        )

        else -> null
    }

    /**
     * ΜΙΑ ΕΝΟΤΗΤΑ ΜΠΟΡΕΙ ΝΑ ΔΩΣΕΙ ΠΟΛΛΕΣ ΡΑΓΕΣ.
     *
     * Οι ενότητες κατηγοριών (Ζωντανά, Ταινίες, Σειρές) παράγουν μία ράγα ανά
     * ΕΠΙΛΕΓΜΕΝΗ κατηγορία, με τη σειρά που τις διάλεξε ο χρήστης. Όλες οι
     * υπόλοιπες δίνουν το πολύ μία, όπως πάντα.
     *
     * Το id κάθε ράγας φέρει την κατηγορία (`movies:GR | KIDS`), γιατί είναι
     * κλειδί λίστας στο Compose και δύο ράγες της ίδιας ενότητας δεν επιτρέπεται
     * να μοιράζονται κλειδί.
     */
    fun railsFor(id: String): List<HomeRail> = when (id) {
        HomeLayoutPolicy.LIVE -> categoryRails(id, sectionTitle(id), live, live = true)

        // Μέσα στην ίδια την ενότητα (πάτησες «Ταινίες» κάτω), η επιλεγμένη ομάδα
        // του καταλόγου κερδίζει: εκεί ο χρήστης φιλτράρει ρητά, δεν συνθέτει.
        HomeLayoutPolicy.MOVIES -> if (selectedDestination == "movies") {
            listOfNotNull(
                HomeRailContentPolicy.rail(id, selectedCatalogGroup ?: sectionTitle(id), displayedMovies)
            )
        } else {
            categoryRails(id, sectionTitle(id), movies)
        }

        HomeLayoutPolicy.SERIES -> if (selectedDestination == "series") {
            listOfNotNull(
                HomeRailContentPolicy.rail(id, selectedCatalogGroup ?: sectionTitle(id), displayedSeries)
            )
        } else {
            categoryRails(id, sectionTitle(id), series)
        }

        else -> listOfNotNull(railFor(id))
    }

    private fun categoryRails(
        id: String,
        label: String,
        pool: List<Channel>,
        live: Boolean = false,
    ): List<HomeRail> {
        val selected = selectedCategoriesOf(id).ifEmpty {
            listOfNotNull(categoryOf(id).takeIf { it.isNotBlank() })
        }
        return selected.mapNotNull { category ->
            val items = pool.filter { it.group.trim() == category }
            HomeRailContentPolicy.rail(
                id = "$id:$category",
                title = categoryTitle(label, category),
                all = items,
                live = live,
            )
        }
    }
}

internal fun HomeRail.toCatalogSection() = CatalogRailSection(
    id = id,
    title = title,
    items = items,
    progress = progress,
    allItems = allItems
)
