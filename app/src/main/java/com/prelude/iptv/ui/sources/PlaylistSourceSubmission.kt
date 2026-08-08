package com.prelude.iptv.ui.sources

import com.prelude.iptv.data.Playlist

enum class PlaylistSourceSubmissionStage {
    VALIDATING,
    CONNECTING,
    PREPARING,
}

data class PlaylistSourceSubmissionResult(
    val playlist: Playlist? = null,
    val validation: PlaylistSourceValidation? = null,
    val message: String,
) {
    val successful: Boolean get() = playlist != null
}

/**
 * Atomic boundary used by both onboarding surfaces: validate the visible draft,
 * test that exact snapshot, then build the Playlist only after provider success.
 */
suspend fun submitPlaylistSource(
    draft: PlaylistSourceDraft,
    onStage: (PlaylistSourceSubmissionStage) -> Unit = {},
    tester: suspend (PlaylistSourceDraft) -> PlaylistConnectionTestResult = ::testPlaylistConnection,
): PlaylistSourceSubmissionResult {
    onStage(PlaylistSourceSubmissionStage.VALIDATING)
    PlaylistSourceDraftPolicy.validation(draft)?.let { validation ->
        return PlaylistSourceSubmissionResult(validation = validation, message = validation.message)
    }

    val snapshot = PlaylistSourceDraftPolicy.normalized(draft)
    onStage(PlaylistSourceSubmissionStage.CONNECTING)
    val test = tester(snapshot)
    if (!test.successful) return PlaylistSourceSubmissionResult(message = test.message)

    onStage(PlaylistSourceSubmissionStage.PREPARING)
    val playlist = PlaylistSourceDraftPolicy.build(snapshot)
        ?: return PlaylistSourceSubmissionResult(message = "Δεν ήταν δυνατή η προετοιμασία της πηγής.")
    return PlaylistSourceSubmissionResult(playlist = playlist, message = test.message)
}
