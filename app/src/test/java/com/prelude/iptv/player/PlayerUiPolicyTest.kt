package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerUiPolicyTest {
    @Test fun progressIsClamped() {
        assertEquals(0, PlayerUiPolicy.liveProgress(100, 200, 50))
        assertEquals(500, PlayerUiPolicy.liveProgress(100, 200, 150))
        assertEquals(1000, PlayerUiPolicy.liveProgress(100, 200, 250))
    }

    @Test fun invalidProgrammeHasZeroProgress() {
        assertEquals(0, PlayerUiPolicy.liveProgress(200, 100, 150))
    }

    @Test fun tvControlsRemainVisibleLonger() {
        assertEquals(5_500L, PlayerUiPolicy.autoHideMs(isTv = true, userSeeking = false))
        assertEquals(4_000L, PlayerUiPolicy.autoHideMs(isTv = false, userSeeking = false))
        assertEquals(12_000L, PlayerUiPolicy.autoHideMs(isTv = false, userSeeking = true))
    }
}
