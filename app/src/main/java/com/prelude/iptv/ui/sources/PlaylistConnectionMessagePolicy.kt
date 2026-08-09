package com.prelude.iptv.ui.sources

import java.util.Locale

enum class PlaylistConnectionFailure {
    INVALID_M3U,
    CREDENTIALS_REJECTED,
    SERVER_NOT_FOUND,
    TIMEOUT,
    CONNECTION_REFUSED,
    NO_INTERNET,
    UNKNOWN,
}

/** Classifies provider/library failures without leaking transport text into UI. */
object PlaylistConnectionMessagePolicy {
    fun failure(raw: String?): PlaylistConnectionFailure {
        val message = raw.orEmpty().trim()
        val normalized = message.lowercase(Locale.ROOT)
        return when {
            "δεν μοιάζει με m3u" in normalized || "#extm3u" in normalized ->
                PlaylistConnectionFailure.INVALID_M3U
            "λάθος στοιχεία" in normalized || "unauthorized" in normalized || "forbidden" in normalized ||
                "401" in normalized || "403" in normalized ->
                PlaylistConnectionFailure.CREDENTIALS_REJECTED
            "unknown host" in normalized || "unable to resolve" in normalized || "no address associated" in normalized ->
                PlaylistConnectionFailure.SERVER_NOT_FOUND
            "timed out" in normalized || "timeout" in normalized || "time out" in normalized ->
                PlaylistConnectionFailure.TIMEOUT
            "connection refused" in normalized || "failed to connect" in normalized ->
                PlaylistConnectionFailure.CONNECTION_REFUSED
            "network is unreachable" in normalized || "no internet" in normalized ->
                PlaylistConnectionFailure.NO_INTERNET
            else -> PlaylistConnectionFailure.UNKNOWN
        }
    }
}
