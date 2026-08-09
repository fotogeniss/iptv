@file:android.annotation.SuppressLint("ProduceStateDoesNotAssignValue")

package com.prelude.iptv.ui.components.epg

import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.localization.localizedEpgRuntime
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Date

@Immutable
data class EpgWindow(
    val startMs: Long,
    val endMs: Long,
    val totalMinutes: Int
)

@Immutable
data class EpgPalette(
    val primary: Color,
    val secondary: Color,
    val surface: Color
)

enum class EpgProgramVisualState {
    Default,
    Focused,
    Live,
    Recording,
    Selected
}

@Composable
fun rememberEpgNow(tickMs: Long = 30_000L): Long {
    val now by produceState(initialValue = System.currentTimeMillis(), key1 = tickMs) {
        while (true) {
            value = System.currentTimeMillis()
            delay(tickMs)
        }
    }
    return now
}

fun epgWindow(nowMs: Long, hoursSpan: Int = 6): EpgWindow {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = nowMs
        add(Calendar.MINUTE, -30)
        set(Calendar.MINUTE, if (get(Calendar.MINUTE) < 30) 0 else 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val totalMinutes = hoursSpan * 60
    return EpgWindow(
        startMs = calendar.timeInMillis,
        endMs = calendar.timeInMillis + totalMinutes * 60_000L,
        totalMinutes = totalMinutes
    )
}

fun programmesFor(channel: Channel, window: EpgWindow): List<EpgManager.Prog> =
    if (channel.tvgId.isBlank()) emptyList()
    else EpgManager.programmes(channel.tvgId, window.startMs, window.endMs)

@Composable
fun epgTime(ms: Long): String =
    DateFormat.getTimeFormat(LocalContext.current).format(Date(ms))

@Composable
fun epgTimeRange(programme: EpgManager.Prog): String =
    "${epgTime(programme.startMs)} – ${epgTime(programme.stopMs)}"

@Composable
fun epgRuntime(programme: EpgManager.Prog): String {
    val minutes = ((programme.stopMs - programme.startMs) / 60_000L).coerceAtLeast(1L)
    return localizedEpgRuntime(minutes)
}

fun epgProgress(programme: EpgManager.Prog, nowMs: Long): Float {
    val duration = (programme.stopMs - programme.startMs).coerceAtLeast(1L)
    return ((nowMs - programme.startMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

fun epgPalette(channel: Channel): EpgPalette {
    val palettes = listOf(
        EpgPalette(Color(0xFF527C89), Color(0xFF8C4939), Color(0xFF14282F)),
        EpgPalette(Color(0xFF9B3D45), Color(0xFF51316F), Color(0xFF2A1118)),
        EpgPalette(Color(0xFF657DB9), Color(0xFFB08A45), Color(0xFF111B31)),
        EpgPalette(Color(0xFF40949A), Color(0xFF225A66), Color(0xFF09282D)),
        EpgPalette(Color(0xFFB47542), Color(0xFF5E835A), Color(0xFF321C0E)),
        EpgPalette(Color(0xFFA84D91), Color(0xFF6376B3), Color(0xFF291327))
    )
    return palettes[(channel.name.hashCode() and Int.MAX_VALUE) % palettes.size]
}

@Composable
fun EpgCinematicBackdrop(
    channel: Channel,
    modifier: Modifier = Modifier
) {
    val target = epgPalette(channel)
    val primary by animateColorAsState(target.primary, tween(motionDuration(Motion.Slow), easing = Motion.StandardEasing), label = "epgPrimary")
    val secondary by animateColorAsState(target.secondary, tween(motionDuration(Motion.Slow), easing = Motion.StandardEasing), label = "epgSecondary")
    val surface by animateColorAsState(target.surface, tween(motionDuration(Motion.Slow), easing = Motion.StandardEasing), label = "epgSurface")

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(IptvColors.Background, surface, IptvColors.Background)
                )
            )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.46f), Color.Transparent),
                        radius = 980f,
                        center = androidx.compose.ui.geometry.Offset(1500f, 190f)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = 0.30f), Color.Transparent),
                        radius = 820f,
                        center = androidx.compose.ui.geometry.Offset(1050f, 400f)
                    )
                )
        )
        Crossfade(targetState = channel.logo, animationSpec = tween(motionDuration(Motion.Hero), easing = Motion.EmphasizedEasing), label = "epgLogoBackdrop") { logo ->
            if (logo.isNotBlank()) {
                AsyncImage(
                    model = logo,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alpha = 0.18f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxWidth(0.52f)
                        .fillMaxHeight(0.62f)
                        .padding(40.dp)
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to IptvColors.Background,
                        0.36f to IptvColors.Background.copy(alpha = 0.92f),
                        0.72f to IptvColors.Background.copy(alpha = 0.30f),
                        1f to IptvColors.Background.copy(alpha = 0.68f)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.18f),
                        0.48f to Color.Transparent,
                        0.78f to IptvColors.Background.copy(alpha = 0.82f),
                        1f to IptvColors.Background
                    )
                )
        )
    }
}

@Composable
fun EpgChannelLogo(
    channel: Channel,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White else IptvColors.SurfaceRaised),
        contentAlignment = Alignment.Center
    ) {
        if (channel.logo.isNotBlank()) {
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(7.dp)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.LiveTv,
                    contentDescription = null,
                    tint = if (selected) Color.Black else IptvColors.TextSecondary,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    channel.name,
                    color = if (selected) Color.Black else IptvColors.TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EpgProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .height(3.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.18f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(IptvColors.Primary)
        )
    }
}
