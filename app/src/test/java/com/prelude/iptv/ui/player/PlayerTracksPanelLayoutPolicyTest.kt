package com.prelude.iptv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTracksPanelLayoutPolicyTest {
    @Test
    fun commonLandscapePhoneUsesMaximumWidthWithoutCrossingRightEdge() {
        val containerWidth = 853f
        val panelWidth = PlayerTracksPanelLayoutPolicy.landscapePanelWidthDp(containerWidth)

        assertEquals(460f, panelWidth, .001f)
        assertTrue(
            panelWidth +
                PlayerTracksPanelLayoutPolicy.LANDSCAPE_START_INSET_DP +
                PlayerTracksPanelLayoutPolicy.LANDSCAPE_END_INSET_DP <= containerWidth,
        )
    }

    @Test
    fun compactLandscapeUsesFractionAndKeepsBothSafeInsets() {
        val containerWidth = 600f
        val panelWidth = PlayerTracksPanelLayoutPolicy.landscapePanelWidthDp(containerWidth)

        assertEquals(336f, panelWidth, .001f)
        assertTrue(
            panelWidth +
                PlayerTracksPanelLayoutPolicy.LANDSCAPE_START_INSET_DP +
                PlayerTracksPanelLayoutPolicy.LANDSCAPE_END_INSET_DP <= containerWidth,
        )
    }

    @Test
    fun extremelyNarrowOrInvalidWidthNeverProducesOverflow() {
        assertEquals(20f, PlayerTracksPanelLayoutPolicy.landscapePanelWidthDp(60f), .001f)
        assertEquals(0f, PlayerTracksPanelLayoutPolicy.landscapePanelWidthDp(0f), .001f)
        assertEquals(0f, PlayerTracksPanelLayoutPolicy.landscapePanelWidthDp(Float.NaN), .001f)
    }
}
