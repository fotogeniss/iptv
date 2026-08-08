package com.prelude.iptv.ui.route

import android.content.*
import android.os.*
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.contract.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.*
import coil.compose.*
import com.prelude.iptv.*
import com.prelude.iptv.data.*
import com.prelude.iptv.ui.*
import com.prelude.iptv.ui.components.library.*
import com.prelude.iptv.ui.design.*
import kotlinx.coroutines.*

@Composable
internal fun TabHeader(title: String) {
    Text(
        title, color = TextHi, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 18.dp, bottom = 14.dp)
    )
}

/** μικρός helper για indexed items σε LazyColumn */
internal inline fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedPlaylists(
    playlists: List<Playlist>,
    crossinline row: @Composable (Int, Playlist) -> Unit
) {
    items(playlists.size) { i -> row(i, playlists[i]) }
}

@OptIn(ExperimentalFoundationApi::class)   // combinedClickable (παρατεταμένο OK)
@Composable
internal fun PlaylistCard(
    pl: Playlist, modifier: Modifier = Modifier,
    onOpen: () -> Unit, onEdit: () -> Unit = {}, onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) BgElev2 else BgElev)
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Line, RoundedCornerShape(16.dp))
            .onFocusChanged { focused = it.isFocused }
            // Στην τηλεόραση το D-pad δύσκολα φτάνει στο μικρό ⋮ μέσα σε
            // focusable κάρτα. Το παρατεταμένο OK ανοίγει το ίδιο μενού.
            .combinedClickable(onClick = { onOpen() }, onLongClick = { menu = true })
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, tag) = when (pl.type) {
            PlaylistType.XTREAM -> Icons.Default.Bolt to "XTREAM"
            PlaylistType.STALKER -> Icons.Default.SettingsInputAntenna to "MAC PORTAL"
            else -> Icons.Default.PlaylistPlay to if (pl.isUrl) "M3U" else "M3U — ΑΡΧΕΙΟ"
        }
        // χρώμα/εικονίδιο προφίλ όπως επιλέχθηκε στη φόρμα προσθήκης
        val col = ProfileColors.getOrElse(pl.avatar) { Accent }
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(col.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = col, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(pl.name, color = TextHi, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(5.dp)).background(col.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) { Text(tag, color = col, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                if (focused) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "κράτα OK για επιλογές",
                        color = TextLo, fontSize = 10.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Πλήθη ανά ενότητα. -1 σημαίνει «δεν έχει φορτωθεί ποτέ αυτή η
            // ενότητα» — τη γράφουμε ΜΟΝΟ όταν έχει πράγματι φορτώσει, ώστε να
            // μη δείχνουμε ψευδές 0 σε πηγή που ο χρήστης απλώς δεν άνοιξε ποτέ.
            val catalogSummary = remember(pl.liveCount, pl.vodCount, pl.seriesCount) {
                buildList {
                    if (pl.liveCount >= 0) add("${pl.liveCount} Live")
                    if (pl.vodCount >= 0) add("${pl.vodCount} Ταινίες")
                    if (pl.seriesCount >= 0) add("${pl.seriesCount} Σειρές")
                }.joinToString(" · ")
            }
            if (catalogSummary.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    catalogSummary, color = TextLo, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box {
            TvIconButton(Icons.Default.MoreVert, "Περισσότερα", tint = TextMid) { menu = true }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = BgElev2) {
                // το μενού πρέπει να πάρει focus μόλις ανοίξει, αλλιώς στην
                // τηλεόραση φαίνεται αλλά δεν πατιέται τίποτα
                val mf = rememberInitialFocus()
                DropdownMenuItem(
                    text = { Text("Άνοιγμα", color = TextHi) },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = TextMid) },
                    modifier = Modifier.focusRequester(mf).tvFocus(RoundedCornerShape(6.dp)),
                    onClick = { menu = false; onOpen() })
                DropdownMenuItem(
                    text = { Text("Επεξεργασία", color = TextHi) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = TextMid) },
                    modifier = Modifier.tvFocus(RoundedCornerShape(6.dp)),
                    onClick = { menu = false; onEdit() })
                DropdownMenuItem(
                    text = { Text("Διαγραφή", color = AccentSoft) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = AccentSoft) },
                    modifier = Modifier.tvFocus(RoundedCornerShape(6.dp)),
                    onClick = { menu = false; onDelete() })
            }
        }
    }
}

