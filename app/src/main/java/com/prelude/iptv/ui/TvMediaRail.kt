package com.prelude.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue

@Composable
fun TvMediaRail(
    section: CatalogRailSection,
    favoriteKeys: Set<String>,
    onOpen: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = rememberPresentationMetrics()
    Column(modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            color = IptvColors.TextPrimary,
            modifier = Modifier.padding(start = metrics.horizontalPadding, bottom = 13.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = metrics.horizontalPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing)
        ) {
            itemsIndexed(
                section.items,
                key = { index, item -> "tv:${section.id}:$index:${PlaybackQueue.favKey(item)}" }
            ) { index, channel ->
                TvMediaCard(
                    channel = channel,
                    rank = if (section.ranked) index + 1 else null,
                    progress = section.progress[PlaybackQueue.favKey(channel)],
                    favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                    onClick = { onOpen(channel) }
                )
            }
        }
    }
}

@Composable
private fun TvMediaCard(
    channel: Channel,
    rank: Int?,
    progress: Float?,
    favorite: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.width(238.dp).height(136.dp).clip(shape)
            .background(IptvColors.Surface)
            .border(1.dp, IptvColors.Divider, shape)
            .onFocusChanged { focused = it.isFocused }
            .tvFocus(shape, tint = false)
            .clickable(onClick = onClick)
    ) {
        if (channel.logo.isNotBlank()) {
            AsyncImage(channel.logo, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Outlined.Movie, null, tint = IptvColors.TextTertiary,
                modifier = Modifier.align(Alignment.Center).size(40.dp))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color(0xE3000000))))
        rank?.let {
            Text(
                it.toString(), color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 9.dp, bottom = 31.dp)
            )
        }
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 9.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (focused) {
                Box(Modifier.size(27.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(7.dp))
            }
            Text(
                channel.name,
                color = IptvColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (favorite) Icon(Icons.Default.Check, "Στη λίστα μου", tint = Color.White, modifier = Modifier.size(17.dp))
        }
        progress?.let { MediaProgress(it, Modifier.align(Alignment.BottomCenter)) }
    }
}
