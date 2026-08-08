package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TvSeekKeyPolicyTest {
    @Test fun leftAndRightBecomeTenSecondNudges() {
        assertEquals(
            TvSeekKeyDecision.Nudge(-10_000L),
            TvSeekKeyPolicy.decide(true, false, TvSeekKey.LEFT, TvKeyPhase.DOWN, 0),
        )
        assertEquals(
            TvSeekKeyDecision.Nudge(10_000L),
            TvSeekKeyPolicy.decide(true, false, TvSeekKey.RIGHT, TvKeyPhase.DOWN, 3),
        )
    }

    @Test fun keyUpIsConsumedToPreventFrameworkDoubleMove() {
        assertEquals(
            TvSeekKeyDecision.Consume,
            TvSeekKeyPolicy.decide(true, false, TvSeekKey.RIGHT, TvKeyPhase.UP, 0),
        )
    }

    @Test fun confirmOnlyFiresOnFirstDownEvent() {
        assertEquals(
            TvSeekKeyDecision.Confirm,
            TvSeekKeyPolicy.decide(true, false, TvSeekKey.CONFIRM, TvKeyPhase.DOWN, 0),
        )
        assertEquals(
            TvSeekKeyDecision.Consume,
            TvSeekKeyPolicy.decide(true, false, TvSeekKey.CONFIRM, TvKeyPhase.DOWN, 1),
        )
    }

    @Test fun liveMobileAndUnrelatedKeysPassThrough() {
        assertEquals(
            TvSeekKeyDecision.PassThrough,
            TvSeekKeyPolicy.decide(true, true, TvSeekKey.LEFT, TvKeyPhase.DOWN, 0),
        )
        assertEquals(
            TvSeekKeyDecision.PassThrough,
            TvSeekKeyPolicy.decide(false, false, TvSeekKey.LEFT, TvKeyPhase.DOWN, 0),
        )
        assertEquals(
            TvSeekKeyDecision.PassThrough,
            TvSeekKeyPolicy.decide(true, false, TvSeekKey.OTHER, TvKeyPhase.DOWN, 0),
        )
    }
}
