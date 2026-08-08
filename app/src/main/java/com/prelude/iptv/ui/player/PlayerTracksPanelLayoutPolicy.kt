package com.prelude.iptv.ui.player

import kotlin.math.min

/**
 * Pure sizing rules for the fullscreen tracks panel.
 *
 * Keeping the calculation outside Compose makes the most failure-prone part of
 * the landscape layout deterministic and testable without an emulator.
 */
internal object PlayerTracksPanelLayoutPolicy {
    const val LANDSCAPE_WIDTH_FRACTION = .56f
    const val LANDSCAPE_MAX_WIDTH_DP = 460f
    const val LANDSCAPE_START_INSET_DP = 16f
    const val LANDSCAPE_END_INSET_DP = 24f

    fun landscapePanelWidthDp(containerWidthDp: Float): Float {
        if (!containerWidthDp.isFinite() || containerWidthDp <= 0f) return 0f
        val availableWidth = (
            containerWidthDp - LANDSCAPE_START_INSET_DP - LANDSCAPE_END_INSET_DP
        ).coerceAtLeast(0f)
        return min(
            min(containerWidthDp * LANDSCAPE_WIDTH_FRACTION, LANDSCAPE_MAX_WIDTH_DP),
            availableWidth,
        )
    }
}
