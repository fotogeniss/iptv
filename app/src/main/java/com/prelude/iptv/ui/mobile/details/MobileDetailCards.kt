package com.prelude.iptv.ui.mobile.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.ContentQualityPolicy
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.WatchProgress
import com.prelude.iptv.ui.design.MotionSkeleton
import com.prelude.iptv.ui.components.rememberEpisodeMeta
import com.prelude.iptv.ui.localization.localizedWatchRemaining

@Composable
internal fun MobileEpisodeCard(
    episode: Channel,
    number: Int,
    progress: WatchProgress?,
    onClick: () -> Unit,
    seriesTitle: String,
    seriesYear: String,
    season: Int,
    /** TMDB id του παρόχου· όταν υπάρχει, δεν γίνεται αναζήτηση με τίτλο. */
    seriesTmdbId: String = "",
) {
    val tmdbEpisode = rememberEpisodeMeta(
        seriesTitle = seriesTitle,
        seriesYear = seriesYear,
        season = season,
        episodeNumber = number,
        // Το ίδιο το επεισόδιο πρώτο: για Stalker το κουβαλάει από τη γραμμή
        // σεζόν. Η τιμή της σειράς είναι εφεδρεία για πηγές όπου τα επεισόδια
        // ξαναχτίζονται από τον normalizer και δεν το έχουν.
        seriesTmdbId = episode.tmdbId.ifBlank { seriesTmdbId },
    )
    val artwork = tmdbEpisode?.still?.takeIf(String::isNotBlank) ?: episode.logo
    val title = tmdbEpisode?.title?.takeIf(String::isNotBlank) ?: episode.name
    val displayTitle = if (title.isBlank()) {
        stringResource(R.string.details_episode_number, number)
    } else title
    val overview = tmdbEpisode?.overview?.takeIf(String::isNotBlank) ?: episode.plot
    val remainingLabel = if (progress != null) localizedWatchRemaining(progress) else null
    val quality = ContentQualityPolicy.label(episode.name, episode.group)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.width(132.dp).aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(IptvColors.Surface)
                .border(1.dp, IptvColors.Divider, RoundedCornerShape(10.dp))
        ) {
            if (artwork.isNotBlank()) {
                AsyncImage(
                    model = artwork,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Outlined.Movie,
                    null,
                    tint = IptvColors.TextTertiary,
                    modifier = Modifier.align(Alignment.Center).size(28.dp)
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.74f))
                )
            )
            Box(
                Modifier.align(Alignment.Center).size(34.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
            Text(
                stringResource(R.string.details_episode_badge, number),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
            if (quality.isNotBlank()) {
                Text(
                    quality,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .background(Color.Black.copy(alpha = .65f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color.White.copy(alpha = .72f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            progress?.let {
                StreamingProgress(it.fraction, Modifier.align(Alignment.BottomCenter))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    R.string.details_numbered_title,
                    number,
                    displayTitle,
                ),
                color = IptvColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val episodeMeta = listOf(episode.duration, episode.year).filter(String::isNotBlank).joinToString(" · ")
            if (episodeMeta.isNotBlank() || progress != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(
                        episodeMeta.takeIf(String::isNotBlank),
                        remainingLabel
                    ).joinToString(" · "),
                    color = IptvColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (overview.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    overview,
                    color = IptvColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    Box(Modifier.padding(start = 160.dp).fillMaxWidth().height(1.dp).background(IptvColors.Divider))
}

@Composable
internal fun MobileDetailSkeleton() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp)) {
        repeat(3) {
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                MotionSkeleton(
                    modifier = Modifier.width(132.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
                    baseColor = IptvColors.Surface,
                    highlightColor = IptvColors.SurfaceRaised
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    MotionSkeleton(
                        modifier = Modifier.fillMaxWidth(.72f).height(16.dp).clip(RoundedCornerShape(5.dp)),
                        baseColor = IptvColors.SurfaceRaised,
                        highlightColor = IptvColors.SurfaceSelected
                    )
                    Spacer(Modifier.height(9.dp))
                    MotionSkeleton(
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(5.dp)),
                        baseColor = IptvColors.Surface,
                        highlightColor = IptvColors.SurfaceRaised
                    )
                    Spacer(Modifier.height(7.dp))
                    MotionSkeleton(
                        modifier = Modifier.fillMaxWidth(.84f).height(12.dp).clip(RoundedCornerShape(5.dp)),
                        baseColor = IptvColors.Surface,
                        highlightColor = IptvColors.SurfaceRaised
                    )
                }
            }
        }
    }
}
