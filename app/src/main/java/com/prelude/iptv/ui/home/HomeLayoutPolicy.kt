package com.prelude.iptv.ui.home

/**
 * Μία ενότητα της αρχικής, όπως τη βλέπει ο χρήστης στην «Επεξεργασία αρχικής».
 *
 * Το [id] είναι αυτό που γράφεται στον δίσκο και ΔΕΝ αλλάζει ποτέ. Το display
 * κείμενο ανήκει στο UI resource mapping, όχι σε αυτό το Android-free model.
 */
data class HomeSection(
    val id: String,
    /**
     * Σταθερή: ούτε κρύβεται ούτε μετακινείται.
     *
     * Δεν είναι διακοσμητική — αντιστοιχεί σε κάτι που υπάρχει πάντα (η κεφαλίδα).
     * Φαίνεται στη λίστα ώστε ο χρήστης να καταλαβαίνει τη σειρά που βλέπει· αν
     * έλειπε, η πρώτη κινούμενη γραμμή θα έμοιαζε να είναι η κορυφή της οθόνης ενώ
     * δεν είναι.
     */
    val fixed: Boolean = false,
    /** Έχει κουμπί «Καθάρισμα» — δηλαδή δείχνει ιστορικό, όχι κατάλογο. */
    val clearable: Boolean = false,
    /** Δένεται με μια κατηγορία που διαλέγει ο χρήστης (π.χ. Ζωντανά · DIGEA). */
    val categorised: Boolean = false,
)

/** Μια ενότητα μαζί με το αν είναι ορατή αυτή τη στιγμή. */
data class HomeEntry(val section: HomeSection, val visible: Boolean)

/**
 * Σειρά και ορατότητα των ενοτήτων της αρχικής.
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΟ ΚΑΙ ΧΩΡΙΣ ANDROID: η διάταξη είναι το μόνο κομμάτι της
 * αρχικής που ο χρήστης μπορεί να χαλάσει, και το μόνο που πρέπει να αντέξει
 * αποθηκευμένα δεδομένα από παλιότερη έκδοση. Και τα δύο δοκιμάζονται εδώ, σε
 * δευτερόλεπτα, χωρίς συσκευή.
 */
object HomeLayoutPolicy {

    const val HEADER = "header"
    const val HERO = "hero"
    const val SUGGESTIONS = "suggestions"
    const val CONTINUE = "continue"
    const val RECENT_LIVE = "recent-live"
    const val NEW_LIVE = "new-live"
    const val NEW_MOVIES = "new-movies"
    const val NEW_EPISODES = "new-episodes"
    const val LIVE = "live"
    const val MOVIES = "movies"
    const val SERIES = "series"

    /**
     * Η προεπιλεγμένη σειρά — και ταυτόχρονα ο ΚΑΤΑΛΟΓΟΣ όσων υπάρχουν.
     *
     * Οι σταθερές μπαίνουν πρώτες. Δεν είναι σύμβαση ευγενείας: το [resolve] τις
     * ανεβάζει πάντα στην κορυφή, οπότε αν εδώ ήταν αλλού, η λίστα που βλέπει ο
     * χρήστης θα διέφερε από τη λίστα που φτιάχνει ο κώδικας.
     */
    // ΤΑ ΤΡΙΑ ΠΛΑΚΙΔΙΑ ΑΦΑΙΡΕΘΗΚΑΝ.
    //
    // Επαναλάμβαναν τη μπάρα πλοήγησης από κάτω, που έχει ήδη Ζωντανά, Ταινίες και
    // Σειρές — δύο σημεία για την ίδια ενέργεια, με το ένα να τρώει το πάνω τρίτο
    // της οθόνης. Ο μετρητής («10.793») είναι χρήσιμος μία φορά, όταν στήνεις την
    // πηγή, και μετά είναι θόρυβος πάνω από την εικόνα της ταινίας.
    val DEFAULT: List<HomeSection> = listOf(
        HomeSection(HEADER, fixed = true),
        HomeSection(HERO),
        HomeSection(SUGGESTIONS),
        HomeSection(CONTINUE, clearable = true),
        HomeSection(RECENT_LIVE, clearable = true),
        HomeSection(NEW_LIVE),
        HomeSection(NEW_MOVIES),
        HomeSection(NEW_EPISODES),
        HomeSection(LIVE, categorised = true),
        HomeSection(MOVIES, categorised = true),
        HomeSection(SERIES, categorised = true),
    )

