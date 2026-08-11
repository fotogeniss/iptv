package com.prelude.iptv.ui.home

import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.CatalogRankingPolicy
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
     * «Νέα»: με ημερομηνία προσθήκης όταν υπάρχει, αλλιώς με τη σειρά του παρόχου.
     *
     * ΠΡΩΤΑ Η ΑΛΗΘΕΙΑ. Τα Stalker portals στέλνουν `added` («2025-07-30
     * 01:08:10») και αυτό είναι η πραγματική απάντηση στο «τι είναι νέο» — όχι
     * το έτος παραγωγής, που είναι πότε γυρίστηκε.
     *
     * Η ΕΦΕΔΡΕΙΑ ΜΕΝΕΙ, γιατί M3U και Xtream δεν στέλνουν τίποτα τέτοιο: εκεί το
     * μόνο που ξέρουμε είναι ότι οι πάροχοι προσθέτουν στο τέλος του αρχείου.
     * Είναι εικασία, αλλά η ίδια που κάνουν όλες οι εφαρμογές του είδους.
     *
     * Το κατώφλι δεν είναι «έστω ένα με ημερομηνία»: αν σε κατάλογο 5.000
     * ταινιών μόνο τρεις έχουν `added`, μια ράγα με τρεις κάρτες είναι χειρότερη
     * από την παλιά εικασία με είκοσι. Η ταξινόμηση κερδίζει μόνο όταν μπορεί να
     * γεμίσει τη ράγα μόνη της.
     */
    fun newest(items: List<Channel>, limit: Int = NEW_LIMIT): List<Channel> {
        val dated = CatalogRankingPolicy.newestFirst(items)
        if (dated.size >= limit) return dated.take(limit)
        return items.takeLast(limit).asReversed()
    }

    /**
     * «Κορυφαία»: με τη βαθμολογία του παρόχου, ή τίποτα.
     *
     * ΚΕΝΟ ΕΙΝΑΙ ΘΕΜΙΤΟ ΑΠΟΤΕΛΕΣΜΑ. Το [rail] επιστρέφει `null` σε άδεια λίστα,
     * οπότε σε πηγή χωρίς βαθμολογίες η ενότητα απλώς δεν ζωγραφίζεται. Αυτό
     * είναι σωστότερο από το να γεμίσει με ό,τι να 'ναι: μια ράγα που λέει
     * «Κορυφαίες» οφείλει να έχει κριτήριο, αλλιώς λέει ψέματα — και ακριβώς
     * αυτό ήταν το παράπονο για την παλιά ράγα «Κορυφαία», που ήταν
     * αταξινόμητη με νούμερα από πάνω.
     */
    fun topRated(items: List<Channel>, limit: Int = RAIL_LIMIT): List<Channel> =
        CatalogRankingPolicy.topRatedFirst(items).take(limit)

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
