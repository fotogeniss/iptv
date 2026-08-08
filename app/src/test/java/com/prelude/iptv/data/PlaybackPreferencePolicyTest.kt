package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackPreferencePolicyTest {
    @Test
    fun unknownLanguageFallsBackToAutomatic() {
        assertEquals("", PlaybackPreferencePolicy.normalizeLanguage("unknown"))
        assertEquals("Αυτόματα", PlaybackPreferencePolicy.languageLabel("unknown"))
    }

    @Test
    fun languageCodesAreNormalized() {
        assertEquals("el", PlaybackPreferencePolicy.normalizeLanguage(" EL "))
        assertEquals("Ελληνικά", PlaybackPreferencePolicy.languageLabel("el"))
    }

    @Test
    fun subtitleSizeSnapsToSupportedChoice() {
        assertEquals(85, PlaybackPreferencePolicy.normalizeSubtitleSize(80))
        assertEquals(100, PlaybackPreferencePolicy.normalizeSubtitleSize(104))
        assertEquals(120, PlaybackPreferencePolicy.normalizeSubtitleSize(130))
    }

    @Test
    fun preferredSubtitleLanguageIsSearchedBeforeEnglishFallback() {
        assertEquals(listOf("fr", "en"), PlaybackPreferencePolicy.subtitleSearchLanguages("fr"))
        assertEquals(listOf("en"), PlaybackPreferencePolicy.subtitleSearchLanguages("en"))
        assertEquals(listOf("el", "en"), PlaybackPreferencePolicy.subtitleSearchLanguages(""))
    }
}
