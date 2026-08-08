package com.prelude.iptv.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.prelude.iptv.ui.IptvTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Device smoke coverage for the fullscreen CC/audio panel on touch devices. */
class PlayerTracksPanelInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun panelAndCloseActionRemainVisibleAndDismissible() {
        var dismissed = false
        composeRule.setContent {
            IptvTheme {
                PlayerTracksPanel(
                    initialTab = PlayerTracksTab.SUBTITLES,
                    audioTracks = emptyList(),
                    subtitleTracks = emptyList(),
                    subtitleSize = 100,
                    subtitleBackground = "None",
                    subtitleBold = false,
                    subtitleQuery = "",
                    searchSubtitles = null,
                    onAutoFetchSubtitles = null,
                    onSelectAudio = {},
                    onSelectSubtitle = {},
                    onSubtitleSize = {},
                    onSubtitleBackground = {},
                    onSubtitleBold = {},
                    onSubtitleChosen = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("player-tracks-panel").assertIsDisplayed()
        composeRule.onNodeWithTag("player-tracks-close").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(dismissed) }
    }
}
