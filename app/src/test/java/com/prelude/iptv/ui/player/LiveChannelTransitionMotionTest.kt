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
}
