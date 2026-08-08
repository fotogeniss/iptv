package com.prelude.iptv.ui.coordinator

import android.app.Application
import com.prelude.iptv.data.CatalogNormalizer
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.tvhome.TvHomeSyncScheduler
import com.prelude.iptv.ui.LibraryPolicy
import com.prelude.iptv.ui.LoadPolicy
import com.prelude.iptv.ui.UiState
import com.prelude.iptv.ui.WatchProgress
import com.prelude.iptv.ui.WatchProgressPolicy
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Αγαπημένα, ιστορικό, αναζήτηση και πρόοδος παρακολούθησης.
 *
 * Το `MainViewModel` παραμένει το δημόσιο όριο· εδώ απομονώνεται ό,τι αφορά τη
 * ΒΙΒΛΙΟΘΗΚΗ του χρήστη — δηλαδή τα δικά του δεδομένα, όχι τον κατάλογο του
 * παρόχου. Δύο διαφορετικά πράγματα που ζούσαν ανακατεμένα σε 1.800 γραμμές.
 *
 * Χωρίς coroutines και χωρίς πρόσβαση σε πάροχο: κάθε λειτουργία είναι
 * ντετερμινιστική ανάγνωση ή μετατροπή των [store] και [state], που κρατά τη
 * συμπεριφορά ίδια με πριν.
 *
 * @param sourceId διαβάζεται κάθε φορά και δεν αποθηκεύεται: η ενεργή πηγή
 *   αλλάζει όσο ζει ο coordinator, και μια παγωμένη τιμή θα έγραφε τα αγαπημένα
 *   της μιας πηγής στα δεδομένα της άλλης.
 * @param favKey σταθερή ταυτότητα αντικειμένου — ίδια συνάρτηση με τον player,
 *   ώστε η θέση που αποθηκεύει η αναπαραγωγή να είναι αυτή που διαβάζει η λίστα.
 */
