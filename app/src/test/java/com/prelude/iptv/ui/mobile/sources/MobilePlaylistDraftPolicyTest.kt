package com.prelude.iptv.ui.mobile.sources

import com.prelude.iptv.data.PlaylistType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePlaylistDraftPolicyTest {
    @Test fun formatsMacWhileUserTypes() {
        assertEquals("00:1A:79:AB:CD:EF", MobilePlaylistDraftPolicy.formatMac("001a79ab-cd ef"))
    }

    @Test fun buildsM3uUrlPlaylist() {
        val playlist = MobilePlaylistDraftPolicy.build(
            MobilePlaylistDraft(
                method = MobilePlaylistMethod.URL,
                playlistUrl = "https://example.com/list.m3u",
                name = "Main",
            ),
        )
        assertEquals(PlaylistType.M3U, playlist?.type)
        assertEquals("Main", playlist?.name)
        assertTrue(playlist?.isUrl == true)
    }

    @Test fun xtreamRequiresAllCredentials() {
        val error = MobilePlaylistDraftPolicy.validationMessage(
            MobilePlaylistDraft(
                method = MobilePlaylistMethod.XTREAM,
                server = "https://example.com",
                username = "demo",
            ),
        )
        assertEquals("Συμπλήρωσε το password.", error)
    }

    @Test fun buildsNormalizedStalkerPlaylist() {
        val playlist = MobilePlaylistDraftPolicy.build(
            MobilePlaylistDraft(
                method = MobilePlaylistMethod.MAC,
                portal = "https://portal.example.com/c/",
                macAddress = "001a79abcdef",
            ),
        )
        assertEquals(PlaylistType.STALKER, playlist?.type)
        assertEquals("00:1A:79:AB:CD:EF", playlist?.mac)
    }

    @Test fun localFileRequiresImportedPathAndBuildsNonUrlM3u() {
        assertTrue(
            MobilePlaylistDraftPolicy.validationMessage(
                MobilePlaylistDraft(method = MobilePlaylistMethod.FILE),
            )?.isNotBlank() == true,
        )
        val playlist = MobilePlaylistDraftPolicy.build(
            MobilePlaylistDraft(
                method = MobilePlaylistMethod.FILE,
                filePath = "C:/app/files/playlists/list.m3u",
                fileLabel = "list.m3u",
            ),
        )
        assertNull(MobilePlaylistDraftPolicy.validationMessage(
            MobilePlaylistDraft(method = MobilePlaylistMethod.FILE, filePath = "C:/app/files/playlists/list.m3u"),
        ))
        assertEquals(PlaylistType.M3U, playlist?.type)
        assertEquals(false, playlist?.isUrl)
        assertEquals("list.m3u", playlist?.name)
    }
}
