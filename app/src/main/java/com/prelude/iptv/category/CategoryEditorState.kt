package com.prelude.iptv.category

data class CategoryEditorSection(
    val available: List<CategoryOption> = emptyList(),
    val layout: CategoryLayout = CategoryLayout(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    val entries: List<CategoryEntry> get() = CategoryLayoutPolicy.resolve(available, layout)
    val deletedEntries: List<CategoryOption> get() = CategoryLayoutPolicy.deleted(available, layout)
}

data class CategoryEditorState(
    val sourceId: String = "",
    val sections: Map<String, CategoryEditorSection> = emptyMap(),
) {
    fun section(type: String): CategoryEditorSection = sections[type] ?: CategoryEditorSection()
}
