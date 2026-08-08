package com.prelude.iptv.ui

/**
 * Pure rules for the user-curated "featured" group rails on the catalog home.
 *
 * The home shows up to [MAX] group rails with big artwork. The user picks which
 * groups appear (per tab); before any choice is made we fall back to the first
 * [MAX] available groups so the screen is never empty. All logic here is
 * Android-free and deterministic so it is unit-testable.
 */
object FeaturedGroupsPolicy {
    const val MAX = 6

    /**
     * Resolves the group titles to feature.
     *
     * @param saved     the user's persisted ordered selection (may contain stale
     *                  titles from before a refresh, or be empty for "no choice").
     * @param available the group titles that currently exist, in catalog order.
     * @return up to [MAX] valid titles in the user's order, or the first [MAX]
     *         available titles when the user has made no valid selection.
     */
    fun resolve(saved: List<String>, available: List<String>): List<String> {
        val valid = saved.filter { it in available }.distinct().take(MAX)
        return valid.ifEmpty { available.take(MAX) }
    }

    /**
     * Adds or removes [group] from [current]. Adding past [MAX] is ignored so the
     * caller can bind this straight to a checkbox without extra guarding.
     */
    fun toggle(current: List<String>, group: String): List<String> = when {
        group in current -> current - group
        current.size >= MAX -> current
        else -> current + group
    }

    fun isFull(current: List<String>): Boolean = current.size >= MAX
}
