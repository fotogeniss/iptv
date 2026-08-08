package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRemoteInputPolicyTest {
    private fun context(
        live: Boolean = true,
        queue: Int = 10,
        overlay: Boolean = false,
        panel: Boolean = false,
        tv: Boolean = true,
        focusMissing: Boolean = false,
        seekFocused: Boolean = false,
    ) = PlayerRemoteContext(live, queue, overlay, panel, tv, focusMissing, seekFocused)

    @Test fun hiddenLiveDpadRoutesToZapAndChannelPanel() {
        assertEquals(
            PlayerRemoteAction.ShowChannelPanel,
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Left, context())
        )
        assertEquals(
            PlayerRemoteAction.StepChannel(1),
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Up, context())
        )
        assertEquals(
            PlayerRemoteAction.StepChannel(-1),
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Down, context())
        )
    }

    @Test fun visibleChromeLetsAndroidMoveFocus() {
        assertEquals(
            PlayerRemoteAction.RefreshChromeAndDelegate,
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Right, context(overlay = true))
        )
    }

    @Test fun focusedVodSeekBarOwnsLeftRightAndConfirm() {
        val c = context(live = false, overlay = true, seekFocused = true)
        assertEquals(PlayerRemoteAction.HandleSeekBar, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Left, c))
        assertEquals(PlayerRemoteAction.HandleSeekBar, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Right, c))
        assertEquals(PlayerRemoteAction.HandleSeekBar, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Confirm, c))
    }

    @Test fun lostTvFocusIsRestoredBeforeAnyNavigation() {
        assertEquals(
            PlayerRemoteAction.RestoreOverlayFocus,
            PlayerRemoteInputPolicy.decide(
                PlayerRemoteKey.Down,
                context(overlay = true, focusMissing = true)
            )
        )
    }

    @Test fun channelPanelOnlyConsumesLeftForClose() {
        val c = context(panel = true, overlay = true)
        assertEquals(PlayerRemoteAction.HideChannelPanel, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Left, c))
        assertEquals(PlayerRemoteAction.Delegate, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Down, c))
    }

    @Test fun mediaKeysRemainAvailableWithVisibleChrome() {
        assertEquals(
            PlayerRemoteAction.TogglePlay,
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.PlayPause, context(overlay = true))
        )
        assertEquals(
            PlayerRemoteAction.Seek(10_000L),
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.FastForward, context(live = false, overlay = true))
        )
    }

    @Test fun digitsOnlyZapWhenLiveQueueExists() {
        assertEquals(
            PlayerRemoteAction.AppendDigit(7),
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Digit(7), context())
        )
        assertEquals(
            PlayerRemoteAction.RefreshChromeAndDelegate,
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Digit(7), context(queue = 0, overlay = true))
        )
    }
    @Test fun channelKeysZapOnlyForLiveContent() {
        assertEquals(
            PlayerRemoteAction.StepChannel(1),
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.ChannelUp, context(live = true))
        )
        assertEquals(
            PlayerRemoteAction.StepChannel(-1),
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.ChannelDown, context(live = true))
        )
        assertEquals(
            PlayerRemoteAction.ShowOverlay,
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.ChannelUp, context(live = false))
        )
    }

    @Test fun dedicatedMenuAndCaptionsKeysStayDirectlyRoutable() {
        assertEquals(
            PlayerRemoteAction.ShowMoreMenu,
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.MenuOrInfo, context(overlay = true))
        )
        assertEquals(
            PlayerRemoteAction.ShowSubtitleMenu,
            PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Captions, context(overlay = true))
        )
    }

    @Test fun hiddenVodArrowsFocusSeekBarWithoutChangingPosition() {
        val vod = context(live = false, overlay = false)
        assertEquals(PlayerRemoteAction.ShowSeekBar, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Left, vod))
        assertEquals(PlayerRemoteAction.ShowSeekBar, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Right, vod))
        assertEquals(PlayerRemoteAction.ShowOverlay, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Up, vod))
        assertEquals(PlayerRemoteAction.ShowOverlay, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Down, vod))
    }

    @Test fun explicitPlayPauseAndStopActionsAreNeverDelegated() {
        val c = context(overlay = true)
        assertEquals(PlayerRemoteAction.EnsurePlaying, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.Play, c))
        assertEquals(PlayerRemoteAction.EnsurePaused, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.PauseOrStop, c))
        assertEquals(PlayerRemoteAction.TogglePlay, PlayerRemoteInputPolicy.decide(PlayerRemoteKey.PlayPause, c))
    }

}