internal class LibraryCoordinator(
    private val app: Application,
    private val store: PlaylistStore,
    private val state: MutableStateFlow<UiState>,
    private val sourceId: () -> String,
    private val favKey: (Channel) -> String,
) {

    /**
     * ΕΝΑ φίλτρο γονικού ελέγχου για ΚΑΘΕ διαδρομή που βγάζει περιεχόμενο.
     *
     * Πριν, το κλείδωμα εφαρμοζόταν στη λίστα αλλά όχι στα «Συνέχισε να βλέπεις»
     * και στο hero της αρχικής: φαινόταν να δουλεύει, ενώ το περιεχόμενο διέρρεε
     * από αλλού. Όποια νέα λίστα φτιαχτεί, περνά από εδώ.
     */
    fun parentalAllowed(ch: Channel): Boolean {
        val s = state.value
        return LoadPolicy.groupAllowed(ch.group, s.lockedGroups, s.parentalUnlocked)
    }

    /* ==================== Ιστορικό ==================== */

    /**
     * ΤΑ ΖΩΝΤΑΝΑ ΦΙΛΤΡΑΡΟΝΤΑΙ ΚΑΙ ΣΤΗΝ ΑΝΑΓΝΩΣΗ, ΟΧΙ ΜΟΝΟ ΣΤΗΝ ΕΓΓΡΑΦΗ.
     *
     * Ο φύλακας στην εγγραφή εμποδίζει να μπουν καινούργια. Δεν κάνει όμως τίποτα
     * για όσα γράφτηκαν από παλιότερες εκδόσεις, πριν υπάρξει ο φύλακας: εκείνα
     * κάθονται στον δίσκο του χρήστη και εμφανίζονται για πάντα.
     *
     * Ένα φίλτρο στην ανάγνωση τα εξαφανίζει χωρίς να χρειαστεί μετανάστευση
     * δεδομένων, και προστατεύει και από οποιαδήποτε μελλοντική διαδρομή ξεχάσει
     * τον φύλακα.
     */
    private fun watchable(items: List<Channel>): List<Channel> =
        items.filter { it.kind != "live" && parentalAllowed(it) }

    fun recents(): List<Channel> = watchable(store.loadRecents(sourceId()))

    /**
     * Τα ζωντανά ΔΕΝ μπαίνουν στο ιστορικό.
     *
     * Ένα κανάλι δεν «το είδες μέχρι εδώ» — παίζει συνέχεια. Αν έμπαινε, το
     * ιστορικό θα γέμιζε με ό,τι πέρασε από μπροστά σου κάνοντας ζάπινγκ και θα
     * έκρυβε τις ταινίες που όντως άφησες στη μέση.
     */
    /** Ζωντανά που είδε πρόσφατα — ξεχωριστά από το ιστορικό, βλ. [addRecent]. */
    fun recentLive(): List<Channel> =
        store.loadRecentLive(sourceId()).filter { parentalAllowed(it) }

    fun clearRecentLive() {
        store.clearRecentLive(sourceId())
        bumpRecents()
    }

    /**
     * Καθαρίζει το ιστορικό μιας ενότητας της αρχικής.
     *
     * Δύο κουμπιά «Καθάρισμα» στην ίδια οθόνη που κάνουν διαφορετικά πράγματα —
     * γι' αυτό η επιλογή γίνεται εδώ, με βάση το id της ενότητας, και όχι σε δύο
     * χωριστά callbacks που κάποιο σημείο κλήσης θα μπέρδευε.
     */
    fun clearHomeHistory(sectionId: String) {
        when (sectionId) {
            "recent-live" -> clearRecentLive()
            "continue" -> {
                // Σβήνει η ΠΡΟΟΔΟΣ, όχι το ιστορικό: «δεν θέλω να μου το θυμίζεις»
                // δεν σημαίνει «δεν το είδα ποτέ».
                store.loadRecents(sourceId()).forEach { store.clearPosition(sourceId(), favKey(it)) }
                bumpRecents()
            }
        }
    }

    fun addRecent(ch: Channel) {
        if (ch.kind == "live") {
            store.addRecentLive(sourceId(), ch)
            bumpRecents()
            return
        }
        store.addRecent(sourceId(), ch)
        // ΕΙΔΟΠΟΙΗΣΗ ΑΛΛΑΓΗΣ — ΕΛΕΙΠΕ.
        //
        // Οι λίστες της διεπαφής είναι memoized πάνω στο recentsVersion, ώστε να
        // μην ξαναχτίζονται σε κάθε recomposition. Το addRecent όμως έγραφε στον
        // δίσκο ΧΩΡΙΣ να το αυξήσει: η ταινία που μόλις είδες αποθηκευόταν
        // κανονικά, αλλά το ιστορικό συνέχιζε να δείχνει την παλιά λίστα μέχρι
        // να τύχει κάποια άλλη ενέργεια να τη σηκώσει.
        //
        // Γι' αυτό «δεν τα εμφάνιζε όλα»: έλειπαν ακριβώς τα πιο πρόσφατα.
        TvHomeSyncScheduler.schedule(app)
        bumpRecents()
    }

    fun historyItems(): List<Channel> =
        LibraryPolicy.unique(watchable(store.loadRecents(sourceId())))

    fun removeHistoryItem(ch: Channel) {
        val key = favKey(ch)
        if (key.isBlank()) return
        store.removeRecent(sourceId(), key)
        TvHomeSyncScheduler.schedule(app)
        bumpRecents()
    }

    /* ==================== Συνέχισε να βλέπεις ==================== */

    /**
     * Ζωντανά κανάλια αποκλείονται: δεν έχουν «πού είχα μείνει». Επίσης ό,τι δεν
     * έχει αποθηκευμένη πρόοδο — μια εγγραφή στο ιστορικό δεν σημαίνει από μόνη
     * της ότι υπάρχει κάτι να συνεχίσεις.
     */
    fun continueWatching(): List<Pair<Channel, Float>> =
        watchable(store.loadRecents(sourceId())).mapNotNull { ch ->
            val progress = WatchProgressPolicy.from(
                store.loadPosition(sourceId(), favKey(ch))
            ) ?: return@mapNotNull null
            ch to progress.fraction
        }

    /* ==================== Αναζήτηση ==================== */

    /** Ό,τι γνωρίζει η τρέχουσα συνεδρία, συν αγαπημένα και ιστορικό από τον δίσκο. */
    private fun candidates(): List<Channel> = LibraryPolicy.unique(
        buildList {
            addAll(store.loadFavoriteItems(sourceId()))
            addAll(store.loadRecents(sourceId()))
            addAll(state.value.channels)
        }
    ).filter(::parentalAllowed)

    /** Σταθερός κατάλογος αναζήτησης: ένα αποτέλεσμα ανά ταινία/σειρά/κανάλι. */
    fun searchUniverse(): List<Channel> = CatalogNormalizer.searchEntries(candidates())

    fun searchLibrary(query: String): List<Channel> =
        LibraryPolicy.search(searchUniverse(), query)

    /**
     * Γρήγορη αναζήτηση πάνω σε ΗΔΗ υπολογισμένο [universe] (από [searchUniverse]).
     *
     * Το ακριβό κομμάτι —ξαναχτίσιμο υποψηφίων, κανονικοποίηση, ανάγνωση δίσκου—
     * γίνεται μία φορά· ανά πλήκτρο τρέχει μόνο το φιλτράρισμα κειμένου, ώστε η
     * πληκτρολόγηση να μη σέρνεται.
     */
    fun searchInUniverse(universe: List<Channel>, query: String): List<Channel> =
        LibraryPolicy.search(universe, query)

    /* ==================== Αγαπημένα ==================== */

    fun favoriteLibraryItems(): List<Channel> =
        LibraryPolicy.favorites(candidates(), state.value.favorites)

    fun toggleFavorite(ch: Channel) {
        val source = sourceId()
        val key = favKey(ch)
        if (source.isBlank() || key.isEmpty()) return
        val favs = state.value.favorites.toMutableSet()
        val added = favs.add(key)
        if (added) store.addFavoriteItem(source, ch)
        else {
            favs.remove(key)
            store.removeFavoriteItem(source, key)
        }
        state.value = state.value.copy(favorites = favs)
        TvHomeSyncScheduler.schedule(app)
    }

    /* ==================== Πρόοδος ==================== */

    /** null σημαίνει «από την αρχή». */
    fun watchProgress(ch: Channel): WatchProgress? =
        WatchProgressPolicy.from(store.loadPosition(sourceId(), favKey(ch)))

    fun watchProgress(items: List<Channel>): Map<String, WatchProgress> =
        items.mapNotNull { ch ->
            val key = favKey(ch)
            WatchProgressPolicy.from(store.loadPosition(sourceId(), key))?.let { key to it }
        }.toMap()

    /** Σβήνει ΚΑΙ τον δείκτη συνέχειας ΚΑΙ την εγγραφή ιστορικού. */
    fun clearWatchProgress(ch: Channel) {
        val key = favKey(ch)
        if (key.isBlank()) return
        store.clearPosition(sourceId(), key)
        store.removeRecent(sourceId(), key)
        TvHomeSyncScheduler.schedule(app)
        bumpRecents()
    }

    /**
     * Σηματοδοτεί στη διεπαφή ότι το ιστορικό άλλαξε.
     *
     * Οι λίστες είναι memoized πάνω σε αυτόν τον μετρητή· χωρίς αύξηση, η αλλαγή
     * γράφεται στον δίσκο και η οθόνη συνεχίζει να δείχνει τα παλιά.
     */
    private fun bumpRecents() {
        state.value = state.value.copy(recentsVersion = state.value.recentsVersion + 1)
    }
}
