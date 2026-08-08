package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekBarPositionMapperTest {
    @Test fun normalDurationsMapOneToOne() {
        assertEquals(120_000, SeekBarPositionMapper.maxProgress(120_000L))
        assertEquals(45_000, SeekBarPositionMapper.progress(45_000L, 120_000L))
        assertEquals(45_000L, SeekBarPositionMapper.positionMs(45_000, 120_000L))
    }

    @Test fun veryLongDurationsScaleWithoutIntOverflow() {
        val duration = Int.MAX_VALUE.toLong() * 3L
        val halfProgress = SeekBarPositionMapper.progress(duration / 2L, duration)
        val roundTrip = SeekBarPositionMapper.positionMs(halfProgress, duration)

        assertEquals(Int.MAX_VALUE, SeekBarPositionMapper.maxProgress(duration))
        assertTrue(halfProgress in (Int.MAX_VALUE / 2 - 1)..(Int.MAX_VALUE / 2 + 1))
        assertTrue(kotlin.math.abs(roundTrip - duration / 2L) < 10L)
    }

    @Test fun invalidDurationMapsToZero() {
        assertEquals(0, SeekBarPositionMapper.maxProgress(0L))
        assertEquals(0, SeekBarPositionMapper.progress(50L, 0L))
        assertEquals(0L, SeekBarPositionMapper.positionMs(50, 0L))
    }
}
