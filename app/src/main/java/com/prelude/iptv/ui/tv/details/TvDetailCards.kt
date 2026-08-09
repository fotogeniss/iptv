package com.prelude.iptv.ui.tv.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.WatchProgress
import androidx.compose.animation.core.tween
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale
import com.prelude.iptv.ui.localization.localizedUppercase
import com.prelude.iptv.ui.localization.localizedWatchRemaining

@Composable
internal fun TvEpisodeCard(
    episode: Channel,
    number: Int,
    progress: WatchProgress?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    // Στοιχεία της ΣΕΙΡΑΣ, για να βρεθεί το στιγμιότυπο του επεισοδίου στο TMDB.
    seriesTitle: String = "",
    seriesYear: String = "",
    season: Int = 1
) {
    // Εικόνα επεισοδίου: TMDB still -> ό,τι έχει η λίστα -> placeholder.
    val tmdbEpisode = com.prelude.iptv.ui.components.rememberEpisodeMeta(
        seriesTitle = seriesTitle,
        seriesYear = seriesYear,
        season = season,
        episodeNumber = number
    )
    val artwork = tmdbEpisode?.still?.takeIf { it.isNotBlank() } ?: episode.logo
    val displayTitle = if (episode.name.isBlank()) {
        stringResource(R.string.details_episode_number, number)
    } else episode.name
    val remainingLabel = if (progress != null) localizedWatchRemaining(progress) else null
    var focused by remember(episode) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvActionScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "episodeFocusScale"
    )
    val shape = RoundedCornerShape(11.dp)
    Box(
        Modifier.width(245.dp).height(138.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 24.dp else 8.dp, shape, clip = false)
            .clip(shape)
            .background(IptvColors.Surface)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) Color.White else IptvColors.Divider,
                shape = shape
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(onClick = onClick)
    ) {
        if (artwork.isNotBlank()) {
            AsyncImage(
                model = artwork,
                contentDescription = episode.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Outlined.Movie, null, tint = IptvColors.TextTertiary, modifier = Modifier.align(Alignment.Center).size(40.dp))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, .56f to Color.Black.copy(alpha = .12f), 1f to Color.Black.copy(alpha = .94f))
            )
        )
        if (focused) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(11.dp).size(34.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.04f)))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp)) {
            Text(
                localizedUppercase(stringResource(R.string.details_episode_number, number)),
                color = Color.White.copy(alpha = .80f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                displayTitle,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOfNotNull(episode.duration.takeIf(String::isNotBlank), remainingLabel).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, color = IptvColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        progress?.let { StreamingProgress(it.fraction, Modifier.align(Alignment.BottomCenter)) }
    }
}

@Composable
internal fun TvEpisodeInfoPanel(
    episode: Channel?,
    number: Int,
    nextTitle: String,
    seriesTitle: String = "",
    seriesYear: String = "",
    season: Int = 1
) {
    // Περιγραφή ανά επεισόδιο από TMDB· αν λείπει, ό,τι δίνει η λίστα.
    val tmdbEpisode = if (episode == null) null else com.prelude.iptv.ui.components.rememberEpisodeMeta(
        seriesTitle = seriesTitle,
        seriesYear = seriesYear,
        season = season,
        episodeNumber = number
    )
    val episodeTitle = tmdbEpisode?.title?.takeIf { it.isNotBlank() } ?: episode?.name.orEmpty()
    val episodeOverview = tmdbEpisode?.overview?.takeIf { it.isNotBlank() } ?: episode?.plot.orEmpty()
    val displayOverview = if (episodeOverview.isBlank()) {
        stringResource(R.string.details_no_episode_description)
    } else episodeOverview
    Column(
        Modifier.width(300.dp).fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(IptvColors.SurfaceRaised.copy(alpha = 0.82f))
            .border(1.dp, IptvColors.Divider, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text(stringResource(R.string.details_focused_episode), color = IptvColors.Primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(
            if (episode == null) stringResource(R.string.details_select_episode)
            else stringResource(R.string.details_numbered_title, number, episodeTitle),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (episode != null) {
            val meta = listOf(episode.duration, episode.year).filter(String::isNotBlank).joinToString(" · ")
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(meta, color = IptvColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(11.dp))
            Text(
                displayOverview,
                color = IptvColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 7,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
        if (nextTitle.isNotBlank()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(IptvColors.Divider))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.details_next_title, nextTitle), color = IptvColors.TextTertiary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

@Composable
internal fun TvRelatedCard(
    channel: Channel,
    onClick: () -> Unit
) {
    var focused by remember(channel) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvActionScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "relatedScale"
    )
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.width(150.dp).aspectRatio(2f / 3f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 24.dp else 7.dp, shape, clip = false)
            .clip(shape)
            .background(IptvColors.Surface)
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
    ) {
        val artwork = com.prelude.iptv.ui.components.rememberPosterArtwork(channel)
        if (artwork.isNotBlank()) {
            AsyncImage(artwork, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Outlined.Movie, null, tint = IptvColors.TextTertiary, modifier = Modifier.align(Alignment.Center).size(42.dp))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(.45f to Color.Transparent, 1f to Color.Black.copy(alpha = .88f))))
        if (focused) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .05f)))
        Text(
            channel.name,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(11.dp)
        )
    }
}
