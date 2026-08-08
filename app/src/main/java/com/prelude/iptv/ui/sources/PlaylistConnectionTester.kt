package com.prelude.iptv.ui.sources

import com.prelude.iptv.data.Repository
import com.prelude.iptv.source.StalkerClient
import com.prelude.iptv.source.XtreamClient
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
suspend fun testPlaylistConnection(draft: PlaylistSourceDraft): PlaylistConnectionTestResult {
    PlaylistSourceDraftPolicy.validationMessage(draft)?.let { validation ->
        return PlaylistConnectionTestResult(false, validation)
    }

    return withContext(Dispatchers.IO) {
        try {
            val result = when (draft.method) {
                PlaylistSourceMethod.URL -> Repository.testM3u(
                    draft.playlistUrl.trim(),
                    isUrl = true,
                )

                PlaylistSourceMethod.XTREAM -> XtreamClient.test(
                    draft.server.trim(),
                    draft.username.trim(),
                    draft.password,
                )

                PlaylistSourceMethod.MAC -> StalkerClient(
                    draft.portal.trim(),
                    PlaylistSourceDraftPolicy.formatMac(draft.macAddress),
                    draft.userAgent.trim(),
                ).testConnection()

                PlaylistSourceMethod.FILE -> Repository.testM3u(
                    draft.filePath,
                    isUrl = false,
                )
            }
            PlaylistConnectionTestResult(
                successful = result.first,
                message = if (result.first) result.second else PlaylistConnectionMessagePolicy.failure(result.second),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            PlaylistConnectionTestResult(
                successful = false,
                message = PlaylistConnectionMessagePolicy.failure(error.message),
            )
        }
    }
}
