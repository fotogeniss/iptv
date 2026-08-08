package com.prelude.iptv.ui

/**
 * Καθαρή λογική αποφάσεων του ViewModel — απομονωμένη από Android ώστε να
 * δοκιμάζεται. Εδώ ζουν τα σημεία που ΕΣΠΑΣΑΝ ή θα μπορούσαν να σπάσουν
 * σιωπηλά: υπολογισμός index μετά από διαγραφή, φρεσκάδα cache, λήξη PIN.
 *
 * Ο κανόνας: αν μια απόφαση είναι «καθαρή» (in -> out, χωρίς I/O), ΔΕΝ έχει
 * λόγο να ζει μέσα σε coroutine με getApplication() γύρω της. Βγαίνει εδώ,
 * δοκιμάζεται, και το ViewModel απλά την καλεί.
 */
object LoadPolicy {

    /**
     * Νέος τρέχων δείκτης λίστας μετά τη διαγραφή της [removedIndex].
     *
     * Το λάθος εδώ είναι ύπουλο: διαγραφή λίστας ΠΡΙΝ την τρέχουσα μετατοπίζει
     * όλα τα index κατά ένα — αν δεν το λάβεις υπόψη, μετά τη διαγραφή δείχνεις
     * ΑΛΛΗ λίστα από αυτή που έβλεπες.
     *
     * @param sizeAfter πλήθος λιστών ΜΕΤΑ τη διαγραφή
     * @param current   ο δείκτης που έβλεπε ο χρήστης ΠΡΙΝ
     */
    fun indexAfterDelete(sizeAfter: Int, removedIndex: Int, current: Int): Int = when {
        sizeAfter <= 0 -> 0
        removedIndex < current -> (current - 1).coerceIn(0, sizeAfter - 1)
        current >= sizeAfter -> sizeAfter - 1
        else -> current.coerceIn(0, sizeAfter - 1)
    }

    /**
     * Χρειάζεται ανανέωση από δίκτυο; (stale-while-revalidate)
     * @param force ρητό «Ανανέωση» από τον χρήστη
     */
    fun isStale(savedAtMs: Long, nowMs: Long, ttlMs: Long, force: Boolean): Boolean =
        force || savedAtMs <= 0L || (nowMs - savedAtMs) > ttlMs

    /** Έληξε το ξεκλείδωμα γονικού ελέγχου; */
    fun isUnlockExpired(unlockedAtMs: Long, nowMs: Long, ttlMs: Long): Boolean =
        unlockedAtMs <= 0L || (nowMs - unlockedAtMs) > ttlMs

    /**
     * Μετακινεί την πρώτη εμφάνιση του [first] στην αρχή, διατηρώντας τη
     * σχετική σειρά όλων των υπόλοιπων στοιχείων. Αν το στοιχείο λείπει ή
     * βρίσκεται ήδη πρώτο, επιστρέφεται η αρχική λίστα χωρίς allocation.
     */
    fun <T> orderWithFirst(items: List<T>, first: T): List<T> {
        val index = items.indexOf(first)
        if (index <= 0) return items

        return buildList(items.size) {
            add(items[index])
            items.forEachIndexed { itemIndex, item ->
                if (itemIndex != index) add(item)
            }
        }
    }

    /**
     * Επιτρέπεται η προβολή group; ΚΑΘΑΡΗ λογική γονικού ελέγχου.
     * Το bug που έκλεισε: τα recents/continue διάβαζαν κατευθείαν από τον δίσκο
     * παρακάμπτοντας αυτόν τον έλεγχο -> αφίσες κλειδωμένων ταινιών στην αρχική.
     */
    fun groupAllowed(group: String, lockedGroups: Set<String>, unlocked: Boolean): Boolean {
        if (unlocked || lockedGroups.isEmpty()) return true
        return group.ifEmpty { "Χωρίς ομάδα" } !in lockedGroups
    }
}
