package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TrackLabelPolicyTest {

    private val greek = Locale.forLanguageTag("el")

    private fun label(language: String?, providerLabel: String?, index: Int): String =
        TrackLabelPolicy.trackLabel(language, providerLabel, "Κομμάτι $index", greek)

    @Test
    fun greekIsRecognisedInAllThreeCodesThatProvidersUse() {
        // «el» = ISO 639-1, «ell» = 639-2/T, «gre» = 639-2/B. Οι πάροχοι
        // χρησιμοποιούν και τους τρεις, συχνά στην ίδια λίστα.
        assertEquals("Ελληνικά", TrackLabelPolicy.languageName("el", greek))
        assertEquals("Ελληνικά", TrackLabelPolicy.languageName("ell", greek))
        assertEquals("Ελληνικά", TrackLabelPolicy.languageName("gre", greek))
    }

    @Test
    fun regionSuffixesAreStripped() {
        assertEquals("Ελληνικά", TrackLabelPolicy.languageName("el-GR", greek))
        assertEquals("Πορτογαλικά", TrackLabelPolicy.languageName("pt_BR", greek))
    }

    @Test
    fun caseAndWhitespaceDoNotMatter() {
        assertEquals("Αγγλικά", TrackLabelPolicy.languageName("  ENG ", greek))
    }

    @Test
    fun displayLanguageFollowsTheRequestedLocale() {
        assertEquals("Greek", TrackLabelPolicy.languageName("ell", Locale.ENGLISH))
        assertEquals(
            "Track 2",
            TrackLabelPolicy.trackLabel(null, null, "Track 2", Locale.ENGLISH),
        )
    }

    @Test
    fun undeterminedIsNotALanguage() {
        // «und» είναι δήλωση άγνοιας, όχι γλώσσα — δεν πρέπει να εμφανίζεται ως
        // επιλογή με όνομα.
        assertEquals("", TrackLabelPolicy.languageName("und", greek))
        assertEquals("", TrackLabelPolicy.languageName(null, greek))
        assertEquals("", TrackLabelPolicy.languageName("", greek))
    }

    @Test
    fun unknownCodesAreShownAsGivenRatherThanHidden() {
        // Καλύτερα «SWA» παρά τίποτα: ο χρήστης μπορεί να το αναγνωρίσει, εμείς
        // απλώς δεν έχουμε μετάφραση.
        assertEquals("SWA", TrackLabelPolicy.languageName("swa", greek))
    }

    /* ---------------- πλήρης ετικέτα ---------------- */

    @Test
    fun languageComesFirstBecauseThatIsWhatPeopleLookFor() {
        assertEquals(
            "Ελληνικά · AC3 5.1",
            label("ell", "AC3 5.1", 1)
        )
    }

    @Test
    fun providerLabelIsDroppedWhenItOnlyRepeatsTheLanguage() {
        // Πολλοί πάροχοι γράφουν «Greek» στο label. «Ελληνικά · Greek» δεν
        // προσθέτει τίποτα.
        assertEquals("Ελληνικά", label("el", "Ελληνικά", 1))
        assertEquals("Αγγλικά", label("eng", "eng", 1))
    }

    @Test
    fun labelAloneIsUsedWhenLanguageIsMissing() {
        assertEquals("Director commentary", label(null, "Director commentary", 2))
    }

    @Test
    fun trackWithNoInformationFallsBackToItsNumber() {
        // Χωρίς αυτό θα έμενε κενή γραμμή στο μενού, που δεν επιλέγεται με σιγουριά.
        assertEquals("Κομμάτι 3", label(null, null, 3))
        assertEquals("Κομμάτι 2", label("und", "  ", 2))
    }
}
