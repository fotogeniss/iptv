package com.prelude.iptv.ui.mobile.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prelude.iptv.ui.StreamingButton
import com.prelude.iptv.ui.WatchProgressPolicy
import com.prelude.iptv.ui.components.details.DetailCinematicBackdrop
import com.prelude.iptv.ui.components.details.DetailMetaRow
import com.prelude.iptv.ui.components.details.DetailPresentation
import com.prelude.iptv.ui.components.details.DetailProgress
import com.prelude.iptv.ui.components.details.detailGenreLine
import com.prelude.iptv.ui.components.details.detailSeriesLabel

@Composable
internal fun MobileDetailHero(
    presentation: DetailPresentation,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onFav: () -> Unit,
    onPlay: () -> Unit,
    onRestart: () -> Unit
) {
    var plotExpanded by remember(presentation.title, presentation.plot) {
        mutableStateOf(false)
    }
    var plotCanExpand by remember(presentation.title, presentation.plot) {
        mutableStateOf(false)
    }
    Box(Modifier.fillMaxWidth().heightIn(min = 535.dp)) {
        DetailCinematicBackdrop(
            backdropUrl = presentation.backdropUrl,
            posterUrl = presentation.posterUrl,
            contentDescription = presentation.title,
            mobile = true,
            backdropPending = presentation.backdropPending,
            // Το artwork ακολουθεί το τελικό δυναμικό ύψος του hero, αλλά δεν
            // το επιβάλλει. Έτσι το «Περισσότερα» μπορεί να μεγαλώσει το κείμενο
            // χωρίς να κοπούν τα actions από ένα σταθερό κουτί 535dp.
            modifier = Modifier.matchParentSize(),
        )
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MobileTopAction(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", onBack)
            Text(
                "PRELUDE+",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(42.dp))
        }

        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Text(
                if (presentation.isSeries) "PRELUDE+ ORIGINAL SERIES" else "PRELUDE+ FEATURE FILM",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                presentation.title,
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            DetailMetaRow(
                year = presentation.year,
                rating = presentation.rating,
                ageRating = presentation.ageRating,
                duration = presentation.duration,
                seriesLabel = if (presentation.isSeries) detailSeriesLabel(presentation.seasons) else "",
                quality = presentation.quality
            )
            val genres = detailGenreLine(presentation.genre)
            if (genres.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(genres, color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodySmall)
            }
            if (presentation.plot.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    presentation.plot,
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (plotExpanded) Int.MAX_VALUE else 3,
                    overflow = if (plotExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (!plotExpanded) plotCanExpand = result.hasVisualOverflow
                    }
                )
                if (plotCanExpand) {
                    Text(
                        if (plotExpanded) "Λιγότερα" else "Περισσότερα",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clickable { plotExpanded = !plotExpanded }
                            .padding(top = 6.dp, bottom = 1.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            StreamingButton(
                label = when {
                    presentation.isSeries && presentation.resumeEpisode != null -> "Συνέχεια σειράς"
                    presentation.isSeries && presentation.loading -> "Έναρξη σειράς…"
                    presentation.isSeries -> "Έναρξη σειράς"
                    presentation.movieProgress != null -> "Συνέχεια · ${presentation.movieProgress.percent}%"
                    else -> "Αναπαραγωγή"
                },
                icon = Icons.Default.PlayArrow,
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                // ΠΑΝΤΑ ενεργό: αν τα επεισόδια δεν έχουν φορτώσει, η οθόνη κρατά
                // την πρόθεση και ξεκινά μόλις φτάσουν.
                enabled = true
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MobileHeroVAction(
                    icon = if (presentation.isFav) Icons.Default.Check else Icons.Default.Add,
                    label = "Η λίστα μου",
                    onClick = onFav
                )
                MobileHeroVAction(Icons.Default.Share, "Κοινοποίηση", onClick = onShare)
                if (!presentation.isSeries && presentation.movieProgress != null) {
                    MobileHeroVAction(Icons.Default.RestartAlt, "Από την αρχή", onClick = onRestart)
                }
            }
            presentation.movieProgress?.let { progress ->
                Spacer(Modifier.height(14.dp))
                DetailProgress(
                    progress = progress.fraction,
                    leading = "${progress.percent}%",
                    trailing = WatchProgressPolicy.remainingLabel(progress)
                )
            }
        }
    }
}

@Composable
private fun MobileTopAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(42.dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.48f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MobileHeroVAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
