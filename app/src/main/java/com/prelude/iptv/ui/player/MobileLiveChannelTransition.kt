package com.prelude.iptv.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Lightweight directional refraction drawn above the existing TextureView.
 * It never owns playback or a second surface, so channel resolution remains
 * entirely inside the existing player session.
 */
@Composable
internal fun MobileLiveChannelTransition(
    request: LiveChannelTransitionRequest?,
    onFinished: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (request == null) return
    val progress = remember(request.sequence) { Animatable(0f) }
    val currentOnFinished = rememberUpdatedState(onFinished)

    DisposableEffect(request.sequence) {
        onDispose { request.outgoingFrame?.recycle() }
    }

    // Πριν το startReveal γίνει true, ΔΕΝ τρέχει animation — το progress
    // μένει στο 0, όπου το outgoingFrame ήδη καλύπτει ολόκληρη την οθόνη
    // (βλ. edgeFraction), κρατώντας παγωμένο το τελευταίο καρέ πάνω από την
    // πραγματική επιφάνεια όσο διαρκεί η επίλυση/άνοιγμα του νέου καναλιού.
    LaunchedEffect(request.sequence, request.startReveal) {
        if (!request.startReveal) return@LaunchedEffect
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = LiveChannelTransitionMotion.DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
        currentOnFinished.value(request.sequence)
    }

    Canvas(modifier) {
        val phase = progress.value
        val intensity = LiveChannelTransitionMotion.intensity(phase)
        // At phase zero the outgoing snapshot must still fully cover the new
        // frame; returning on zero intensity would flash the incoming stream for
        // one frame before the reveal begins.
        if (phase >= .999f) return@Canvas

        val edgeX = size.width * LiveChannelTransitionMotion.edgeFraction(
            progress = phase,
            direction = request.direction,
        )
        val bandWidth = size.width.coerceAtLeast(1f) * .25f

        // The new stream is already rendering underneath this Canvas. Keep the
        // captured outgoing frame above it and remove that frame behind a wavy,
        // directional edge, matching the approved incoming/outgoing prototype
        // without creating a second player or video surface.
        request.outgoingFrame?.let { frame ->
            val outgoingClip = Path()
            val segments = 18
            val firstBend = sin((phase * 10f).toDouble()).toFloat() * size.width * .018f
            if (request.direction >= 0) {
                outgoingClip.moveTo(0f, 0f)
                outgoingClip.lineTo(edgeX + firstBend, 0f)
            } else {
                outgoingClip.moveTo(size.width, 0f)
                outgoingClip.lineTo(edgeX + firstBend, 0f)
            }
            repeat(segments) { zeroBased ->
                val index = zeroBased + 1
                val y = size.height * index / segments
                val bend = sin((index * 1.31f + phase * 10f).toDouble()).toFloat() *
                    size.width * .018f
                outgoingClip.lineTo(edgeX + bend, y)
            }
            outgoingClip.lineTo(
                if (request.direction >= 0) 0f else size.width,
                size.height,
            )
            outgoingClip.close()

            val frameLeft = ((size.width - frame.widthPx) / 2f).roundToInt()
            val frameTop = ((size.height - frame.heightPx) / 2f).roundToInt()
            clipPath(outgoingClip) {
                drawImage(
                    image = frame.image,
                    dstOffset = IntOffset(frameLeft, frameTop),
                    dstSize = IntSize(frame.widthPx, frame.heightPx),
                )
                drawRect(Color.Black.copy(alpha = .22f * intensity))
            }
        }

        // A restrained dip and glint make the boundary readable even when the
        // current backend cannot provide a TextureView snapshot (LibVLC).
        drawRect(Color.Black.copy(alpha = .16f * intensity))
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF79D8FF).copy(alpha = .14f * intensity),
                    Color.White.copy(alpha = .38f * intensity),
                    Color(0xFFBDEFFF).copy(alpha = .22f * intensity),
                    Color.Transparent,
                ),
                startX = edgeX - bandWidth,
                endX = edgeX + bandWidth,
            ),
            topLeft = Offset(edgeX - bandWidth, 0f),
            size = Size(bandWidth * 2f, size.height),
        )

        val wave = Path()
        val segments = 18
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
            style = Stroke(width = 36f),
        )
        drawPath(
            path = wave,
            color = Color.White.copy(alpha = .36f * intensity),
            style = Stroke(width = 2.2f),
        )
    }
}
