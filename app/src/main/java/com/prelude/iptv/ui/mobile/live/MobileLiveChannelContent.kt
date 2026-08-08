package com.prelude.iptv.ui.mobile.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.IptvColors

@Composable
internal fun ChannelGrid(
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    keyOf: (Channel) -> String,
    onPlay: (Channel) -> Unit,
    nowTextFor: (Channel) -> String?,
    bottomPadding: Dp,
    state: LazyGridState,
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(channels, key = { keyOf(it).ifBlank { it.name } }) { channel ->
            LiveCard(
                channel = channel,
                favorite = keyOf(channel) in favoriteKeys,
                epgNow = nowTextFor(channel),
                onClick = { onPlay(channel) },
                fixedWidth = false
            )
        }
    }
}

@Composable
internal fun ChannelList(
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    keyOf: (Channel) -> String,
    onPlay: (Channel) -> Unit,
    nowTextFor: (Channel) -> String?,
    bottomPadding: Dp,
    state: LazyListState,
) {
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(channels, key = { keyOf(it).ifBlank { it.name } }) { channel ->
            LiveListCard(
                channel = channel,
                favorite = keyOf(channel) in favoriteKeys,
                epgNow = nowTextFor(channel),
                onClick = { onPlay(channel) },
            )
        }
    }
}

@Composable
private fun LiveListCard(
    channel: Channel,
    favorite: Boolean,
    epgNow: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(72.dp)
                .height(92.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IptvColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                )
            } else {
                Icon(
                    Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = IptvColors.TextTertiary.copy(alpha = .5f),
                    modifier = Modifier.size(30.dp),
                )
            }
            if (favorite) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(IptvColors.Primary)
                )
            }
        }
        Column(
            Modifier.weight(1f).padding(start = 14.dp, end = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                channel.name,
                color = IptvColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                epgNow?.takeIf(String::isNotBlank)?.let { "Τώρα · $it" }
                    ?: "Χωρίς διαθέσιμο EPG",
                color = if (epgNow.isNullOrBlank()) IptvColors.TextTertiary else IptvColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (epgNow.isNullOrBlank()) FontWeight.Normal else FontWeight.SemiBold,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun LiveCard(
    channel: Channel,
    favorite: Boolean,
    epgNow: String?,
    onClick: () -> Unit,
    fixedWidth: Boolean = true,
) {
    Column(
        Modifier
            .then(if (fixedWidth) Modifier.width(106.dp) else Modifier.fillMaxWidth())
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .clip(RoundedCornerShape(12.dp))
                .background(IptvColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(14.dp)
                )
            } else {
                Icon(
                    Icons.Default.LiveTv, null,
                    tint = IptvColors.TextTertiary.copy(alpha = .5f),
                    modifier = Modifier.size(36.dp)
                )
            }
            if (favorite) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(IptvColors.Primary)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            channel.name,
            color = IptvColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 32.dp),
        )
        Text(
            epgNow?.takeIf(String::isNotBlank)?.let { "Τώρα · $it" }
                ?: "Χωρίς διαθέσιμο EPG",
            color = if (epgNow.isNullOrBlank()) IptvColors.TextTertiary else IptvColors.TextSecondary,
            fontSize = 9.5.sp,
            fontWeight = if (epgNow.isNullOrBlank()) FontWeight.Normal else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 24.dp).padding(top = 3.dp),
        )
    }
}

@Composable
internal fun EmptyLive(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = IptvColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}
