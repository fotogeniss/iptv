package com.prelude.iptv.player

/**
 * Pure presentation policy shared by the View-based player chrome.
 * Keeping timing/progress math outside PlayerActivity makes it deterministic
 * and prevents UI rewrites from touching the playback engines.
 */
object PlayerUiPolicy {
    fun liveProgress(startMs: Long, stopMs: Long, nowMs: Long): Int {
        val duration = stopMs - startMs
        if (duration <= 0L) return 0
        return (((nowMs - startMs).toDouble() / duration.toDouble()) * 1000.0)
            .toInt()
            .coerceIn(0, 1000)
    }

    fun autoHideMs(isTv: Boolean, userSeeking: Boolean): Long = when {
        userSeeking -> 12_000L
        isTv -> 5_500L
        else -> 4_000L
    }
}
