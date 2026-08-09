package com.prelude.iptv.ui.sources

import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistType
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

enum class PlaylistSourceMethod {
    URL,
    XTREAM,
    MAC,
    FILE,
}

data class PlaylistSourceDraft(
    val method: PlaylistSourceMethod = PlaylistSourceMethod.URL,
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

enum class PlaylistSourceField {
    PLAYLIST_URL,
    SERVER,
    USERNAME,
    PASSWORD,
    PORTAL,
    MAC_ADDRESS,
    FILE,
}

data class PlaylistSourceValidation(
    val field: PlaylistSourceField,
    val reason: PlaylistSourceValidationReason,
)

enum class PlaylistSourceValidationReason {
    PLAYLIST_URL_REQUIRED,
    PLAYLIST_URL_INVALID,
    SERVER_REQUIRED,
    SERVER_INVALID,
    USERNAME_REQUIRED,
    PASSWORD_REQUIRED,
    PORTAL_REQUIRED,
    PORTAL_INVALID,
    MAC_INVALID,
    FILE_REQUIRED,
}

data class PlaylistSourceDetection(
    val draft: PlaylistSourceDraft,
    val kind: PlaylistSourceDetectionKind,
)

enum class PlaylistSourceDetectionKind { PORTAL_MAC, XTREAM, PLAYLIST_URL }

/**
 * Pure source-onboarding rules shared by touch and DPAD surfaces.
 *
 * This boundary deliberately owns credential detection, normalization,
 * field-specific validation and Playlist construction. UI code never parses
 * pasted provider text and the network tester never guesses incomplete data.
 */
object PlaylistSourceDraftPolicy {
    private val macPattern = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")
    private val macInText = Regex("(?i)(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}")
    private val httpUrlInText = Regex("(?i)https?://[^\\s<>\"]+")
    private val usernameLabel = Regex("(?im)^(?:user(?:name)?|όνομα\\s+χρήστη)\\s*[:=]\\s*(\\S+)\\s*$")
    private val passwordLabel = Regex("(?im)^(?:pass(?:word)?|κωδικός)\\s*[:=]\\s*(\\S+)\\s*$")

    fun methodForInitialTab(initialTab: Int): PlaylistSourceMethod = when (initialTab) {
        1 -> PlaylistSourceMethod.XTREAM
        2 -> PlaylistSourceMethod.MAC
        3 -> PlaylistSourceMethod.FILE
        else -> PlaylistSourceMethod.URL
    }

    fun formatMac(raw: String): String {
        val hex = raw.uppercase(Locale.ROOT).filter { it in '0'..'9' || it in 'A'..'F' }.take(12)
        return hex.chunked(2).joinToString(":")
    }

    /** Adds the compatibility scheme many providers omit when copying a host. */
    fun normalizeHttpUrl(raw: String): String {
        val value = raw.trim()
        if (value.isBlank() || value.any(Char::isWhitespace)) return value
        return if ("://" in value) value else "http://$value"
    }

    fun normalized(draft: PlaylistSourceDraft): PlaylistSourceDraft = draft.copy(
        playlistUrl = normalizeHttpUrl(draft.playlistUrl),
        server = normalizeHttpUrl(draft.server).trimEnd('/'),
        portal = normalizeHttpUrl(draft.portal),
        macAddress = formatMac(draft.macAddress),
        username = draft.username.trim(),
        userAgent = draft.userAgent.trim(),
        name = draft.name.trim(),
        epgUrl = draft.epgUrl.trim(),
    )

    fun validation(draft: PlaylistSourceDraft): PlaylistSourceValidation? {
        val value = normalized(draft)
        return when (value.method) {
            PlaylistSourceMethod.URL -> when {
                value.playlistUrl.isBlank() -> invalid(PlaylistSourceField.PLAYLIST_URL, PlaylistSourceValidationReason.PLAYLIST_URL_REQUIRED)
                !isHttpUrl(value.playlistUrl) -> invalid(PlaylistSourceField.PLAYLIST_URL, PlaylistSourceValidationReason.PLAYLIST_URL_INVALID)
                else -> null
            }

            PlaylistSourceMethod.XTREAM -> when {
                value.server.isBlank() -> invalid(PlaylistSourceField.SERVER, PlaylistSourceValidationReason.SERVER_REQUIRED)
                !isHttpUrl(value.server) -> invalid(PlaylistSourceField.SERVER, PlaylistSourceValidationReason.SERVER_INVALID)
                value.username.isBlank() -> invalid(PlaylistSourceField.USERNAME, PlaylistSourceValidationReason.USERNAME_REQUIRED)
                value.password.isBlank() -> invalid(PlaylistSourceField.PASSWORD, PlaylistSourceValidationReason.PASSWORD_REQUIRED)
                else -> null
            }

            PlaylistSourceMethod.MAC -> when {
                value.portal.isBlank() -> invalid(PlaylistSourceField.PORTAL, PlaylistSourceValidationReason.PORTAL_REQUIRED)
                !isHttpUrl(value.portal) -> invalid(PlaylistSourceField.PORTAL, PlaylistSourceValidationReason.PORTAL_INVALID)
                !macPattern.matches(value.macAddress) -> invalid(
                    PlaylistSourceField.MAC_ADDRESS,
                    PlaylistSourceValidationReason.MAC_INVALID,
                )
                else -> null
            }

            PlaylistSourceMethod.FILE -> when {
                value.filePath.isBlank() -> invalid(PlaylistSourceField.FILE, PlaylistSourceValidationReason.FILE_REQUIRED)
                else -> null
            }
        }
    }

    fun build(draft: PlaylistSourceDraft, fallbackName: String): Playlist? {
        val value = normalized(draft)
        if (validation(value) != null) return null
        val epg = value.epgUrl.takeIf(String::isNotBlank).orEmpty()
        return when (value.method) {
            PlaylistSourceMethod.URL -> Playlist(
                name = value.name.ifBlank {
                    value.playlistUrl.substringBefore('?').substringAfterLast('/').ifBlank { fallbackName }
                },
                type = PlaylistType.M3U,
                source = value.playlistUrl,
                isUrl = true,
                epgUrl = epg,
            )

            PlaylistSourceMethod.XTREAM -> Playlist(
                name = value.name.ifBlank { "Xtream ${value.username}" },
                type = PlaylistType.XTREAM,
                server = value.server,
                username = value.username,
                password = value.password,
                epgUrl = epg,
            )

            PlaylistSourceMethod.MAC -> Playlist(
                name = value.name.ifBlank { "MAC ${value.macAddress.takeLast(8)}" },
                type = PlaylistType.STALKER,
                portal = value.portal,
                mac = value.macAddress,
                userAgent = value.userAgent,
                epgUrl = epg,
            )

            PlaylistSourceMethod.FILE -> Playlist(
                name = value.name.ifBlank { value.fileLabel.ifBlank { fallbackName } },
                type = PlaylistType.M3U,
                source = value.filePath,
                isUrl = false,
            )
        }
    }

    fun detect(raw: String): PlaylistSourceDetection? {
        val text = raw.trim().replace("&amp;", "&", ignoreCase = true)
        if (text.isBlank()) return null
        val explicitUrl = httpUrlInText.find(text)?.value?.trimEnd('.', ',', ';', ')', ']')
        val url = explicitUrl ?: text.takeIf { value -> value.none(Char::isWhitespace) }
            ?.let(::normalizeHttpUrl)?.takeIf(::isHttpUrl)
        val mac = macInText.find(text)?.value?.let(::formatMac)

        if (url != null && mac != null) {
            return PlaylistSourceDetection(
                PlaylistSourceDraft(method = PlaylistSourceMethod.MAC, portal = url, macAddress = mac),
                PlaylistSourceDetectionKind.PORTAL_MAC,
            )
        }

        if (url != null) {
            val query = queryValues(url)
            val username = query["username"] ?: usernameLabel.find(text)?.groupValues?.getOrNull(1)
            val password = query["password"] ?: passwordLabel.find(text)?.groupValues?.getOrNull(1)
            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                return PlaylistSourceDetection(
                    PlaylistSourceDraft(
                        method = PlaylistSourceMethod.XTREAM,
                        server = serverRoot(url),
                        username = username,
                        password = password,
                    ),
                    PlaylistSourceDetectionKind.XTREAM,
                )
            }
            return PlaylistSourceDetection(
                PlaylistSourceDraft(method = PlaylistSourceMethod.URL, playlistUrl = url),
                PlaylistSourceDetectionKind.PLAYLIST_URL,
            )
        }

        val lines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        if (lines.size >= 3) {
            val server = normalizeHttpUrl(lines[0])
            if (isHttpUrl(server)) {
                return PlaylistSourceDetection(
                    PlaylistSourceDraft(
                        method = PlaylistSourceMethod.XTREAM,
                        server = server,
                        username = lines[1],
                        password = lines[2],
                    ),
                    PlaylistSourceDetectionKind.XTREAM,
                )
            }
        }
        return null
    }

    private fun invalid(field: PlaylistSourceField, reason: PlaylistSourceValidationReason) =
        PlaylistSourceValidation(field, reason)

    private fun isHttpUrl(value: String): Boolean = runCatching {
        val parsed = URI(value)
        parsed.scheme.equals("http", ignoreCase = true) || parsed.scheme.equals("https", ignoreCase = true)
    }.mapCatching { validScheme ->
        validScheme && !URI(value).host.isNullOrBlank()
    }.getOrDefault(false)

    private fun serverRoot(url: String): String = runCatching {
        val parsed = URI(url)
        URI(parsed.scheme, parsed.userInfo, parsed.host, parsed.port, null, null, null).toString().trimEnd('/')
    }.getOrDefault(url.substringBefore('?').trimEnd('/'))

    private fun queryValues(url: String): Map<String, String> = runCatching {
        URI(url).rawQuery.orEmpty().split('&').mapNotNull { part ->
            val pieces = part.split('=', limit = 2)
            if (pieces.size != 2) null else {
                val key = URLDecoder.decode(pieces[0], StandardCharsets.UTF_8.name()).lowercase()
                val value = URLDecoder.decode(pieces[1], StandardCharsets.UTF_8.name())
                key to value
            }
        }.toMap()
    }.getOrDefault(emptyMap())
}
