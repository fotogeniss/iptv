package com.prelude.iptv.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test
    fun language_tags_resolve_to_stable_app_choices() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("de-DE"))
        assertEquals(AppLanguage.GREEK, AppLanguage.fromLanguageTag("el"))
        assertEquals(AppLanguage.GREEK, AppLanguage.fromLanguageTag("el-GR"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("EN-us"))
    }

    @Test
    fun public_picker_stays_hidden_until_translation_parity() {
        assertFalse(
            LocalizationRolloutPolicy.pickerVisible(
                ownerQaBuild = false,
                translationParityComplete = false,
            )
        )
        assertTrue(
            LocalizationRolloutPolicy.pickerVisible(
                ownerQaBuild = true,
                translationParityComplete = false,
            )
        )
        assertTrue(
            LocalizationRolloutPolicy.pickerVisible(
                ownerQaBuild = false,
                translationParityComplete = true,
            )
        )
    }
}
