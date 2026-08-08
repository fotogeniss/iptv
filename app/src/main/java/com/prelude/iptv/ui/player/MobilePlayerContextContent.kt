package com.prelude.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.ContentQualityPolicy
import com.prelude.iptv.data.SubtitleSearchPolicy
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.rememberEpisodeMeta
import com.prelude.iptv.ui.mobile.details.MobileSeasonHeader

@Composable
internal fun MobilePlayerContextContent(
    playing: Channel,
    infoChannel: Channel,
    metadata: TmdbClient.Meta?,
    related: List<Channel>,
    seasons: List<Pair<String, List<Channel>>>,
    playingQuality: String,
    onPlay: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 34.dp)
    ) {
        if (playing.kind == "live") {
            PlayerSectionTitle(
                title = "Περισσότερα από ${playing.group.ifBlank { "την ίδια ομάδα" }}",
                trailing = "${related.size} κανάλια",
            )
            PlayerChannelRail(playing, related, onPlay)
            return@Column
        }

        PlayerInfo(infoChannel, metadata)

        if (playing.kind == "series_ep" || infoChannel.kind == "series") {
            PlayerEpisodes(
                seasons = seasons,
                playing = playing,
                // Keep the raw provider title here: TmdbClient extracts both its year and
                // provider suffixes before matching the series.
                seriesTitle = infoChannel.name,
                seriesYear = metadata?.year?.takeIf(String::isNotBlank) ?: infoChannel.year,
                playingQuality = playingQuality,
                onPlay = onPlay,
            )
            PlayerSectionTitle("Προτεινόμενες σειρές")
        } else {
            PlayerSectionTitle("Προτεινόμενες ταινίες")
        }
        PlayerPosterRail(related, onPlay)
    }
}

