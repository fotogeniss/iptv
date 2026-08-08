package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaylistIdentityTest {
    @Test fun `renaming a source keeps its history namespace`() {
        val first = Playlist("Old name", PlaylistType.XTREAM, server = "https://provider.test/", username = "user")
        val renamed = first.copy(name = "New name")
        assertEquals(PlaylistIdentity.stableId(first), PlaylistIdentity.stableId(renamed))
    }

    @Test fun `different accounts on the same server stay isolated`() {
        val a = Playlist("A", PlaylistType.XTREAM, server = "https://provider.test", username = "user-a")
        val b = Playlist("B", PlaylistType.XTREAM, server = "https://provider.test", username = "user-b")
        assertNotEquals(PlaylistIdentity.stableId(a), PlaylistIdentity.stableId(b))
    }

    @Test fun `different m3u urls stay isolated and raw url is not exposed`() {
        val url = "https://provider.test/list.m3u?token=secret"
        val id = PlaylistIdentity.stableId(Playlist("A", PlaylistType.M3U, source = url))
        assertNotEquals(id, PlaylistIdentity.stableId(Playlist("B", PlaylistType.M3U, source = "$url-2")))
        assertFalse(id.contains("secret"))
        assertEquals(32, id.length)
    }
}
