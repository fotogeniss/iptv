package com.prelude.iptv.player

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.SubtitleSearchRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLaunchRequestTest {
    @Test
    fun channelRoutePreservesSourceMetadataAndSubtitleIdentity() {
        val channel = Channel(
            name = "Example",
            kind = "series_ep",
            tvgId = "example.gr",
            logo = "https://img/logo.png",
            plot = "fallback plot",
            year = "2025",
        )
        val subtitle = SubtitleSearchRequest("Example", 2025, 2, 4, "episode")

        val request = PlayerLaunchRequest.forChannel(
            url = "https://stream/play.m3u8",
            channel = channel,
            sourceId = "source-1",
            positionKey = "episode-key",
            subtitle = subtitle,
            metadata = mapOf(PlayerLaunchRequest.EXTRA_PLOT to "resolved plot"),
        )

        assertEquals("source-1", request.sourceId)
        assertEquals("episode-key", request.positionKey)
        assertEquals("example.gr", request.tvgId)
        assertEquals("resolved plot", request.plot)
        assertEquals(subtitle, request.subtitle)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankUrlIsRejectedAtTheRouteBoundary() {
        PlayerLaunchRequest(url = "   ", title = "Invalid")
    }
}
