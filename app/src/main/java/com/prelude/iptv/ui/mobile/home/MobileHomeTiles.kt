package com.prelude.iptv.ui.mobile.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import java.text.NumberFormat
import java.util.Locale

/**
 * Τα τρία πλακίδια: Ζωντανά · Ταινίες · Σειρές, με το πλήθος του καθενός.
 *
 * ΓΙΑΤΙ ΜΕ ΑΡΙΘΜΟΥΣ: το πλήθος είναι η μόνη ένδειξη ότι η λίστα κατέβηκε σωστά.
 * Χωρίς αυτό, μια πηγή που έφερε 12 ταινίες αντί για 10.000 φαίνεται ακριβώς ίδια
 * με μια υγιή — μέχρι να ψάξεις κάτι και να μην το βρεις.
 */
@Composable
internal fun MobileHomeTiles(
    liveCount: Int,
    movieCount: Int,
    seriesCount: Int,
    onOpenLive: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Tile(Modifier.weight(1f), Icons.Default.LiveTv, "Ζωντανά", liveCount, onOpenLive)
        Tile(Modifier.weight(1f), Icons.Default.Movie, "Ταινίες", movieCount, onOpenMovies)
        Tile(Modifier.weight(1f), Icons.Default.Tv, "Σειρές", seriesCount, onOpenSeries)
    }
}

@Composable
private fun Tile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(IptvColors.Surface)
            .border(1.dp, IptvColors.Divider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(top = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = IptvColors.TextPrimary, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, color = IptvColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            // Με διαχωριστικό χιλιάδων: «10747» διαβάζεται δύο φορές, «10.747» μία.
            if (count > 0) NumberFormat.getIntegerInstance(Locale("el", "GR")).format(count) else "—",
            color = IptvColors.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
