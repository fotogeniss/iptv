package com.prelude.iptv.ui.tv.live

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.live.LiveChannelArtwork
import com.prelude.iptv.ui.components.live.LiveProgressBar
import com.prelude.iptv.ui.components.live.liveNowNext
import com.prelude.iptv.ui.components.live.liveProgress
import com.prelude.iptv.ui.components.live.liveRemaining
import com.prelude.iptv.ui.components.live.liveTimeRange
import com.prelude.iptv.ui.localization.localizedLiveProgress
import com.prelude.iptv.ui.localization.localizedLiveRemaining
import com.prelude.iptv.ui.localization.localizedUppercase
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

@Composable
internal fun TvLiveHero(
    channel: Channel,
    nowMs: Long,
    favorite: Boolean,
    onPlay: () -> Unit,
    onEpg: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (now, _) = remember(channel.tvgId, nowMs / 30_000L) { liveNowNext(channel, nowMs) }
    Row(
        modifier.padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                stringResource(R.string.live_now_with_channel, localizedUppercase(channel.name)),
                color = Color.White.copy(alpha = .78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(9.dp))
            Text(
                now?.title ?: channel.name,
                color = Color.White,
                fontSize = 43.sp,
                lineHeight = 47.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                LivePill(stringResource(R.string.live_label), IptvColors.Primary)
                channel.group.takeIf { it.isNotBlank() }?.let { LivePill(it, Color.White.copy(alpha = .13f)) }
                Text(liveTimeRange(now), color = IptvColors.TextSecondary, fontSize = 13.sp)
            }
            val description = now?.desc?.takeIf { it.isNotBlank() }
                ?: channel.plot.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.live_broadcast_from, channel.name)
            Spacer(Modifier.height(12.dp))
            Text(
                description,
                color = Color.White.copy(alpha = .76f),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(.88f)
            )
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(.84f), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(localizedLiveRemaining(liveRemaining(now, nowMs)), color = IptvColors.TextSecondary, fontSize = 12.sp)
                Text(localizedLiveProgress(liveProgress(now, nowMs)), color = IptvColors.TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            LiveProgressBar(liveProgress(now, nowMs), Modifier.fillMaxWidth(.84f))
            Spacer(Modifier.height(17.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvLiveAction(stringResource(R.string.live_watch), Icons.Default.PlayArrow, primary = true, onClick = onPlay)
                TvLiveAction(stringResource(R.string.live_guide), Icons.Default.CalendarMonth, onClick = onEpg)
                TvLiveAction(
                    stringResource(if (favorite) R.string.live_favorite else R.string.live_add),
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    onClick = onFavorite
                )
            }
        }

        Box(
            Modifier.weight(.92f).fillMaxHeight(.88f).clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = .42f))
                .graphicsLayer { shadowElevation = 26.dp.toPx(); shape = RoundedCornerShape(22.dp); clip = true }
        ) {
            LiveChannelArtwork(
                channel = channel,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(.58f).fillMaxHeight(.52f)
            )
            Box(
                Modifier.align(Alignment.TopStart).padding(16.dp)
                    .background(IptvColors.Primary, RoundedCornerShape(6.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) { Text(stringResource(R.string.live_label), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black) }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp)) {
                Text(channel.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(now?.title ?: stringResource(R.string.live_broadcast), color = Color.White.copy(alpha = .78f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LivePill(label: String, background: Color) {
    Box(Modifier.background(background, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun TvLiveAction(
    label: String,
    icon: ImageVector,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusedScale = motionScale(Motion.TvActionScale)
    val scale by animateFloatAsState(
        if (focused) focusedScale else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "liveActionScale"
    )
    Row(
        Modifier
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                shadowElevation = if (focused) 16.dp.toPx() else 0f
                shape = RoundedCornerShape(9.dp)
                clip = true
            }
            .background(
                if (primary || focused) Color.White else Color.White.copy(alpha = .14f),
                RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .height(46.dp).padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (primary || focused) Color.Black else Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = if (primary || focused) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}
