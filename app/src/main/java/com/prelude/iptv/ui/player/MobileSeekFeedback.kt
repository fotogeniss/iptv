package com.prelude.iptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.icons.filled.ChevronLeft
import com.prelude.iptv.ui.icons.filled.ChevronRight

/**
 * Μισοφέγγαρο του διπλού πατήματος (prototypes/redesign_player_gestures.html):
 * καλύπτει το μισό που πατήθηκε και λέει πόσο και πού θα βρεθείς.
 */
@Composable
internal fun SeekRipple(forward: Boolean, label: String, target: String, modifier: Modifier = Modifier) {
    val shape = if (forward) RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
    else RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
    Box(
        modifier
            .fillMaxHeight()
            .fillMaxWidth(.38f)
            .clip(shape)
            .background(Color.White.copy(alpha = .12f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                repeat(3) { index ->
                    // Τα βελάκια «τρέχουν» προς την κατεύθυνση: το πιο φωτεινό μπροστά.
                    val strength = if (forward) index else 2 - index
                    Icon(
                        if (forward) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = .35f + .3f * strength),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(target, color = Color.White.copy(alpha = .75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** «» ×4 1:31:20 +18:40» πάνω-πάνω όσο κρατιέται το δάχτυλο. */
@Composable
internal fun HoldSeekPill(forward: Boolean, level: Int, target: String, delta: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(99.dp)
    Row(
        modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = .72f))
            .border(1.dp, Color.White.copy(alpha = .18f), shape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (forward) "»" else "«", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text("×$level", color = IptvColors.Glow, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(target, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(delta, color = Color.White.copy(alpha = .55f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
