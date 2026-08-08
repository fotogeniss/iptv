package com.prelude.iptv.player

/** Android-free representation of player remote/media keys. */
sealed interface PlayerRemoteKey {
    data class Digit(val value: Int) : PlayerRemoteKey
    data object PlayPause : PlayerRemoteKey
    data object Play : PlayerRemoteKey
    data object PauseOrStop : PlayerRemoteKey
    data object FastForward : PlayerRemoteKey
    data object Rewind : PlayerRemoteKey
    data object ChannelUp : PlayerRemoteKey
    data object ChannelDown : PlayerRemoteKey
    data object MenuOrInfo : PlayerRemoteKey
    data object Captions : PlayerRemoteKey
    data object Confirm : PlayerRemoteKey
    data object Left : PlayerRemoteKey
    data object Right : PlayerRemoteKey
    data object Up : PlayerRemoteKey
    data object Down : PlayerRemoteKey
    data object Other : PlayerRemoteKey
}

data class PlayerRemoteContext(
    val isLive: Boolean,
    val queueSize: Int,
    val overlayVisible: Boolean,
    val channelPanelVisible: Boolean,
    val isTv: Boolean,
    val focusMissing: Boolean,
    val seekBarFocused: Boolean,
)

sealed interface PlayerRemoteAction {
    data class AppendDigit(val digit: Int) : PlayerRemoteAction
    data object TogglePlay : PlayerRemoteAction
    data object EnsurePlaying : PlayerRemoteAction
    data object EnsurePaused : PlayerRemoteAction
    data class Seek(val deltaMs: Long) : PlayerRemoteAction
    data class StepChannel(val delta: Int) : PlayerRemoteAction
    data object ShowMoreMenu : PlayerRemoteAction
    data object ShowSubtitleMenu : PlayerRemoteAction
    data object ShowOverlay : PlayerRemoteAction
    /** Reveal the VOD chrome with focus on its progress bar; does not seek yet. */
    data object ShowSeekBar : PlayerRemoteAction
    data object ShowChannelPanel : PlayerRemoteAction
    data object HideChannelPanel : PlayerRemoteAction
    data object RestoreOverlayFocus : PlayerRemoteAction
    data object HandleSeekBar : PlayerRemoteAction
    /** Keep the chrome alive, then let Android move focus normally. */
    data object RefreshChromeAndDelegate : PlayerRemoteAction
    data object Delegate : PlayerRemoteAction
}

/**
 * One routing table for every TV/media shortcut used by PlayerActivity.
 *
 * The critical invariant is that visible controls do not consume DPAD arrows:
 * Android must be allowed to move focus between buttons. Only a focused VOD
 * seek bar receives direct LEFT/RIGHT handling.
 */
object PlayerRemoteInputPolicy {
    fun decide(
        key: PlayerRemoteKey,
        context: PlayerRemoteContext,
        seekStepMs: Long = 10_000L,
    ): PlayerRemoteAction {
        if (key is PlayerRemoteKey.Digit && context.isLive && context.queueSize > 0) {
            return PlayerRemoteAction.AppendDigit(key.value.coerceIn(0, 9))
        }

        when (key) {
            PlayerRemoteKey.PlayPause -> return PlayerRemoteAction.TogglePlay
            PlayerRemoteKey.Play -> return PlayerRemoteAction.EnsurePlaying
            PlayerRemoteKey.PauseOrStop -> return PlayerRemoteAction.EnsurePaused
            PlayerRemoteKey.FastForward -> return if (context.isLive) {
                PlayerRemoteAction.ShowOverlay
            } else {
                PlayerRemoteAction.Seek(seekStepMs)
            }
            PlayerRemoteKey.Rewind -> return if (context.isLive) {
                PlayerRemoteAction.ShowOverlay
            } else {
                PlayerRemoteAction.Seek(-seekStepMs)
            }
            PlayerRemoteKey.ChannelUp -> return if (context.isLive) {
                PlayerRemoteAction.StepChannel(1)
            } else {
                PlayerRemoteAction.ShowOverlay
            }
            PlayerRemoteKey.ChannelDown -> return if (context.isLive) {
                PlayerRemoteAction.StepChannel(-1)
            } else {
                PlayerRemoteAction.ShowOverlay
            }
            PlayerRemoteKey.MenuOrInfo -> return PlayerRemoteAction.ShowMoreMenu
            PlayerRemoteKey.Captions -> return PlayerRemoteAction.ShowSubtitleMenu
            else -> Unit
        }

        if (context.channelPanelVisible) {
            return if (key == PlayerRemoteKey.Left) PlayerRemoteAction.HideChannelPanel
            else PlayerRemoteAction.Delegate
        }

        if (!context.overlayVisible) {
            return when (key) {
                PlayerRemoteKey.Confirm -> PlayerRemoteAction.ShowOverlay
                PlayerRemoteKey.Right -> if (context.isLive) {
                    PlayerRemoteAction.ShowOverlay
                } else {
                    PlayerRemoteAction.ShowSeekBar
                }
                PlayerRemoteKey.Left -> if (context.isLive) {
                    PlayerRemoteAction.ShowChannelPanel
                } else {
                    PlayerRemoteAction.ShowSeekBar
                }
                PlayerRemoteKey.Up -> if (context.isLive) {
                    PlayerRemoteAction.StepChannel(1)
                } else {
                    PlayerRemoteAction.ShowOverlay
                }
                PlayerRemoteKey.Down -> if (context.isLive) {
                    PlayerRemoteAction.StepChannel(-1)
                } else {
                    PlayerRemoteAction.ShowOverlay
                }
                else -> PlayerRemoteAction.Delegate
            }
        }

        if (context.isTv && context.focusMissing) {
            return PlayerRemoteAction.RestoreOverlayFocus
        }
        if (!context.isLive && context.seekBarFocused &&
            key in setOf(PlayerRemoteKey.Left, PlayerRemoteKey.Right, PlayerRemoteKey.Confirm)
        ) {
            return PlayerRemoteAction.HandleSeekBar
        }
        return PlayerRemoteAction.RefreshChromeAndDelegate
    }
}
