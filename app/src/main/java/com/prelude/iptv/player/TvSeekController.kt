package com.prelude.iptv.player

/**
 * Android-free policy for the TV seek bar. PlayerActivity only translates
 * KeyEvent values to these enums; all routing rules remain JVM-testable.
 */
enum class TvSeekKey { LEFT, RIGHT, CONFIRM, OTHER }
enum class TvKeyPhase { DOWN, UP, OTHER }

sealed interface TvSeekKeyDecision {
    data object PassThrough : TvSeekKeyDecision
    data object Consume : TvSeekKeyDecision
    data class Nudge(val deltaMs: Long) : TvSeekKeyDecision
    data object Confirm : TvSeekKeyDecision
}

object TvSeekKeyPolicy {
    fun decide(
        isTv: Boolean,
        isLive: Boolean,
        key: TvSeekKey,
        phase: TvKeyPhase,
        repeatCount: Int,
        stepMs: Long = DEFAULT_STEP_MS,
    ): TvSeekKeyDecision {
        if (!isTv || isLive || key == TvSeekKey.OTHER) {
            return TvSeekKeyDecision.PassThrough
        }

        // SeekBar also handles ACTION_UP internally. Consume it so the
        // framework cannot apply a second visual-only progress movement.
        if (phase == TvKeyPhase.UP) return TvSeekKeyDecision.Consume
        if (phase != TvKeyPhase.DOWN) return TvSeekKeyDecision.Consume

        return when (key) {
            TvSeekKey.LEFT -> TvSeekKeyDecision.Nudge(-stepMs)
            TvSeekKey.RIGHT -> TvSeekKeyDecision.Nudge(stepMs)
            TvSeekKey.CONFIRM -> {
                // A held OK key emits repeated DOWN events. Confirm once.
                if (repeatCount == 0) TvSeekKeyDecision.Confirm
                else TvSeekKeyDecision.Consume
            }
            TvSeekKey.OTHER -> TvSeekKeyDecision.PassThrough
        }
    }

    const val DEFAULT_STEP_MS = 10_000L
}

sealed interface TvSeekUpdate {
    /** Preview a known-duration target and debounce the real player seek. */
    data class Preview(val targetMs: Long, val durationMs: Long) : TvSeekUpdate

    /** Duration is unknown, so execute a direct relative seek immediately. */
    data class RelativeSeek(val deltaMs: Long) : TvSeekUpdate
}

/**
 * Owns pending TV scrub state independently from Activity/UI lifecycle.
 * Repeated LEFT/RIGHT presses accumulate into one target, then commit once.
 */
class TvSeekController {
    private var pendingTargetMs: Long? = null

    fun nudge(deltaMs: Long, currentPositionMs: Long, durationMs: Long): TvSeekUpdate {
        if (durationMs <= 0L) {
            pendingTargetMs = null
            return TvSeekUpdate.RelativeSeek(deltaMs)
        }

        val base = pendingTargetMs ?: currentPositionMs.coerceIn(0L, durationMs)
        val target = PlaybackSeekPolicy.relativeTarget(base, deltaMs, durationMs)
        pendingTargetMs = target
        return TvSeekUpdate.Preview(target, durationMs)
    }

    fun hasPending(): Boolean = pendingTargetMs != null

    fun pendingTarget(): Long? = pendingTargetMs

    fun commit(): Long? = pendingTargetMs.also { pendingTargetMs = null }

    fun cancel() {
        pendingTargetMs = null
    }
}

/** Shared boundary handling for touch, remote, media-key and gesture seeks. */
object PlaybackSeekPolicy {
    fun absoluteTarget(requestedMs: Long, durationMs: Long): Long {
        return if (durationMs > 0L) requestedMs.coerceIn(0L, durationMs)
        else requestedMs.coerceAtLeast(0L)
    }

    fun relativeTarget(currentPositionMs: Long, deltaMs: Long, durationMs: Long): Long {
        val current = currentPositionMs.coerceAtLeast(0L)
        val requested = when {
            deltaMs > 0L && current > Long.MAX_VALUE - deltaMs -> Long.MAX_VALUE
            deltaMs < 0L && current < Long.MIN_VALUE - deltaMs -> Long.MIN_VALUE
            else -> current + deltaMs
        }
        return absoluteTarget(requested, durationMs)
    }
}

/**
 * Maps millisecond positions to Android SeekBar's Int range. Most videos fit
 * directly; very long timelines are scaled instead of overflowing to negative.
 */
object SeekBarPositionMapper {
    fun maxProgress(durationMs: Long): Int {
        if (durationMs <= 0L) return 0
        return durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun progress(positionMs: Long, durationMs: Long): Int {
        if (durationMs <= 0L) return 0
        val position = positionMs.coerceIn(0L, durationMs)
        return if (durationMs <= Int.MAX_VALUE.toLong()) {
            position.toInt()
        } else {
            ((position.toDouble() / durationMs.toDouble()) * Int.MAX_VALUE.toDouble())
                .toInt()
                .coerceIn(0, Int.MAX_VALUE)
        }
    }

    fun positionMs(progress: Int, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        val safeProgress = progress.coerceIn(0, maxProgress(durationMs))
        return if (durationMs <= Int.MAX_VALUE.toLong()) {
            safeProgress.toLong()
        } else {
            ((safeProgress.toDouble() / Int.MAX_VALUE.toDouble()) * durationMs.toDouble())
                .toLong()
                .coerceIn(0L, durationMs)
        }
    }
}
