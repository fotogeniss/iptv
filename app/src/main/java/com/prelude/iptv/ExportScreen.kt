package com.prelude.iptv

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.Exporter
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.ui.MainViewModel
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

private val Bg = Color(0xFF121216)
private val Surf1 = Color(0xFF1C1C22)
private val Surf2 = Color(0xFF26262E)
private val Acc = Color(0xFFE11D2A)
private val AccSoft = Color(0xFFFF6B6B)

@Composable
fun ExportScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val state by vm.exportState.collectAsStateWithLifecycle()
    var generating by remember { mutableStateOf(false) }
    var pendingContent by remember { mutableStateOf<String?>(null) }

    // SAF: ο χρήστης διαλέγει πού θα αποθηκευτεί το .m3u
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        val content = pendingContent
        if (uri != null && content != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                Toast.makeText(ctx, ctx.getString(R.string.export_toast_saved), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, ctx.getString(R.string.export_toast_save_error), Toast.LENGTH_LONG).show()
            }
        }
        pendingContent = null
    }
    val all = remember { vm.exportableChannels() }
    val isMac = vm.currentPlaylist()?.type == PlaylistType.STALKER

    // ομαδοποίηση διατηρώντας σειρά
    val noGroupLabel = stringResource(R.string.export_group_no_group)
    val groups = remember {
        val map = LinkedHashMap<String, MutableList<Int>>()
        all.forEachIndexed { i, ch ->
            map.getOrPut(ch.group.ifEmpty { noGroupLabel }) { mutableListOf() }.add(i)
        }
        map
    }
    val selected = remember { mutableStateMapOf<Int, Boolean>().apply { all.indices.forEach { put(it, true) } } }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    fun selectedList(): List<Channel> =
        all.indices.filter { selected[it] == true }.map { all[it] }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            // header
            Row(
                Modifier.fillMaxWidth().background(Surf1).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.export_back), tint = Color.White) }
                Text(stringResource(R.string.export_title), color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    pluralStringResource(
                        R.plurals.export_selected_count,
                        selected.values.count { it },
                        selected.values.count { it }
                    ),
                    color = AccSoft, fontSize = 12.sp
                )
            }

            // γρήγορα κουμπιά
            Row(Modifier.padding(8.dp)) {
                SmallChip(stringResource(R.string.export_group_all)) { all.indices.forEach { selected[it] = true } }
                SmallChip(stringResource(R.string.export_group_none)) { all.indices.forEach { selected[it] = false } }
                SmallChip(stringResource(R.string.export_group_favorites)) {
                    all.indices.forEach { selected[it] = vm.favKey(all[it]) in state.favorites }
                }
            }

            // δέντρο ομάδων
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                groups.forEach { (group, idxs) ->
                    item {
                        val allSel = idxs.all { selected[it] == true }
                        val someSel = idxs.any { selected[it] == true }
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                val target = !allSel
                                idxs.forEach { selected[it] = target }
                            }.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TriBox(allSel, someSel)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$group  (${idxs.size})", color = Color.White,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { expanded[group] = !(expanded[group] ?: false) }) {
                                Icon(
                                    if (expanded[group] == true) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null, tint = AccSoft
                                )
                            }
                        }
                    }
                    if (expanded[group] == true) {
                        items(idxs) { i ->
                            Row(
                                Modifier.fillMaxWidth().clickable { selected[i] = !(selected[i] ?: false) }
                                    .padding(start = 40.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected[i] == true,
                                    onCheckedChange = { selected[i] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Acc)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(all[i].name, color = Color(0xFFCFCFD6), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // ενέργειες
            Surface(color = Surf1, tonalElevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    if (isMac) {
                        Text(
                            stringResource(R.string.export_mac_notice),
                            color = Color(0xFF8A8A94), fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        // --- M3U αρχείο (resolve) ---
                        Button(
                            onClick = {
                                if (generating) return@Button
                                generating = true
                                scope.launch {
                                    val m3u = vm.buildResolvedM3u(selectedList())
                                    generating = false
                                    pendingContent = m3u
                                    saveLauncher.launch((vm.currentPlaylist()?.name ?: "mac") + ".m3u")
                                }
                            },
                            enabled = !generating,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Acc)
                        ) {
                            if (generating) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.export_action_generating))
                            } else {
                                Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.export_action_save_m3u))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Surf2)
                        Spacer(Modifier.height(10.dp))
                        // --- Relay ---
                        if (state.relayRunning) {
                            Text(state.relayUrl, color = AccSoft, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Row {
                                Button(
                                    onClick = { copyToClipboard(ctx, state.relayUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Surf2)
                                ) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.export_action_copy_url)) }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { vm.stopRelay() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A2E2E))
                                ) { Text(stringResource(R.string.export_action_stop_relay)) }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val url = vm.startRelay(selectedList())
                                    Toast.makeText(ctx, ctx.getString(R.string.export_toast_relay_started, url), Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Icon(Icons.Default.Wifi, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.export_action_start_relay)) }
                        }
                    } else {
                        Button(
                            onClick = {
                                val sel = selectedList()
                                pendingContent = Exporter.buildDirectM3u(sel)
                                saveLauncher.launch((vm.currentPlaylist()?.name ?: "playlist") + ".m3u")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Acc)
                        ) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.export_action_save_m3u)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallChip(label: String, onClick: () -> Unit) {
    Surface(
        color = Surf2, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(end = 8.dp).clickable { onClick() }
    ) {
        Text(label, color = AccSoft, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun TriBox(all: Boolean, some: Boolean) {
    val icon = when {
        all -> Icons.Default.CheckBox
        some -> Icons.Default.IndeterminateCheckBox
        else -> Icons.Default.CheckBoxOutlineBlank
    }
    Icon(icon, null, tint = if (all || some) Acc else Color(0xFF6E6E78))
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cb.setPrimaryClip(android.content.ClipData.newPlainText("relay", text))
    Toast.makeText(ctx, ctx.getString(R.string.export_toast_copied), Toast.LENGTH_SHORT).show()
}
