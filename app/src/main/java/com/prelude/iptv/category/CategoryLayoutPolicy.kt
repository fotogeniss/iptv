package com.prelude.iptv.category

/** Provider category with the stable id required when loading its content. */
data class CategoryOption(val id: String, val title: String)

/** Persisted user customisation for one source and one content type. */
data class CategoryLayout(
    val order: List<String> = emptyList(),
    val orderedTitles: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val deleted: Set<String> = emptySet(),
)

data class CategoryEntry(
    val option: CategoryOption,
    val visible: Boolean,
)

/** Pure, Android-free rules shared by the screen, persistence and tests. */
object CategoryLayoutPolicy {
    /** Applies the user's provider-group order and appends genuinely new groups deterministically. */
    fun <T> orderByTitle(
        items: List<T>,
        preferredTitles: List<String>,
        titleOf: (T) -> String,
    ): List<T> {
        val rank = preferredTitles.withIndex().associate { it.value to it.index }
        return items.sortedWith(
            compareBy<T> { rank[titleOf(it)] ?: Int.MAX_VALUE }
                .thenBy { if (titleOf(it) in rank) "" else titleOf(it).lowercase() }
        )
    }

    fun resolve(available: List<CategoryOption>, layout: CategoryLayout): List<CategoryEntry> {
        val unique = available.distinctBy { it.id }
        val rank = layout.order.withIndex().associate { it.value to it.index }
        return unique
            .filterNot { it.id in layout.deleted }
            .sortedWith(compareBy<CategoryOption> { rank[it.id] ?: Int.MAX_VALUE }
                .thenBy { if (it.id in rank) "" else it.title.lowercase() })
            .map { CategoryEntry(it, it.id !in layout.hidden) }
    }

    fun deleted(available: List<CategoryOption>, layout: CategoryLayout): List<CategoryOption> =
        available.distinctBy { it.id }.filter { it.id in layout.deleted }.sortedBy { it.title.lowercase() }

    fun move(entries: List<CategoryEntry>, from: Int, to: Int): List<CategoryEntry> {
        if (from !in entries.indices || to !in entries.indices || from == to) return entries
        return entries.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun layoutOf(entries: List<CategoryEntry>, previous: CategoryLayout): CategoryLayout =
        previous.copy(
            order = entries.map { it.option.id },
            orderedTitles = entries.map { it.option.title },
            hidden = entries.filterNot { it.visible }.map { it.option.id }.toSet(),
        )

    fun delete(layout: CategoryLayout, id: String): CategoryLayout =
        layout.copy(hidden = layout.hidden - id, deleted = layout.deleted + id)

    fun restore(layout: CategoryLayout, id: String): CategoryLayout =
        layout.copy(deleted = layout.deleted - id, hidden = layout.hidden - id)

    fun selectedIds(entries: List<CategoryEntry>): List<String> =
        entries.filter { it.visible }.map { it.option.id }

    /**
     * Το νέο UI δεν ξεχωρίζει «διαγραμμένη» από «κρυφή»: μία κατηγορία είτε
     * φαίνεται είτε όχι. Οι παλιές διαγραμμένες γυρίζουν ως κρυφές, ώστε να
     * μπορεί κανείς να τις ξαναβρεί από τη λίστα «Κρυφές».
     */
    fun normalized(layout: CategoryLayout): CategoryLayout =
        if (layout.deleted.isEmpty()) layout
        else layout.copy(hidden = layout.hidden + layout.deleted, deleted = emptySet())

    /**
     * Συγχρονίζει με την επιλογή που ήδη ισχύει στον κατάλογο (`ids == null`
     * σημαίνει «όλες»). Χωρίς αυτό, ο επεξεργαστής θα έδειχνε ορατή μια
     * κατηγορία που ο χρήστης είχε κρύψει από άλλο σημείο.
     */
    fun withChoice(layout: CategoryLayout, available: List<CategoryOption>, ids: List<String>?): CategoryLayout {
        if (ids == null || available.isEmpty()) return layout
        val visible = ids.toSet()
        return layout.copy(hidden = available.map { it.id }.filterNot(visible::contains).toSet())
    }

    /** Ανάβει ή σβήνει μαζί πολλές κατηγορίες (χώρα, «όλες»), χωρίς να αλλάζει τη σειρά. */
    fun setVisible(entries: List<CategoryEntry>, ids: Set<String>, visible: Boolean): List<CategoryEntry> =
        entries.map { if (it.option.id in ids) it.copy(visible = visible) else it }

    /** Α–Ω με σύγκριση που αγνοεί πεζά/κεφαλαία. */
    fun sortedByTitle(entries: List<CategoryEntry>): List<CategoryEntry> =
        entries.sortedBy { it.option.title.lowercase() }

    /** Στην κορυφή ή στο τέλος των ορατών. */
    fun moveToEdge(entries: List<CategoryEntry>, id: String, top: Boolean): List<CategoryEntry> {
        val item = entries.firstOrNull { it.option.id == id } ?: return entries
        val rest = entries.filterNot { it.option.id == id }
        return if (top) listOf(item) + rest else rest + item
    }
}
