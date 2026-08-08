package com.prelude.iptv.tvhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvHomePlaybackRoutePolicyTest {
    private val token = "123e4567-e89b-12d3-a456-426614174000"

    @Test fun `accepts exact play next route`() {
        assertEquals(
            TvHomePlaybackRoute("play-next", token),
            TvHomePlaybackRoutePolicy.parse("upl", "play-next", listOf(token))
        )
    }

    @Test fun `accepts exact my list route`() {
        assertEquals(
            TvHomePlaybackRoute("my-list", token),
            TvHomePlaybackRoutePolicy.parse("upl", "my-list", listOf(token))
        )
    }

    @Test fun `rejects unsupported scheme and host`() {
        assertNull(TvHomePlaybackRoutePolicy.parse("https", "play-next", listOf(token)))
        assertNull(TvHomePlaybackRoutePolicy.parse("upl", "other", listOf(token)))
    }

    @Test fun `rejects extra path segments`() {
        assertNull(TvHomePlaybackRoutePolicy.parse("upl", "play-next", listOf(token, "extra")))
    }

    @Test fun `rejects non uuid and oversized token`() {
        assertNull(TvHomePlaybackRoutePolicy.parse("upl", "play-next", listOf("not-a-token")))
        assertNull(TvHomePlaybackRoutePolicy.parse("upl", "play-next", listOf(token + "x".repeat(200))))
    }
    @Test fun `rejects non canonical token formatting`() {
        assertNull(TvHomePlaybackRoutePolicy.parse("upl", "play-next", listOf(token.uppercase())))
        assertNull(TvHomePlaybackRoutePolicy.parse("upl", "play-next", listOf(" $token")))
        assertNull(TvHomePlaybackRoutePolicy.parse("upl", "play-next", listOf("$token ")))
    }

}
