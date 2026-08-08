package com.prelude.iptv.ui

/**
 * Pure decisions for removing a playlist without accidentally blanking or
 * deleting data owned by the source that is still active.
 */
data class SourceDeletionDecision(
    val newActiveIndex: Int,
    val removedActiveSource: Boolean,
    val hasReplacementSource: Boolean,
)

object SourceDeletionPolicy {
    fun decide(sizeAfter: Int, removedIndex: Int, activeIndex: Int): SourceDeletionDecision {
        val removedActive = removedIndex == activeIndex
        return SourceDeletionDecision(
            newActiveIndex = LoadPolicy.indexAfterDelete(sizeAfter, removedIndex, activeIndex),
            removedActiveSource = removedActive,
            hasReplacementSource = removedActive && sizeAfter > 0,
        )
    }

    /**
     * Source-scoped history, favorites, selections and local files may be
     * deleted only when no remaining playlist points to the same stable source.
     */
    fun isLastReference(removedSourceId: String, remainingSourceIds: Collection<String>): Boolean =
        removedSourceId.isNotBlank() && removedSourceId !in remainingSourceIds
}
