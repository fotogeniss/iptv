package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvSeekControllerTest {
    @Test fun repeatedNudgesAccumulateAndCommitOnce() {
        val controller = TvSeekController()

        assertEquals(
            TvSeekUpdate.Preview(40_000L, 120_000L),
            controller.nudge(10_000L, 30_000L, 120_000L),
        )
        assertEquals(
            TvSeekUpdate.Preview(50_000L, 120_000L),
            controller.nudge(10_000L, 5_000L, 120_000L),
        )
        assertTrue(controller.hasPending())
        assertEquals(50_000L, controller.commit())
        assertFalse(controller.hasPending())
        assertNull(controller.commit())
    }

    @Test fun targetIsClampedAtBothEnds() {
        val controller = TvSeekController()

        assertEquals(
            TvSeekUpdate.Preview(0L, 60_000L),
            controller.nudge(-10_000L, 4_000L, 60_000L),
        )
        controller.cancel()
        assertEquals(
            TvSeekUpdate.Preview(60_000L, 60_000L),
            controller.nudge(10_000L, 58_000L, 60_000L),
        )
    }

    @Test fun unknownDurationUsesImmediateRelativeSeekAndClearsPending() {
        val controller = TvSeekController()
        controller.nudge(10_000L, 20_000L, 60_000L)

        assertEquals(
            TvSeekUpdate.RelativeSeek(-10_000L),
            controller.nudge(-10_000L, 20_000L, 0L),
        )
        assertFalse(controller.hasPending())
    }

    @Test fun cancelDropsPendingTarget() {
        val controller = TvSeekController()
        controller.nudge(10_000L, 0L, 60_000L)
        controller.cancel()
        assertNull(controller.commit())
    }
}
