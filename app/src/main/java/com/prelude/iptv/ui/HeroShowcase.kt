package com.prelude.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient

/** Dedicated touch-first home hero. TV has its own implementation. */
@Composable
fun HeroShowcase(
    items: List<Channel>,
    recents: List<Pair<Channel, Float>>,
    isFav: (Channel) -> Boolean,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?,
    onPlay: (Channel) -> Unit,
    onDetails: (Channel) -> Unit,
    onFav: (Channel) -> Unit
) {
    if (items.isEmpty()) return
    val heroes = remember(items) { items.filter { it.logo.isNotBlank() }.take(6).ifEmpty { items.take(6) } }
    val pager = rememberPagerState { heroes.size }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val heroHeight = when {
            maxHeight > 760.dp -> 510.dp
            maxWidth < 360.dp -> 410.dp
            else -> 460.dp
        }
        Column(Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pager,
                pageSpacing = 0.dp,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth().height(heroHeight)
            ) { page ->
                val channel = heroes[page]
                var meta by remember(channel) { mutableStateOf<TmdbClient.Meta?>(null) }
                LaunchedEffect(channel) { meta = tmdbFor(channel) }
                val image = meta?.backdrop?.takeIf { it.isNotBlank() } ?: channel.logo

                Box(Modifier.fillMaxSize().clickable { onDetails(channel) }) {
                    if (image.isNotBlank()) {
                        AsyncImage(
                            model = image,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f to Color(0x10000000),
                                .43f to Color(0x28000000),
                                .72f to Color(0xD0000000),
                                1f to IptvColors.Background
                            )
                        )
                    )
                    Column(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp)
                    ) {
                        Text(
                            TmdbClient.cleanTitle(channel.name).ifBlank { channel.name },
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            meta?.rating?.takeIf { it.isNotBlank() }?.let {
                                Text("$it Match", color = IptvColors.Success, style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(10.dp))
                            }
                            val year = meta?.year?.takeIf { it.isNotBlank() } ?: channel.year
                            if (year.isNotBlank()) Text(year, color = IptvColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                            val genre = meta?.genres?.takeIf { it.isNotBlank() } ?: channel.genre
                            if (genre.isNotBlank()) {
                                Spacer(Modifier.width(10.dp))
                                Text(genre, color = IptvColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        meta?.overview?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                it,
                                color = Color(0xFFD2D2D2),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(15.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            PrimaryMediaAction(
                                label = "Αναπαραγωγή",
                                icon = Icons.Default.PlayArrow,
                                onClick = { onPlay(channel) },
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryMediaAction(Icons.Default.Info, "Πληροφορίες", onClick = { onDetails(channel) })
                            SecondaryMediaAction(
                                icon = if (isFav(channel)) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Η λίστα μου",
                                selected = isFav(channel),
                                onClick = { onFav(channel) }
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                heroes.indices.forEach { index ->
                    Box(
                        Modifier.padding(horizontal = 3.dp).height(4.dp)
                            .width(if (index == pager.currentPage) 22.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (index == pager.currentPage) Color.White else Color.White.copy(alpha = .28f))
                    )
                }
            }

            if (recents.isNotEmpty()) {
                Text(
                    "Συνέχισε να βλέπεις",
                    color = IptvColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = AppDimens.MobileHorizontal, bottom = 11.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AppDimens.MobileHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recents, key = { it.first.name + it.first.url + it.first.seriesId }) { (channel, progress) ->
                        RecentCard(channel, progress) { onPlay(channel) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RecentCard(channel: Channel, progress: Float, onClick: () -> Unit) {
    Column(Modifier.width(172.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().height(98.dp).clip(RoundedCornerShape(AppDimens.CardRadius))
                .background(IptvColors.Surface).border(1.dp, IptvColors.Divider, RoundedCornerShape(AppDimens.CardRadius))
        ) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(channel.logo, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Outlined.Movie, null, tint = IptvColors.TextTertiary,
                    modifier = Modifier.align(Alignment.Center).size(30.dp))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color(0xC4000000))))
            Box(
                Modifier.align(Alignment.Center).size(38.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(25.dp)) }
            MediaProgress(
                progress = progress,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            TmdbClient.cleanTitle(channel.name).ifBlank { channel.name },
            color = IptvColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
