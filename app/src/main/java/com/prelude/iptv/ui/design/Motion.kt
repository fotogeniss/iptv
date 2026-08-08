package com.prelude.iptv.ui.design

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Shared motion tokens for every Mobile and Android TV surface.
 *
 * Durations are intentionally semantic. Screens should select a token based on
 * the role of an animation instead of introducing local millisecond values.
 */
object Motion {
    const val Fast = 160
    // Πιο ομαλή μετάβαση focus στα TV cards/chips (πριν 180ms — «πεταγόταν» πολύ
    // γρήγορα από κάρτα σε κάρτα). Το mobile press χρησιμοποιεί Fast, δεν επηρεάζεται.
    const val Focus = 250
    const val Medium = 280
    const val Overlay = 320
    const val Slow = 460
    const val Hero = 680

    const val MobilePressedScale = 0.975f
    const val TvFocusScale = 1.04f
    const val TvEmphasisScale = 1.075f
    const val TvActionScale = 1.055f

    val StandardEasing: Easing = CubicBezierEasing(0.20f, 0.80f, 0.20f, 1.00f)
    val EmphasizedEasing: Easing = CubicBezierEasing(0.16f, 1.00f, 0.30f, 1.00f)

    /** Returns an effectively instant duration when reduced motion is enabled. */
    fun duration(token: Int, reducedMotion: Boolean): Int = if (reducedMotion) 1 else token

    /** Removes decorative scale while preserving focus state and contrast. */
    fun scale(target: Float, reducedMotion: Boolean): Float = if (reducedMotion) 1f else target
}

/** True when the Android system animator scale is disabled. */
fun isReducedMotionEnabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) == 0f
}.getOrDefault(false)

fun viewMotionDuration(context: Context, token: Int): Long =
    Motion.duration(token, isReducedMotionEnabled(context)).toLong()

fun viewMotionScale(context: Context, target: Float): Float =
    Motion.scale(target, isReducedMotionEnabled(context))

val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Provides the system reduced-motion preference to the Compose hierarchy.
 * Android exposes this preference through the animator duration scale.
 */
@Composable
fun MotionSystem(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion, content = content)
}

@Composable
fun motionDuration(token: Int): Int = Motion.duration(token, LocalReducedMotion.current)

@Composable
fun motionScale(target: Float): Float = Motion.scale(target, LocalReducedMotion.current)


/** Shared skeleton shimmer. Reduced motion renders a stable placeholder. */
@Composable
fun MotionSkeleton(
    modifier: Modifier = Modifier,
    baseColor: Color,
    highlightColor: Color
) {
    if (LocalReducedMotion.current) {
        Box(modifier.background(baseColor))
        return
    }
    val transition = rememberInfiniteTransition(label = "premiumSkeleton")
    val offset = transition.animateFloat(
        initialValue = -500f,
        targetValue = 1_500f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.Hero * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "premiumSkeletonOffset"
    ).value
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(offset - 420f, 0f),
                end = Offset(offset, 420f)
            )
        )
    )
}
