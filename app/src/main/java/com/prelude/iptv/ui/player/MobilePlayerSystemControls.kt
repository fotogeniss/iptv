package com.prelude.iptv.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings

/** System controls driven by vertical gestures over the fullscreen mobile player. */
internal enum class VerticalPlayerControl { BRIGHTNESS, VOLUME }

private tailrec fun Context.playerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.playerActivity()
    else -> null
}

internal fun currentBrightness(context: Context): Float {
    val windowValue = context.playerActivity()?.window?.attributes?.screenBrightness ?: -1f
    if (windowValue >= 0f) return windowValue.coerceIn(0f, 1f)
    return runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(.5f).coerceIn(0f, 1f)
}

internal fun setBrightness(context: Context, value: Float) {
    val window = context.playerActivity()?.window ?: return
    window.attributes = window.attributes.apply {
        // Never leave the user with a completely black player surface.
        screenBrightness = value.coerceIn(.01f, 1f)
    }
}

internal fun currentVolume(context: Context): Float {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0f
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

internal fun setVolume(context: Context, value: Float) {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    audio.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        (value.coerceIn(0f, 1f) * max).toInt(),
        0,
    )
}
