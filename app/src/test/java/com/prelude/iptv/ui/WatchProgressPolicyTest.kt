package com.prelude.iptv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchProgressPolicyTest {
    @Test
    fun rejectsTinyCompletedAndInvalidProgress() {
        assertNull(WatchProgressPolicy.from(null))
        assertNull(WatchProgressPolicy.from(20_000L to 3_600_000L))
        assertNull(WatchProgressPolicy.from(3_500_000L to 3_600_000L))
        assertNull(WatchProgressPolicy.from(1_000L to 0L))
    }

    @Test
    fun calculatesStableFractionAndRemainingLabel() {
        val progress = WatchProgressPolicy.from(1_800_000L to 3_600_000L)!!
        assertEquals(50, progress.percent)
        assertEquals(0.5f, progress.fraction, 0.0001f)
        assertEquals("Απομένουν 30λ", WatchProgressPolicy.remainingLabel(progress))
    }
}
