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
}
