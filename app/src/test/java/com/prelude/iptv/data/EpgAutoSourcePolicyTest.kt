package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EpgAutoSourcePolicyTest {
    @Test
    fun customUrlHasHighestPriority() {
        assertEquals("custom.xml", EpgAutoSourcePolicy.choose(" custom.xml ", "embedded.xml", "xtream.xml"))
    }

    @Test
    fun embeddedM3uUrlPrecedesXtreamFallback() {
        assertEquals("embedded.xml", EpgAutoSourcePolicy.choose("", "embedded.xml", "xtream.xml"))
    }

    @Test
    fun xtreamIsUsedAsFinalAutomaticFallback() {
        assertEquals("xtream.xml", EpgAutoSourcePolicy.choose("", "", "xtream.xml"))
    }

    @Test
    fun noCandidateReturnsBlank() {
        assertEquals("", EpgAutoSourcePolicy.choose(" ", "", "  "))
    }
}
