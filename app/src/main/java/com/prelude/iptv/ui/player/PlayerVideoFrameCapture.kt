package com.prelude.iptv.ui.player

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val MAX_CAPTURE_PIXELS = 1920L * 1080L

/**
 * Narrow bridge for taking one frame from the active Compose-hosted video view.
 *
 * The player remains the sole owner of playback and of the video surface. This
 * handle exposes only a short-lived bitmap copy for visual transitions; it does
 * not retain an Activity, create a second surface or control the engine.
 */
class PlayerVideoFrameCapture {
    private var textureView: TextureView? = null
    private var surfaceView: SurfaceView? = null
    private val pixelCopyHandler = Handler(Looper.getMainLooper())

    internal fun attach(view: TextureView) {
        textureView = view
    }

    internal fun detach(view: TextureView) {
        if (textureView === view) textureView = null
    }

    internal fun attach(view: SurfaceView) {
        surfaceView = view
    }

    internal fun detach(view: SurfaceView) {
        if (surfaceView === view) surfaceView = null
    }

    internal suspend fun capture(): CapturedVideoFrame? {
        textureView?.takeIf {
            it.isAvailable && it.width > 0 && it.height > 0
        }?.let { view ->
            val bitmap = runCatching { view.bitmap }.getOrNull() ?: return@let
            if (bitmap.width > 0 && bitmap.height > 0) return CapturedVideoFrame(bitmap)
            bitmap.recycle()
        }

        val view = surfaceView?.takeIf {
            it.width > 0 && it.height > 0 && it.holder.surface.isValid
        } ?: return null
        val sourcePixels = view.width.toLong() * view.height.toLong()
        val captureScale = if (sourcePixels > MAX_CAPTURE_PIXELS) {
            sqrt(MAX_CAPTURE_PIXELS.toDouble() / sourcePixels.toDouble())
        } else {
            1.0
        }
        val captureWidth = (view.width * captureScale).roundToInt().coerceAtLeast(1)
        val captureHeight = (view.height * captureScale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(
            captureWidth,
            captureHeight,
            Bitmap.Config.ARGB_8888,
        )
        return suspendCancellableCoroutine { continuation ->
            try {
                PixelCopy.request(
                    view,
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS && continuation.isActive) {
                            continuation.resume(
                                CapturedVideoFrame(
                                    bitmap = bitmap,
                                    widthPx = view.width,
                                    heightPx = view.height,
                                )
                            )
                        } else {
                            if (!bitmap.isRecycled) bitmap.recycle()
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                    pixelCopyHandler,
                )
            } catch (_: IllegalArgumentException) {
                if (!bitmap.isRecycled) bitmap.recycle()
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}

internal class CapturedVideoFrame(
    private val bitmap: Bitmap,
    val widthPx: Int = bitmap.width,
    val heightPx: Int = bitmap.height,
) {
    val image: ImageBitmap = bitmap.asImageBitmap()

    fun recycle() {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}
