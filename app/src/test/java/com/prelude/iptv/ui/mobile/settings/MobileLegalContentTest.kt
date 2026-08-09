package com.prelude.iptv.ui.mobile.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileLegalContentTest {
    @Test
    fun navigationTabsAndContentIdsStayUnique() {
        assertEquals(
            listOf(MobileLegalTab.PRIVACY, MobileLegalTab.TERMS, MobileLegalTab.SERVICES),
            MobileLegalTab.entries.toList(),
        )

        val disclosureIds = MobileLegalContent.localDisclosures.map { it.id } +
            MobileLegalContent.networkDisclosures.map { it.id }
        assertEquals(listOf("sources", "preferences", "cache", "network", "diagnostics"), disclosureIds)
        assertEquals(disclosureIds.size, disclosureIds.toSet().size)

        val serviceIds = MobileLegalContent.services.map { it.id }
        assertEquals(
            listOf("iptv", "tmdb", "opensubtitles", "google_play", "firebase_crashlytics"),
            serviceIds,
        )
        assertEquals(serviceIds.size, serviceIds.toSet().size)
    }

    @Test
    fun requiredExternalRecipientsAreDisclosed() {
        val serviceIds = MobileLegalContent.services.map { it.id }.toSet()
        assertTrue("iptv" in serviceIds)
        assertTrue("tmdb" in serviceIds)
        assertTrue("opensubtitles" in serviceIds)
        assertTrue("google_play" in serviceIds)
        assertTrue("firebase_crashlytics" in serviceIds)
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

    @Test
    fun legalVersionAndEffectiveDateStayAlignedWithCanonicalDocuments() {
        assertEquals("1.1-draft", MobileLegalContent.POLICY_VERSION)
        assertEquals("2026-08-02", MobileLegalContent.EFFECTIVE_DATE)
        assertEquals(MobileLegalTerm.entries.toList(), MobileLegalContent.terms)
    }
}
