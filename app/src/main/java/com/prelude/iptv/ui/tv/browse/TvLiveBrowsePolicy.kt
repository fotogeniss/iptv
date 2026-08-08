package com.prelude.iptv.ui.tv.browse

/**
 * Καθαροί κανόνες πλοήγησης για την οθόνη ζωντανών καναλιών.
 *
 * Είναι σκόπιμα Android-free: όλη η «απόφαση» ζει εδώ και δοκιμάζεται με unit
 * tests, ενώ το Compose κρατά μόνο τη ζωγραφική και το focus. Η οθόνη πέρασε από
 * πολλές επαναλήψεις με λάθη που δεν φαίνονταν παρά μόνο πάνω στη συσκευή —
 * αυτός ο διαχωρισμός επιτρέπει να πιάνονται πριν φτάσουν εκεί.
 */
object TvLiveBrowsePolicy {

    /** Σε ποιο επίπεδο βρίσκεται η αριστερή στήλη. */
    enum class Level { CATEGORIES, CHANNELS }

    /** Τι πρέπει να συμβεί όταν πατηθεί BACK, με βάση την τρέχουσα κατάσταση. */
    enum class BackAction {
        /** Κλείσε την αναλυτική πληροφορία προγράμματος. */
        CLOSE_DETAILS,

        /** Γύρνα από τα κανάλια στις κατηγορίες. */
        BACK_TO_CATEGORIES,

        /** Τίποτα δικό μας — ας το χειριστεί η από πάνω οθόνη. */
        DELEGATE,
    }

    /**
     * Η σειρά είναι κρίσιμη: πρώτα φεύγει ό,τι είναι «από πάνω» (η αναλυτική
     * πληροφορία) και μόνο μετά αλλάζει επίπεδο η λίστα. Χωρίς αυτή τη σειρά,
     * ένα BACK έκλεινε δύο πράγματα ταυτόχρονα.
     */
    fun onBack(
        detailsOpen: Boolean,
        level: Level,
    ): BackAction = when {
        detailsOpen -> BackAction.CLOSE_DETAILS
        level == Level.CHANNELS -> BackAction.BACK_TO_CATEGORIES
        else -> BackAction.DELEGATE
    }

    /** Τι κάνει το OK πάνω σε κανάλι. */
    enum class ChannelAction {
        /** Ξεκίνα προεπισκόπηση αυτού του καναλιού. */
        PREVIEW,

        /** Ήδη σε προεπισκόπηση: μεγάλωσε σε πλήρη οθόνη. */
        OPEN_PLAYER,

        /** Έχει οπλιστεί multiview: αυτό είναι το ΔΕΥΤΕΡΟ κανάλι. */
        START_MULTIVIEW,
    }

    /**
     * Πρώτο OK δείχνει το κανάλι στην προεπισκόπηση· δεύτερο OK στο ΙΔΙΟ κανάλι
     * το ανοίγει σε πλήρη οθόνη. Αν όμως έχει οπλιστεί multiview (παρατεταμένο OK
     * σε άλλο κανάλι), το OK εδώ ξεκινά διπλή προβολή.
     *
     * Η σύγκριση γίνεται με κλειδί, όχι με αντικείμενο: η λίστα ξαναχτίζεται σε
     * κάθε ενημέρωση καταλόγου και τα instances αλλάζουν.
     */
    fun onChannelConfirm(
        previewKey: String?,
        targetKey: String,
        multiviewPrimaryKey: String? = null,
    ): ChannelAction = when {
        // Το ίδιο κανάλι δεν μπορεί να παίξει δύο φορές δίπλα-δίπλα.
        multiviewPrimaryKey != null && multiviewPrimaryKey != targetKey -> ChannelAction.START_MULTIVIEW
        previewKey != null && previewKey == targetKey -> ChannelAction.OPEN_PLAYER
        else -> ChannelAction.PREVIEW
    }

    /**
     * Το πρόγραμμα που παίζει τώρα. ΜΟΝΑΔΙΚΗ πηγή αλήθειας — τη χρησιμοποιούν
     * τόσο η λίστα καναλιών όσο και το αναλυτικό πρόγραμμα, ώστε να μη δείχνουν
     * ποτέ διαφορετικό EPG για το ίδιο κανάλι (πραγματικό bug που εμφανίστηκε
     * όταν οι δύο όψεις ρωτούσαν διαφορετικές πηγές).
     */
    fun currentProgramme(programmes: List<LiveProgramme>): LiveProgramme? =
        programmes.firstOrNull { it.isNow }

    /** Ο τίτλος που δείχνεται δίπλα στο κανάλι· κενός όταν δεν υπάρχει EPG. */
    fun nowTitle(programmes: List<LiveProgramme>): String =
        currentProgramme(programmes)?.title.orEmpty()

    /**
     * Τι εμφανίζεται όταν ζητηθεί αναλυτική πληροφορία για κανάλι χωρίς τρέχον
     * πρόγραμμα: εφεδρική εγγραφή με το όνομα του καναλιού, ώστε ο διάλογος να
     * μην ανοίγει ποτέ κενός.
     */
    fun detailsFor(programmes: List<LiveProgramme>, channelName: String): LiveProgramme =
        currentProgramme(programmes) ?: LiveProgramme(
            time = "",
            title = channelName,
            description = "Δεν υπάρχει διαθέσιμη πληροφορία προγράμματος.",
            isNow = true,
        )
}
