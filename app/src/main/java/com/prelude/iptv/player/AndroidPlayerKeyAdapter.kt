package com.prelude.iptv.player

import android.view.KeyEvent

/** Thin Android adapter; routing decisions stay in [PlayerRemoteInputPolicy]. */
internal fun playerRemoteKey(keyCode: Int): PlayerRemoteKey = when (keyCode) {
    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> PlayerRemoteKey.Digit(keyCode - KeyEvent.KEYCODE_0)
    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> PlayerRemoteKey.PlayPause
    KeyEvent.KEYCODE_MEDIA_PLAY -> PlayerRemoteKey.Play
    KeyEvent.KEYCODE_MEDIA_PAUSE,
    KeyEvent.KEYCODE_MEDIA_STOP -> PlayerRemoteKey.PauseOrStop
    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> PlayerRemoteKey.FastForward
    KeyEvent.KEYCODE_MEDIA_REWIND -> PlayerRemoteKey.Rewind
    KeyEvent.KEYCODE_CHANNEL_UP,
    KeyEvent.KEYCODE_MEDIA_NEXT -> PlayerRemoteKey.ChannelUp
    KeyEvent.KEYCODE_CHANNEL_DOWN,
    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> PlayerRemoteKey.ChannelDown
    KeyEvent.KEYCODE_MENU,
    KeyEvent.KEYCODE_INFO -> PlayerRemoteKey.MenuOrInfo
    KeyEvent.KEYCODE_CAPTIONS -> PlayerRemoteKey.Captions
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER -> PlayerRemoteKey.Confirm
    KeyEvent.KEYCODE_DPAD_LEFT -> PlayerRemoteKey.Left
    KeyEvent.KEYCODE_DPAD_RIGHT -> PlayerRemoteKey.Right
    KeyEvent.KEYCODE_DPAD_UP -> PlayerRemoteKey.Up
    KeyEvent.KEYCODE_DPAD_DOWN -> PlayerRemoteKey.Down
    else -> PlayerRemoteKey.Other
}
