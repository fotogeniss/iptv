package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.data.SourceLoadProgress
import com.prelude.iptv.ui.epg.EpgSourceKind
import com.prelude.iptv.ui.epg.EpgSourceOption
import com.prelude.iptv.ui.epg.EpgStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UiStateSlicesTest {

    private val source = Playlist("Test", PlaylistType.M3U, source = "https://example/list.m3u")

    @Test
    fun `catalog progress does not invalidate catalog content slice`() {
        val base = UiState(channels = listOf(Channel("Movie", kind = "vod")), loading = true)
        val updated = base.copy(
            sourceProgress = mapOf("source" to SourceLoadProgress(42, "Parsing", "vod", true))
        )

        assertEquals(base.toCatalogUiState(), updated.toCatalogUiState())
        assertNotEquals(base.toCatalogProgressUiState(), updated.toCatalogProgressUiState())
    }

    @Test
    fun `catalog changes do not invalidate app shell slice`() {
        val base = UiState(playlists = listOf(source), currentIndex = 0, fontScale = 1.1f)
        val updated = base.copy(
            channels = listOf(Channel("Live", kind = "live")),
            status = "Loaded",
            favorites = setOf("live-key")
        )

        assertEquals(base.toAppShellUiState(), updated.toAppShellUiState())
        assertNotEquals(base.toCatalogUiState(), updated.toCatalogUiState())
    }

    @Test
    fun `epg changes are isolated from settings slice`() {
        val base = UiState(playlists = listOf(source), channels = listOf(Channel("One")))
        val updated = base.copy(
            epgLoaded = true,
            epgSources = listOf(EpgSourceOption(EpgSourceKind.XtreamProvider, "https://example/guide.xml")),
            epgStatus = EpgStatus.Ready(matches = 1),
        )

        assertEquals(base.toSettingsUiState(), updated.toSettingsUiState())
        assertEquals(
            base.toCatalogUiState(),
            base.copy(epgStatus = EpgStatus.Ready(matches = 1)).toCatalogUiState(),
        )
        assertNotEquals(base.toEpgUiState(), updated.toEpgUiState())
    }

    @Test
    fun `export slice changes only with favorites or relay state`() {
        val base = UiState(favorites = setOf("a"))
        assertEquals(base.toExportUiState(), base.copy(status = "Other update").toExportUiState())
        assertNotEquals(base.toExportUiState(), base.copy(favorites = setOf("a", "b")).toExportUiState())
        assertNotEquals(base.toExportUiState(), base.copy(relayRunning = true, relayUrl = "http://127.0.0.1:8080/list.m3u").toExportUiState())
    }
}
