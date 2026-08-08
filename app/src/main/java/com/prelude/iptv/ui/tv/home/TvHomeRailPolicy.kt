package com.prelude.iptv.ui.tv.home

/**
 * Καθαροί κανόνες για την κάθετη πλοήγηση της αρχικής (μία σειρά τη φορά).
 *
 * Η αρχική δεν χρησιμοποιεί κάθετο scroll container: εμφανίζεται μόνο η ενεργή
 * σειρά και τα πάνω/κάτω βελάκια αλλάζουν ποια είναι αυτή. Επειδή η πλοήγηση
 * είναι πλέον δική μας ευθύνη (και όχι του LazyColumn), οι κανόνες ζουν εδώ και
 * δοκιμάζονται — αλλιώς ένα off-by-one φαίνεται μόνο πάνω στη συσκευή.
 */
object TvHomeRailPolicy {

    /** Το αποτέλεσμα ενός πατήματος πάνω/κάτω. */
    data class Move(val index: Int, val consumed: Boolean)

    /**
     * Κάτω βελάκι. Στην τελευταία σειρά ΔΕΝ καταναλώνεται το πάτημα, ώστε να
     * μπορεί να το χειριστεί κάποιος άλλος (π.χ. να μη «κολλάει» το D-pad).
     */
    fun moveDown(current: Int, railCount: Int): Move =
        if (current < railCount - 1) Move(current + 1, consumed = true)
        else Move(current, consumed = false)

    /**
     * Πάνω βελάκι. Στην πρώτη σειρά δεν καταναλώνεται, ώστε το focus να μπορεί
     * να ανέβει στο hero ή να φύγει αριστερά στο μενού.
     */
    fun moveUp(current: Int): Move =
        if (current > 0) Move(current - 1, consumed = true)
        else Move(current, consumed = false)

    /**
     * Ασφαλής δείκτης όταν αλλάξει ο κατάλογος: οι σειρές μπορεί να λιγοστέψουν
     * ενώ βρίσκεσαι σε μεγάλο index (π.χ. αλλαγή ομάδας ή partial publish).
     */
    fun coerce(index: Int, railCount: Int): Int =
        if (railCount <= 0) 0 else index.coerceIn(0, railCount - 1)

    /**
     * Αλλάζοντας ομάδα/ενότητα ξεκινάμε πάντα από την πρώτη σειρά. Η υπογραφή
     * είναι τα ids των rails: αλλάζει όταν αλλάζει πραγματικά η ομάδα, ΟΧΙ στα
     * partial publishes που απλώς προσθέτουν στοιχεία στις ίδιες σειρές.
     */
    fun signature(railIds: List<String>): String = railIds.joinToString("|")

    fun shouldResetToFirst(previousSignature: String, currentSignature: String): Boolean =
        previousSignature != currentSignature
}
