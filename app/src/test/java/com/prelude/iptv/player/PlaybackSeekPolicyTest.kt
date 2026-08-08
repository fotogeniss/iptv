package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSeekPolicyTest {
    @Test fun absoluteTargetHonoursKnownDuration() {
        assertEquals(0L, PlaybackSeekPolicy.absoluteTarget(-1L, 60_000L))
        assertEquals(30_000L, PlaybackSeekPolicy.absoluteTarget(30_000L, 60_000L))
        assertEquals(60_000L, PlaybackSeekPolicy.absoluteTarget(80_000L, 60_000L))
    }

    @Test fun unknownDurationStillPreventsNegativeTargets() {
        assertEquals(0L, PlaybackSeekPolicy.absoluteTarget(-10_000L, 0L))
        assertEquals(90_000L, PlaybackSeekPolicy.absoluteTarget(90_000L, -1L))
    }

    @Test fun relativeTargetClampsWithoutOverflow() {
        assertEquals(0L, PlaybackSeekPolicy.relativeTarget(2_000L, -10_000L, 60_000L))
        assertEquals(60_000L, PlaybackSeekPolicy.relativeTarget(58_000L, 10_000L, 60_000L))
        assertEquals(Long.MAX_VALUE, PlaybackSeekPolicy.relativeTarget(Long.MAX_VALUE - 1, 10L, 0L))
    }
}
