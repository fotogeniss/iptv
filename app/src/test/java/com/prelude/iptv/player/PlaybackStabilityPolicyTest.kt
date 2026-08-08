package com.prelude.iptv.player

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStabilityPolicyTest {
    @Test fun zapDeltaIsCoalescedAndBounded() {
        assertEquals(2, PlaybackStabilityPolicy.mergeZapDelta(1, 1))
        assertEquals(0, PlaybackStabilityPolicy.mergeZapDelta(1, -1))
        assertEquals(10, PlaybackStabilityPolicy.mergeZapDelta(10, 1))
    }

    @Test fun retryIsLimitedAndRequiresCurrentIoRequest() {
        assertTrue(PlaybackStabilityPolicy.shouldRetryTransientIo(0, true, true))
        assertTrue(PlaybackStabilityPolicy.shouldRetryTransientIo(1, true, true))
        assertFalse(PlaybackStabilityPolicy.shouldRetryTransientIo(2, true, true))
        assertFalse(PlaybackStabilityPolicy.shouldRetryTransientIo(0, false, true))
        assertFalse(PlaybackStabilityPolicy.shouldRetryTransientIo(0, true, false))
    }

    @Test fun wrappedIoFailuresAreDetected() {
        assertTrue(PlaybackStabilityPolicy.hasIoCause(IOException("direct")))
        assertTrue(PlaybackStabilityPolicy.hasIoCause(IllegalStateException("wrapped", IOException("network"))))
        assertFalse(PlaybackStabilityPolicy.hasIoCause(IllegalArgumentException("parser")))
        assertFalse(PlaybackStabilityPolicy.hasIoCause(null))
    }
}
