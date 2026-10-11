package com.prelude.iptv.ui.policy

import com.prelude.iptv.data.Channel

/** Pure post-download category visibility for one complete catalog snapshot. */
internal object CatalogCategoryVisibilityPolicy {
    private const val UNGROUPED = "Χωρίς ομάδα"

    /**
     * ΤΟ ΔΗΛΩΜΕΝΟ ΠΛΗΘΟΣ ΝΙΚΑΕΙ ΤΟ ΜΕΤΡΗΜΕΝΟ.
     *
     * Το μέτρημα των φορτωμένων είναι σωστό μόνο όταν έχουν φορτωθεί όλα. Όσο ο
     * κατάλογος κατεβαίνει, λέει «14» για κατηγορία με 4.282 ταινίες — και αυτό
     * ακριβώς το ψέμα ήταν μία από τις αιτίες που το προοδευτικό μοντέλο
     * απορρίφθηκε. Το `total_items` του παρόχου το ξέρει από την 1η σελίδα.
     *
     * [declaredTotals] κενό (Xtream, M3U, ή portal που δεν το δηλώνει) =
     * συμπεριφορά ακριβώς όπως πριν.
     */
    fun counts(
        categories: List<Pair<String, String>>,
        channels: List<Channel>,
        declaredTotals: Map<String, Int> = emptyMap(),
    ): Map<String, Int> {
        val countsByTitle = channels.groupingBy { it.group.ifBlank { UNGROUPED } }.eachCount()
        return categories.associate { (id, title) ->
            id to (declaredTotals[id] ?: countsByTitle[title] ?: 0)
        }
    }

    fun visibleChannels(
        categories: List<Pair<String, String>>,
        channels: List<Channel>,
        selectedIds: List<String>?,
    ): List<Channel> {
        if (selectedIds == null) return channels
        val selected = selectedIds.toHashSet()
        val visibleTitles = categories.asSequence()
            .filter { (id, _) -> id in selected }
            .map { (_, title) -> title }
            .toHashSet()
        return channels.filter { it.group.ifBlank { UNGROUPED } in visibleTitles }
    }

    /** Invalid legacy ids must not turn a complete catalog into an empty screen. */
    fun initialSelection(
        categories: List<Pair<String, String>>,
        hasRememberedChoice: Boolean,
        rememberedIds: List<String>?,
    ): Set<String>? {
        if (!hasRememberedChoice || rememberedIds == null) return null
        if (rememberedIds.isEmpty()) return emptySet()
        val validIds = categories.mapTo(HashSet()) { it.first }
        val validRemembered = rememberedIds.filterTo(LinkedHashSet()) { it in validIds }
        return validRemembered.takeIf(Set<String>::isNotEmpty)
    }
}