    private val BY_ID = DEFAULT.associateBy { it.id }

    /** Πόσες σταθερές γραμμές υπάρχουν — δηλαδή από ποιον δείκτη κι έπειτα σύρεται. */
    val FIXED_COUNT: Int = DEFAULT.count { it.fixed }

    /**
     * Η τελική λίστα που ζωγραφίζεται, από ό,τι βρέθηκε στον δίσκο.
     *
     * Αντέχει τρία πράγματα που ΘΑ συμβούν:
     *
     * - **Άγνωστα id**: ενότητα που καταργήθηκε σε νεότερη έκδοση. Αγνοείται.
     * - **Ενότητα που λείπει**: προστέθηκε μετά την τελευταία αποθήκευση. Μπαίνει
     *   στο τέλος αντί να εξαφανιστεί — μια καινούργια δυνατότητα που δεν
     *   εμφανίζεται ποτέ σε παλιούς χρήστες είναι χειρότερη από μια που εμφανίζεται
     *   σε λάθος θέση.
     * - **Χαλασμένη σειρά**: οι σταθερές ανεβαίνουν πάντα στην κορυφή, ό,τι κι αν
     *   λέει το αρχείο.
     *
     * Κενή [savedOrder] σημαίνει «δεν έχει πειράξει τίποτα» και δίνει το [DEFAULT].
     */
    fun resolve(
        savedOrder: List<String> = emptyList(),
        hidden: Set<String> = emptySet(),
    ): List<HomeEntry> {
        val known = savedOrder.mapNotNull { BY_ID[it] }.distinctBy { it.id }
        val missing = DEFAULT.filter { section -> known.none { it.id == section.id } }
        val all = known + missing
        val ordered = all.filter { it.fixed }.sortedBy { fixedRank(it.id) } + all.filter { !it.fixed }
        return ordered.map { HomeEntry(it, visible = it.fixed || it.id !in hidden) }
    }

    private fun fixedRank(id: String): Int = DEFAULT.indexOfFirst { it.id == id }

    /**
     * Μετακίνηση μιας γραμμής. Επιστρέφει τη ΝΕΑ σειρά ως λίστα από id.
     *
     * Οι σταθερές δεν μετακινούνται και δεν δέχονται άλλες πάνω τους: ένα
     * σύρσιμο που θα τις περνούσε σταματά στο πρώτο επιτρεπτό σημείο, αντί να
     * αγνοηθεί. Το σύρσιμο που «δεν κάνει τίποτα» μοιάζει με κόλλημα.
     *
     * Δείκτες εκτός ορίων επιστρέφουν τη λίστα ως έχει — ο καλών είναι μια
     * χειρονομία με το δάχτυλο, όχι κώδικας που μπορούμε να εμπιστευτούμε.
     */
    fun move(order: List<String>, from: Int, to: Int): List<String> {
        if (from !in order.indices || to !in order.indices || from == to) return order
        val fixedTop = order.takeWhile { BY_ID[it]?.fixed == true }.size
        if (from < fixedTop) return order
        val target = to.coerceAtLeast(fixedTop)
        val out = order.toMutableList()
        out.add(target, out.removeAt(from))
        return out
    }

    /** Άναμμα/σβήσιμο του ματιού. Οι σταθερές δεν κρύβονται. */
    fun toggle(hidden: Set<String>, id: String): Set<String> {
        if (BY_ID[id]?.fixed != false) return hidden
        return if (id in hidden) hidden - id else hidden + id
    }

    /** Τα id με τη σειρά τους — αυτό που γράφεται στον δίσκο. */
    fun idsOf(entries: List<HomeEntry>): List<String> = entries.map { it.section.id }
}
