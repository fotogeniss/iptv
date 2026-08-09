package com.prelude.iptv.ui.components.settings

import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.data.SourceLoadProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSourcePolicyTest {
    @Test
    fun sourcePresentationKeepsDisplayCopyOutOfThePureModel() {
        val remote = Playlist(
            name = "Provider name",
            type = PlaylistType.XTREAM,
            server = "https://user:secret@provider.example:8080/path",
        )
        val source = buildSettingsSources(listOf(remote), currentIndex = 0, currentChannelCount = 12).single()

        assertEquals("Provider name", source.name)
        assertEquals("provider.example:8080", source.endpoint)
        assertFalse(source.local)
        assertEquals(SettingsSourceStatus.Active, source.status)
    }

    @Test
    fun localAndLoadingStateRemainTypedForTheAndroidResourceBoundary() {
        val local = Playlist(name = "", type = PlaylistType.M3U, source = "C:/playlist.m3u", isUrl = false)
        val progress = SourceLoadProgress(percent = 40, stage = "provider stage", active = true)
        val source = buildSettingsSources(
            playlists = listOf(local),
            currentIndex = 0,
            currentChannelCount = 0,
            sourceProgress = mapOf(PlaylistIdentity.stableId(local) to progress),
        ).single()

        assertEquals("", source.name)
        assertEquals("", source.endpoint)
        assertTrue(source.local)
        assertEquals(SettingsSourceStatus.Loading, source.status)
        assertEquals("provider stage", source.progressStage)
    }
}
