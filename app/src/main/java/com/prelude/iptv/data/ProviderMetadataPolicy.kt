package com.prelude.iptv.data

/**
 * «Άδεια» μεταδεδομένα παρόχου που φτάνουν στην οθόνη ως κείμενο.
 *
 * ΤΟ ΠΡΟΒΛΗΜΑ: πολλοί πάροχοι δεν αφήνουν κενό ένα πεδίο που δεν γνωρίζουν —
 * γράφουν `N/A`, `null`, `-` ή `unknown`. Η εφαρμογή τα αντιμετώπιζε ως
 * κανονικό περιεχόμενο, με δύο ορατά αποτελέσματα:
 *
 * 1. Η γραμμή ειδών σπάει το `genre` στα `,` `/` `·` `|` `&` για να φτιάξει
 *    ετικέτες. Το `N/A` περιέχει κάθετο, οπότε γινόταν **δύο** ετικέτες και
 *    εμφανιζόταν κυριολεκτικά ως «N · A».
 * 2. Η περίληψη έδειχνε `N/A` αντί να θεωρηθεί απούσα, οπότε ούτε το
 *    εφεδρικό κείμενο («καμία περιγραφή») εμφανιζόταν ποτέ, ούτε φαινόταν ότι
 *    λείπει κάτι.
 *
 * ΠΡΟΣΟΧΗ ΣΤΟ ΠΟΙΑ ΠΕΔΙΑ ΑΓΓΙΖΟΥΜΕ. Τα `year` και `duration` συμμετέχουν στα
 * εφεδρικά κλειδιά ταυτότητας του [CatalogNormalizer] (`movieIdentity`,
 * `seriesIdentity`) και στο `localSeriesId`, που αποθηκεύεται. Αλλαγή τους θα
 * μετακινούσε αγαπημένα και ιστορικό — ακριβώς το λάθος που έγινε μία φορά με
 * το `PlaybackQueue.favKey`. Εδώ καθαρίζονται ΜΟΝΟ πεδία καθαρής προβολής.
 */
object ProviderMetadataPolicy {

    /**
     * Τιμές που σημαίνουν «δεν ξέρω», γραμμένες ως περιεχόμενο.
     *
     * Η σύγκριση γίνεται σε πεζά και χωρίς κενά, ώστε να πιάνει `N/A`, `n/a`
     * και ` N / A ` το ίδιο. Δεν μπαίνει το `0`: είναι νόμιμη τιμή για
     * διάρκεια ή αριθμό επεισοδίου και δεν αφορά τα πεδία που καθαρίζουμε.
     */
    private val PLACEHOLDERS = setOf(
        "n/a", "na", "n.a.", "null", "nil", "none", "nan", "undefined",
        "unknown", "no data", "nodata", "no info", "noinfo",
        "-", "--", "---", "?", "??", "...", "n/a.", "not available",
        "χ/υ", "δ/υ", "αγνωστο", "άγνωστο", "μη διαθεσιμο", "μη διαθέσιμο",
    )

    /**
     * Το κείμενο, ή κενό αν ο πάροχος απλώς δήλωσε άγνοια.
     *
     * Επιστρέφει το ΑΡΧΙΚΟ κείμενο όταν είναι υπαρκτό — χωρίς περικοπές ή
     * αλλαγές — ώστε να μη γίνει σιωπηλά επεξεργασία περιεχομένου παρόχου.
     */
    fun text(value: String): String {
        if (value.isBlank()) return ""
        val normalized = value.trim().lowercase().replace(" ", "")
        return if (normalized in PLACEHOLDERS || value.trim().lowercase() in PLACEHOLDERS) {
            ""
        } else {
            value
        }
    }

    /**
     * Καθαρίζει τα πεδία προβολής ενός στοιχείου καταλόγου.
     *
     * Επιστρέφει το ΙΔΙΟ αντικείμενο όταν δεν υπάρχει τίποτα να αλλάξει: ο
     * κατάλογος μπορεί να έχει δεκάδες χιλιάδες στοιχεία και ένα `copy()` ανά
     * στοιχείο σε κάθε φόρτωση είναι περιττή πίεση στη μνήμη.
     */
    fun sanitize(channel: Channel): Channel {
        val plot = text(channel.plot)
        val genre = text(channel.genre)
        val cast = text(channel.cast)
        val director = text(channel.director)
        val unchanged = plot == channel.plot && genre == channel.genre &&
            cast == channel.cast && director == channel.director
        return if (unchanged) {
            channel
        } else {
            channel.copy(plot = plot, genre = genre, cast = cast, director = director)
        }
    }
}
