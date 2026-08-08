package com.prelude.iptv.ui.mobile.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.live.LiveChannelArtwork
import com.prelude.iptv.ui.components.live.LiveFilterOption
import com.prelude.iptv.ui.components.live.LiveProgressBar
import com.prelude.iptv.ui.components.live.isSportsChannel
import com.prelude.iptv.ui.components.live.liveNowNext
import com.prelude.iptv.ui.components.live.liveProgress
import com.prelude.iptv.ui.components.live.liveTime
import com.prelude.iptv.ui.localization.localizedLabel

@Composable
internal fun MobileLiveFilterRow(
    filters: List<LiveFilterOption>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters, key = { it.id }) { item ->
            val selected = item.id == selectedId
            Text(
                item.localizedLabel(),
                color = if (selected) Color.White else IptvColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier
                    .background(if (selected) IptvColors.Primary else IptvColors.Surface, RoundedCornerShape(99.dp))
                    .clickable { onSelect(item.id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
internal fun MobileLiveRailSection(
    title: String,
    subtitle: String,
    channels: List<Channel>,
    selected: Channel,
    favoriteKeys: Set<String>,
    nowMs: Long,
    keyOf: (Channel) -> String,
    onSelect: (Channel) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = IptvColors.TextTertiary, fontSize = 10.5.sp)
        }
        if (channels.isEmpty()) {
            Text(
                stringResource(R.string.live_empty_filter),
                color = IptvColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(channels, key = keyOf) { channel ->
                    MobileLiveChannelCard(
                        channel = channel,
                        selected = keyOf(channel) == keyOf(selected),
                        favorite = keyOf(channel) in favoriteKeys,
                        nowMs = nowMs,
                        onClick = { onSelect(channel) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun MobileSportsRail(
    channels: List<Channel>,
    selected: Channel,
    favoriteKeys: Set<String>,
    nowMs: Long,
    keyOf: (Channel) -> String,
    onSelect: (Channel) -> Unit
) {
    val sports = remember(channels) { channels.filter(::isSportsChannel).take(20) }
    if (sports.isNotEmpty()) {
        MobileLiveRailSection(
            title = stringResource(R.string.live_sports_heading),
            subtitle = pluralStringResource(
                R.plurals.live_broadcast_count,
                sports.size,
                sports.size,
            ),
            channels = sports,
            selected = selected,
            favoriteKeys = favoriteKeys,
            nowMs = nowMs,
            keyOf = keyOf,
            onSelect = onSelect
        )
    }
}

@Composable
private fun MobileLiveChannelCard(
    channel: Channel,
    selected: Boolean,
    favorite: Boolean,
    nowMs: Long,
    onClick: () -> Unit
) {
    val (now, _) = remember(channel.tvgId, nowMs / 30_000L) { liveNowNext(channel, nowMs) }
    Column(
        Modifier.width(150.dp).clip(RoundedCornerShape(13.dp))
            .background(if (selected) Color.White.copy(alpha = .12f) else IptvColors.Surface)
            .clickable(onClick = onClick).padding(9.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(9.dp))
        ) {
            LiveChannelArtwork(channel, Modifier.fillMaxWidth().height(68.dp), selected = false)
            Box(
                Modifier.align(Alignment.TopStart).padding(5.dp)
                    .background(IptvColors.Primary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) { Text(stringResource(R.string.live_label), color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black) }
            if (favorite) {
                Text(
                    "♥",
                    color = IptvColors.Primary,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(channel.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(
            now?.title ?: channel.group.ifBlank { stringResource(R.string.live_broadcast) },
            color = IptvColors.TextTertiary,
            fontSize = 9.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(7.dp))
        LiveProgressBar(liveProgress(now, nowMs), Modifier.fillMaxWidth(), height = 3)
    }
}

@Composable
internal fun MobileQuickGuideHeader(
    channelCount: Int,
    onOpenEpg: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(IptvColors.Surface)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(stringResource(R.string.live_quick_guide), color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Black)
            Text(
                pluralStringResource(
                    R.plurals.live_group_channel_count,
                    channelCount,
                    channelCount,
                ),
                color = IptvColors.TextTertiary,
                fontSize = 9.5.sp
            )
        }
        Text(
            stringResource(R.string.live_full_epg),
            color = Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(IptvColors.SurfaceRaised, RoundedCornerShape(9.dp))
                .clickable(onClick = onOpenEpg)
                .padding(horizontal = 11.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun MobileQuickGuideRow(
    channel: Channel,
    nowMs: Long,
    selected: Boolean,
    last: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (now, _) = remember(channel.tvgId, nowMs / 30_000L) { liveNowNext(channel, nowMs) }
    val rowShape = if (last) {
        RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(0.dp)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (selected) Color.White.copy(alpha = .055f) else IptvColors.Surface)
            .clickable(onClick = onSelect)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveChannelArtwork(
            channel,
            Modifier
                .size(width = 48.dp, height = 36.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                channel.name,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                now?.title ?: stringResource(R.string.live_broadcast),
                color = IptvColors.TextTertiary,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (now != null) {
                Spacer(Modifier.height(5.dp))
                LiveProgressBar(liveProgress(now, nowMs), Modifier.fillMaxWidth(), height = 3)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(now?.let { liveTime(it.startMs) }.orEmpty(), color = IptvColors.TextSecondary, fontSize = 9.5.sp)
        Spacer(Modifier.width(5.dp))
        Icon(
            Icons.Default.PlayArrow,
            stringResource(R.string.live_watch_channel, channel.name),
            tint = IptvColors.TextTertiary,
            modifier = Modifier.size(15.dp)
        )
    }
}
