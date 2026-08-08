package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Καρφώνει τη συμπεριφορά που έκρυβε το σφάλμα «η σειρά δείχνει ΕΝΑ επεισόδιο».
 *
 * Δεν δοκιμάζει το `openSeries` (θέλει ViewModel και δίκτυο). Δοκιμάζει το ΓΕΓΟΝΟΣ
 * πάνω στο οποίο στηριζόταν η λανθασμένη εφεδρεία: ότι οι γονείς μιας σειράς **δεν
 * περιέχουν** τα επεισόδιά τους, οπότε μια δεύτερη κανονικοποίηση της
 * δημοσιευμένης λίστας δεν μπορεί να τα ανακτήσει.
 */
class CatalogNormalizerSeriesTest {

    private fun episode(series: String, season: Int, number: Int, url: String) = Channel(
        name = "$series S%02dE%02d".format(season, number),
        kind = "series",
        url = url,
    )

    @Test
    fun `τα επεισόδια βγαίνουν χωριστά από τους γονείς`() {
        val raw = (1..6).map { episode("Ο Γιατρός", 1, it, "http://x/$it") }
        val result = CatalogNormalizer.normalize("series", raw)

        // Ένας γονέας, έξι επεισόδια — και τα επεισόδια ΔΕΝ είναι στα items.
        assertEquals(1, result.items.size)
        assertTrue(result.items.none { it.kind == "series_ep" })

        val seasons = result.seriesEpisodes.values.single()
        assertEquals(1, seasons.size)
        assertEquals(6, seasons.single().second.size)
    }

    @Test
    fun `ξανακανονικοποιώντας τους γονείς δεν επιστρέφουν επεισόδια`() {
        // Αυτό ήταν το λάθος: το openSeries ξανακανονικοποιούσε τη δημοσιευμένη
        // λίστα (γονείς) και περίμενε να βρει επεισόδια. Δεν υπάρχουν εκεί.
        val raw = (1..6).map { episode("Ο Γιατρός", 1, it, "http://x/$it") }
        val parents = CatalogNormalizer.normalize("series", raw).items

        val again = CatalogNormalizer.normalize("series", parents)
        assertTrue(
            "Οι γονείς δεν κρύβουν επεισόδια — η παλιά εφεδρεία ήταν αδύνατη",
            again.seriesEpisodes.values.all { seasons -> seasons.all { it.second.isEmpty() } } ||
                again.seriesEpisodes.isEmpty()
        )
    }

    @Test
    fun `γονείς συν ενα επεισοδιο ιστορικου δινουν σειρα με ΕΝΑ επεισοδιο`() {
        // Η ακριβής αναπαραγωγή του σφάλματος: αυτό έβλεπε ο χρήστης.
        //
        // Ο υπολογισμός δεν είναι λάθος — τα δεδομένα που του δίνονταν ήταν. Ένα
        // επεισόδιο στο ιστορικό είναι ένα επεισόδιο, και η οθόνη πίστευε ότι αυτό
        // είναι όλη η σειρά.
        val raw = (1..6).map { episode("Ο Γιατρός", 1, it, "http://x/$it") }
        val full = CatalogNormalizer.normalize("series", raw)
        val watched = full.seriesEpisodes.values.single().single().second.first()

        val poisoned = CatalogNormalizer.normalize("series", full.items + watched)
        val seasons = poisoned.seriesEpisodes.values.firstOrNull().orEmpty()
        assertEquals(1, seasons.size)
        assertEquals(1, seasons.single().second.size)
    }

    @Test
    fun `πολλές σεζόν ομαδοποιούνται και ταξινομούνται`() {
        val raw = listOf(
            episode("Το Σόι Σου", 2, 1, "http://x/s2e1"),
            episode("Το Σόι Σου", 1, 2, "http://x/s1e2"),
            episode("Το Σόι Σου", 1, 1, "http://x/s1e1"),
            episode("Το Σόι Σου", 3, 1, "http://x/s3e1"),
        )
        val seasons = CatalogNormalizer.normalize("series", raw).seriesEpisodes.values.single()
        assertEquals(listOf("Season 1", "Season 2", "Season 3"), seasons.map { it.first })
        assertEquals(2, seasons.first().second.size)
    }

    @Test
    fun `διπλότυπα επεισόδια με το ίδιο url μετριούνται μία φορά`() {
        val raw = listOf(
            episode("Η Γη Της Ελιάς", 1, 1, "http://x/1"),
            episode("Η Γη Της Ελιάς", 1, 1, "http://x/1"),
        )
        val seasons = CatalogNormalizer.normalize("series", raw).seriesEpisodes.values.single()
        assertEquals(1, seasons.single().second.size)
    }
}
