package com.prelude.iptv.player

import kotlin.math.abs
import kotlin.math.max

/** User-facing Auto Frame Rate modes persisted by [com.prelude.iptv.data.PlaylistStore]. */
enum class AutoFrameRateMode(val storageValue: String) {
    OFF("off"),
    SEAMLESS("seamless"),
    ALWAYS("always");

    companion object {
        fun fromStorage(value: String?): AutoFrameRateMode = when (value?.trim()?.lowercase()) {
            SEAMLESS.storageValue -> SEAMLESS
            ALWAYS.storageValue -> ALWAYS
            else -> OFF
        }
    }
}

/** Android-free display-mode description used by the matching policy and JVM tests. */
data class DisplayModeInfo(
    val id: Int,
    val width: Int,
    val height: Int,
    val refreshRate: Float
)

/**
 * Pure Auto Frame Rate matching policy.
 *
 * A mode is eligible only when it keeps the current output resolution and its
 * refresh rate is an integer multiple of the content frame rate. This avoids
 * silently switching a 4K TV to a lower-resolution timing mode.
 */
object FrameRateMatchPolicy {
    private const val MIN_CONTENT_FPS = 10f
    private const val MAX_CONTENT_FPS = 240f
    private const val MAX_MULTIPLE = 6

    fun sanitizeContentFrameRate(frameRate: Float): Float? =
        frameRate.takeIf { it.isFinite() && it in MIN_CONTENT_FPS..MAX_CONTENT_FPS }

    fun isCompatible(refreshRate: Float, contentFrameRate: Float): Boolean =
        compatibility(refreshRate, contentFrameRate) != null

    fun chooseDisplayMode(
        contentFrameRate: Float,
        currentMode: DisplayModeInfo,
        supportedModes: List<DisplayModeInfo>
    ): DisplayModeInfo? {
        val fps = sanitizeContentFrameRate(contentFrameRate) ?: return null
        return supportedModes.asSequence()
            .filter { it.id > 0 && it.width == currentMode.width && it.height == currentMode.height }
            .mapNotNull { mode ->
                val compatibility = compatibility(mode.refreshRate, fps) ?: return@mapNotNull null
                val score = compatibility.errorHz * 100f +
                    (compatibility.multiple - 1) * 0.08f +
                    if (mode.id == currentMode.id) 0f else 0.01f
                mode to score
            }
            .minByOrNull { it.second }
            ?.first
    }

    private data class Compatibility(val multiple: Int, val errorHz: Float)

    private fun compatibility(refreshRate: Float, contentFrameRate: Float): Compatibility? {
        val fps = sanitizeContentFrameRate(contentFrameRate) ?: return null
        if (!refreshRate.isFinite() || refreshRate <= 0f) return null

        var best: Compatibility? = null
        for (multiple in 1..MAX_MULTIPLE) {
            val target = fps * multiple
            val error = abs(refreshRate - target)
            // Allows the common 23.976/24 and 29.97/30 clock families while
            // still rejecting 60 Hz for 24 fps (the judder-inducing 3:2 case).
            val tolerance = max(0.12f, target * 0.003f)
            if (error <= tolerance && (best == null || error < best.errorHz)) {
                best = Compatibility(multiple, error)
            }
        }
        return best
    }
}
