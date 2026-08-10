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

/** Pure large-screen tuning kept separate from the mobile visual treatment. */
internal object TvLiveChannelTransitionMotion {
    const val DURATION_MS = 780
    const val WAVE_AMPLITUDE_FRACTION = .010f
    const val BAND_WIDTH_FRACTION = .18f
    const val MAX_DIM_ALPHA = .08f

    fun direction(step: Int): Int = LiveChannelTransitionMotion.direction(step)

    fun edgeFraction(progress: Float, direction: Int): Float =
        LiveChannelTransitionMotion.edgeFraction(progress, direction)

    fun intensity(progress: Float): Float = LiveChannelTransitionMotion.intensity(progress)
}

/**
 * Restrained TV refraction drawn above the existing fullscreen video surface.
 * A Canvas has no focus or key-input modifiers, so DPAD ownership stays in PlayerHost.
 */
@Composable
internal fun TvLiveChannelTransition(
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
                durationMillis = TvLiveChannelTransitionMotion.DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
        currentOnFinished.value(request.sequence)
    }

    Canvas(modifier) {
        val phase = progress.value
        val intensity = TvLiveChannelTransitionMotion.intensity(phase)
        if (phase >= .999f) return@Canvas

        val edgeX = size.width * TvLiveChannelTransitionMotion.edgeFraction(
            progress = phase,
            direction = request.direction,
        )
        val bandWidth = size.width.coerceAtLeast(1f) *
            TvLiveChannelTransitionMotion.BAND_WIDTH_FRACTION
        val bendAmplitude = size.width *
            TvLiveChannelTransitionMotion.WAVE_AMPLITUDE_FRACTION

        request.outgoingFrame?.let { frame ->
            val outgoingClip = Path()
            val segments = 20
            val firstBend = sin((phase * 9f).toDouble()).toFloat() * bendAmplitude
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
                val bend = sin((index * 1.19f + phase * 9f).toDouble()).toFloat() *
                    bendAmplitude
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
                drawRect(Color.Black.copy(alpha = .12f * intensity))
            }
        }

        drawRect(
            Color.Black.copy(
                alpha = TvLiveChannelTransitionMotion.MAX_DIM_ALPHA * intensity,
            )
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF83DCFF).copy(alpha = .08f * intensity),
                    Color.White.copy(alpha = .26f * intensity),
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
        val segments = 20
        repeat(segments + 1) { index ->
            val y = size.height * index / segments
            val bend = sin((index * 1.19f + phase * 9f).toDouble()).toFloat() *
                bendAmplitude
            val x = edgeX + bend
            if (index == 0) wave.moveTo(x, y) else wave.lineTo(x, y)
        }
        drawPath(
            path = wave,
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = .12f * intensity),
                    Color.Transparent,
                ),
                startX = edgeX - 18f,
                endX = edgeX + 18f,
            ),
            style = Stroke(width = 26f),
        )
        drawPath(
            path = wave,
            color = Color.White.copy(alpha = .24f * intensity),
            style = Stroke(width = 1.6f),
        )
    }
}
