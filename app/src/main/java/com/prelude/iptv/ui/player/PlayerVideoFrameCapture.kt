package com.prelude.iptv.ui.player

import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Narrow bridge for taking one frame from a Compose-hosted [TextureView].
 *
 * The player remains the sole owner of playback and of the video surface. This
 * handle exposes only a short-lived bitmap copy for visual transitions; it does
 * not retain an Activity, create a second surface or control the engine.
 */
class PlayerVideoFrameCapture {
    private var textureView: TextureView? = null

    internal fun attach(view: TextureView) {
        textureView = view
    }

    internal fun detach(view: TextureView) {
        if (textureView === view) textureView = null
    }

    internal fun capture(): CapturedVideoFrame? {
        val view = textureView?.takeIf {
            it.isAvailable && it.width > 0 && it.height > 0
        } ?: return null
        val bitmap = runCatching { view.bitmap }.getOrNull() ?: return null
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            bitmap.recycle()
            return null
        }
        return CapturedVideoFrame(bitmap)
    }
}

internal class CapturedVideoFrame(
    private val bitmap: Bitmap,
) {
    val image: ImageBitmap = bitmap.asImageBitmap()
    val widthPx: Int = bitmap.width
    val heightPx: Int = bitmap.height

    fun recycle() {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}