@Composable
private fun PlayerInfo(channel: Channel, metadata: TmdbClient.Meta?) {
    val overview = metadata?.overview?.takeIf(String::isNotBlank)
        ?: channel.plot.takeIf(String::isNotBlank)
    val genres = metadata?.genres?.takeIf(String::isNotBlank)
        ?: channel.genre.takeIf(String::isNotBlank)
    val year = metadata?.year?.takeIf(String::isNotBlank)
        ?: channel.year.takeIf(String::isNotBlank)
    val quality = ContentQualityPolicy.label(channel.name, channel.group)
    var descriptionExpanded by remember(channel, overview) { mutableStateOf(false) }
    var descriptionCanExpand by remember(channel, overview) { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(channel.name, color = IptvColors.TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            metadata?.rating?.takeIf(String::isNotBlank)?.let {
                Text("★ $it", color = IptvColors.Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            year?.let { Text(it, color = IptvColors.TextSecondary, fontSize = 12.sp) }
            channel.duration.takeIf(String::isNotBlank)?.let {
                Text(it, color = IptvColors.TextSecondary, fontSize = 12.sp)
            }
            if (quality.isNotBlank()) {
                Text(
                    quality,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = .72f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        overview?.let {
            Spacer(Modifier.height(13.dp))
            Text(
                text = it,
                color = Color(0xFFD0D0D0),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                overflow = if (descriptionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (!descriptionExpanded) descriptionCanExpand = result.hasVisualOverflow
                },
            )
            if (descriptionCanExpand) {
                Text(
                    text = if (descriptionExpanded) "Λιγότερα" else "Περισσότερα",
                    color = IptvColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clickable { descriptionExpanded = !descriptionExpanded }
                        .padding(top = 6.dp, bottom = 2.dp),
                )
            }
        }
        val cast = metadata?.cast?.take(4)?.joinToString { it.name }
            ?.takeIf(String::isNotBlank) ?: channel.cast.takeIf(String::isNotBlank)
        if (cast != null || genres != null) {
            Spacer(Modifier.height(11.dp))
            cast?.let { PlayerCredit("Πρωταγωνιστούν", it) }
            genres?.let { PlayerCredit("Είδη", it) }
        }
    }
}

@Composable
private fun PlayerCredit(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = IptvColors.TextTertiary,
        fontSize = 11.sp,
        lineHeight = 17.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PlayerSectionTitle(title: String, trailing: String = "") {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, color = IptvColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        if (trailing.isNotBlank()) Text(trailing, color = IptvColors.TextTertiary, fontSize = 11.sp)
    }
}

@Composable
private fun PlayerChannelRail(playing: Channel, channels: List<Channel>, onPlay: (Channel) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(channels, key = { "${it.tvgId}:${it.name}:${it.url}" }) { channel ->
            Column(Modifier.width(112.dp).clickable { onPlay(channel) }) {
                Box(
                    Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (channel == playing) IptvColors.SurfaceSelected else IptvColors.Surface),
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
                        Icon(Icons.Default.LiveTv, null, tint = IptvColors.TextTertiary, modifier = Modifier.size(30.dp))
                    }
                }
                Text(channel.name, color = IptvColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                Text(if (channel == playing) "Παίζει τώρα" else channel.group, color = IptvColors.TextTertiary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PlayerPosterRail(channels: List<Channel>, onPlay: (Channel) -> Unit) {
    if (channels.isEmpty()) {
        Text("Δεν υπάρχουν ακόμη προτάσεις.", color = IptvColors.TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(channels, key = { "${it.kind}:${it.seriesId}:${it.name}:${it.url}" }) { channel ->
            val quality = ContentQualityPolicy.label(channel.name, channel.group)
            Column(Modifier.width(118.dp).clickable { onPlay(channel) }) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(.68f).clip(RoundedCornerShape(11.dp)).background(IptvColors.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (channel.logo.isNotBlank()) {
                        AsyncImage(channel.logo, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.PlayArrow, null, tint = IptvColors.TextTertiary, modifier = Modifier.size(36.dp))
                    }
                    if (quality.isNotBlank()) {
                        Text(
                            quality,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(7.dp)
                                .background(Color.Black.copy(alpha = .68f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = .72f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    channel.name,
                    color = IptvColors.TextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 7.dp).heightIn(min = 30.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayerEpisodes(
    seasons: List<Pair<String, List<Channel>>>,
    playing: Channel,
    seriesTitle: String,
    seriesYear: String,
    playingQuality: String,
    onPlay: (Channel) -> Unit,
) {
    if (seasons.isEmpty()) return
    var selectedSeason by remember(playing, seasons) {
        mutableIntStateOf(
            seasons.indexOfFirst { (_, episodes) -> playing in episodes }.coerceAtLeast(0)
        )
    }
    var episodesDescending by remember(seriesTitle) { mutableStateOf(false) }
    MobileSeasonHeader(
        seasons = seasons,
        selected = selectedSeason,
        descending = episodesDescending,
        onSelected = { selectedSeason = it },
        onToggleOrder = { episodesDescending = !episodesDescending },
    )
    val episodes = seasons.getOrNull(selectedSeason)?.second.orEmpty()
    val displayedEpisodes = remember(episodes, episodesDescending) {
        episodes.mapIndexed { sourceIndex, episode -> sourceIndex to episode }
            .let { indexed -> if (episodesDescending) indexed.asReversed() else indexed }
    }
    val seasonNumber = SubtitleSearchPolicy.seasonNumber(
        seasons.getOrNull(selectedSeason)?.first.orEmpty(),
        selectedSeason + 1,
    ) ?: selectedSeason + 1
    val episodeListState = rememberLazyListState()
    LaunchedEffect(selectedSeason, playing, displayedEpisodes) {
        if (displayedEpisodes.isNotEmpty()) {
            val playingIndex = displayedEpisodes.indexOfFirst { (_, episode) -> episode == playing }
                .coerceAtLeast(0)
            episodeListState.scrollToItem(playingIndex)
        }
    }
    LazyColumn(
        state = episodeListState,
        modifier = Modifier
            .fillMaxWidth()
            // Τρεις σειρές των 156dp: τα υπόλοιπα επεισόδια κυλούν ΜΕΣΑ εδώ,
            // ώστε οι προτάσεις να μένουν αμέσως από κάτω.
            .height(468.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF111111))
            .border(1.dp, IptvColors.Divider, RoundedCornerShape(15.dp)),
    ) {
        itemsIndexed(
            items = displayedEpisodes,
            key = { _, item ->
                val (sourceIndex, episode) = item
                "player-episode:$selectedSeason:$sourceIndex:${episode.name}:${episode.url}"
            },
        ) { _, item ->
            val (sourceIndex, episode) = item
            val episodeNumber = SubtitleSearchPolicy.episodeNumber(episode.name, sourceIndex + 1)
                ?: sourceIndex + 1
            val tmdbEpisode = rememberEpisodeMeta(
                seriesTitle = seriesTitle,
                seriesYear = seriesYear,
                season = seasonNumber,
                episodeNumber = episodeNumber,
            )
            val artwork = tmdbEpisode?.still?.takeIf(String::isNotBlank) ?: episode.logo
            val title = tmdbEpisode?.title?.takeIf(String::isNotBlank) ?: episode.name
            val overview = tmdbEpisode?.overview?.takeIf(String::isNotBlank) ?: episode.plot
            val quality = ContentQualityPolicy.label(episode.name, episode.group)
                .ifBlank { playingQuality }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clickable { onPlay(episode) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.width(138.dp).height(88.dp).clip(RoundedCornerShape(9.dp))
                        .background(if (episode == playing) IptvColors.SurfaceSelected else IptvColors.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (artwork.isNotBlank()) {
                        AsyncImage(artwork, title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.PlayArrow, null, tint = IptvColors.TextSecondary)
                    }
                    Text(
                        "EP $episodeNumber",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(7.dp)
                            .background(Color.Black.copy(alpha = .62f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    if (quality.isNotBlank()) {
                        Text(
                            quality,
                            color = Color.White,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(7.dp)
                                .background(Color.Black.copy(alpha = .66f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = .72f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("$episodeNumber. $title", color = IptvColors.TextPrimary, fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (quality.isNotBlank()) {
                        Text(
                            quality,
                            color = IptvColors.Primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    overview.takeIf(String::isNotBlank)?.let {
                        Text(it, color = IptvColors.TextSecondary, fontSize = 9.5.sp, lineHeight = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                    }
                    episode.duration.takeIf(String::isNotBlank)?.let {
                        Text(it, color = IptvColors.TextTertiary, fontSize = 8.5.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }
    Text(
        "Σύρε μέσα στα επεισόδια · εμφανίζονται 3",
        color = IptvColors.TextTertiary,
        fontSize = 8.5.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
