package com.prelude.iptv.player

import com.prelude.iptv.data.Channel

/**
 * Ποιο επεισόδιο παίζει μετά, και πότε να προταθεί.
 *
 * Καθαρή πολιτική γιατί τα λάθη εδώ είναι σιωπηλά και στα άκρα: το τελευταίο
 * επεισόδιο μιας σεζόν πρέπει να οδηγεί στο πρώτο της επόμενης, το τελευταίο
 * όλων σε τίποτα, και ένα επεισόδιο που δεν υπάρχει στη λίστα δεν πρέπει να
 * δείχνει «επόμενο» το πρώτο της σειράς — ακριβώς το είδος σφάλματος που είχαμε
 * ήδη πληρώσει μία φορά στην ουρά αναπαραγωγής.
 */
object NextEpisodePolicy {

    /**
     * Το επόμενο επεισόδιο μετά το [current], ή null αν δεν υπάρχει.
     *
     * Οι σεζόν δίνονται όπως τις κρατά η οθόνη: ζεύγη ετικέτας και επεισοδίων,
     * σε σειρά προβολής. Τις ισοπεδώνουμε, ώστε το πέρασμα από σεζόν σε σεζόν να
     * είναι απλώς «το επόμενο στοιχείο» και όχι ξεχωριστή περίπτωση.
     *
     * @param keyOf σταθερή ταυτότητα επεισοδίου. Η σύγκριση ΔΕΝ γίνεται με
     *   ισότητα αντικειμένων: η λίστα ξαναχτίζεται σε κάθε ενημέρωση καταλόγου
     *   και τα instances αλλάζουν.
     */
    fun nextAfter(
        current: Channel,
        seasons: List<Pair<String, List<Channel>>>,
        keyOf: (Channel) -> String,
    ): Channel? {
        val all = seasons.flatMap { it.second }
        val currentKey = keyOf(current)
        val index = all.indexOfFirst { keyOf(it) == currentKey }
        if (index < 0) return null
        return all.getOrNull(index + 1)
    }

    /**
     * ΠΟΥ ΑΡΧΙΖΟΥΝ ΟΙ ΤΙΤΛΟΙ ΤΕΛΟΥΣ — ΕΚΤΙΜΗΣΗ, ΟΧΙ ΓΝΩΣΗ.
     *
     * Οι υπηρεσίες που πηδούν τους τίτλους με ακρίβεια έχουν σημάνσεις από τον
     * πάροχο. Οι ροές IPTV δεν στέλνουν τίποτα τέτοιο: ξέρουμε μόνο τη συνολική
     * διάρκεια. Υποθέτουμε λοιπόν ότι οι τίτλοι πιάνουν το τελευταίο λεπτό, που
     * είναι ο συνήθης χρόνος για επεισόδιο σειράς.
     *
     * Είναι παραδοχή και μπορεί να πέσει έξω σε ταινία με μακροσκελείς τίτλους.
     * Γράφεται ρητά εδώ ώστε να αλλάζει σε ΕΝΑ σημείο, αντί να είναι σκόρπια
     * μαγικά νούμερα στη διεπαφή.
     */
    const val CREDITS_TAIL_MS = 60_000L

    /** Πόσο ΠΡΙΝ τους τίτλους εμφανίζεται η πρόταση. */
    const val CARD_LEAD_MS = 2 * 60_000L

    /** Πόσο ΜΕΤΑ την αρχή των τίτλων ξεκινά μόνο του το επόμενο. */
    const val AUTOPLAY_DELAY_MS = 0L

    /**
     * Πολύ σύντομα videos δεν θεωρούνται επεισόδια με τίτλους τέλους.
     *
     * Το όριο είναι inclusive: ένα clip ακριβώς δύο λεπτών παραμένει clip και
     * δεν πρέπει να εμφανίζει κάρτα ή να ξεκινά αυτόματα άλλο περιεχόμενο.
     */
    const val MIN_AUTO_NEXT_DURATION_MS = 2 * 60_000L

    /** Η εκτιμώμενη στιγμή έναρξης των τίτλων τέλους. */
    fun creditsStartMs(durationMs: Long): Long = durationMs - CREDITS_TAIL_MS

    /**
     * Πολύ κοντό περιεχόμενο δεν έχει «τίτλους τέλους» με αυτή την έννοια.
     *
     * Χωρίς αυτόν τον έλεγχο, σε κλιπ δύο λεπτών η πρόταση θα εμφανιζόταν σχεδόν
     * από την αρχή και η αυτόματη έναρξη θα ερχόταν πριν προλάβεις να δεις τίποτα.
     */
    private fun tooShort(durationMs: Long): Boolean =
        durationMs <= MIN_AUTO_NEXT_DURATION_MS

    /**
     * Πρέπει να φανεί η πρόταση «επόμενο επεισόδιο»;
     *
     * Απαιτεί γνωστή διάρκεια: σε ζωντανή ροή ή όσο ο player δεν την ξέρει ακόμη
     * είναι 0, και μια πρόταση τότε θα εμφανιζόταν αμέσως μόλις ξεκινήσει.
     */
    fun shouldOffer(positionMs: Long, durationMs: Long, hasNext: Boolean): Boolean {
        if (!hasNext || durationMs <= 0L || positionMs <= 0L) return false
        if (tooShort(durationMs)) return false
        return positionMs >= creditsStartMs(durationMs) - CARD_LEAD_MS
    }

    /** Ήρθε η ώρα να ξεκινήσει μόνο του το επόμενο επεισόδιο; */
    fun shouldAutoPlay(positionMs: Long, durationMs: Long, hasNext: Boolean): Boolean {
        if (!hasNext || durationMs <= 0L || positionMs <= 0L) return false
        if (tooShort(durationMs)) return false
        return positionMs >= creditsStartMs(durationMs) + AUTOPLAY_DELAY_MS
    }

    /**
     * Δευτερόλεπτα μέχρι την αυτόματη έναρξη, για την αντίστροφη μέτρηση.
     *
     * Ποτέ αρνητικά: η κάρτα δείχνει «0» και όχι παρελθόντα χρόνο, ακόμη κι αν το
     * τικ της θέσης καθυστερήσει λίγο.
     */
    fun autoPlayInSeconds(positionMs: Long, durationMs: Long): Int {
        if (durationMs <= 0L) return 0
        val target = creditsStartMs(durationMs) + AUTOPLAY_DELAY_MS
        return ((target - positionMs).coerceAtLeast(0L) / 1000).toInt()
    }
}
