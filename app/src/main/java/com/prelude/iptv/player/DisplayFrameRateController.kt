package com.prelude.iptv.player

import android.app.Activity
import android.view.WindowManager
import kotlin.math.abs

/**
 * Small Android adapter around [FrameRateMatchPolicy].
 *
 * SEAMLESS requests a refresh rate without forcing a disruptive mode switch.
 * ALWAYS requests a concrete same-resolution display mode and may briefly blank
 * the TV while HDMI timing changes. All preferences are cleared when playback
 * leaves the foreground.
 */
class DisplayFrameRateController(private val activity: Activity) {
    private var mode: AutoFrameRateMode = AutoFrameRateMode.OFF
    private var lastFrameRate: Float? = null
    private var requestIsActive = false

    fun request(mode: AutoFrameRateMode, frameRate: Float?) {
        val fps = frameRate?.let(FrameRateMatchPolicy::sanitizeContentFrameRate)
        val previousFrameRate = lastFrameRate
        val sameRequest = requestIsActive && this.mode == mode && when {
            fps == null && previousFrameRate == null -> true
            fps != null && previousFrameRate != null -> abs(fps - previousFrameRate) < 0.01f
            else -> false
        }
        this.mode = mode
        lastFrameRate = fps
        if (sameRequest) return
        if (mode == AutoFrameRateMode.OFF || fps == null) {
            requestIsActive = false
            clearWindowPreference()
            return
        }

        val attrs = activity.window.attributes
        attrs.preferredDisplayModeId = 0
        attrs.preferredRefreshRate = 0f

        when (mode) {
            AutoFrameRateMode.OFF -> Unit
            AutoFrameRateMode.SEAMLESS -> attrs.preferredRefreshRate = fps
            AutoFrameRateMode.ALWAYS -> {
                val display = activity.windowManager.defaultDisplay
                val current = display.mode.toInfo()
                val selected = FrameRateMatchPolicy.chooseDisplayMode(
                    contentFrameRate = fps,
                    currentMode = current,
                    supportedModes = display.supportedModes.map { it.toInfo() }
                )
                if (selected != null) attrs.preferredDisplayModeId = selected.id
                else attrs.preferredRefreshRate = fps
            }
        }
        activity.window.attributes = attrs
        requestIsActive = true
    }

    /** Clear the active request but retain enough state to reapply in onStart. */
    fun suspendForBackground() {
        requestIsActive = false
        clearWindowPreference()
    }

    fun reapply() {
        request(mode, lastFrameRate)
    }

    fun release() {
        lastFrameRate = null
        mode = AutoFrameRateMode.OFF
        requestIsActive = false
        clearWindowPreference()
    }

    private fun clearWindowPreference() {
        val attrs: WindowManager.LayoutParams = activity.window.attributes
        if (attrs.preferredDisplayModeId == 0 && attrs.preferredRefreshRate == 0f) return
        attrs.preferredDisplayModeId = 0
        attrs.preferredRefreshRate = 0f
        activity.window.attributes = attrs
    }

    private fun android.view.Display.Mode.toInfo() = DisplayModeInfo(
        id = modeId,
        width = physicalWidth,
        height = physicalHeight,
        refreshRate = refreshRate
    )
}
