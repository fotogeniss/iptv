package com.prelude.iptv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleAutoSearchPolicyTest {
    @Test fun `automatic search uses the approved short typing debounce`() {
        assertEquals(400L, SubtitleAutoSearchPolicy.DEBOUNCE_MS)
    }

    @Test fun `editable title is normalized before automatic search`() {
        assertEquals(
            "Hometown Cha-Cha-Cha S01E03",
            SubtitleAutoSearchPolicy.normalizedQuery(
                "  Hometown   Cha-Cha-Cha   S01E03  "
            ),
        )
    }

    @Test fun `blank edited title does not send a provider request`() {
        assertFalse(SubtitleAutoSearchPolicy.shouldSearch("   \t  "))
    }

    @Test fun `non blank edited title triggers automatic search`() {
        assertTrue(SubtitleAutoSearchPolicy.shouldSearch("The Last Witness"))
    }
}
