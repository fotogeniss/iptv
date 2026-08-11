package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue

data class CatalogRailSection(
    val id: String,
    val title: String,
    val items: List<Channel>,
    val ranked: Boolean = false,
    val progress: Map<String, Float> = emptyMap(),
    val allItems: List<Channel> = items
)

/** App-owned rail copy is injected by the UI; provider group titles stay data. */
data class CatalogRailLabels(
    val continueWatching: String,
    val myList: String,
    val trending: String,
    val newReleases: String,
)

/**
 * Deterministic catalog policy, intentionally independent from Compose.
 * It can be unit-tested without an emulator and keeps expensive grouping out of
 * the rendering layer.
 */
fun buildCatalogRailSections(
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    continueWatching: List<Pair<Channel, Float>>,
    labels: CatalogRailLabels,
): List<CatalogRailSection> {
    if (channels.isEmpty()) return emptyList()

    // Η ΑΡΧΙΚΗ ΕΙΝΑΙ ΒΙΒΛΙΟΘΗΚΗ, ΟΧΙ ΤΗΛΕΟΡΑΣΗ.
    //
    // Τα ζωντανά κανάλια δεν έχουν αφίσα, περίληψη, διάρκεια ή «συνέχισε» — οι
    // ράγες της αρχικής είναι φτιαγμένες για ταινίες και σειρές, και ένα κανάλι
    // ανάμεσά τους εμφανίζεται ως κενό πλακίδιο. Έχουν ήδη τη δική τους ενότητα,
    // με λίστα και πρόγραμμα, που τους ταιριάζει.
    @Suppress("NAME_SHADOWING")
    val channels = channels.filter { it.kind != "live" }
    if (channels.isEmpty()) return emptyList()

    fun key(ch: Channel) = PlaybackQueue.favKey(ch)
    fun unique(source: Iterable<Channel>): List<Channel> {
        val seen = HashSet<String>()
        return source.filter { seen.add(key(it).ifBlank { "${it.name}|${it.logo}" }) }
    }
    fun section(
        id: String,
        title: String,
        all: List<Channel>,
        ranked: Boolean = false,
        progress: Map<String, Float> = emptyMap()
    ) = CatalogRailSection(
        id = id,
        title = title,
        items = all.take(20),
        ranked = ranked,
        progress = progress,
        allItems = all
    )

    val out = ArrayList<CatalogRailSection>()
    if (continueWatching.isNotEmpty()) {
        val progress = continueWatching.associate { key(it.first) to it.second }
        val all = unique(continueWatching.map { it.first })
        out += section(
            id = "continue",
            title = labels.continueWatching,
            all = all,
            progress = progress
        )
    }

    val favorites = unique(channels.filter { key(it) in favoriteKeys })
    if (favorites.isNotEmpty()) {
        out += section("my-list", labels.myList, favorites)
    }

    // ΚΟΡΥΦΑΙΑ: με βαθμολογία, όχι με τη σειρά που τα έστειλε ο πάροχος.
    //
    // Η ράγα τυπώνει θέσεις 1, 2, 3 — άρα οφείλει να είναι όντως κατάταξη.
    // Στοιχεία χωρίς βαθμολογία μένουν έξω, δεν πέφτουν στο τέλος: μια θέση σε
    // πίνακα κατάταξης είναι ισχυρισμός, και για αυτά δεν έχουμε κανέναν.
    // ΠΡΟΣΟΧΗ ΣΤΗΝ ΕΦΕΔΡΕΙΑ: αυτή είναι η ΚΥΡΙΑ ράγα της αρχικής και υπήρχε
    // πάντα. Αν μια πηγή δεν στέλνει καθόλου βαθμολογίες — M3U, και αρκετά
    // portals — μια σκέτη «ταξινόμηση με βαθμολογία» θα την εξαφάνιζε και η
    // αρχική θα άδειαζε. Οπότε: με βαθμολογίες γίνεται πραγματική κατάταξη·
    // χωρίς αυτές μένει ό,τι ήταν, μόνο που ΧΑΝΕΙ τα νούμερα θέσης, τα οποία
    // ήταν και η αιτία που έμοιαζε τυχαία.
    val rated = unique(CatalogRankingPolicy.topRatedFirst(channels))
    val hasRealRanking = rated.size >= 4
    out += section(
        id = "trending",
        title = labels.trending,
        all = if (hasRealRanking) rated else unique(channels),
        ranked = hasRealRanking
    )

    // ΝΕΑ: με το πότε μπήκαν στον κατάλογο, με εφεδρεία το έτος.
    val newest = unique(CatalogRankingPolicy.newestFirst(channels))
    if (newest.size >= 4) out += section("new", labels.newReleases, newest)

    // ΟΛΑ τα groups που κατέβασε ο χρήστης γίνονται sections, ώστε να φαίνονται
    // όλα στα (scrollable) chips και να επιλέγονται. Το πλήθος των rails που
    // εμφανίζονται στο «Για εσένα» το ελέγχει το FeaturedGroupsPolicy (top 6),
    // οπότε δεν χρειάζεται καθολικό cap εδώ.
    channels
        .groupBy { it.group.trim() }
        .asSequence()
        .filter { (group, _) -> group.isNotBlank() }
        .sortedByDescending { it.value.size }
        .forEach { (group, items) ->
            val all = unique(items)
            out += section(
                id = "group:${group.lowercase()}",
                title = group,
                all = all
            )
        }

    return out
}
