package com.prelude.iptv.player

import java.io.IOException

/** Small rules used by PlayerActivity and covered by JVM tests. */
object PlaybackStabilityPolicy {
    const val TV_ZAP_DEBOUNCE_MS = 180L
    const val MAX_TRANSIENT_RETRIES = 2

    fun mergeZapDelta(pendingDelta: Int, newDelta: Int): Int =
        (pendingDelta + newDelta).coerceIn(-10, 10)

    /** Media3 commonly wraps the network IOException one or more levels deep. */
    fun hasIoCause(error: Throwable?): Boolean {
        var current = error
        var depth = 0
        while (current != null && depth++ < 12) {
            if (current is IOException) return true
            current = current.cause
        }
        return false
    }

    fun shouldRetryTransientIo(attempt: Int, isIoFailure: Boolean, requestStillCurrent: Boolean): Boolean =
        isIoFailure && requestStillCurrent && attempt < MAX_TRANSIENT_RETRIES

    fun retryDelayMs(attempt: Int): Long = when (attempt) {
        0 -> 500L
        else -> 1_200L
    }
}
