package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackLabelPolicyTest {

    @Test
    fun greekIsRecognisedInAllThreeCodesThatProvidersUse() {
        // «el» = ISO 639-1, «ell» = 639-2/T, «gre» = 639-2/B. Οι πάροχοι
        // χρησιμοποιούν και τους τρεις, συχνά στην ίδια λίστα.
        assertEquals("Ελληνικά", TrackLabelPolicy.languageName("el"))
        assertEquals("Ελληνικά", TrackLabelPolicy.languageName("ell"))
        assertEquals("Ελληνικά", TrackLabelPolicy.languageName("gre"))
    }

    @Test
    fun regionSuffixesAreStripped() {
        assertEquals("Ελληνικά", TrackLabelPolicy.languageName("el-GR"))
        assertEquals("Πορτογαλικά", TrackLabelPolicy.languageName("pt_BR"))
    }

    @Test
    fun caseAndWhitespaceDoNotMatter() {
        assertEquals("Αγγλικά", TrackLabelPolicy.languageName("  ENG "))
    }

    @Test
    fun undeterminedIsNotALanguage() {
        // «und» είναι δήλωση άγνοιας, όχι γλώσσα — δεν πρέπει να εμφανίζεται ως
        // επιλογή με όνομα.
        assertEquals("", TrackLabelPolicy.languageName("und"))
        assertEquals("", TrackLabelPolicy.languageName(null))
        assertEquals("", TrackLabelPolicy.languageName(""))
    }

    @Test
    fun unknownCodesAreShownAsGivenRatherThanHidden() {
        // Καλύτερα «SWA» παρά τίποτα: ο χρήστης μπορεί να το αναγνωρίσει, εμείς
        // απλώς δεν έχουμε μετάφραση.
        assertEquals("SWA", TrackLabelPolicy.languageName("swa"))
    }

    /* ---------------- πλήρης ετικέτα ---------------- */

    @Test
    fun languageComesFirstBecauseThatIsWhatPeopleLookFor() {
        assertEquals(
            "Ελληνικά · AC3 5.1",
            TrackLabelPolicy.trackLabel("ell", "AC3 5.1", fallbackIndex = 1)
        )
    }

    @Test
    fun providerLabelIsDroppedWhenItOnlyRepeatsTheLanguage() {
        // Πολλοί πάροχοι γράφουν «Greek» στο label. «Ελληνικά · Greek» δεν
        // προσθέτει τίποτα.
        assertEquals("Ελληνικά", TrackLabelPolicy.trackLabel("el", "Ελληνικά", 1))
        assertEquals("Αγγλικά", TrackLabelPolicy.trackLabel("eng", "eng", 1))
    }

    @Test
    fun labelAloneIsUsedWhenLanguageIsMissing() {
        assertEquals("Director commentary", TrackLabelPolicy.trackLabel(null, "Director commentary", 2))
    }

    @Test
    fun trackWithNoInformationFallsBackToItsNumber() {
        // Χωρίς αυτό θα έμενε κενή γραμμή στο μενού, που δεν επιλέγεται με σιγουριά.
        assertEquals("Κομμάτι 3", TrackLabelPolicy.trackLabel(null, null, 3))
        assertEquals("Κομμάτι 2", TrackLabelPolicy.trackLabel("und", "  ", 2))
    }
}
