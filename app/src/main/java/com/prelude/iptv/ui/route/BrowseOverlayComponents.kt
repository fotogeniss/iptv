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
internal fun ContentChooser(
    current: String = "",
    onClose: (() -> Unit)? = null,
    onPick: (String) -> Unit
) {
    val first = rememberInitialFocus()
    Box(Modifier.fillMaxSize().background(Bg)) {
        // όταν αλλάζεις ενότητα εκ των υστέρων, μπορείς να το κλείσεις χωρίς αλλαγή
        if (onClose != null) {
            TvIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω",
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp), onClick = onClose)
        }
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ποια ενότητα θέλεις;", color = TextHi, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Κάθε ενότητα κατεβάζει φρέσκα δεδομένα από την πηγή",
                color = TextLo, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))
            listOf(
                Triple("Όλα", Icons.Default.SelectAll, "all"),
                Triple("Live TV", Icons.Outlined.LiveTv, "live"),
                Triple("Ταινίες", Icons.Default.Movie, "vod"),
                Triple("Σειρές", Icons.Default.Tv, "series")
            ).forEach { (label, icon, key) ->
                val sel = current == key
                Row(
                    Modifier.fillMaxWidth(0.9f).padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp)).background(BgElev)
                        .border(1.dp, if (sel) Accent else Line, RoundedCornerShape(16.dp))
                        .then(if (key == "all") Modifier.focusRequester(first) else Modifier)
                        .tvFocus(RoundedCornerShape(16.dp))
                        .clickable { onPick(key) }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = if (sel) Accent else AccentSoft, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(label, color = TextHi, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    if (sel) Icon(Icons.Default.Check, null, tint = Accent, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpgSheet(channel: Channel, vm: MainViewModel, onDismiss: () -> Unit) {
    var listings by remember { mutableStateOf<List<EpgEntry>?>(null) }
    LaunchedEffect(channel) { listings = vm.fetchEpg(channel) }
    val isTv = isTvDevice()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BgElev2) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(channel.name, color = TextHi, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            val currentListings = listings
            when {
                currentListings == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp)); Text("Φόρτωση…", color = TextMid)
                }
                currentListings.isEmpty() -> Text("Δεν υπάρχει πρόγραμμα.", color = TextMid)
                else -> {
                    // ΤΗΛΕΟΡΑΣΗ: χωρίς focusable στοιχεία, το D-pad δεν είχε πού
                    // να σταθεί και η λίστα ΔΕΝ σκρολάριζε — μόνο το back δούλευε.
                    // Κάθε γραμμή γίνεται focusable (μόνο σε TV) και το focus
                    // προσγειώνεται στην πρώτη, οπότε πάνω/κάτω = κύλιση.
                    val fEpg = rememberInitialFocus(enabled = isTv, key = currentListings.size)
                    // ποια γραμμή είναι ανοιχτή (-1 = καμία). Μία τη φορά, ώστε να
                    // μη γίνεται η λίστα σεντόνι.
                    var expanded by remember { mutableStateOf(-1) }
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        itemsIndexed(currentListings) { i, e ->
                            val isOpen = expanded == i
                            Row(
                                Modifier.fillMaxWidth()
                                    .then(if (isTv && i == 0) Modifier.focusRequester(fEpg) else Modifier)
                                    .then(if (isTv) Modifier.tvFocus(RoundedCornerShape(8.dp)) else Modifier)
                                    // ΚΛΙΚ = άνοιγμα/κλείσιμο πλήρους περιγραφής.
                                    // Πριν: 2 γραμμές με «…» και ΚΑΜΙΑ πρόσβαση στη συνέχεια.
                                    .clickable { expanded = if (isOpen) -1 else i }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(fmtTime(e.start), color = if (i == 0) Accent else TextMid, fontSize = 12.sp,
                                    modifier = Modifier.width(56.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.title, color = if (i == 0) TextHi else Color(0xFFCFCFD6),
                                        fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal)
                                    if (e.desc.isNotEmpty()) Text(
                                        e.desc, color = TextLo, fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        maxLines = if (isOpen) Int.MAX_VALUE else 2,
                                        overflow = if (isOpen) TextOverflow.Clip else TextOverflow.Ellipsis
                                    )
                                }
                                // βελάκι: δείχνει ότι η γραμμή ΑΝΟΙΓΕΙ (affordance)
                                if (e.desc.length > 90) Icon(
                                    if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null, tint = TextLo,
                                    modifier = Modifier.size(18.dp).align(Alignment.CenterVertically)
                                )
                            }
                            HorizontalDivider(color = Line)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Φορτώνει metadata και δείχνει το premium DetailScreen. */
