package com.prelude.iptv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchKeyboardPolicyTest {
    @Test fun activeAppLanguageSelectsInitialAlphabet() {
        assertEquals(SearchKeyboardMode.GREEK, SearchKeyboardPolicy.initialMode("el"))
        assertEquals(SearchKeyboardMode.LATIN, SearchKeyboardPolicy.initialMode("en"))
        assertEquals(SearchKeyboardMode.LATIN, SearchKeyboardPolicy.initialMode("de"))
    }

    @Test fun everyLayoutKeepsEditingAndLanguageActionsReachable() {
        SearchKeyboardMode.entries.forEach { mode ->
            val actions = SearchKeyboardPolicy.keys(mode).map { it.action }
            assertTrue(SearchKeyboardAction.SPACE in actions)
            assertTrue(SearchKeyboardAction.BACKSPACE in actions)
            assertTrue(SearchKeyboardAction.CLEAR in actions)
            assertTrue(SearchKeyboardAction.CHARACTER in actions)
            assertTrue(SearchKeyboardAction.NUMERIC in actions || mode == SearchKeyboardMode.NUMERIC)
        }
    }
}
