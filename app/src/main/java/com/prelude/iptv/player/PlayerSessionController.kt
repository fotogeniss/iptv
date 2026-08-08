package com.prelude.iptv.player

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.SubtitleSearchPolicy
import com.prelude.iptv.data.SubtitleSearchRequest

/**
 * Owns the identity of the content shown by PlayerActivity and the generation of
 * asynchronous channel changes.
 *
 * The controller deliberately separates the last committed playback session
 * from a pending zap. While a provider resolves the target URL, the old stream
 * remains committed and playing, but the UI may present the target channel.
 * A failed or stale resolve always returns to the last *committed* channel — not
 * to another unresolved intermediate selection.
 */
class PlayerSessionController(
    initialState: PlayerSessionState,
    val sourceId: String,
    private val queue: PlayerSessionQueue,
) {
    init {
        require(initialState.playbackUrl.isNotBlank()) { "Initial playback URL must not be blank" }
    }

    private var committedState: PlayerSessionState = initialState
    private var committedQueueIndex: Int = queue.index.coerceInQueue(queue.size)
    private var pendingTargetIndex: Int? = null
    private var requestGeneration: Int = 0

    var state: PlayerSessionState = initialState
        private set

    /** Identity of the stream that is actually prepared/playing. */
    val playbackState: PlayerSessionState
        get() = committedState

    val generation: Int
        get() = requestGeneration

    val queueIndex: Int
        get() = queue.index

    val queueSize: Int
        get() = queue.size

    fun currentChannel(): Channel? = queue.itemAt(queue.index)

    fun canStep(delta: Int): Boolean = queue.index + delta in 0 until queue.size

    /**
     * Starts a channel transition without replacing the committed playback URL.
     * Callers should save the old resume position before invoking this method.
     */
    fun beginStep(delta: Int): PlayerSessionTransition? {
        val targetIndex = queue.index + delta
        val channel = queue.itemAt(targetIndex) ?: return null

        requestGeneration += 1
        queue.index = targetIndex
        pendingTargetIndex = targetIndex
        state = committedState.copy(
            title = channel.name,
            kind = channel.kind,
            tvgId = channel.tvgId,
            positionKey = queue.positionKey(channel),
            subtitle = queue.subtitleRequest(channel) ?: SubtitleSearchPolicy.fromChannel(channel),
        )
        return PlayerSessionTransition(
            generation = requestGeneration,
            targetIndex = targetIndex,
            channel = channel,
        )
    }

    /** Commits a resolved URL only when it still belongs to the active zap. */
    fun commitResolved(generation: Int, resolvedUrl: String): Boolean {
        if (!isCurrent(generation) || resolvedUrl.isBlank() || pendingTargetIndex == null) return false
        val committed = state.copy(playbackUrl = resolvedUrl)
        state = committed
        committedState = committed
        committedQueueIndex = pendingTargetIndex ?: queue.index
        pendingTargetIndex = null
        return true
    }

    /**
     * Restores the last successfully committed session. Incrementing the
     * generation also invalidates any duplicate callback from the failed load.
     */
    fun rollback(generation: Int): Boolean {
        if (!isCurrent(generation)) return false
        requestGeneration += 1
        pendingTargetIndex = null
        queue.index = committedQueueIndex.coerceInQueue(queue.size)
        state = committedState
        return true
    }

    fun isCurrent(generation: Int): Boolean = generation == requestGeneration

    /** Manual subtitle searches update the visible identity without changing playback. */
    fun updateSubtitle(request: SubtitleSearchRequest) {
        state = state.copy(subtitle = request)
        if (pendingTargetIndex == null) committedState = committedState.copy(subtitle = request)
    }

    private fun Int.coerceInQueue(size: Int): Int = when {
        size <= 0 -> 0
        else -> coerceIn(0, size - 1)
    }
}

data class PlayerSessionState(
    val playbackUrl: String,
    val title: String,
    val kind: String,
    val tvgId: String,
    val positionKey: String,
    val subtitle: SubtitleSearchRequest,
)

data class PlayerSessionTransition(
    val generation: Int,
    val targetIndex: Int,
    val channel: Channel,
)

/** Small queue boundary so session transitions are unit-testable without Android. */
interface PlayerSessionQueue {
    var index: Int
    val size: Int
    fun itemAt(index: Int): Channel?
    fun positionKey(channel: Channel): String
    fun subtitleRequest(channel: Channel): SubtitleSearchRequest?
}

/** Compatibility adapter for the existing in-memory PlaybackQueue singleton. */
object PlaybackQueueSessionAdapter : PlayerSessionQueue {
    override var index: Int
        get() = PlaybackQueue.index
        set(value) {
            PlaybackQueue.index = value
        }

    override val size: Int
        get() = PlaybackQueue.items.size

    override fun itemAt(index: Int): Channel? = PlaybackQueue.items.getOrNull(index)

    override fun positionKey(channel: Channel): String = PlaybackQueue.favKey(channel)

    override fun subtitleRequest(channel: Channel): SubtitleSearchRequest? =
        PlaybackQueue.subtitleRequest(channel)
}
