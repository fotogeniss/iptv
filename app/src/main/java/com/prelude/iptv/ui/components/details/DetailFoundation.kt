package com.prelude.iptv.ui.components.details

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration

@Immutable
enum class DetailSection {
    Episodes,
    About,
    Cast,
    Similar
}

/**
 * Το φόντο της οθόνης πληροφοριών.
 *
 * ΤΟ ΠΡΟΒΛΗΜΑ ΠΟΥ ΛΥΝΕΙ: μέχρι τώρα έπαιρνε μία «εικόνα ήρωα» που ήταν
 * `backdrop ?: poster`. Ανοίγοντας μια ταινία, το TMDB δεν έχει απαντήσει ακόμη,
 * άρα έμπαινε η ΑΦΙΣΑ — κατακόρυφη εικόνα — κομμένη με [ContentScale.Crop] για να
 * γεμίσει μια οθόνη 16:9. Για να χωρέσει το πλάτος, κόβεται σχεδόν όλο το ύψος:
 * αυτό ακριβώς φαίνεται ως «ζουμαρισμένο». Μόλις έφτανε το πραγματικό backdrop,
 * το crossfade το αντικαθιστούσε και «καθόταν σωστά».
 *
 * Δεν ήταν animation. Ήταν δύο διαφορετικές εικόνες, η πρώτη σε λάθος σχήμα.
 *
 * ΤΩΡΑ οι δύο πηγές δηλώνονται χωριστά, γιατί ΔΕΝ είναι εναλλάξιμες:
 *
 * - **backdrop** (πλατύ): γεμίζει την οθόνη με [ContentScale.Crop].
 * - **αφίσα** (κατακόρυφη): **δεν κόβεται ποτέ**. Μπαίνει με [ContentScale.Fit]
 *   δεξιά, σε χαμηλή διαφάνεια, με τις σκοτεινές διαβαθμίσεις από πάνω. Φαίνεται
 *   σε σωστές αναλογίες — δηλαδή δεν φαίνεται «ζουμαρισμένη», γιατί δεν είναι.
 *
 * ΚΑΙ ΠΡΟΠΑΝΤΩΝ: όσο το TMDB δεν έχει απαντήσει ([backdropPending]) δεν
 * ζωγραφίζεται ΤΙΠΟΤΑ. Έτσι ο χρήστης βλέπει μία εικόνα, μία φορά.
 *
 * ---
 *
 * ΤΙ ΠΗΓΕ ΣΤΡΑΒΑ ΤΗΝ ΠΡΩΤΗ ΦΟΡΑ: η προηγούμενη προσπάθεια κρατούσε την αφίσα σε
 * [ContentScale.Crop] και βασιζόταν σε `Modifier.blur` για να μη φαίνεται το
 * κόψιμο. Το `Modifier.blur` **δεν κάνει απολύτως τίποτα κάτω από Android 12** —
 * χωρίς σφάλμα, χωρίς προειδοποίηση. Στα TV box, που τρέχουν συνήθως Android 9
 * έως 11, η αφίσα εμφανιζόταν καθαρή και κομμένη, δηλαδή ακριβώς όπως πριν τη
 * «διόρθωση».
 *
 * Γι' αυτό εδώ δεν υπάρχει κανένα εφέ που εξαρτάται από έκδοση: μόνο διάταξη και
 * διαφάνεια, που δουλεύουν παντού.
 */
@Composable
fun DetailCinematicBackdrop(
    /** Πλατιά εικόνα. Κενή όσο δεν έχει απαντήσει το TMDB, ή αν δεν υπάρχει. */
    backdropUrl: String,
    /** Κατακόρυφη αφίσα — εφεδρεία, ποτέ σε πλήρη κάλυψη. */
    posterUrl: String,
    contentDescription: String?,
    mobile: Boolean,
    /** true όσο περιμένουμε το TMDB. Δες [DetailPresentation.backdropPending]. */
    backdropPending: Boolean = false,
    modifier: Modifier = Modifier
) {
    val wide = backdropUrl.isNotBlank()
    // Όσο περιμένουμε, δεν δείχνουμε την αφίσα: θα ήταν η πρώτη από τις δύο
    // εικόνες που παραπονιέται ο χρήστης.
    val image = when {
        wide -> backdropUrl
        backdropPending -> ""
        else -> posterUrl
    }
    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        Crossfade(
            targetState = image to wide,
            animationSpec = tween(motionDuration(Motion.Hero), easing = Motion.EmphasizedEasing),
            label = "detailBackdrop"
        ) { (url, isWide) ->
            if (url.isNotBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = contentDescription,
                    // ΕΔΩ ΕΙΝΑΙ ΟΛΗ Η ΔΙΟΡΘΩΣΗ: το πλατύ κόβεται για να γεμίσει,
                    // η κατακόρυφη αφίσα ΧΩΡΑΕΙ ολόκληρη.
                    contentScale = if (isWide) ContentScale.Crop else ContentScale.Fit,
                    alignment = if (isWide) Alignment.Center else Alignment.CenterEnd,
                    modifier = Modifier
                        .fillMaxSize()
                        // Η αφίσα σκουραίνει ώστε να είναι φόντο και όχι θέμα. Η
                        // διαφάνεια δουλεύει σε ΚΑΘΕ έκδοση Android — σε αντίθεση
                        // με το θόλωμα.
                        .alpha(if (isWide) 1f else .45f)
                )
            }
        }
        if (mobile) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.12f),
                        0.45f to Color.Black.copy(alpha = 0.20f),
                        0.74f to Color.Black.copy(alpha = 0.78f),
                        1f to IptvColors.Background
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.34f),
                        0.58f to Color.Transparent
                    )
                )
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.98f),
                        0.37f to Color.Black.copy(alpha = 0.78f),
                        0.72f to Color.Black.copy(alpha = 0.15f),
                        1f to Color.Black.copy(alpha = 0.28f)
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.58f to Color.Black.copy(alpha = 0.08f),
                        0.80f to IptvColors.Background.copy(alpha = 0.90f),
                        1f to IptvColors.Background
                    )
                )
            )
        }
    }
}

@Composable
fun DetailMetaRow(
    year: String,
    rating: String,
    ageRating: String,
    duration: String,
    seriesLabel: String = "",
    quality: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rating.isNotBlank()) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = IptvColors.Success,
                modifier = Modifier.size(15.dp)
            )
            Text(
                rating,
                color = IptvColors.Success,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        listOf(year, ageRating, duration, seriesLabel)
            .filter(String::isNotBlank)
            .forEach { value ->
                Text(
                    value,
                    color = IptvColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        if (quality.isNotBlank()) {
            Text(
                quality,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = .72f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun DetailProgress(
    progress: Float,
    leading: String,
    trailing: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(leading, color = IptvColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text(trailing, color = IptvColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(7.dp))
        StreamingProgress(progress)
    }
}

fun detailSeriesLabel(seasons: List<Pair<String, *>>): String = when (seasons.size) {
    0 -> ""
    1 -> "1 σεζόν"
    else -> "${seasons.size} σεζόν"
}

fun detailGenreLine(genre: String): String = genre
    .split(',', '/', '|')
    .map(String::trim)
    .filter(String::isNotBlank)
    .take(4)
    .joinToString(" · ")

fun detailInitials(name: String): String = name
    .split(' ')
    .mapNotNull { it.firstOrNull()?.uppercase() }
    .take(2)
    .joinToString("")
