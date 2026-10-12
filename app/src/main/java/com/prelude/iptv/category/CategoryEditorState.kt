package com.prelude.iptv.category

enum class CategoryEditorFailure { LoadFailed }

data class CategoryEditorSection(
    val available: List<CategoryOption> = emptyList(),
    val layout: CategoryLayout = CategoryLayout(),
    val loading: Boolean = false,
    val error: CategoryEditorFailure? = null,
    /** Πλήθος καναλιών/τίτλων ανά κατηγορία, όταν ο κατάλογος είναι ήδη στη μνήμη. */
    val counts: Map<String, Int> = emptyMap(),
) {
    val entries: List<CategoryEntry> get() = CategoryLayoutPolicy.resolve(available, layout)
    val deletedEntries: List<CategoryOption> get() = CategoryLayoutPolicy.deleted(available, layout)
}

data class CategoryEditorState(
    val sourceId: String = "",
    val sections: Map<String, CategoryEditorSection> = emptyMap(),
    /** Αν ο επεξεργαστής φαίνεται. Ο ίδιος φορτωτής εξυπηρετεί και την Αρχική χωρίς οθόνη. */
    val visible: Boolean = false,
) {
    fun section(type: String): CategoryEditorSection = sections[type] ?: CategoryEditorSection()
}
