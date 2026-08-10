package com.prelude.iptv.ui.player

import kotlin.math.PI
import kotlin.math.sin

internal data class LiveChannelTransitionRequest(
    val sequence: Int,
    val direction: Int,
    val outgoingFrame: CapturedVideoFrame?,
    // false = «κράτημα»: το παγωμένο outgoingFrame καλύπτει ήδη ολόκληρη την
    // οθόνη (βλ. edgeFraction στο phase 0) αλλά δεν κινείται ακόμα — έτσι
    // καλύπτεται η πραγματική εναλλαγή ροής χωρίς να φανεί σαν ξένο "φλας".
    // true = ξεκινά το κανονικό reveal animation προς το νέο κανάλι.
    val startReveal: Boolean = false,
)

/** Android-free transition policy shared by the mobile and TV renderers. */
internal object LiveChannelTransitionMotion {
    const val DURATION_MS = 720
    const val FIRST_FRAME_TIMEOUT_MS = 12_000L

    fun direction(step: Int): Int = if (step >= 0) 1 else -1

    fun edgeFraction(progress: Float, direction: Int): Float {
        val safe = progress.coerceIn(0f, 1f)
        return if (direction >= 0) 1f - safe else safe
    }

    fun intensity(progress: Float): Float =
        sin(progress.coerceIn(0f, 1f) * PI).toFloat().coerceIn(0f, 1f)

    fun hasCommittedFrame(
        framesBeforeOpen: Int,
        renderedFrames: Int,
        hasPlaybackError: Boolean,
    ): Boolean = !hasPlaybackError && renderedFrames > framesBeforeOpen
}
