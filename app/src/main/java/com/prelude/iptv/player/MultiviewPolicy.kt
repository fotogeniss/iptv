package com.prelude.iptv.player

object MultiviewPolicy {
    fun nextPane(current: Int, keyCode: Int): Int = when (keyCode) {
        21 -> 0 // KEYCODE_DPAD_LEFT
        22 -> 1 // KEYCODE_DPAD_RIGHT
        else -> current.coerceIn(0, 1)
    }

    fun canLaunch(lastLaunchAtMs: Long, nowMs: Long, debounceMs: Long = 800L): Boolean =
        nowMs - lastLaunchAtMs >= debounceMs
}
