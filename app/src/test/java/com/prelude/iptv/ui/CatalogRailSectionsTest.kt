package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Οι ράγες που βλέπει η ΤΗΛΕΟΡΑΣΗ.
 *
 * Το κινητό χτίζει την αρχική του από τον `HomeLayoutPolicy`, όπου ο χρήστης
 * ορίζει σειρά και ορατότητα. Η τηλεόραση δεν έχει τέτοιον επεξεργαστή: η
 * αρχική της είναι ακριβώς η λίστα που επιστρέφει το
 * [buildCatalogRailSections], οπότε ό,τι λείπει από εδώ δεν υπάρχει σε
 * τηλεόραση. Αυτό ήταν και η αιτία που οι νέες ράγες φάνηκαν μόνο στο κινητό.
 */
class CatalogRailSectionsTest {

    private val labels = CatalogRailLabels(
        continueWatching = "Συνέχισε",
        myList = "Η λίστα μου",
        trending = "Κορυφαία",
        newReleases = "Νέα",
        newMovies = "Νέες ταινίες",
        newEpisodes = "Νέα επεισόδια",
        topMovies = "Κορυφαίες ταινίες",
        topSeries = "Κορυφαίες σειρές",
    )

    private fun movie(name: String, rating: String = "", addedAt: String = "") =
        Channel(name = name, kind = "vod", url = "http://p/$name", rating = rating, addedAt = addedAt)

    private fun show(name: String, rating: String = "", addedAt: String = "") =
        Channel(name = name, kind = "series", seriesId = name, rating = rating, addedAt = addedAt)

    private fun build(channels: List<Channel>) =
        buildCatalogRailSections(channels, emptySet(), emptyList(), labels)

    @Test
    fun theTelevisionGetsTheSameFourRailsAsTheHandset() {
        val channels = (1..5).map { movie("Ταινία $it", rating = "$it", addedAt = "2025-01-0$it 00:00:00") } +
            (1..5).map { show("Σειρά $it", rating = "$it", addedAt = "2025-02-0$it 00:00:00") }

        val ids = build(channels).map { it.id }

        assertTrue("new-movies" in ids)
        assertTrue("new-episodes" in ids)
        assertTrue("top-movies" in ids)
        assertTrue("top-series" in ids)
    }

    @Test
    fun topMoviesIsOrderedByRatingAndMarkedAsRanked() {
        val channels = listOf(
            movie("μέτρια", rating = "4"),
            movie("άριστη", rating = "9"),
            movie("καλή", rating = "7"),
            movie("πολύ καλή", rating = "8"),
        )

        val rail = build(channels).first { it.id == "top-movies" }

        assertEquals(listOf("άριστη", "πολύ καλή", "καλή", "μέτρια"), rail.items.map(Channel::name))
        assertTrue("η ράγα τυπώνει θέσεις, άρα οφείλει να είναι κατάταξη", rail.ranked)
    }

    @Test
    fun newMoviesIsOrderedByWhenItWasAddedNotByTheProviderOrder() {
        val channels = listOf(
            movie("τρίτη", addedAt = "2025-01-01 00:00:00"),
            movie("πρώτη", addedAt = "2025-03-01 00:00:00"),
            movie("τέταρτη", addedAt = "2024-12-01 00:00:00"),
            movie("δεύτερη", addedAt = "2025-02-01 00:00:00"),
        )

        val rail = build(channels).first { it.id == "new-movies" }

        assertEquals(listOf("πρώτη", "δεύτερη", "τρίτη", "τέταρτη"), rail.items.map(Channel::name))
        assertFalse("τα «νέα» δεν είναι κατάταξη — δεν παίρνουν νούμερα", rail.ranked)
    }

    @Test
    fun railsThatCannotBeFilledAreLeftOutEntirely() {
        // Τρεις ταινίες με βαθμολογία δεν κάνουν πίνακα κορυφαίων.
        val channels = listOf(movie("α", rating = "9"), movie("β", rating = "8"), movie("γ"))

        val ids = build(channels).map { it.id }

        assertFalse("top-movies" in ids)
        assertFalse("top-series" in ids)
    }

    @Test
    fun theMainTrendingRailSurvivesASourceWithoutAnyRatings() {
        // Φρουρός εφεδρείας: είναι η κύρια ράγα της αρχικής και προϋπήρχε. Αν
        // εξαφανιζόταν σε πηγή χωρίς βαθμολογίες, η τηλεόραση θα άδειαζε.
        val channels = (1..6).map { movie("Χωρίς βαθμό $it") }

        val trending = build(channels).firstOrNull { it.id == "trending" }

        assertTrue("η κύρια ράγα πρέπει να υπάρχει", trending != null)
        assertEquals(6, trending!!.allItems.size)
        assertFalse("χωρίς βαθμολογίες δεν επιτρέπονται νούμερα θέσης", trending.ranked)
    }

    @Test
    fun liveChannelsNeverEnterTheseRails() {
        val channels = listOf(
            movie("ταινία", rating = "9"),
            Channel(name = "κανάλι", kind = "live", url = "http://p/live", rating = "10"),
        )

        val everyItem = build(channels).flatMap { it.allItems }

        assertTrue(everyItem.none { it.kind == "live" })
    }
}
