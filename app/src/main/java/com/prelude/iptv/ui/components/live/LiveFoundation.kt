@file:android.annotation.SuppressLint("ProduceStateDoesNotAssignValue")

package com.prelude.iptv.ui.components.live

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.LiveTvPolicy
import com.prelude.iptv.ui.components.epg.epgPalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration

@Immutable
data class LiveFilterOption(
    val id: String,
    /** Null for app-owned filters; provider labels remain untranslated data. */
    val providerLabel: String? = null,
)

data class LiveRemaining(val hours: Int, val minutes: Int)

@Composable
fun rememberLiveNow(tickMs: Long = 30_000L): Long {
    val value by produceState(initialValue = System.currentTimeMillis(), key1 = tickMs) {
        while (true) {
            value = System.currentTimeMillis()
            delay(tickMs)
        }
    }
    return value
}

fun liveFilterOptions(channels: List<Channel>): List<LiveFilterOption> {
    // Όλα τα groups που κατέβασε ο χρήστης (scrollable & επιλέξιμα) — χωρίς cap.
    val groups = channels.asSequence()
        .map { it.group.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .map { LiveFilterOption("group:$it", providerLabel = it) }
        .toList()
    return listOf(
        LiveFilterOption("all"),
        LiveFilterOption("favorites"),
        LiveFilterOption("recent")
    ) + groups
}

fun liveVisibleChannels(
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    recentKeys: Set<String>,
    keyOf: (Channel) -> String,
    filterId: String
): List<Channel> = LiveTvPolicy.filter(channels, favoriteKeys, recentKeys, keyOf, filterId)

fun liveNowNext(channel: Channel, nowMs: Long): Pair<EpgManager.Prog?, EpgManager.Prog?> =
    if (channel.tvgId.isBlank()) null to null else EpgManager.nowNext(channel.tvgId, nowMs)

fun liveProgress(programme: EpgManager.Prog?, nowMs: Long): Float = LiveTvPolicy.progress(programme, nowMs)

fun liveTime(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

fun liveTimeRange(programme: EpgManager.Prog?): String =
    programme?.let { "${liveTime(it.startMs)} – ${liveTime(it.stopMs)}" }.orEmpty()

fun liveRemaining(programme: EpgManager.Prog?, nowMs: Long): LiveRemaining? {
    if (programme == null) return null
    val minutes = ((programme.stopMs - nowMs).coerceAtLeast(0L) / 60_000L).toInt()
    return LiveRemaining(hours = minutes / 60, minutes = minutes % 60)
}

fun isSportsChannel(channel: Channel): Boolean {
    val source = "${channel.group} ${channel.name} ${channel.genre}".lowercase(Locale.getDefault())
    return listOf("sport", "sports", "αθλη", "ποδόσφ", "football", "basket").any(source::contains)
}

@Composable
fun LiveCinematicBackdrop(
    channel: Channel,
    mobile: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = epgPalette(channel)
    val primary by animateColorAsState(palette.primary, tween(motionDuration(Motion.Slow), easing = Motion.StandardEasing), label = "livePrimary")
    val secondary by animateColorAsState(palette.secondary, tween(motionDuration(Motion.Slow), easing = Motion.StandardEasing), label = "liveSecondary")
    val surface by animateColorAsState(palette.surface, tween(motionDuration(Motion.Slow), easing = Motion.StandardEasing), label = "liveSurface")

    Box(
        modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(IptvColors.Background, surface, IptvColors.Background))
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = if (mobile) .48f else .58f), Color.Transparent),
                    radius = if (mobile) 820f else 1180f,
                    center = androidx.compose.ui.geometry.Offset(if (mobile) 550f else 1460f, 250f)
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = .30f), Color.Transparent),
                    radius = if (mobile) 650f else 900f,
                    center = androidx.compose.ui.geometry.Offset(if (mobile) 170f else 1060f, 500f)
                )
            )
        )
        Crossfade(targetState = channel.logo, animationSpec = tween(motionDuration(Motion.Hero), easing = Motion.EmphasizedEasing), label = "liveBackdropLogo") { logo ->
            if (logo.isNotBlank()) {
                AsyncImage(
                    model = logo,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alpha = if (mobile) .20f else .18f,
                    modifier = Modifier
                        .align(if (mobile) Alignment.TopCenter else Alignment.TopEnd)
                        .fillMaxSize(if (mobile) .82f else .58f)
                        .padding(if (mobile) 44.dp else 58.dp)
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                if (mobile) {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = .12f),
                        .50f to Color.Transparent,
                        .78f to IptvColors.Background.copy(alpha = .82f),
                        1f to IptvColors.Background
                    )
                } else {
                    Brush.horizontalGradient(
                        0f to IptvColors.Background.copy(alpha = .98f),
                        .37f to IptvColors.Background.copy(alpha = .82f),
                        .70f to IptvColors.Background.copy(alpha = .20f),
                        1f to IptvColors.Background.copy(alpha = .58f)
                    )
                }
            )
        )
        if (!mobile) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = .15f),
                        .60f to Color.Transparent,
                        .80f to IptvColors.Background.copy(alpha = .84f),
                        1f to IptvColors.Background
                    )
                )
            )
        }
    }
}

@Composable
fun LiveChannelArtwork(
    channel: Channel,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    var failed by remember(channel.logo) { mutableStateOf(false) }
    val background = if (selected) Color.White else IptvColors.SurfaceRaised
    val foreground = if (selected) Color.Black else IptvColors.TextSecondary
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(background),
        contentAlignment = Alignment.Center
    ) {
        if (channel.logo.isNotBlank() && !failed) {
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize().padding(7.dp)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.LiveTv, null, tint = foreground, modifier = Modifier.size(23.dp))
                Spacer(Modifier.size(2.dp))
                Text(
                    channel.name,
                    color = foreground,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 5.dp)
                )
            }
        }
    }
}
