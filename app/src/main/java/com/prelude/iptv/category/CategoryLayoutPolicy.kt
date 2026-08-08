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
}
