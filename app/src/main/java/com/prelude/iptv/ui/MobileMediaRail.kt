package com.prelude.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun MobileMediaRail(
    section: CatalogRailSection,
    favoriteKeys: Set<String>,
    onOpen: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = rememberPresentationMetrics()
    val landscape = section.progress.isNotEmpty()
    Column(modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            color = IptvColors.TextPrimary,
            modifier = Modifier.padding(start = metrics.horizontalPadding, bottom = 9.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = metrics.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing)
        ) {
            itemsIndexed(
                section.items,
                key = { index, item -> "mobile:${section.id}:$index:${PlaybackQueue.favKey(item)}" }
            ) { index, channel ->
                MobileMediaCard(
                    channel = channel,
                    landscape = landscape,
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
private fun MobileMediaCard(
    channel: Channel,
    landscape: Boolean,
    rank: Int?,
    progress: Float?,
    favorite: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    val width = if (landscape) 172.dp else 124.dp
    val height = if (landscape) 98.dp else 184.dp
    Column(Modifier.width(width).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().height(height).clip(shape)
                .background(IptvColors.Surface)
                .border(1.dp, IptvColors.Divider, shape)
        ) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Outlined.Movie, null, tint = IptvColors.TextTertiary,
                    modifier = Modifier.align(Alignment.Center))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color(0xB9000000))))
            rank?.let {
                Text(
                    text = it.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                )
            }
            if (favorite) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(7.dp).clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = .72f)).padding(5.dp),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Check, "Στη λίστα μου", tint = Color.White) }
            }
            progress?.let {
                MediaProgress(
                    progress = it,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 7.dp)
                )
            }
        }
        Text(
            text = channel.name,
            color = IptvColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}
