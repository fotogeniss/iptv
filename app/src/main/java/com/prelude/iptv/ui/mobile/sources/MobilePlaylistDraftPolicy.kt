package com.prelude.iptv.ui.mobile.sources

import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistType
import java.net.URI

enum class MobilePlaylistMethod {
    URL,
    XTREAM,
    MAC,
    FILE,
}

data class MobilePlaylistDraft(
    val method: MobilePlaylistMethod = MobilePlaylistMethod.URL,
    val playlistUrl: String = "",
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val portal: String = "",
    val macAddress: String = "",
    val userAgent: String = "",
    val filePath: String = "",
    val fileLabel: String = "",
    val name: String = "",
    val epgUrl: String = "",
)

object MobilePlaylistDraftPolicy {
    private val macPattern = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")

    fun methodForInitialTab(initialTab: Int): MobilePlaylistMethod = when (initialTab) {
        1 -> MobilePlaylistMethod.XTREAM
        2 -> MobilePlaylistMethod.MAC
        3 -> MobilePlaylistMethod.FILE
        else -> MobilePlaylistMethod.URL
    }

    fun formatMac(raw: String): String {
        val hex = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(12)
        return hex.chunked(2).joinToString(":")
    }

    fun validationMessage(draft: MobilePlaylistDraft): String? = when (draft.method) {
        MobilePlaylistMethod.URL -> when {
            draft.playlistUrl.isBlank() -> "Συμπλήρωσε το URL της λίστας."
            !isHttpUrl(draft.playlistUrl) -> "Το URL της λίστας δεν είναι έγκυρο."
            else -> null
        }

        MobilePlaylistMethod.XTREAM -> when {
            draft.server.isBlank() -> "Συμπλήρωσε το Server URL."
            !isHttpUrl(draft.server) -> "Το Server URL δεν είναι έγκυρο."
            draft.username.isBlank() -> "Συμπλήρωσε το username."
            draft.password.isBlank() -> "Συμπλήρωσε το password."
            else -> null
        }

        MobilePlaylistMethod.MAC -> when {
            draft.portal.isBlank() -> "Συμπλήρωσε το Portal URL."
            !isHttpUrl(draft.portal) -> "Το Portal URL δεν είναι έγκυρο."
            !macPattern.matches(formatMac(draft.macAddress)) ->
                "Η MAC address πρέπει να έχει μορφή 00:1A:79:00:00:00."
            else -> null
        }

        MobilePlaylistMethod.FILE -> when {
            draft.filePath.isBlank() -> "Διάλεξε ένα αρχείο M3U από τη συσκευή."
            else -> null
        }
    }

    fun build(draft: MobilePlaylistDraft): Playlist? {
        if (validationMessage(draft) != null) return null
        val epg = draft.epgUrl.trim().takeIf(String::isNotBlank).orEmpty()
        return when (draft.method) {
            MobilePlaylistMethod.URL -> {
                val source = draft.playlistUrl.trim()
                Playlist(
                    name = draft.name.trim().ifBlank {
                        source.substringBefore('?').substringAfterLast('/').ifBlank { "Playlist" }
                    },
                    type = PlaylistType.M3U,
                    source = source,
                    isUrl = true,
                    epgUrl = epg,
                )
            }

            MobilePlaylistMethod.XTREAM -> Playlist(
                name = draft.name.trim().ifBlank { "Xtream ${draft.username.trim()}" },
                type = PlaylistType.XTREAM,
                server = draft.server.trim().trimEnd('/'),
                username = draft.username.trim(),
                password = draft.password,
                epgUrl = epg,
            )

            MobilePlaylistMethod.MAC -> {
                val normalizedMac = formatMac(draft.macAddress)
                Playlist(
                    name = draft.name.trim().ifBlank { "MAC ${normalizedMac.takeLast(8)}" },
                    type = PlaylistType.STALKER,
                    portal = draft.portal.trim(),
                    mac = normalizedMac,
                    userAgent = draft.userAgent.trim(),
                    epgUrl = epg,
                )
            }

            MobilePlaylistMethod.FILE -> Playlist(
                name = draft.name.trim().ifBlank { draft.fileLabel.ifBlank { "Τοπικό M3U" } },
                type = PlaylistType.M3U,
                source = draft.filePath,
                isUrl = false,
            )
        }
    }

    private fun isHttpUrl(value: String): Boolean = runCatching {
        val parsed = URI(value.trim())
        (parsed.scheme.equals("http", ignoreCase = true) ||
            parsed.scheme.equals("https", ignoreCase = true)) &&
            !parsed.host.isNullOrBlank()
    }.getOrDefault(false)
}
