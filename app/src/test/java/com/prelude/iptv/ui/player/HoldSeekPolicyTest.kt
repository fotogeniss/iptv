package com.prelude.iptv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class HoldSeekPolicyTest {
    @Test fun `hold accelerates from x2 to x4 to x8`() {
        assertEquals(2, HoldSeekPolicy.level(0))
        assertEquals(2, HoldSeekPolicy.level(1_999))
        assertEquals(4, HoldSeekPolicy.level(2_000))
        assertEquals(8, HoldSeekPolicy.level(4_000))
    }

    @Test fun `each tick moves proportionally to the level`() {
        assertEquals(1_000L, HoldSeekPolicy.stepMs(500, 100))
        assertEquals(3_000L, HoldSeekPolicy.stepMs(2_500, 100))
        assertEquals(9_000L, HoldSeekPolicy.stepMs(5_000, 100))
    }
}
