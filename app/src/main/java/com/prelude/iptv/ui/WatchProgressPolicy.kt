package com.prelude.iptv.ui

/**
 * Immutable playback-progress value used by UI and tests.
 * Keeping the calculation outside Compose prevents progress labels and rails
 * from reimplementing slightly different percentage rules.
 */
data class WatchProgress(
    val positionMs: Long,
    val durationMs: Long
) {
    val fraction: Float
        get() = (positionMs.toDouble() / durationMs.toDouble()).toFloat().coerceIn(0f, 1f)

    val percent: Int
        get() = (fraction * 100f).toInt().coerceIn(0, 100)

    val remainingMs: Long
        get() = (durationMs - positionMs).coerceAtLeast(0L)
}

data class WatchRemaining(val hours: Int, val minutes: Int)

object WatchProgressPolicy {
    const val MIN_RESUME_MS = 60_000L
    const val COMPLETED_PERCENT = 95

    /**
     * Accept only meaningful resumable progress. This mirrors PlaylistStore's
     * persistence rule and also protects the UI from corrupt/legacy values.
     */
    fun from(raw: Pair<Long, Long>?): WatchProgress? {
        val (position, duration) = raw ?: return null
        if (duration <= 0L || position < MIN_RESUME_MS || position >= duration) return null
        if (position * 100L >= duration * COMPLETED_PERCENT) return null
        return WatchProgress(position, duration)
    }

    fun remaining(progress: WatchProgress): WatchRemaining {
        val totalMinutes = (progress.remainingMs / 60_000L).coerceAtLeast(1L)
        return WatchRemaining(
            hours = (totalMinutes / 60L).toInt(),
            minutes = (totalMinutes % 60L).toInt(),
        )
    }
}
