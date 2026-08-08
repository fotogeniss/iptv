package com.prelude.iptv.ui.mobile.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileLegalContentTest {
    @Test
    fun navigationTabsAndContentIdsStayUnique() {
        assertEquals(MobileLegalTab.entries.size, MobileLegalTab.entries.map { it.label }.toSet().size)

        val disclosureIds = MobileLegalContent.localDisclosures.map { it.id } +
            MobileLegalContent.networkDisclosures.map { it.id }
        assertEquals(disclosureIds.size, disclosureIds.toSet().size)

        val serviceIds = MobileLegalContent.services.map { it.id }
        assertEquals(serviceIds.size, serviceIds.toSet().size)
    }

    @Test
    fun requiredExternalRecipientsAreDisclosed() {
        val serviceIds = MobileLegalContent.services.map { it.id }.toSet()
        assertTrue("iptv" in serviceIds)
        assertTrue("tmdb" in serviceIds)
        assertTrue("opensubtitles" in serviceIds)
        assertTrue("google_play" in serviceIds)
    }

    @Test
    fun releaseIdentityCannotAppearConfiguredWhilePlaceholderIsEmpty() {
        assertFalse(MobileLegalContent.identityConfigured)
    }

    @Test
    fun tmdbAttributionMatchesRequiredNotice() {
        assertEquals(
            "This product uses the TMDB API but is not endorsed or certified by TMDB.",
            MobileLegalContent.TMDB_ATTRIBUTION,
        )
    }
}

