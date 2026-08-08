package com.prelude.iptv.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

internal data class LiveChannelTransitionRequest(
    val sequence: Int,
    val direction: Int,
)

/** Android-free motion math kept deterministic for tests and reduced surprises. */
internal object LiveChannelTransitionMotion {
    const val DURATION_MS = 640

    fun direction(step: Int): Int = if (step >= 0) 1 else -1

    fun edgeFraction(progress: Float, direction: Int): Float {
        val safe = progress.coerceIn(0f, 1f)
        return if (direction >= 0) 1f - safe else safe
    }

    fun intensity(progress: Float): Float =
        sin(progress.coerceIn(0f, 1f) * PI).toFloat().coerceIn(0f, 1f)
}

/**
 * Lightweight directional refraction drawn above the existing TextureView.
 * It never owns playback or a second surface, so channel resolution remains
 * entirely inside the existing player session.
 */
@Composable
internal fun MobileLiveChannelTransition(
    request: LiveChannelTransitionRequest?,
    modifier: Modifier = Modifier,
) {
    if (request == null) return
    val progress = remember { Animatable(1f) }
    LaunchedEffect(request.sequence) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = LiveChannelTransitionMotion.DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Canvas(modifier) {
        val phase = progress.value
        val intensity = LiveChannelTransitionMotion.intensity(phase)
        if (intensity <= 0.001f) return@Canvas

        val edgeX = size.width * LiveChannelTransitionMotion.edgeFraction(
            progress = phase,
            direction = request.direction,
        )
        val bandWidth = size.width.coerceAtLeast(1f) * .18f

        // The short dark dip hides the provider's decode gap without flashing
        // or covering the controls with an opaque transition card.
        drawRect(Color.Black.copy(alpha = .20f * intensity))
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF79D8FF).copy(alpha = .08f * intensity),
                    Color.White.copy(alpha = .20f * intensity),
                    Color(0xFFBDEFFF).copy(alpha = .13f * intensity),
                    Color.Transparent,
                ),
                startX = edgeX - bandWidth,
                endX = edgeX + bandWidth,
            ),
            topLeft = Offset(edgeX - bandWidth, 0f),
            size = Size(bandWidth * 2f, size.height),
        )

        val wave = Path()
        val segments = 14
        repeat(segments + 1) { index ->
            val y = size.height * index / segments
            val bend = sin((index * 1.31f + phase * 10f).toDouble()).toFloat() *
                size.width * .018f
            val x = edgeX + bend
            if (index == 0) wave.moveTo(x, y) else wave.lineTo(x, y)
        }
        drawPath(
            path = wave,
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0xFF75D5FF).copy(alpha = .05f * intensity),
                    Color.White.copy(alpha = .18f * intensity),
                    Color(0xFF9CE7FF).copy(alpha = .05f * intensity),
                ),
                startX = edgeX - 22f,
                endX = edgeX + 22f,
            ),
            style = Stroke(width = 22f),
        )
        drawPath(
            path = wave,
            color = Color.White.copy(alpha = .24f * intensity),
            style = Stroke(width = 1.5f),
        )
    }
}
