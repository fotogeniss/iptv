package com.prelude.iptv.ui.components.details

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailDurationPolicyTest {
    @Test fun `provider duration formats become minutes`() {
        assertEquals(101, DetailDurationPolicy.minutes("101"))
        assertEquals(101, DetailDurationPolicy.minutes("101 min"))
        assertEquals(101, DetailDurationPolicy.minutes("01:41:00"))
        assertEquals(101, DetailDurationPolicy.minutes("1:41"))
        assertEquals(101, DetailDurationPolicy.minutes("6060"))
        assertEquals(101, DetailDurationPolicy.minutes("1h 41m"))
    }

    @Test fun `unknown values are left to the caller`() {
        assertNull(DetailDurationPolicy.minutes(""))
        assertNull(DetailDurationPolicy.minutes("N/A"))
        assertNull(DetailDurationPolicy.minutes("0"))
    }
}
