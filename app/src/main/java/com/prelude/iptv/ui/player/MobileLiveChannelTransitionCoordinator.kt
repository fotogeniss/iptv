package com.prelude.iptv.ui.player

import com.prelude.iptv.data.Channel
import com.prelude.iptv.player.PlaybackEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Opens mobile playback and prepares the optional live-channel visual handoff.
 *
 * URL resolution and first-frame confirmation belong together: starting the
 * visual transition at either the swipe callback or target publication lets it
 * finish while the provider is still resolving. This coordinator keeps that
 * ordering out of the already large playback overlay.
 */
internal class MobileLiveChannelTransitionCoordinator(
    private val engine: PlaybackEngine,
    private val frameCapture: PlayerVideoFrameCapture,
) {
    suspend fun open(
        channel: Channel,
        isLive: Boolean,
        transitionDirection: Int?,
        resolveUrl: suspend (Channel) -> String,
        loadResumeMs: (Channel) -> Long,
    ): LiveChannelOpenResult {
        var outgoingFrame = transitionDirection?.let { frameCapture.capture() }
        var frameHandedOff = false
        try {
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
            val prepared = committedState?.takeIf {
                LiveChannelTransitionMotion.hasCommittedFrame(
                    framesBeforeOpen = framesBeforeOpen,
                    renderedFrames = it.renderedFrames,
                    hasPlaybackError = it.error != null,
                )
            }?.let {
                PreparedLiveChannelTransition(
                    direction = transitionDirection,
                    outgoingFrame = outgoingFrame,
                )
            }
            if (prepared != null) {
                outgoingFrame = null
                frameHandedOff = true
            }
            return LiveChannelOpenResult.Opened(prepared)
        } finally {
            if (!frameHandedOff) outgoingFrame?.recycle()
        }
    }
}

internal sealed interface LiveChannelOpenResult {
    data class Opened(
        val transition: PreparedLiveChannelTransition? = null,
    ) : LiveChannelOpenResult

    data class Failed(
        val cause: Throwable? = null,
    ) : LiveChannelOpenResult
}

internal data class PreparedLiveChannelTransition(
    val direction: Int,
    val outgoingFrame: CapturedVideoFrame?,
)
