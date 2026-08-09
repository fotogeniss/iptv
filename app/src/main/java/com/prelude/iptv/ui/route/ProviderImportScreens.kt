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
import com.prelude.iptv.player.PlayerLaunchRequest
import com.prelude.iptv.data.*
import com.prelude.iptv.ui.*
import com.prelude.iptv.ui.components.library.*
import com.prelude.iptv.ui.design.*
import com.prelude.iptv.ui.epg.EpgStatus
import com.prelude.iptv.ui.localization.localizedLabel
import com.prelude.iptv.ui.localization.localizedText
import kotlinx.coroutines.*

@Composable
internal fun XtreamTab(
    state: AppShellUiState, vm: MainViewModel,
    onAdd: () -> Unit, onOpen: (Int) -> Unit, onEdit: (Int) -> Unit
) {
    val xtreams = remember(state.playlists) {
        state.playlists.mapIndexedNotNull { i, p -> if (p.type == PlaylistType.XTREAM) i to p else null }
    }
    var confirmDelete by remember { mutableStateOf(-1) }
    val firstX = rememberInitialFocus(
        enabled = isTvDevice() && xtreams.isNotEmpty(), key = xtreams.size
    )
    Column(Modifier.fillMaxSize()) {
        TabHeader("Xtream")
        if (xtreams.isEmpty()) {
            XtreamLanding(onAddProfile = onAdd)
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
                items(xtreams.size) { n ->
                    val (idx, pl) = xtreams[n]
                    PlaylistCard(pl,
                        modifier = if (n == 0) Modifier.focusRequester(firstX) else Modifier,
                        onOpen = { onOpen(idx) }, onEdit = { onEdit(idx) },
                        onDelete = { confirmDelete = idx })
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
    if (confirmDelete >= 0) DeleteConfirmDialog(
        name = state.playlists.getOrNull(confirmDelete)?.name ?: "",
        onConfirm = { vm.deletePlaylist(confirmDelete); confirmDelete = -1 },
        onCancel = { confirmDelete = -1 }
    )
}

/** Επιβεβαίωση διαγραφής — η άμεση διαγραφή με long-press ήταν επικίνδυνη σε TV. */
@Composable
internal fun DeleteConfirmDialog(name: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val f = rememberInitialFocus()
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = BgElev2,
        title = { Text("Διαγραφή λίστας;", color = TextHi) },
        text = {
            Text("«$name» — θα διαγραφεί οριστικά, μαζί με τις αποθηκευμένες επιλογές της.",
                color = TextMid, fontSize = 13.sp, lineHeight = 18.sp)
        },
        confirmButton = {
            TextButton(onClick = onConfirm,
                modifier = Modifier.focusRequester(f).tvFocus(RoundedCornerShape(8.dp))) {
                Text("Διαγραφή", color = AccentSoft, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.tvFocus(RoundedCornerShape(8.dp))) {
                Text("Άκυρο", color = TextMid)
            }
        }
    )
}

/* ================================= tab: EPG ================================ */

@Composable
internal fun EpgTabScreen(onAdd: () -> Unit) {
    val src = com.prelude.iptv.data.EpgManager.currentSource()
    val loaded = com.prelude.iptv.data.EpgManager.isLoaded()
    Column(Modifier.fillMaxSize()) {
        TabHeader("EPG")
        if (!loaded) {
            EmptyWithArrow(
                title = "Δεν έχεις EPG",
                subtitle = "Για να προσθέσεις EPG, πάτα το",
                bold = "κουμπί ＋"
            )
        } else {
            val f = rememberInitialFocus(enabled = isTvDevice())
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgElev)
                        .border(1.dp, Line, RoundedCornerShape(16.dp))
                        .focusRequester(f).tvFocus(RoundedCornerShape(16.dp))
                        .clickable { onAdd() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.CalendarMonth, null, tint = AccentSoft, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("XMLTV φορτωμένο", color = TextHi, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(src ?: "", color = TextLo, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.Refresh, null, tint = TextMid, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("Το πρόγραμμα εμφανίζεται στα live κανάλια και στον player.",
                    color = TextLo, fontSize = 12.sp)
            }
        }
    }
}

/** Διάλογος για κατέβασμα XMLTV EPG — δείχνει ΚΑΙ τις πηγές που βρέθηκαν μόνες τους. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun XmltvDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    // Το vm.searchEpg() υπήρχε αλλά ΔΕΝ το καλούσε κανείς — ο χρήστης δεν
    // μάθαινε ποτέ ότι η λίστα του έχει δικό της EPG (url-tvg / xmltv.php).
    val st by vm.epgState.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf(com.prelude.iptv.data.EpgManager.currentSource() ?: "") }
    val f = rememberInitialFocus()
    val busy = when (st.status) {
        EpgStatus.Loading,
        EpgStatus.LoadingWithExistingGuide,
        EpgStatus.Downloading,
        EpgStatus.DownloadingWithExistingGuide -> true
        else -> false
    }
    val statusText = st.status.localizedText()
    LaunchedEffect(Unit) { vm.searchEpg() }
    val close = { vm.closeEpgSearch(); onDismiss() }

    AlertDialog(
        onDismissRequest = { if (!busy) close() },
        containerColor = BgElev2,
        title = { Text("EPG (XMLTV)", color = TextHi) },
        text = {
            Column {
                if (st.sources.isNotEmpty()) {
                    Text("Βρέθηκαν πηγές για αυτή τη λίστα — πάτα για κατέβασμα:",
                        color = TextMid, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        st.sources.forEach { source ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .tvFocus(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !busy) {
                                        url = source.url
                                        vm.useEpgSource(source.url)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, null, tint = AccentSoft,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(source.localizedLabel(), color = TextHi, fontSize = 13.sp)
                                    Text(source.url, color = TextLo, fontSize = 10.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(8.dp))
                }
                Text("…ή δώσε URL σε .xml / .xml.gz", color = TextMid, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                SettingField("XMLTV URL", url, modifier = Modifier.focusRequester(f)) { url = it }
                if (busy) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp)); Text("Κατέβασμα…", color = TextMid, fontSize = 12.sp)
                    }
                } else if (statusText.isNotEmpty()) {
                    // «✓ Ταιριάζει σε Χ κανάλια» / «⚠ δεν ταιριάζει» / «✗ απέτυχε»
                    Spacer(Modifier.height(10.dp))
                    Text(statusText, color = AccentSoft, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && url.isNotBlank(),
                // ίδια ροή με τις προτεινόμενες πηγές -> ίδιο feedback «σε Χ κανάλια»
                onClick = { vm.useEpgSource(url.trim()) }
            ) { Text("Κατέβασμα", color = AccentSoft) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = close) {
                Text(if (st.loaded) "Κλείσιμο" else "Άκυρο", color = TextMid)
            }
        }
    )
}

/* ---------------------------------------------------------------- κοινά --- */

/** Παίζει ένα μεμονωμένο stream χωρίς να μπει σε λίστα. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SingleStreamDialog(onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val f = rememberInitialFocus()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgElev2,
        title = { Text("Αναπαραγωγή stream", color = TextHi) },
        text = {
            Column {
                Text("Δώσε απευθείας URL (m3u8, ts, mp4…) για άμεση αναπαραγωγή.",
                    color = TextMid, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                SettingField("Όνομα (προαιρετικό)", name, modifier = Modifier.focusRequester(f)) { name = it }
                SettingField("URL", url) { url = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank(),
                onClick = {
                    // μεμονωμένο stream: άδεια ουρά, ώστε prev/next να μη δείχνει αλλού
                    PlaybackQueue.items = emptyList()
                    PlaybackQueue.index = 0
                    PlaybackQueue.stalker = null
                    PlaybackQueue.sourceId = ""
                    ctx.startActivity(
                        PlayerLaunchRequest(
                            url = url.trim(),
                            title = name.ifBlank { "Stream" },
                            kind = "live",
                        ).toIntent(ctx)
                    )
                    onDismiss()
                }
            ) { Text("Αναπαραγωγή", color = AccentSoft) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Άκυρο", color = TextMid) } }
    )
}
