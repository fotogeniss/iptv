package com.prelude.iptv.ui.home

import com.prelude.iptv.data.Channel
import kotlin.random.Random

/** Έτοιμο rail: τι λέει ο τίτλος του και τι δείχνει. */
data class HomeRail(
    val id: String,
    val title: String,
    val items: List<Channel>,
    /** Ολόκληρο το σύνολο, για το «Όλα ›». Ίδιο με τα [items] αν δεν κόπηκε. */
    val allItems: List<Channel> = items,
    /** Πρόοδος ανά κλειδί, μόνο στο «Συνέχισε να βλέπεις». */
    val progress: Map<String, Float> = emptyMap(),
    /** Ζωντανά: πλατιά πλακίδια αντί για αφίσες. */
    val live: Boolean = false,
    /** Δείχνει «×» σε κάθε κάρτα (ιστορικό που σβήνεται ένα-ένα). */
    val removable: Boolean = false,
)

/**
 * Από πού παίρνει περιεχόμενο κάθε ενότητα της αρχικής.
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΑ ΑΠΟ ΤΟ [HomeLayoutPolicy]: εκείνο ξέρει τη ΣΕΙΡΑ, αυτό ξέρει το
 * ΠΕΡΙΕΧΟΜΕΝΟ. Είναι δύο ερωτήσεις που αλλάζουν για διαφορετικούς λόγους — η μία
 * όταν ο χρήστης σύρει μια γραμμή, η άλλη όταν αλλάξει η πηγή.
 */
object HomeRailContentPolicy {

    /** Πόσα δείχνει ένα rail πριν χρειαστεί το «Όλα ›». */
    const val RAIL_LIMIT = 20

    /** Πόσα θεωρούνται «νέα». Ένα rail με 200 «νέες» ταινίες δεν λέει τίποτα. */
    const val NEW_LIMIT = 20

    /**
     * Κατάλογος ανά είδος. Το `series_ep` μετριέται ΜΑΖΙ με τις σειρές: για τον
     * χρήστη ένα επεισόδιο είναι σειρά, όχι τρίτο είδος.
     */
    fun liveOf(channels: List<Channel>): List<Channel> = channels.filter { it.kind == "live" }
    fun moviesOf(channels: List<Channel>): List<Channel> = channels.filter { it.kind == "vod" }
    fun seriesOf(channels: List<Channel>): List<Channel> =
        channels.filter { it.kind == "series" || it.kind == "series_ep" }

    /**
     * Οι κατηγορίες μιας ενότητας, με τις μεγαλύτερες πρώτα.
     *
     * Η σειρά έχει σημασία: η πρώτη είναι αυτή που διαλέγεται μόνη της όταν ο
     * χρήστης δεν έχει πει τίποτα, και μια κατηγορία με 3 κανάλια θα έκανε την
     * αρχική να δείχνει άδεια χωρίς να φταίει τίποτα.
     */
    fun categoriesOf(items: List<Channel>): List<String> =
        items.asSequence()
            .map { it.group.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }

    /**
     * Η κατηγορία που θα δείξει ένα rail: αυτή που ζήτησε ο χρήστης, αν υπάρχει
     * ακόμη· αλλιώς η μεγαλύτερη.
     *
     * Το «αν υπάρχει ακόμη» δεν είναι υπερβολή: οι πάροχοι μετονομάζουν ομάδες
     * συχνά, και μια αποθηκευμένη επιλογή που έπαψε να υπάρχει θα άφηνε το rail
     * μόνιμα άδειο — με τον χρήστη να βλέπει το όνομά της γραμμένο από πάνω.
     */
    fun resolveCategory(saved: String, available: List<String>): String =
        saved.takeIf { it.isNotBlank() && it in available } ?: available.firstOrNull().orEmpty()

    /**
     * «Νέα»: τα τελευταία του καταλόγου.
     *
     * Δεν υπάρχει ημερομηνία προσθήκης σε M3U ή Xtream — το μόνο που ξέρουμε
     * είναι η σειρά που τα έστειλε ο πάροχος, και οι πάροχοι προσθέτουν στο
     * τέλος. Είναι εικασία, αλλά είναι η ΙΔΙΑ εικασία που κάνουν όλες οι
     * εφαρμογές του είδους, και δίνει σωστό αποτέλεσμα στη συντριπτική
     * πλειοψηφία των λιστών.
     */
    fun newest(items: List<Channel>, limit: Int = NEW_LIMIT): List<Channel> =
        items.takeLast(limit).asReversed()

    /**
     * Τυχαίες αλλά ισορροπημένες προτάσεις από ΟΛΟ τον διαθέσιμο κατάλογο.
     *
     * Ένα απλό `shuffled()` μπορεί εύκολα να βγάλει τις πρώτες 20 κάρτες από τη
     * μεγαλύτερη κατηγορία. Εδώ ανακατεύονται πρώτα οι ομάδες και τα περιεχόμενά
     * τους και μετά διαβάζουμε μία κάρτα από κάθε ομάδα κυκλικά. Άρα το rail δεν
     * αντιγράφει την επιλεγμένη κατηγορία και παραμένει πραγματικό μείγμα.
     *
     * Το seed δίνεται από την οθόνη και μένει σταθερό όσο αυτή ζει, ώστε οι
     * κάρτες να μη μετακινούνται σε κάθε Compose recomposition.
     */
    fun suggestions(
        items: List<Channel>,
        limit: Int = RAIL_LIMIT,
        seed: Int,
    ): List<Channel> {
        if (items.isEmpty() || limit <= 0) return emptyList()
        val unique = items.distinctBy { channel ->
            listOf(channel.kind, channel.seriesId, channel.streamId, channel.url, channel.name)
                .joinToString("|")
        }
        val groups = unique
            .groupBy { it.group.trim().ifBlank { UNGROUPED } }
            .entries
            .shuffled(Random(seed))
            .map { (group, channels) ->
                channels.shuffled(Random(seed xor group.hashCode())).toMutableList()
            }
            .toMutableList()
        val result = ArrayList<Channel>(minOf(limit, unique.size))
        while (result.size < limit && groups.isNotEmpty()) {
            val iterator = groups.iterator()
            while (iterator.hasNext() && result.size < limit) {
                val group = iterator.next()
                if (group.isEmpty()) iterator.remove()
                else {
                    result += group.removeAt(group.lastIndex)
                    if (group.isEmpty()) iterator.remove()
                }
            }
        }
        return result
    }

    /**
     * Το κομμάτι ενός rail που ζωγραφίζεται, με το σύνολο κρατημένο για το «Όλα».
     */
    fun rail(
        id: String,
        title: String,
        all: List<Channel>,
        live: Boolean = false,
        removable: Boolean = false,
        progress: Map<String, Float> = emptyMap(),
    ): HomeRail? {
        if (all.isEmpty()) return null
        return HomeRail(
            id = id,
            title = title,
            items = all.take(RAIL_LIMIT),
            allItems = all,
            progress = progress,
            live = live,
            removable = removable,
        )
    }

    private const val UNGROUPED = "__ungrouped__"
}
