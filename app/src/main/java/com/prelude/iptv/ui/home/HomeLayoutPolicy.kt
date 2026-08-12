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
    const val TOP_MOVIES = "top-movies"
    const val TOP_SERIES = "top-series"
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
        // ΚΡΥΜΜΕΝΕΣ ΑΠΟ ΠΡΟΕΠΙΛΟΓΗ ΣΤΗΝ ΑΡΧΙΚΗ.
        //
        // Η Αρχική είναι βιβλιοθήκη ταινιών και σειρών· τα ζωντανά έχουν δική
        // τους ενότητα με λίστα και πρόγραμμα, που τους ταιριάζει. Ένα κανάλι
        // ανάμεσα σε αφίσες εμφανίζεται ως κενό πλακίδιο, χωρίς περίληψη και
        // χωρίς διάρκεια. Παραμένουν στη λίστα του επεξεργαστή ώστε να τα
        // ανάψει όποιος τα θέλει — απλώς δεν είναι πια η προεπιλογή.
        HomeSection(NEW_LIVE),
        HomeSection(NEW_MOVIES),
        HomeSection(NEW_EPISODES),
        // Εμφανίζονται ΜΟΝΟ όταν ο πάροχος στέλνει βαθμολογίες. Χωρίς αυτές το
        // rail επιστρέφει null και η ενότητα απλώς δεν ζωγραφίζεται — παραμένει
        // όμως ορατή στον επεξεργαστή, ώστε να μη μοιάζει με σφάλμα η απουσία
        // της σε μία πηγή όταν υπάρχει σε άλλη.
        HomeSection(TOP_MOVIES),
        HomeSection(TOP_SERIES),
        HomeSection(LIVE, categorised = true),
        HomeSection(MOVIES, categorised = true),
        HomeSection(SERIES, categorised = true),
    )

    /* ------------------------------------------------- προορισμοί -------- */

    const val DEST_HOME = "home"
    const val DEST_LIVE = "live"
    const val DEST_MOVIES = "movies"
    const val DEST_SERIES = "series"

    /** Οι τέσσερις οθόνες που έχουν δική τους διάταξη, με σειρά εμφάνισης. */
    val DESTINATIONS: List<String> = listOf(DEST_HOME, DEST_LIVE, DEST_MOVIES, DEST_SERIES)

    /**
     * Ποιες ενότητες ΜΠΟΡΟΥΝ να υπάρξουν σε κάθε οθόνη.
     *
     * Δεν είναι θέμα γούστου, είναι θέμα δεδομένων: στην οθόνη Ζωντανά δεν
     * υπάρχουν σειρές στη μνήμη, οπότε μια ενότητα «Νέα επεισόδια» εκεί δεν θα
     * ζωγραφιζόταν ποτέ όσο κι αν την ενεργοποιούσε ο χρήστης. Ο επεξεργαστής
     * σταματά να υπόσχεται πράγματα που δεν μπορούν να συμβούν.
     *
     * Η Αρχική τα έχει όλα: εκεί συνενώνονται και οι τρεις ενότητες.
     */
    fun allowedIn(destination: String): List<HomeSection> = when (destination) {
        DEST_LIVE -> DEFAULT.filter { it.id in setOf(HEADER, RECENT_LIVE, NEW_LIVE, LIVE) }
        DEST_MOVIES -> DEFAULT.filter {
            it.id in setOf(HEADER, HERO, SUGGESTIONS, CONTINUE, NEW_MOVIES, TOP_MOVIES, MOVIES)
        }
        DEST_SERIES -> DEFAULT.filter {
            it.id in setOf(HEADER, HERO, SUGGESTIONS, CONTINUE, NEW_EPISODES, TOP_SERIES, SERIES)
        }
        else -> DEFAULT
    }

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
    /**
     * Ενότητες που ξεκινούν ΚΡΥΦΕΣ στην Αρχική, μέχρι να τις ζητήσει ο χρήστης.
     *
     * Ισχύει μόνο όταν δεν υπάρχει αποθηκευμένη ρύθμιση για αυτές: μόλις ο
     * χρήστης πατήσει το μάτι, η επιλογή του κερδίζει για πάντα. Μια προεπιλογή
     * δεν επιτρέπεται να ξαναγράφει απόφαση που έχει ήδη παρθεί.
     */
    private val HIDDEN_BY_DEFAULT_ON_HOME = setOf(RECENT_LIVE, NEW_LIVE, LIVE)

    fun resolve(
        savedOrder: List<String> = emptyList(),
        hidden: Set<String> = emptySet(),
        destination: String = DEST_HOME,
    ): List<HomeEntry> {
        val allowed = allowedIn(destination)
        val effectiveHidden = if (destination == DEST_HOME && savedOrder.isEmpty()) {
            hidden + HIDDEN_BY_DEFAULT_ON_HOME
        } else hidden
        val allowedIds = allowed.mapTo(HashSet()) { it.id }
        val all = ArrayList(
            savedOrder.mapNotNull { BY_ID[it] }.filter { it.id in allowedIds }.distinctBy { it.id }
        )
        // ΜΙΑ ΝΕΑ ΕΝΟΤΗΤΑ ΜΠΑΙΝΕΙ ΣΤΗ ΘΕΣΗ ΤΗΣ, ΟΧΙ ΣΤΟ ΤΕΛΟΣ.
        //
        // Πριν ήταν `known + missing`, δηλαδή κάθε καινούργια ενότητα
        // προσαρτιόταν μετά από ΟΛΕΣ τις αποθηκευμένες. Το σχόλιο παραδεχόταν τον
        // συμβιβασμό — «σε λάθος θέση είναι καλύτερα από ποτέ» — αλλά στην πράξη
        // «το τέλος» σημαίνει κάτω από δεκάδες ράγες κατηγοριών, όπου κανείς δεν
        // σκρολάρει. Οι «Κορυφαίες ταινίες» προστέθηκαν έτσι και ο κάτοχος τις
        // ανέφερε ως ανύπαρκτες· υπήρχαν, απλώς αθέατες.
        //
        // Τώρα κάθε ενότητα που λείπει μπαίνει αμέσως μετά τον πλησιέστερο
        // προηγούμενό της στη [DEFAULT] που υπάρχει ήδη στη λίστα — δηλαδή εκεί
        // που θα ήταν αν ο χρήστης δεν είχε πειράξει ποτέ τίποτα. Η σειρά που
        // ΕΧΕΙ επιλέξει ο χρήστης δεν αλλάζει.
        allowed.forEachIndexed { index, section ->
            if (all.any { it.id == section.id }) return@forEachIndexed
            val anchor = allowed.take(index).lastOrNull { candidate ->
                all.any { it.id == candidate.id }
            }
            val insertAt = if (anchor == null) 0 else all.indexOfFirst { it.id == anchor.id } + 1
            all.add(insertAt, section)
        }
        val ordered = all.filter { it.fixed }.sortedBy { fixedRank(it.id) } + all.filter { !it.fixed }
        return ordered.map { HomeEntry(it, visible = it.fixed || it.id !in effectiveHidden) }
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
