package com.prelude.iptv.data

import com.prelude.iptv.source.StalkerClient

/**
 * Καθολική ουρά αναπαραγωγής: τι λίστα «βλέπει» ο player για prev/next/zapping.
 *
 * ΠΡΟΣΟΧΗ: αν το project σου ορίζει ήδη PlaybackQueue σε άλλο αρχείο
 * (π.χ. Models.kt), κράτα ΜΟΝΟ ΕΝΑ από τα δύο — αλλιώς duplicate declaration.
 * Αυτή η έκδοση καλύπτει ακριβώς ό,τι χρησιμοποιεί ο κώδικας:
 * items / index / stalker / current() / hasPrev() / hasNext() / favKey().
 *
 * Γνωστός περιορισμός (by design): είναι in-memory singleton. Αν το Android
 * σκοτώσει τη διεργασία όσο ο player είναι μπροστά, με την επαναφορά η ουρά
 * είναι άδεια — το κανάλι συνεχίζει να παίζει από το intent, αλλά τα
 * prev/next δείχνουν «Τέλος λίστας» μέχρι να ξανανοίξεις από τη λίστα.
 */
object PlaybackQueue {
    var items: List<Channel> = emptyList()
    var index: Int = 0
    var stalker: StalkerClient? = null
    /** Stable hashed playlist identity used only for source-scoped history. */
    var sourceId: String = ""
    /** Exact subtitle identities for items in the active queue (especially episodes). */
    var subtitleRequests: Map<String, SubtitleSearchRequest> = emptyMap()

    fun current(): Channel? = items.getOrNull(index)
    fun hasPrev(): Boolean = index - 1 in items.indices
    fun hasNext(): Boolean = index + 1 in items.indices
    fun subtitleRequest(channel: Channel): SubtitleSearchRequest? =
        subtitleRequests[favKey(channel)]

    /**
     * ΤΟ ΜΟΝΑΔΙΚΟ κλειδί ταυτότητας καναλιού για favorites, recents ΚΑΙ
     * resume position. Το MainViewModel και το PlaylistStore πλέον
     * ΔΕΛΕΓΚΑΡΟΥΝ εδώ — πριν υπήρχαν 3 αντίγραφα της ίδιας λογικής και
     * αν απέκλιναν, τα αγαπημένα «χάνονταν» ανάλογα από πού τα πατούσες.
     *
     * ΕΠΕΙΣΟΔΙΑ: ΤΟ `cmd` ΔΕΝ ΤΑΥΤΟΠΟΙΕΙ ΕΠΕΙΣΟΔΙΟ.
     *
     * Σε Stalker/Ministra ΟΛΑ τα επεισόδια μιας σεζόν μοιράζονται ΤΟ ΙΔΙΟ
     * `cmd` (τον περιγραφέα της σεζόν) και ξεχωρίζουν μόνο από το `series=`
     * που στέλνει το create_link — δες `StalkerClient.buildEpisodeChannel`.
     * Χωρίς το `streamId` εδώ, κάθε επεισόδιο της σεζόν έπαιρνε το ίδιο
     * κλειδί, με τρεις ορατές συνέπειες: το «επόμενο επεισόδιο» έδειχνε
     * πάντα το δεύτερο της σεζόν (το `NextEpisodePolicy.nextAfter` έβρισκε
     * πάντα τη θέση 0), η αποθηκευμένη θέση ήταν κοινή ώστε κάθε επεισόδιο
     * ξεκινούσε στο λεπτό του προηγούμενου, και ένα αγαπημένο επεισόδιο
     * σήμαινε ολόκληρη τη σεζόν αγαπημένη.
     *
     * Σε Xtream κάθε επεισόδιο έχει δικό του `url`, οπότε η πρώτη γραμμή το
     * καλύπτει ήδη και δεν αλλάζει τίποτα εκεί. Ο έλεγχος είναι σκόπιμα
     * στενός (μόνο `series_ep` χωρίς `url`) ώστε τα κλειδιά για ζωντανά,
     * ταινίες και σειρές να μείνουν byte-προς-byte ίδια.
     */
    fun favKey(ch: Channel): String =
        if (ch.kind == "series_ep" && ch.url.isEmpty() && ch.streamId.isNotEmpty()) {
            "${ch.cmd}|ep|${ch.streamId}"
        } else {
            ch.url.ifEmpty { ch.cmd.ifEmpty { ch.seriesId } }
        }
}
