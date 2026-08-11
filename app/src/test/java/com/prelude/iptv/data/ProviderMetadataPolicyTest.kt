package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Ο πάροχος που δεν ξέρει κάτι το γράφει, αντί να το αφήσει κενό. Το «N/A»
 * έφτανε στην οθόνη ως περιεχόμενο και, επειδή η γραμμή ειδών σπάει στο `/`,
 * εμφανιζόταν κυριολεκτικά ως «N · A».
 */
class ProviderMetadataPolicyTest {

    /* ---------------- το σύμπτωμα που αναφέρθηκε ---------------- */

    @Test
    fun theReportedPlaceholderBecomesEmpty() {
        assertEquals("", ProviderMetadataPolicy.text("N/A"))
        assertEquals("", ProviderMetadataPolicy.text("n/a"))
        assertEquals("", ProviderMetadataPolicy.text("  N / A  "))
    }

    @Test
    fun aPlaceholderGenreNoLongerSplitsIntoTwoTags() {
        // Η γραμμή ειδών σπάει στο «/». Με το «N/A» έβγαζε δύο ετικέτες.
        val cleaned = ProviderMetadataPolicy.sanitize(
            Channel(name = "Σειρά", genre = "N/A", plot = "N/A"),
        )
        val tags = cleaned.genre.split(",", "/", "·", "|", "&")
            .map(String::trim).filter(String::isNotBlank)

        assertEquals(emptyList<String>(), tags)
        assertEquals("", cleaned.plot)
    }

    @Test
    fun commonPlaceholderSpellingsAreCovered() {
        listOf("null", "NULL", "none", "Unknown", "-", "--", "?", "undefined", "no data")
            .forEach { assertEquals("«$it» έπρεπε να θεωρηθεί κενό", "", ProviderMetadataPolicy.text(it)) }
    }

    @Test
    fun greekPlaceholdersAreCovered() {
        listOf("Άγνωστο", "αγνωστο", "Μη διαθέσιμο", "Χ/Υ")
            .forEach { assertEquals("«$it» έπρεπε να θεωρηθεί κενό", "", ProviderMetadataPolicy.text(it)) }
    }

    /* ---------------- το πραγματικό περιεχόμενο επιβιώνει ---------------- */

    @Test
    fun realContentIsReturnedUntouched() {
        val plot = "Τρεις ξεχωριστοί μπαμπάδες συναντιούνται κάθε μέρα στην είσοδο του νηπιαγωγείου."
        assertEquals(plot, ProviderMetadataPolicy.text(plot))
        assertEquals("Κωμωδία, Οικογενειακή", ProviderMetadataPolicy.text("Κωμωδία, Οικογενειακή"))
    }

    @Test
    fun textIsNeverReformattedOnlyAcceptedOrDropped() {
        // Δεν κάνουμε trim ούτε άλλη επεξεργασία σε υπαρκτό περιεχόμενο:
        // είναι δεδομένα παρόχου.
        assertEquals("  Δράση  ", ProviderMetadataPolicy.text("  Δράση  "))
    }

    @Test
    fun wordsThatMerelyContainAPlaceholderAreKept() {
        // Ολόκληρη η τιμή πρέπει να είναι placeholder, όχι μέρος της.
        assertEquals("Nashville", ProviderMetadataPolicy.text("Nashville"))
        assertEquals("Unknown Origins", ProviderMetadataPolicy.text("Unknown Origins"))
        assertEquals("Drama/Nonе", ProviderMetadataPolicy.text("Drama/Nonе"))
    }

    /* ---------------- πεδία ταυτότητας: ΔΕΝ αγγίζονται ---------------- */

    @Test
    fun identityBearingFieldsAreNeverModified() {
        // Τα year/duration συμμετέχουν στα εφεδρικά κλειδιά του
        // CatalogNormalizer και στο αποθηκευμένο localSeriesId. Αλλαγή τους θα
        // μετακινούσε αγαπημένα και ιστορικό.
        val raw = Channel(
            name = "Σειρά",
            year = "N/A",
            duration = "N/A",
            url = "http://p/1.ts",
            streamId = "9",
            cmd = "/media/1.mpg",
            seriesId = "77",
            genre = "N/A",
        )
        val cleaned = ProviderMetadataPolicy.sanitize(raw)

        assertEquals("N/A", cleaned.year)
        assertEquals("N/A", cleaned.duration)
        assertEquals(raw.url, cleaned.url)
        assertEquals(raw.streamId, cleaned.streamId)
        assertEquals(raw.cmd, cleaned.cmd)
        assertEquals(raw.seriesId, cleaned.seriesId)
        assertEquals(raw.name, cleaned.name)
    }

    /* ---------------- κόστος ---------------- */

    @Test
    fun anAlreadyCleanChannelIsReturnedAsTheSameInstance() {
        // Ο κατάλογος έχει δεκάδες χιλιάδες στοιχεία· ένα copy() ανά στοιχείο
        // σε κάθε φόρτωση είναι περιττή πίεση στη μνήμη.
        val clean = Channel(name = "Σειρά", genre = "Δράση", plot = "Μια περίληψη.")
        assertSame(clean, ProviderMetadataPolicy.sanitize(clean))
    }

    @Test
    fun blankStaysBlank() {
        assertEquals("", ProviderMetadataPolicy.text(""))
        assertEquals("", ProviderMetadataPolicy.text("   "))
        val empty = Channel(name = "Σειρά")
        assertSame(empty, ProviderMetadataPolicy.sanitize(empty))
    }
}
