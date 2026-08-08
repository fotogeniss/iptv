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
     */
    fun favKey(ch: Channel): String =
        ch.url.ifEmpty { ch.cmd.ifEmpty { ch.seriesId } }
}
