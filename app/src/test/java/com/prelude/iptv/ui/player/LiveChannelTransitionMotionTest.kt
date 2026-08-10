package com.prelude.iptv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveChannelTransitionMotionTest {
    @Test fun `next channel refraction travels from right to left`() {
        assertEquals(1f, LiveChannelTransitionMotion.edgeFraction(0f, direction = 1), 0.0001f)
        assertEquals(.5f, LiveChannelTransitionMotion.edgeFraction(.5f, direction = 1), 0.0001f)
        assertEquals(0f, LiveChannelTransitionMotion.edgeFraction(1f, direction = 1), 0.0001f)
    }

    @Test fun `previous channel refraction travels from left to right`() {
        assertEquals(0f, LiveChannelTransitionMotion.edgeFraction(0f, direction = -1), 0.0001f)
        assertEquals(.5f, LiveChannelTransitionMotion.edgeFraction(.5f, direction = -1), 0.0001f)
        assertEquals(1f, LiveChannelTransitionMotion.edgeFraction(1f, direction = -1), 0.0001f)
    }

    @Test fun `transition is transparent at endpoints and strongest at midpoint`() {
        assertEquals(0f, LiveChannelTransitionMotion.intensity(0f), 0.0001f)
        assertEquals(1f, LiveChannelTransitionMotion.intensity(.5f), 0.0001f)
        assertEquals(0f, LiveChannelTransitionMotion.intensity(1f), 0.0001f)
    }

    @Test fun `arbitrary channel steps normalize to two directions`() {
        assertEquals(1, LiveChannelTransitionMotion.direction(4))
        assertEquals(1, LiveChannelTransitionMotion.direction(0))
        assertEquals(-1, LiveChannelTransitionMotion.direction(-3))
    }

    @Test fun `transition waits for a newly rendered frame`() {
        assertEquals(
            false,
            LiveChannelTransitionMotion.hasCommittedFrame(
                framesBeforeOpen = 4,
                renderedFrames = 4,
                hasPlaybackError = false,
            ),
        )
        assertEquals(
            true,
            LiveChannelTransitionMotion.hasCommittedFrame(
                framesBeforeOpen = 4,
                renderedFrames = 5,
                hasPlaybackError = false,
            ),
        )
    }

    @Test fun `failed playback never commits a visual transition`() {
        assertEquals(
            false,
            LiveChannelTransitionMotion.hasCommittedFrame(
                framesBeforeOpen = 4,
                renderedFrames = 5,
                hasPlaybackError = true,
            ),
        )
    }

    @Test fun `TV channel keys preserve opposite transition directions`() {
        assertEquals(1, TvLiveChannelTransitionMotion.direction(1))
        assertEquals(-1, TvLiveChannelTransitionMotion.direction(-1))
        assertEquals(
            1f,
            TvLiveChannelTransitionMotion.edgeFraction(0f, direction = 1),
            0.0001f,
        )
        assertEquals(
            0f,
            TvLiveChannelTransitionMotion.edgeFraction(0f, direction = -1),
            0.0001f,
        )
    }

    @Test fun `TV tuning remains restrained for a large screen`() {
        assertEquals(.010f, TvLiveChannelTransitionMotion.WAVE_AMPLITUDE_FRACTION, 0.0001f)
        assertEquals(.18f, TvLiveChannelTransitionMotion.BAND_WIDTH_FRACTION, 0.0001f)
        assertEquals(.08f, TvLiveChannelTransitionMotion.MAX_DIM_ALPHA, 0.0001f)
        assertEquals(1f, TvLiveChannelTransitionMotion.intensity(.5f), 0.0001f)
    }

    // Regression for the reported pre-effect "flash": a held (not-yet-revealed)
    // request must default to startReveal=false, and at phase 0 (the held
    // state) the outgoing frame's clip already spans the full width in both
    // directions — i.e. the frozen frame fully covers the real surface before
    // any reveal animation starts, instead of leaving it briefly uncovered.
    @Test fun `a held transition request defaults to not revealing yet`() {
        val held = LiveChannelTransitionRequest(
            sequence = 1,
            direction = 1,
            outgoingFrame = null,
        )
        assertEquals(false, held.startReveal)
    }

    @Test fun `outgoing frame fully covers the screen at the held phase`() {
        assertEquals(1f, LiveChannelTransitionMotion.edgeFraction(0f, direction = 1), 0.0001f)
        assertEquals(0f, LiveChannelTransitionMotion.edgeFraction(0f, direction = -1), 0.0001f)
    }
}
