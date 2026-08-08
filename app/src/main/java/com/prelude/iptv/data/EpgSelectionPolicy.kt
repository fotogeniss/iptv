package com.prelude.iptv.data

import java.net.URI

/**
 * Pure policy for user-selected EPG sources.
 *
 * Only remote HTTP(S) guides are accepted. A loaded guide may be persisted for
 * a playlist only while the same source is still active and the loaded URL is
 * exactly the URL that the user requested.
 */
object EpgSelectionPolicy {
    fun normalizeRemoteUrl(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return value
    }

    fun shouldCommit(
        requestSourceId: String,
        currentSourceId: String,
        requestedUrl: String,
        loadedUrl: String?
    ): Boolean {
        if (requestSourceId.isBlank() || requestSourceId != currentSourceId) return false
        val requested = normalizeRemoteUrl(requestedUrl) ?: return false
        val loaded = normalizeRemoteUrl(loadedUrl.orEmpty()) ?: return false
        return requested == loaded
    }
}
