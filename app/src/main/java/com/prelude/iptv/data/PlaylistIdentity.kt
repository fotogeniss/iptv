package com.prelude.iptv.data

import java.security.MessageDigest

/**
 * Stable, non-secret identity for data that must stay isolated per source.
 *
 * The raw provider URL, username or MAC address never becomes a preferences key.
 * Renaming a playlist keeps the same identity, while a different account/source
 * receives a different namespace.
 */
object PlaylistIdentity {
    fun stableId(playlist: Playlist): String {
        val raw = when (playlist.type) {
            PlaylistType.M3U -> "m3u|${playlist.source.trim()}"
            PlaylistType.XTREAM -> "xtream|${playlist.server.trim().trimEnd('/')}|${playlist.username.trim()}"
            PlaylistType.STALKER -> "stalker|${playlist.portal.trim().trimEnd('/')}|${playlist.mac.trim().uppercase()}"
        }
        return sha256(raw).take(32)
    }

    fun digest(value: String): String = sha256(value).take(32)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
