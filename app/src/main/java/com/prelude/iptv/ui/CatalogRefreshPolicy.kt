package com.prelude.iptv.ui

/** Pure refresh-selection rules shared by TV/mobile UI and covered by JVM tests. */
object CatalogRefreshPolicy {
    /**
     * null means that the previous choice was "all groups". Otherwise keep only
     * ids that still exist in the freshly downloaded provider category list.
     */
    fun initialSelection(
        freshCategories: List<Pair<String, String>>,
        rememberedIds: List<String>?
    ): Set<String>? {
        if (rememberedIds == null) return null
        val available = freshCategories.asSequence().map { it.first }.toHashSet()
        return rememberedIds.asSequence()
            .filter { it in available }
            .toCollection(LinkedHashSet())
    }



    /**
     * Refresh persistence is transactional both after an explicit refresh picker
     * and when a refresh falls through directly because the provider exposes no categories.
     */
    fun usesTransactionalSelectionCommit(
        pickerFromRefresh: Boolean,
        directRefreshFallback: Boolean
    ): Boolean = pickerFromRefresh || directRefreshFallback

    /** Keep the visible group after refresh only when it still exists. */
    fun restoredVisibleGroup(
        previousGroup: String,
        freshGroups: List<String>,
        allGroup: String
    ): String = if (previousGroup in freshGroups) previousGroup else allGroup
}
