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
    val failure: PlaylistSourceSubmissionFailure? = null,
) {
    val successful: Boolean get() = playlist != null
}

sealed interface PlaylistSourceSubmissionFailure {
    data class Connection(val reason: PlaylistConnectionFailure) : PlaylistSourceSubmissionFailure
    data object Preparation : PlaylistSourceSubmissionFailure
}

/**
 * Atomic boundary used by both onboarding surfaces: validate the visible draft,
 * test that exact snapshot, then build the Playlist only after provider success.
 */
suspend fun submitPlaylistSource(
    draft: PlaylistSourceDraft,
    fallbackName: String,
    onStage: (PlaylistSourceSubmissionStage) -> Unit = {},
    tester: suspend (PlaylistSourceDraft) -> PlaylistConnectionTestResult = ::testPlaylistConnection,
): PlaylistSourceSubmissionResult {
    onStage(PlaylistSourceSubmissionStage.VALIDATING)
    PlaylistSourceDraftPolicy.validation(draft)?.let { validation ->
        return PlaylistSourceSubmissionResult(validation = validation)
    }

    val snapshot = PlaylistSourceDraftPolicy.normalized(draft)
    onStage(PlaylistSourceSubmissionStage.CONNECTING)
    val test = tester(snapshot)
    if (!test.successful) return PlaylistSourceSubmissionResult(
        failure = PlaylistSourceSubmissionFailure.Connection(
            test.failure ?: PlaylistConnectionFailure.UNKNOWN
        )
    )

    onStage(PlaylistSourceSubmissionStage.PREPARING)
    val playlist = PlaylistSourceDraftPolicy.build(snapshot, fallbackName)
        ?: return PlaylistSourceSubmissionResult(failure = PlaylistSourceSubmissionFailure.Preparation)
    return PlaylistSourceSubmissionResult(playlist = playlist)
}
