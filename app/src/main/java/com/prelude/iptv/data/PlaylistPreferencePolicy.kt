package com.prelude.iptv.data

/** Pure preference decisions shared by storage, startup and settings UI. */
object PlaylistPreferencePolicy {
    private val refreshOptions = listOf(1, 3, 7)

    fun normalizeRefreshDays(days: Int): Int = days.takeIf(refreshOptions::contains) ?: 3

    fun nextRefreshDays(current: Int): Int {
        val normalized = normalizeRefreshDays(current)
        return refreshOptions[(refreshOptions.indexOf(normalized) + 1) % refreshOptions.size]
    }

    fun shouldAutoOpen(alreadyOpened: Boolean, enabled: Boolean, hasSources: Boolean): Boolean =
        !alreadyOpened && enabled && hasSources
}
