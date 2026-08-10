package com.prelude.iptv.ui.player

import com.prelude.iptv.data.Channel
import com.prelude.iptv.player.PlaybackEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Opens playback and prepares an optional live-channel visual handoff.
 *
 * URL resolution and first-frame confirmation belong together: beginning the
 * visual at the key/swipe callback can let it finish while the provider is still
 * resolving. Mobile and TV therefore share this ordering boundary while keeping
 * separate renderers and input policies.
 *
 * The captured outgoing frame is handed to the caller via [onOutgoingFrameCaptured]
 * as soon as it exists — before URL resolution or `engine.open()` even start, not
 * only once the new stream's first frame is confirmed. The real video surface
 * starts changing the moment `engine.open()` runs, and that raw swap (a black
 * frame, a decoder artifact) was visible for the whole resolve+open+buffer
 * window because the transition overlay previously did not exist yet at that
 * point. The caller is expected to cover the surface with that frozen frame
 * immediately (see [LiveChannelTransitionRequest.startReveal]) and only start
 * animating once this function's result confirms the new frame committed.
 * Ownership of the returned frame's lifecycle (recycling) moves to the caller
 * the moment the callback is invoked.
 */
internal class LiveChannelTransitionCoordinator(
    private val engine: PlaybackEngine,
    private val frameCapture: PlayerVideoFrameCapture,
) {
    suspend fun open(
        channel: Channel,
        isLive: Boolean,
        transitionDirection: Int?,
        resolveUrl: suspend (Channel) -> String,
        loadResumeMs: (Channel) -> Long,
        onOutgoingFrameCaptured: (CapturedVideoFrame) -> Unit = {},
    ): LiveChannelOpenResult {
        if (transitionDirection != null) {
            frameCapture.capture()?.let(onOutgoingFrameCaptured)
        }
        val url = try {
            resolveUrl(channel)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return LiveChannelOpenResult.Failed(error)
        }
        currentCoroutineContext().ensureActive()
        if (url.isBlank()) return LiveChannelOpenResult.Failed()

        val framesBeforeOpen = engine.state.value.renderedFrames
        engine.open(
            url,
            resumeMs = if (isLive) 0L else loadResumeMs(channel),
            live = isLive,
        )
        if (transitionDirection == null) return LiveChannelOpenResult.Opened()

        val committedState = withTimeoutOrNull(
            LiveChannelTransitionMotion.FIRST_FRAME_TIMEOUT_MS
        ) {
            engine.state.first {
                it.renderedFrames > framesBeforeOpen || it.error != null
            }
        }
        currentCoroutineContext().ensureActive()
        val committed = committedState?.let {
            LiveChannelTransitionMotion.hasCommittedFrame(
                framesBeforeOpen = framesBeforeOpen,
                renderedFrames = it.renderedFrames,
                hasPlaybackError = it.error != null,
            )
        } ?: false
        return LiveChannelOpenResult.Opened(transitionCommitted = committed)
    }
}

internal sealed interface LiveChannelOpenResult {
    data class Opened(
        val transitionCommitted: Boolean = false,
    ) : LiveChannelOpenResult

    data class Failed(
        val cause: Throwable? = null,
    ) : LiveChannelOpenResult
}
