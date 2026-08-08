package com.prelude.iptv.player

import org.junit.Assert.*
import org.junit.Test

class MultiviewPolicyTest {
    @Test fun dpadSelectsExpectedPane() {
        assertEquals(0, MultiviewPolicy.nextPane(1, 21))
        assertEquals(1, MultiviewPolicy.nextPane(0, 22))
    }

    @Test fun launchDebounceRejectsRapidRepeat() {
        assertFalse(MultiviewPolicy.canLaunch(1000, 1500))
        assertTrue(MultiviewPolicy.canLaunch(1000, 1800))
    }
}
