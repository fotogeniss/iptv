package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistPreferencePolicyTest {
    @Test
    fun invalidRefreshValueFallsBackToThreeDays() {
        assertEquals(3, PlaylistPreferencePolicy.normalizeRefreshDays(0))
        assertEquals(3, PlaylistPreferencePolicy.normalizeRefreshDays(30))
    }

    @Test
    fun refreshCycleIsStableAndWraps() {
        assertEquals(3, PlaylistPreferencePolicy.nextRefreshDays(1))
        assertEquals(7, PlaylistPreferencePolicy.nextRefreshDays(3))
        assertEquals(1, PlaylistPreferencePolicy.nextRefreshDays(7))
    }

    @Test
    fun automaticOpenRequiresEnabledNonEmptySourceAndSingleExecution() {
        assertTrue(PlaylistPreferencePolicy.shouldAutoOpen(false, true, true))
        assertFalse(PlaylistPreferencePolicy.shouldAutoOpen(true, true, true))
        assertFalse(PlaylistPreferencePolicy.shouldAutoOpen(false, false, true))
        assertFalse(PlaylistPreferencePolicy.shouldAutoOpen(false, true, false))
    }
}
