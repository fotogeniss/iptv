package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.Channel
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportRelayCoordinatorTest {
    @Test fun `exportable channels exclude series containers`() {
        val live = Channel(name = "Live")
        val movie = Channel(name = "Movie", kind = "vod")
        val series = Channel(name = "Series", kind = "series")

        val result = coordinator(listOf(live, movie, series)).exportableChannels()

        assertEquals(listOf(live, movie), result)
    }

    @Test fun `resolved M3U skips failures and cleans quoted attributes`() = runTest {
        val good = Channel(
            name = "News",
            group = "World \"News\"",
            logo = "https://logo.example/\"one\".png",
            tvgId = "news\"1",
        )
        val broken = Channel(name = "Broken")
        val result = coordinator(listOf(good, broken)) { channel ->
            if (channel == broken) throw IOException("offline")
            "https://stream.example/news"
        }.buildResolvedM3u(listOf(good, broken))

        assertTrue(result.startsWith("#EXTM3U\n"))
        assertTrue(result.contains("tvg-id=\"news1\""))
        assertTrue(result.contains("group-title=\"World News\",News"))
        assertTrue(result.contains("https://stream.example/news"))
        assertFalse(result.contains("Broken"))
    }

    private fun coordinator(
        channels: List<Channel>,
        resolver: suspend (Channel) -> String = { it.url },
    ) = ExportRelayCoordinator(
        currentStalker = { null },
        currentChannels = { channels },
        resolvePlayableUrl = resolver,
        startRelayService = {},
        stopRelayService = {},
        publishRelayState = { _, _ -> },
    )
}
