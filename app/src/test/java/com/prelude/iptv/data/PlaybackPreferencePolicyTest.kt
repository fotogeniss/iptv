package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackPreferencePolicyTest {
    @Test
    fun unknownLanguageFallsBackToAutomatic() {
        assertEquals("", PlaybackPreferencePolicy.normalizeLanguage("unknown"))
    }

    @Test
    fun languageCodesAreNormalized() {
        assertEquals("el", PlaybackPreferencePolicy.normalizeLanguage(" EL "))
        assertEquals(listOf("", "el", "en", "es", "fr", "de", "it", "pt", "ar"), PlaybackPreferencePolicy.languages.map { it.code })
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
