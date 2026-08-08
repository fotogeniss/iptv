package com.prelude.iptv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.source.StalkerClient
import com.prelude.iptv.source.XtreamClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddPlaylistDialog(
    initialType: PlaylistType = PlaylistType.M3U,
    onDismiss: () -> Unit,
    onAdd: (Playlist) -> Unit
) {
    var tab by remember { mutableStateOf(initialType.ordinal.coerceIn(0, 2)) }
    val titles = listOf("M3U", "Xtream", "MAC")
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var epg by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var portal by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var ua by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var testing by remember { mutableStateOf(false) }
    var testOk by remember { mutableStateOf<Boolean?>(null) }
    var testMsg by remember { mutableStateOf("") }

    fun runTest() {
        testing = true; testOk = null; testMsg = ""
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                try {
                    when (tab) {
                        1 -> XtreamClient.test(server.trim(), user.trim(), pass.trim())
                        2 -> StalkerClient(portal.trim(), mac.trim(), ua.trim()).testConnection()
                        else -> true to "Το M3U θα ελεγχθεί κατά τη φόρτωση."
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    false to (e.message ?: "σφάλμα")
                }
            }
            testing = false; testOk = res.first; testMsg = res.second
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color(0xFF131318),
            tonalElevation = 6.dp
        ) {
            Column(
                Modifier.padding(18.dp).width(340.dp).verticalScroll(rememberScrollState())
            ) {
                Text("Προσθήκη playlist", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(10.dp))
                TabRow(
                    selectedTabIndex = tab, containerColor = Color(0xFF1C1C22),
                    contentColor = Color(0xFFFF6B6B)
                ) {
                    titles.forEachIndexed { i, t ->
                        Tab(selected = tab == i, onClick = { tab = i; testOk = null }, text = { Text(t) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Field("Όνομα", name) { name = it }

                when (tab) {
                    0 -> Field("M3U URL", m3uUrl) { m3uUrl = it }
                    1 -> {
                        Field("Server (http://host:port)", server) { server = it }
                        Field("Username", user) { user = it }
                        Field("Password", pass, isPassword = true) { pass = it }
                    }
                    2 -> {
                        Field("Portal URL", portal) { portal = it }
                        Field("MAC (00:1A:79:..)", mac) { mac = it }
                        Field("User-Agent (προαιρετικό)", ua) { ua = it }
                    }
                }
                Field("EPG URL (προαιρετικό, XMLTV)", epg) { epg = it }

                // δοκιμή σύνδεσης (Xtream/MAC)
                if (tab == 1 || tab == 2) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { runTest() },
                        enabled = !testing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (testing) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFFF6B6B))
                            Spacer(Modifier.width(8.dp)); Text("Δοκιμή…")
                        } else Text("🔌 Δοκιμή σύνδεσης")
                    }
                    testOk?.let { ok ->
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (ok) Icons.Default.CheckCircle else Icons.Default.Error, null,
                                tint = if (ok) Color(0xFF66BB6A) else Color(0xFFE57373)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(testMsg, color = if (ok) Color(0xFF9CCC65) else Color(0xFFEF9A9A), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Άκυρο") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val pl = build(tab, name, epg, m3uUrl, server, user, pass, portal, mac, ua)
                        if (pl == null) error = "Συμπλήρωσε τα απαραίτητα πεδία." else onAdd(pl)
                    }) { Text("Προσθήκη") }
                }
            }
        }
    }
}

private fun build(
    tab: Int, name: String, epg: String, m3uUrl: String,
    server: String, user: String, pass: String,
    portal: String, mac: String, ua: String
): Playlist? = when (tab) {
    0 -> if (m3uUrl.isBlank()) null else Playlist(
        name = name.ifBlank { m3uUrl.substringAfterLast('/').ifBlank { "Playlist" } },
        type = PlaylistType.M3U, source = m3uUrl.trim(), isUrl = true, epgUrl = epg.trim()
    )
    1 -> if (server.isBlank() || user.isBlank() || pass.isBlank()) null else Playlist(
        name = name.ifBlank { "Xtream $user" },
        type = PlaylistType.XTREAM, server = server.trim(),
        username = user.trim(), password = pass.trim(), epgUrl = epg.trim()
    )
    else -> if (portal.isBlank() || mac.isBlank()) null else Playlist(
        name = name.ifBlank { "MAC ${mac.takeLast(8)}" },
        type = PlaylistType.STALKER, portal = portal.trim(),
        mac = mac.trim(), userAgent = ua.trim(), epgUrl = epg.trim()
    )
}

@Composable
private fun Field(label: String, value: String, isPassword: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) }, singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFFE11D2A), unfocusedBorderColor = Color(0xFF262630),
            focusedLabelColor = Color(0xFFFF6B6B), unfocusedLabelColor = Color(0xFF6B6B76)
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
