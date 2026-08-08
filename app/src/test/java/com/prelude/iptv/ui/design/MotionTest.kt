package com.prelude.iptv.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun semanticDurationsRemainOrdered() {
        assertTrue(Motion.Fast < Motion.Focus)
        assertTrue(Motion.Focus < Motion.Medium)
        assertTrue(Motion.Medium < Motion.Overlay)
        assertTrue(Motion.Overlay < Motion.Slow)
        assertTrue(Motion.Slow < Motion.Hero)
    }

    @Test
    fun reducedMotionMakesDurationEffectivelyInstant() {
        assertEquals(1, Motion.duration(Motion.Hero, reducedMotion = true))
        assertEquals(Motion.Hero, Motion.duration(Motion.Hero, reducedMotion = false))
    }

    @Test
    fun reducedMotionRemovesDecorativeScale() {
        assertEquals(1f, Motion.scale(Motion.TvEmphasisScale, reducedMotion = true))
        assertEquals(Motion.TvEmphasisScale, Motion.scale(Motion.TvEmphasisScale, reducedMotion = false))
    }
}
