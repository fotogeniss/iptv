package com.prelude.iptv.ui.sources

import com.prelude.iptv.data.Repository
import com.prelude.iptv.source.StalkerClient
import com.prelude.iptv.source.XtreamClient
import com.prelude.iptv.ui.mobile.sources.MobilePlaylistDraft
import com.prelude.iptv.ui.mobile.sources.MobilePlaylistDraftPolicy
import com.prelude.iptv.ui.mobile.sources.MobilePlaylistMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlaylistConnectionTestResult(
    val successful: Boolean,
    val message: String,
)

/**
 * Runs the same provider-level connection check for the mobile and TV forms.
 * Validation happens before network or disk access so the result always refers
 * to a complete, currently visible draft.
 */
suspend fun testPlaylistConnection(draft: MobilePlaylistDraft): PlaylistConnectionTestResult {
    MobilePlaylistDraftPolicy.validationMessage(draft)?.let { validation ->
        return PlaylistConnectionTestResult(false, validation)
    }

    return withContext(Dispatchers.IO) {
        try {
            val result = when (draft.method) {
                MobilePlaylistMethod.URL -> Repository.testM3u(
                    draft.playlistUrl.trim(),
                    isUrl = true,
                )

                MobilePlaylistMethod.XTREAM -> XtreamClient.test(
                    draft.server.trim(),
                    draft.username.trim(),
                    draft.password,
                )

                MobilePlaylistMethod.MAC -> StalkerClient(
                    draft.portal.trim(),
                    MobilePlaylistDraftPolicy.formatMac(draft.macAddress),
                    draft.userAgent.trim(),
                ).testConnection()

                MobilePlaylistMethod.FILE -> Repository.testM3u(
                    draft.filePath,
                    isUrl = false,
                )
            }
            PlaylistConnectionTestResult(result.first, result.second)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            PlaylistConnectionTestResult(
                successful = false,
                message = error.message ?: "Η δοκιμή σύνδεσης απέτυχε.",
            )
        }
    }
}
