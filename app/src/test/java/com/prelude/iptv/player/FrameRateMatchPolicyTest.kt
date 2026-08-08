package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameRateMatchPolicyTest {
    private val current60 = DisplayModeInfo(1, 3840, 2160, 59.94f)

    @Test fun storageModeParsingIsSafe() {
        assertEquals(AutoFrameRateMode.OFF, AutoFrameRateMode.fromStorage(null))
        assertEquals(AutoFrameRateMode.OFF, AutoFrameRateMode.fromStorage("bad"))
        assertEquals(AutoFrameRateMode.SEAMLESS, AutoFrameRateMode.fromStorage("SEAMLESS"))
        assertEquals(AutoFrameRateMode.ALWAYS, AutoFrameRateMode.fromStorage("always"))
    }

    @Test fun invalidContentFrameRatesAreRejected() {
        assertNull(FrameRateMatchPolicy.sanitizeContentFrameRate(Float.NaN))
        assertNull(FrameRateMatchPolicy.sanitizeContentFrameRate(0f))
        assertNull(FrameRateMatchPolicy.sanitizeContentFrameRate(500f))
        assertEquals(23.976f, FrameRateMatchPolicy.sanitizeContentFrameRate(23.976f))
    }

    @Test fun twentyFourFpsDoesNotTreatSixtyAsCompatible() {
        assertFalse(FrameRateMatchPolicy.isCompatible(60f, 24f))
        assertTrue(FrameRateMatchPolicy.isCompatible(24f, 24f))
        assertTrue(FrameRateMatchPolicy.isCompatible(48f, 24f))
    }

    @Test fun choosesExactCinemaModeOverHigherMultiples() {
        val modes = listOf(
            current60,
            DisplayModeInfo(2, 3840, 2160, 23.976f),
            DisplayModeInfo(3, 3840, 2160, 47.952f)
        )
        assertEquals(2, FrameRateMatchPolicy.chooseDisplayMode(23.976f, current60, modes)?.id)
    }

    @Test fun choosesFiftyForTwentyFiveFpsWhenExactModeIsMissing() {
        val modes = listOf(current60, DisplayModeInfo(4, 3840, 2160, 50f))
        assertEquals(4, FrameRateMatchPolicy.chooseDisplayMode(25f, current60, modes)?.id)
    }

    @Test fun choosesFiftyNineNinetyFourForTwentyNineNinetySeven() {
        val modes = listOf(current60, DisplayModeInfo(5, 3840, 2160, 60f))
        assertEquals(1, FrameRateMatchPolicy.chooseDisplayMode(29.97f, current60, modes)?.id)
    }

    @Test fun neverChangesResolution() {
        val modes = listOf(
            current60,
            DisplayModeInfo(6, 1920, 1080, 23.976f)
        )
        assertNull(FrameRateMatchPolicy.chooseDisplayMode(23.976f, current60, modes))
    }
}
