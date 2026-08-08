@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prelude.iptv.ui.route

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import com.prelude.iptv.ui.IptvTheme
import org.junit.Rule
import org.junit.Test

/** Device-level smoke coverage for the dialogs used by both mobile and Android TV. */
class TvDialogFocusInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadModeDialog_initialFocusStartsOnAllCategories() {
        composeRule.setContent {
            IptvTheme {
                LoadModeDialog(count = 12, onAll = {}, onChoose = {}, onCancel = {})
            }
        }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("load-mode-all").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("load-mode-all").assertIsFocused()
    }

    @Test
    fun loadModeDialog_dpadDownMovesToChooseCategories() {
        composeRule.setContent {
            IptvTheme {
                LoadModeDialog(count = 12, onAll = {}, onChoose = {}, onCancel = {})
            }
        }

        val first = composeRule.onNodeWithTag("load-mode-all")
        composeRule.waitUntil(timeoutMillis = 3_000) {
            runCatching { first.fetchSemanticsNode() }.isSuccess
        }
        first.assertIsFocused().performKeyInput {
            keyDown(Key.DirectionDown)
            keyUp(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("load-mode-choose").assertIsFocused()
    }

    @Test
    fun refreshModeDialog_initialFocusStartsOnSafeExistingSelection() {
        composeRule.setContent {
            IptvTheme {
                RefreshModeDialog(
                    contentType = "live",
                    onExisting = {},
                    onChooseGroups = {},
                    onCancel = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("refresh-mode-existing").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("refresh-mode-existing").assertIsFocused()
    }

    @Test
    fun refreshModeDialog_dpadDownMovesToNewGroups() {
        composeRule.setContent {
            IptvTheme {
                RefreshModeDialog(
                    contentType = "live",
                    onExisting = {},
                    onChooseGroups = {},
                    onCancel = {},
                )
            }
        }

        val existing = composeRule.onNodeWithTag("refresh-mode-existing")
        composeRule.waitUntil(timeoutMillis = 3_000) {
            runCatching { existing.fetchSemanticsNode() }.isSuccess
        }
        existing.assertIsFocused().performKeyInput {
            keyDown(Key.DirectionDown)
            keyUp(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("refresh-mode-new-groups").assertIsFocused()
    }

    @Test
    fun categoryPicker_initialFocusAndRightShortcutReachLoad() {
        composeRule.setContent {
            IptvTheme {
                CategoryPicker(
                    categories = listOf("1" to "News", "2" to "Sports"),
                    onCancel = {},
                    onLoad = {},
                )
            }
        }

        val selectAll = composeRule.onNodeWithTag("category-select-all")
        composeRule.waitUntil(timeoutMillis = 3_000) {
            runCatching { selectAll.fetchSemanticsNode() }.isSuccess
        }
        selectAll.assertIsFocused().performKeyInput {
            keyDown(Key.DirectionRight)
            keyUp(Key.DirectionRight)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category-load").assertIsFocused()
    }

}
