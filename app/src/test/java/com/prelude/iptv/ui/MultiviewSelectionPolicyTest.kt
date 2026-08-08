package com.prelude.iptv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiviewSelectionPolicyTest {
    @Test fun noPrimaryPlaysSingleChannel() {
        assertTrue(
            MultiviewSelectionPolicy.onOpen(null, "secondary") is
                MultiviewSelectionPolicy.OpenDecision.PlaySingle
        )
    }

    @Test fun sameChannelKeepsPrimaryArmed() {
        assertTrue(
            MultiviewSelectionPolicy.onOpen("primary", "primary") is
                MultiviewSelectionPolicy.OpenDecision.KeepPrimaryArmed
        )
    }

    @Test fun differentChannelLaunchesPairInOrder() {
        assertEquals(
            MultiviewSelectionPolicy.OpenDecision.Launch("primary", "secondary"),
            MultiviewSelectionPolicy.onOpen("primary", "secondary")
        )
    }
}
