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
internal fun ContentTypeRow(current: String, onSelect: (String) -> Unit) {
    val tv = isTvDevice()
    StreamingSegmentedControl(
        items = listOf(
            StreamingSegment("live", "Ζωντανά"),
            StreamingSegment("vod", "Ταινίες"),
            StreamingSegment("series", "Σειρές")
        ),
        selected = current,
        onSelect = onSelect,
        tv = tv,
        modifier = Modifier.padding(
            horizontal = if (tv) 20.dp else 16.dp,
            vertical = 8.dp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InlineSearchField(
    value: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ανοίγει με το πληκτρολόγιο έτοιμο — δεν χρειάζεται δεύτερο πάτημα
    val focus = rememberInitialFocus()
    OutlinedTextField(
        value = value, onValueChange = onChange,
        placeholder = { Text("Αναζήτηση…", color = TextLo) },
        singleLine = true, shape = RoundedCornerShape(14.dp),
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = onClear,
                modifier = Modifier.tvFocus(CircleShape)) {
                Icon(Icons.Default.Close, "Καθαρισμός", tint = TextMid, modifier = Modifier.size(18.dp))
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = BgElev, focusedContainerColor = BgElev,
            unfocusedBorderColor = Line, focusedBorderColor = Accent,
            focusedTextColor = TextHi, unfocusedTextColor = TextHi,
            cursorColor = Accent
        ),
        modifier = modifier.padding(end = 8.dp).focusRequester(focus)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GroupChips(
    groups: List<String>, selected: String,
    lockedGroups: Set<String> = emptySet(),
    onLongPress: (String) -> Unit = {},
    onSelect: (String) -> Unit
) {
    // Το επιλεγμένο group φαίνεται ΠΑΝΤΑ: με θυμημένο group βαθιά στη σειρά
    // (π.χ. 40ό από 120), η μπάρα έδειχνε την αρχή και δεν ήξερες καν πού είσαι.
    val chipsState = rememberLazyListState()
    LaunchedEffect(selected, groups) {
        val i = groups.indexOf(selected)
        if (i >= 0) chipsState.animateScrollToItem(i)
    }
    LazyRow(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        state = chipsState,
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(groups) { g ->
            val sel = g == selected
            val locked = g in lockedGroups
            Box(
                Modifier.padding(end = 8.dp).clip(RoundedCornerShape(20.dp))
                    .background(if (sel) Accent else BgElev)
                    .then(if (sel) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(20.dp)))
                    .tvFocus(RoundedCornerShape(20.dp), tint = false)
                    // παρατεταμένο πάτημα = κλείδωμα/ξεκλείδωμα group (με PIN)
                    .combinedClickable(onClick = { onSelect(g) }, onLongClick = { onLongPress(g) })
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    if (locked) "🔒 $g" else g,
                    color = if (sel) Color.White else TextMid, fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun ChannelCard(
    ch: Channel, isFav: Boolean, nowText: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit, onEpg: () -> Unit, onFav: () -> Unit
) {
    val tv = isTvDevice()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (tv) 14.dp else 0.dp)
    Row(
        modifier.fillMaxWidth()
            .then(if (tv) Modifier.padding(vertical = 4.dp).clip(shape) else Modifier)
            .background(if (tv && focused) Color(0xFF252527) else Color.Transparent)
            .then(if (tv) Modifier.border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Line, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = if (tv) 12.dp else 16.dp, vertical = if (tv) 11.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(if (tv) 52.dp else 48.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF1A1A1D)),
            contentAlignment = Alignment.Center
        ) {
            if (ch.logo.isNotEmpty()) {
                AsyncImage(
                    model = ch.logo, contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            } else {
                Icon(
                    if (ch.kind == "series" || ch.kind == "vod") Icons.Default.Movie else Icons.Outlined.LiveTv,
                    null, tint = TextLo, modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                ch.name, color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = if (tv) 16.sp else 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                nowText?.takeIf { it.isNotBlank() } ?: ch.group,
                color = if (!nowText.isNullOrBlank()) Color(0xFFB8B8BD) else Color(0xFF77777D),
                fontSize = if (tv) 12.sp else 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (ch.kind == "live") {
            IconButton(onClick = onEpg, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.CalendarMonth, "EPG", tint = Color(0xFF9A9AA0), modifier = Modifier.size(21.dp))
            }
        }
        IconButton(onClick = onFav, modifier = Modifier.size(42.dp)) {
            Icon(
                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "Αγαπημένο",
                tint = if (isFav) Color.White else Color(0xFF85858B),
                modifier = Modifier.size(22.dp)
            )
        }
    }
    if (!tv) HorizontalDivider(color = Color(0x12FFFFFF), modifier = Modifier.padding(start = 77.dp))
}

/**
 * «Συνέχισε να βλέπεις»: οριζόντια σειρά με posters + μπάρα προόδου.
 * Οι θέσεις σώζονταν ήδη στον δίσκο από τον player — απλά δεν τις έδειχνε
 * ΚΑΝΕΝΑ σημείο του UI. Tap = συνεχίζει από εκεί που έμεινες (posKey resume).
 */
