package com.prelude.iptv.ui.mobile.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.live.LiveCinematicBackdrop
import com.prelude.iptv.ui.components.live.LiveProgressBar
import com.prelude.iptv.ui.components.live.liveNowNext
import com.prelude.iptv.ui.components.live.liveProgress
import com.prelude.iptv.ui.components.live.liveRemaining
import com.prelude.iptv.ui.components.live.liveTimeRange
import com.prelude.iptv.ui.localization.localizedLiveProgress
import com.prelude.iptv.ui.localization.localizedLiveRemaining

@Composable
internal fun MobileLiveHero(
    channel: Channel,
    nowMs: Long,
    favorite: Boolean,
    onPlay: () -> Unit,
    onEpg: () -> Unit,
    onFavorite: () -> Unit,
    onSearch: () -> Unit
) {
    val (now, _) = remember(channel.tvgId, nowMs / 30_000L) { liveNowNext(channel, nowMs) }
    Box(Modifier.fillMaxWidth().height(340.dp)) {
        LiveCinematicBackdrop(channel, mobile = true, modifier = Modifier.fillMaxSize())
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(width = 8.dp, height = 22.dp)
                        .background(IptvColors.Primary, RoundedCornerShape(2.dp))
                )
                Text(
                    "PRELUDE+",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .6.sp
                )
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = .34f))
                    .clickable(onClick = onSearch),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Search,
                    stringResource(R.string.live_search),
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.background(IptvColors.Primary, RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) { Text(stringResource(R.string.live_badge), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                Text(channel.name, color = Color.White.copy(alpha = .80f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(9.dp))
            Text(
                now?.title ?: channel.name,
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                listOf(channel.group, liveTimeRange(now)).filter { it.isNotBlank() }.joinToString(" · "),
                color = Color.White.copy(alpha = .72f),
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val description = now?.desc?.takeIf { it.isNotBlank() }
                ?: channel.plot.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.live_broadcast_from, channel.name)
            Spacer(Modifier.height(7.dp))
            Text(description, color = Color.White.copy(alpha = .66f), fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(localizedLiveRemaining(liveRemaining(now, nowMs)), color = IptvColors.TextSecondary, fontSize = 10.sp)
                Text(localizedLiveProgress(liveProgress(now, nowMs)), color = IptvColors.TextSecondary, fontSize = 10.sp)
            }
            Spacer(Modifier.height(5.dp))
            LiveProgressBar(liveProgress(now, nowMs), Modifier.fillMaxWidth(), height = 3)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MobileLiveAction(stringResource(R.string.live_watch), Icons.Default.PlayArrow, true, onPlay, Modifier.weight(1f))
                MobileLiveAction(stringResource(R.string.live_favorite), if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, false, onFavorite)
                MobileLiveAction(stringResource(R.string.live_epg), Icons.Default.CalendarMonth, false, onEpg)
            }
        }
    }
}

@Composable
private fun MobileLiveAction(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .then(if (primary) Modifier else Modifier.width(48.dp))
            .height(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .then(if (primary) Modifier.fillMaxWidth() else Modifier.width(40.dp))
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (primary) Color.White else Color.White.copy(alpha = .14f))
                .padding(horizontal = if (primary) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, label, tint = if (primary) Color.Black else Color.White, modifier = Modifier.size(16.dp))
            if (primary) {
                Spacer(Modifier.width(5.dp))
                Text(label, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
