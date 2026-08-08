package com.prelude.iptv.ui.tv.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.WatchProgressPolicy
import com.prelude.iptv.ui.components.details.DetailMetaRow
import com.prelude.iptv.ui.components.details.DetailPresentation
import com.prelude.iptv.ui.components.details.DetailProgress
import com.prelude.iptv.ui.components.details.detailGenreLine
import com.prelude.iptv.ui.components.details.detailSeriesLabel
import androidx.compose.animation.core.tween
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

@Composable
internal fun TvDetailHero(
    presentation: DetailPresentation,
    playFocus: FocusRequester,
    // Αρχικό focus: πάει στο «Πίσω», όχι στην Αναπαραγωγή. Το κουμπί
    // αναπαραγωγής είναι χαμηλά στο hero, οπότε το focus εκεί προκαλούσε
    // bring-into-view scroll και το hero φαινόταν κομμένο μόλις άνοιγε η οθόνη.
    backFocus: FocusRequester? = null,
    onBack: () -> Unit,
    onFav: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onRestart: () -> Unit,
    /**
     * Σβήνει την αποθηκευμένη πρόοδο ΧΩΡΙΣ να ξεκινήσει αναπαραγωγή.
     *
     * Διαφέρει από το «Από την αρχή», που σβήνει ΚΑΙ παίζει. Χρειάζονται και τα
     * δύο: άλλο «θέλω να το ξαναδώ τώρα» και άλλο «βγάλ' το από τη λίστα
     * συνέχειας». Για σειρά, σβήνει την πρόοδο ΟΛΩΝ των επεισοδίων.
     */
    onClearProgress: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth().height(430.dp)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 34.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumTvAction(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "Πίσω",
                compact = true,
                modifier = if (backFocus != null) Modifier.focusRequester(backFocus) else Modifier,
                onClick = onBack
            )
            Spacer(Modifier.weight(1f))
            Text(
                "PRELUDE+",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
        }

        Column(
            Modifier.padding(horizontal = 44.dp, vertical = 6.dp).widthIn(max = 640.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(20.dp).background(IptvColors.Primary, RoundedCornerShape(99.dp)))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (presentation.isSeries) "PRELUDE+ ORIGINAL SERIES" else "PRELUDE+ FEATURE FILM",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                presentation.title,
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            DetailMetaRow(
                year = presentation.year,
                rating = presentation.rating,
                ageRating = presentation.ageRating,
                duration = presentation.duration,
                seriesLabel = if (presentation.isSeries) detailSeriesLabel(presentation.seasons) else ""
            )
            val genres = detailGenreLine(presentation.genre)
            if (genres.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(genres, color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyMedium)
            }
            if (presentation.plot.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    presentation.plot,
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (presentation.director.isNotBlank() || presentation.cast.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    buildList {
                        if (presentation.cast.isNotEmpty()) add("Πρωταγωνιστούν: ${presentation.cast.take(3).joinToString { it.name }}")
                        if (presentation.director.isNotBlank()) add("Δημιουργός: ${presentation.director}")
                    }.joinToString("  ·  "),
                    color = IptvColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                PremiumTvAction(
                    icon = Icons.Default.PlayArrow,
                    label = when {
                        presentation.isSeries && presentation.resumeEpisode != null -> "ΣΥΝΕΧΕΙΑ ΣΕΙΡΑΣ"
                        presentation.isSeries && presentation.loading -> "ΕΝΑΡΞΗ ΣΕΙΡΑΣ…"
                        presentation.isSeries -> "ΕΝΑΡΞΗ ΣΕΙΡΑΣ"
                        presentation.movieProgress != null -> "ΣΥΝΕΧΕΙΑ · ${presentation.movieProgress.percent}%"
                        else -> "ΑΝΑΠΑΡΑΓΩΓΗ"
                    },
                    primary = true,
                    // ΠΑΝΤΑ ενεργό: αν τα επεισόδια δεν έχουν φορτώσει ακόμη, η
                    // οθόνη κρατά την πρόθεση και ξεκινά μόλις φτάσουν.
                    enabled = true,
                    modifier = Modifier.focusRequester(playFocus),
                    onClick = onPlay
                )
                PremiumTvAction(
                    icon = if (presentation.isFav) Icons.Default.Check else Icons.Default.Add,
                    label = "Η ΛΙΣΤΑ ΜΟΥ",
                    selected = presentation.isFav,
                    onClick = onFav
                )
                PremiumTvAction(Icons.Default.Share, "ΚΟΙΝΟΠΟΙΗΣΗ", onClick = onShare)
                if (!presentation.isSeries && presentation.movieProgress != null) {
                    PremiumTvAction(Icons.Default.RestartAlt, "Από την αρχή", compact = true, onClick = onRestart)
                }
                // Επαναφορά προόδου: εμφανίζεται μόνο όταν ΥΠΑΡΧΕΙ πρόοδος να
                // σβηστεί — σε ταινία η δική της, σε σειρά οποιουδήποτε επεισοδίου.
                // Ένα κουμπί που δεν έχει τι να κάνει είναι μόνο εμπόδιο στην
                // πλοήγηση με το D-pad.
                val hasProgress = if (presentation.isSeries) {
                    presentation.episodeProgress.isNotEmpty()
                } else presentation.movieProgress != null
                if (hasProgress) {
                    PremiumTvAction(
                        Icons.Default.Delete,
                        "Επαναφορά προόδου",
                        compact = true,
                        onClick = onClearProgress
                    )
                }
            }
            presentation.movieProgress?.let { progress ->
                Spacer(Modifier.height(10.dp))
                DetailProgress(
                    progress = progress.fraction,
                    leading = "${progress.percent}%",
                    trailing = WatchProgressPolicy.remainingLabel(progress),
                    modifier = Modifier.width(420.dp)
                )
            }
        }
    }
}

@Composable
internal fun PremiumTvAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    compact: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvActionScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "detailActionScale"
    )
    val shape = if (compact) CircleShape else RoundedCornerShape(9.dp)
    val background = when {
        primary -> Color.White
        selected -> Color.White.copy(alpha = 0.92f)
        else -> IptvColors.SurfaceRaised.copy(alpha = 0.88f)
    }
    val foreground = if (primary || selected) Color.Black else Color.White
    Row(
        modifier
            .height(48.dp)
            .then(if (compact) Modifier.width(48.dp) else Modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 24.dp else 0.dp, shape, clip = false)
            .clip(shape)
            .background(if (focused && !primary && !selected) IptvColors.SurfaceSelected else background)
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = shape
            )
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled, interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 0.dp else 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = foreground, modifier = Modifier.size(20.dp))
        if (!compact) {
            Spacer(Modifier.width(8.dp))
            Text(label, color = foreground, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}
