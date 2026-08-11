package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ο τίτλος που φεύγει προς το TMDB πρέπει να είναι ο τίτλος του έργου και
 * τίποτε άλλο.
 *
 * Οι πάροχοι κολλούν σημάνσεις γύρω του — πραγματικό δείγμα από λίστα:
 * «To Spiti Dipla Sto Potami #». Το «#» ταξίδευε μέχρι το ερώτημα και η
 * αναζήτηση δεν έβρισκε τίποτα, οπότε τα επεισόδια έπεφταν στη γενική
 * περίληψη της σειράς.
 */
class TmdbTitleCleanupTest {

    /* ---------------- ο θόρυβος φεύγει ---------------- */

    @Test
    fun theReportedProviderMarkerIsRemoved() {
        assertEquals(
            "To Spiti Dipla Sto Potami",
            TmdbClient.cleanTitle("To Spiti Dipla Sto Potami #"),
        )
    }

    @Test
    fun decorationIsRemovedFromBothEnds() {
        assertEquals("To Spiti", TmdbClient.cleanTitle("# To Spiti #"))
        assertEquals("Agries Melisses", TmdbClient.cleanTitle("*** Agries Melisses ***"))
        assertEquals("Sto Para Pente", TmdbClient.cleanTitle("•Sto Para Pente•"))
    }

    @Test
    fun leadingPlaybackAndRatingGlyphsAreRemoved() {
        assertEquals("To Kafe tis Xaras", TmdbClient.cleanTitle("► To Kafe tis Xaras"))
        assertEquals("To Soi sou", TmdbClient.cleanTitle("To Soi sou ★"))
        assertEquals("Oi Treis Xarites", TmdbClient.cleanTitle("Oi Treis Xarites ~"))
    }

    @Test
    fun greekTitlesAreCleanedTheSameWay() {
        assertEquals("Το Σπίτι Δίπλα στο Ποτάμι", TmdbClient.cleanTitle("Το Σπίτι Δίπλα στο Ποτάμι #"))
        assertEquals("Άγριες Μέλισσες", TmdbClient.cleanTitle("★ Άγριες Μέλισσες ★"))
    }

    /* ---------------- οι νόμιμοι τίτλοι επιβιώνουν ---------------- */

    @Test
    fun symbolsInsideATitleAreNeverTouched() {
        // Αν αφαιρεθούν εσωτερικά σύμβολα, αυτοί οι τίτλοι παύουν να υπάρχουν.
        assertEquals("M*A*S*H", TmdbClient.cleanTitle("M*A*S*H"))
        assertEquals("9-1-1", TmdbClient.cleanTitle("9-1-1"))
        assertEquals("Sex/Life", TmdbClient.cleanTitle("Sex/Life"))
        assertEquals("Law & Order", TmdbClient.cleanTitle("Law & Order"))
        assertEquals("Se7en", TmdbClient.cleanTitle("Se7en"))
    }

    @Test
    fun sentenceEndingPunctuationIsNotNoise() {
        // Ένα θαυμαστικό ή ερωτηματικό στο τέλος είναι μέρος του τίτλου.
        assertEquals("Hello!", TmdbClient.cleanTitle("Hello!"))
        assertEquals("Who?", TmdbClient.cleanTitle("Who?"))
    }

    @Test
    fun aCleanTitlePassesThroughUnchanged() {
        assertEquals("To Spiti Dipla Sto Potami", TmdbClient.cleanTitle("To Spiti Dipla Sto Potami"))
        assertEquals("Breaking Bad", TmdbClient.cleanTitle("Breaking Bad"))
    }

    @Test
    fun cleanupSurvivesTitlesThatAreOnlyNoise() {
        assertEquals("", TmdbClient.cleanTitle("###"))
        assertEquals("", TmdbClient.cleanTitle("  "))
        assertEquals("", TmdbClient.cleanTitle(""))
    }

    /* ---------------- συνεργασία με τους υπάρχοντες κανόνες ---------------- */

    @Test
    fun existingProviderPrefixAndQualityRulesStillApply() {
        // Ο νέος καθαρισμός τρέχει ΤΕΛΕΥΤΑΙΟΣ, οπότε πιάνει και ό,τι μένει
        // εκτεθειμένο αφού αφαιρεθούν τα προθέματα και οι σημάνσεις ποιότητας.
        assertEquals("Ο Νονός", TmdbClient.cleanTitle("GR| Ο Νονός HD [MULTI]"))
        assertEquals("Passenger", TmdbClient.cleanTitle("4K Passenger (2026)"))
    }

    @Test
    fun markerAndQualityTagTogether() {
        assertEquals(
            "To Spiti Dipla Sto Potami",
            TmdbClient.cleanTitle("GR| To Spiti Dipla Sto Potami HD #"),
        )
    }

    /* ---------------- η αλυσίδα μέχρι το ερώτημα ---------------- */

    @Test
    fun theCleanedTitleProducesAUsableGreekQuery() {
        val cleaned = TmdbClient.cleanTitle("To Spiti Dipla Sto Potami #")
        val query = GreeklishTitlePolicy.toGreek(cleaned)

        // Κανένα σύμβολο δεν επιβιώνει μέχρι το ερώτημα.
        assertEquals(query, query.filter { it.isLetterOrDigit() || it.isWhitespace() })
        // Και το αποτέλεσμα ταυτοποιεί το πραγματικό έργο.
        assertEquals(
            GreeklishTitlePolicy.latinSkeleton("Το Σπίτι Δίπλα στο Ποτάμι"),
            GreeklishTitlePolicy.latinSkeleton(query),
        )
    }
}
