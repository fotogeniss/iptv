package com.prelude.iptv.player

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.SubtitleSearchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSessionControllerTest {
    private val first = Channel(name = "One", url = "https://one", tvgId = "one", kind = "live")
    private val second = Channel(name = "Two", url = "https://two", tvgId = "two", kind = "live")
    private val third = Channel(name = "Three", url = "https://three", tvgId = "three", kind = "live")

    @Test
    fun pendingStepPresentsTargetButKeepsCommittedStreamAlive() {
        val queue = FakeQueue(listOf(first, second, third))
        val session = session(queue)

        val transition = session.beginStep(1)!!

        assertEquals(1, queue.index)
        assertEquals("Two", session.state.title)
        assertEquals("https://committed", session.state.playbackUrl)
        assertEquals("Initial", session.playbackState.title)
        assertEquals("initial-key", session.playbackState.positionKey)
        assertTrue(session.isCurrent(transition.generation))
    }

    @Test
    fun successfulResolveBecomesTheNextRollbackBaseline() {
        val queue = FakeQueue(listOf(first, second, third))
        val session = session(queue)
        val secondTransition = session.beginStep(1)!!
        assertTrue(session.commitResolved(secondTransition.generation, "https://resolved-two"))

        val thirdTransition = session.beginStep(1)!!
        assertTrue(session.rollback(thirdTransition.generation))

        assertEquals(1, queue.index)
        assertEquals("Two", session.state.title)
        assertEquals("https://resolved-two", session.state.playbackUrl)
    }

    @Test
    fun failedRapidZapReturnsToLastCommittedChannelNotIntermediateTarget() {
        val queue = FakeQueue(listOf(first, second, third))
        val session = session(queue)
        val firstPending = session.beginStep(1)!!
        val secondPending = session.beginStep(1)!!

        assertFalse(session.commitResolved(firstPending.generation, "https://late-two"))
        assertTrue(session.rollback(secondPending.generation))

        assertEquals(0, queue.index)
        assertEquals("Initial", session.state.title)
        assertEquals("https://committed", session.state.playbackUrl)
    }

    @Test
    fun staleResolveCannotReplaceNewerSelection() {
        val queue = FakeQueue(listOf(first, second, third))
        val session = session(queue)
        val old = session.beginStep(1)!!
        val current = session.beginStep(1)!!

        assertFalse(session.commitResolved(old.generation, "https://late-two"))
        assertTrue(session.commitResolved(current.generation, "https://resolved-three"))
        assertEquals("Three", session.state.title)
        assertEquals("https://resolved-three", session.state.playbackUrl)
    }

    @Test
    fun boundaryStepDoesNotMutateQueueOrSession() {
        val queue = FakeQueue(listOf(first))
        val session = session(queue)

        assertFalse(session.canStep(1))
        assertNull(session.beginStep(1))
        assertEquals(0, queue.index)
        assertEquals("Initial", session.state.title)
        assertEquals(0, session.generation)
    }

    @Test
    fun manualSubtitleOverrideSurvivesCommittedState() {
        val queue = FakeQueue(listOf(first, second))
        val session = session(queue)
        val manual = SubtitleSearchRequest("Manual title", 2026, type = "movie")

        session.updateSubtitle(manual)
        val pending = session.beginStep(1)!!
        session.rollback(pending.generation)

        assertEquals(manual, session.state.subtitle)
    }

    private fun session(queue: FakeQueue) = PlayerSessionController(
        initialState = PlayerSessionState(
            playbackUrl = "https://committed",
            title = "Initial",
            kind = "live",
            tvgId = "initial",
            positionKey = "initial-key",
            subtitle = SubtitleSearchRequest("Initial"),
        ),
        sourceId = "source-1",
        queue = queue,
    )

    private class FakeQueue(private val channels: List<Channel>) : PlayerSessionQueue {
        override var index: Int = 0
        override val size: Int get() = channels.size
        override fun itemAt(index: Int): Channel? = channels.getOrNull(index)
        override fun positionKey(channel: Channel): String = channel.url
        override fun subtitleRequest(channel: Channel): SubtitleSearchRequest? =
            SubtitleSearchRequest(channel.name, type = if (channel.kind == "series_ep") "episode" else "all")
    }
}
