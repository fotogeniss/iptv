package com.prelude.iptv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.focusRequester
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.ui.TextEntryDialog
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.AppDimens
import com.prelude.iptv.ui.isTvDevice
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.mobile.settings.MobileEditPlaylistScreen
import com.prelude.iptv.ui.mobile.sources.MobileAddPlaylistScreen
import com.prelude.iptv.ui.tv.sources.TvAddPlaylistScreen
import com.prelude.iptv.ui.tvFocus
import com.prelude.iptv.source.StalkerClient
import com.prelude.iptv.source.XtreamClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val ABg     = IptvColors.Background
private val AElev   = IptvColors.BackgroundRaised
private val AElev2  = IptvColors.Surface
private val ALine   = IptvColors.DividerStrong
private val AAccent = IptvColors.Primary
private val AAcc2   = IptvColors.TextPrimary
private val AHi     = IptvColors.TextPrimary
private val AMid    = IptvColors.TextSecondary
private val ALo     = IptvColors.TextTertiary

/** Χρώματα/εικονίδια προφίλ (αντί για avatar εικόνες). */
val ProfileColors = listOf(
    Color(0xFFE11D2A), Color(0xFF2E86DE), Color(0xFF16A34A),
    Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFF06B6D4)
)
val ProfileIcons = listOf(
    Icons.Default.Person, Icons.Default.Face, Icons.Default.Star,
    Icons.Default.Bolt, Icons.Default.Home, Icons.Default.Tv
)

/**
 * Οθόνη προσθήκης πηγής. Καρτέλες: M3U | XTREAM | MAC | FILE.
 * [initialTab] 0=M3U, 1=Xtream, 2=MAC, 3=Αρχείο.
 */
@Composable
fun AddPlaylistScreen(
    initialTab: Int = 0,
    /** αν δοθεί, η οθόνη λειτουργεί ως «Επεξεργασία» και τα πεδία έρχονται συμπληρωμένα */
    existing: Playlist? = null,
    onDismiss: () -> Unit,
    onAdd: (Playlist) -> Unit
) {
    if (existing == null && !isTvDevice()) {
        MobileAddPlaylistScreen(
            initialTab = initialTab,
            onDismiss = onDismiss,
            onAdd = onAdd,
        )
        return
    }
    if (existing == null && isTvDevice()) {
        TvAddPlaylistScreen(
            initialTab = initialTab,
            onDismiss = onDismiss,
            onAdd = onAdd,
        )
        return
    }

    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val editing = existing != null
    val defaultPlaylistName = stringResource(R.string.sources_default_playlist_name)
    val defaultLocalName = stringResource(R.string.sources_default_local_name)

    if (
        existing != null && !isTvDevice() &&
        (existing.type == PlaylistType.XTREAM || (existing.type == PlaylistType.M3U && existing.isUrl))
    ) {
        MobileEditPlaylistScreen(existing = existing, onBack = onDismiss, onSave = onAdd)
        return
    }

    val startTab = when {
        existing == null -> initialTab.coerceIn(0, 3)
        existing.type == PlaylistType.XTREAM -> 1
        existing.type == PlaylistType.STALKER -> 2
        existing.isUrl -> 0
        else -> 3
    }
    var tab by remember { mutableStateOf(startTab) }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var epg by remember { mutableStateOf(existing?.epgUrl ?: "") }
    var m3uUrl by remember {
        mutableStateOf(if (existing?.type == PlaylistType.M3U && existing.isUrl) existing.source else "")
    }
    var server by remember { mutableStateOf(existing?.server ?: "") }
    var user by remember { mutableStateOf(existing?.username ?: "") }
    var pass by remember { mutableStateOf(existing?.password ?: "") }
    var portal by remember { mutableStateOf(existing?.portal ?: "") }
    var mac by remember { mutableStateOf(existing?.mac ?: "") }
    var ua by remember { mutableStateOf(existing?.userAgent ?: "") }
    var avatar by remember { mutableStateOf(existing?.avatar ?: 0) }
    var error by remember { mutableStateOf<String?>(null) }

    // FILE tab
    var pickedPath by remember {
        mutableStateOf(if (existing?.type == PlaylistType.M3U && !existing.isUrl) existing.source else "")
    }
    var pickedLabel by remember {
        mutableStateOf(if (existing?.type == PlaylistType.M3U && !existing.isUrl)
            existing.source.substringAfterLast('/') else "")
    }

    // Χωρίς αρχικό focus, σε τηλεόραση το τηλεχειριστήριο δεν κάνει τίποτα εδώ.
    val firstFocus = rememberInitialFocus()

    var testing by remember { mutableStateOf(false) }
    var testOk by remember { mutableStateOf<Boolean?>(null) }
    var testMsg by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                try {
                    // Streaming αντίγραφο — το readText() φόρτωνε ΟΛΟ το M3U στη
                    // μνήμη: 100MB λίστα σε TV box = OOM. Η άδεια του URI χάνεται
                    // μετά, γι' αυτό και το τοπικό αντίγραφο.
                    val dir = File(ctx.filesDir, "playlists").apply { mkdirs() }
                    val f = File(dir, "pl_${System.currentTimeMillis()}.m3u")
                    val inp = ctx.contentResolver.openInputStream(uri)
                        ?: return@withContext null to ctx.getString(R.string.sources_file_open_failed)
                    inp.use { src -> f.outputStream().use { dst -> src.copyTo(dst) } }
                    // έλεγχος εγκυρότητας στα πρώτα 4KB — αρκεί για το #EXTM3U
                    val head = f.inputStream().use { st ->
                        val b = ByteArray(4096); val n = st.read(b); String(b, 0, maxOf(n, 0))
                    }
                    if (!head.contains("#EXTINF") && !head.contains("#EXTM3U")) {
                        f.delete()
                        return@withContext null to ctx.getString(R.string.sources_failure_invalid_m3u)
                    }
                    f.absolutePath to (uri.lastPathSegment?.substringAfterLast('/') ?: "playlist.m3u")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    null to (e.message ?: ctx.getString(R.string.sources_read_error))
                }
            }
            val importedPath = res.first
            if (importedPath == null) error = res.second
            else { pickedPath = importedPath; pickedLabel = res.second; error = null }
        }
    }

    fun runTest() {
        testing = true; testOk = null; testMsg = ""
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                try {
                    when (tab) {
                        0 -> com.prelude.iptv.data.Repository.testM3u(m3uUrl.trim(), isUrl = true)
                        1 -> XtreamClient.test(server.trim(), user.trim(), pass.trim())
                        2 -> StalkerClient(portal.trim(), mac.trim(), ua.trim()).testConnection()
                        else -> com.prelude.iptv.data.Repository.testM3u(pickedPath, isUrl = false)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    false to (e.message ?: ctx.getString(R.string.sources_generic_error))
                }
            }
            testing = false; testOk = res.first; testMsg = res.second
        }
    }

    val valid = when (tab) {
        0 -> m3uUrl.isNotBlank()
        1 -> server.isNotBlank() && user.isNotBlank() && pass.isNotBlank()
        2 -> portal.isNotBlank() && mac.isNotBlank()
        else -> pickedPath.isNotBlank()
    }

    Column(Modifier.fillMaxSize().background(ABg).systemBarsPadding()) {

        // ---- top bar ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.sources_back), tint = AHi) }
            Text(
                stringResource(if (editing) R.string.sources_edit_source else R.string.sources_add_source),
                color = AHi, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = if (isTvDevice()) 860.dp else 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isTvDevice()) AppDimens.TvHorizontal else AppDimens.MobileHorizontal)
        ) {
            Text(
                stringResource(if (editing) R.string.sources_update_credentials else R.string.sources_connect_service),
                color = AHi,
                fontSize = if (isTvDevice()) 28.sp else 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.sources_flow_summary),
                color = ALo,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(22.dp))

            // ---- pill segmented tabs ----
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(AElev).padding(4.dp)
            ) {
                listOf("M3U", "XTREAM", "MAC", stringResource(R.string.sources_file_tab)).forEachIndexed { i, label ->
                    val sel = tab == i
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                            .then(
                                if (sel) Modifier.background(AAccent)
                                else Modifier
                            )
                            // η επιλεγμένη καρτέλα παίρνει το αρχικό focus (τηλεόραση)
                            .then(if (i == startTab) Modifier.focusRequester(firstFocus) else Modifier)
                            .tvFocus(RoundedCornerShape(22.dp), tint = false)
                            .clickable { tab = i; testOk = null; error = null }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, color = if (sel) Color.White else AMid, fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // ---- επιλογή προφίλ (Xtream / MAC) ----
            if (tab == 1 || tab == 2) {
                Label(stringResource(R.string.sources_profile_icon))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ProfileColors.size) { i ->
                        val sel = avatar == i
                        Box(
                            Modifier.size(58.dp).clip(RoundedCornerShape(14.dp))
                                .background(ProfileColors[i].copy(alpha = if (sel) 0.30f else 0.14f))
                                .border(
                                    if (sel) 2.dp else 1.dp,
                                    if (sel) ProfileColors[i] else ALine,
                                    RoundedCornerShape(14.dp)
                                )
                                .tvFocus(RoundedCornerShape(14.dp), tint = false)
                                .clickable { avatar = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(ProfileIcons[i], null, tint = ProfileColors[i], modifier = Modifier.size(26.dp))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            Label(stringResource(R.string.sources_name))
            AField(name, stringResource(R.string.sources_name_hint)) { name = it }

            when (tab) {
                0 -> {
                    Label(stringResource(R.string.sources_playlist_url), required = true)
                    AField(m3uUrl, "https://example.com/list.m3u") { m3uUrl = it }
                }
                1 -> {
                    Label(stringResource(R.string.sources_server_address), required = true)
                    AField(server, "http://server:8080") { server = it }
                    Label(stringResource(R.string.sources_username), required = true)
                    AField(user, "username") { user = it }
                    Label(stringResource(R.string.sources_password), required = true)
                    AField(pass, "password", isPassword = true) { pass = it }
                }
                2 -> {
                    Label(stringResource(R.string.sources_portal_address), required = true)
                    AField(portal, "http://portal.com/c/") { portal = it }
                    Label(stringResource(R.string.sources_mac_address), required = true)
                    AField(mac, "00:1A:79:XX:XX:XX") { mac = it }
                    Label(stringResource(R.string.sources_user_agent_optional))
                    AField(ua, stringResource(R.string.sources_user_agent_auto_hint)) { ua = it }
                }
                3 -> {
                    Label(stringResource(R.string.sources_file_from_device), required = true)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(AElev).border(1.dp, ALine, RoundedCornerShape(14.dp))
                            .tvFocus(RoundedCornerShape(14.dp))
                            .clickable { picker.launch(arrayOf("*/*")) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = AAcc2, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            pickedLabel.ifBlank { stringResource(R.string.sources_tap_select_file) },
                            color = if (pickedLabel.isBlank()) ALo else AHi, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                        )
                        if (pickedPath.isNotBlank())
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF66BB6A), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (tab != 3) {
                Label(stringResource(R.string.sources_epg_url_optional_xmltv))
                AField(epg, "https://example.com/epg.xml") { epg = it }
            }

            // ---- δοκιμή σύνδεσης (όλοι οι τύποι) ----
            run {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { runTest() }, enabled = !testing && valid,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ALine),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AHi),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (testing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = AAcc2)
                        Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.sources_testing))
                    } else {
                        Icon(Icons.Default.Cable, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.sources_test_connection))
                    }
                }
                testOk?.let { ok ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (ok) Icons.Default.CheckCircle else Icons.Default.Error, null,
                            tint = if (ok) Color(0xFF66BB6A) else Color(0xFFE57373),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(testMsg, color = if (ok) Color(0xFF9CCC65) else Color(0xFFEF9A9A), fontSize = 12.sp)
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFEF9A9A), fontSize = 13.sp)
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    val pl = build(
                        tab, name, epg, m3uUrl, server, user, pass, portal, mac,
                        ua, pickedPath, pickedLabel, avatar, defaultPlaylistName, defaultLocalName,
                    )
                    if (pl == null) error = ctx.getString(R.string.sources_complete_required) else onAdd(pl)
                },
                enabled = valid,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AAccent,
                    contentColor = Color.White,
                    disabledContainerColor = AElev2,
                    disabledContentColor = ALo
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(if (editing) Icons.Default.Check else Icons.Default.Add, null,
                    tint = if (valid) Color.White else ALo, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (editing) R.string.sources_save
                        else if (tab == 1) R.string.sources_add_profile
                        else R.string.sources_add_playlist
                    ),
                    color = if (valid) Color.White else ALo,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(30.dp))
        }
        }
    }
}

@Composable
private fun Label(text: String, required: Boolean = false) {
    Row(Modifier.padding(bottom = 6.dp, top = 4.dp)) {
        Text(text, color = AHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (required) Text(" *", color = AAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AField(value: String, hint: String, isPassword: Boolean = false, onChange: (String) -> Unit) {
    // Σε τηλεόραση το πληκτρολόγιο ΔΕΝ ανοίγει μόλις περάσει το focus:
    // το πεδίο είναι κουμπί και γράφεις μόνο αν πατήσεις OK.
    if (isTvDevice()) TvField(value, hint, isPassword, onChange)
    else PhoneField(value, hint, isPassword, onChange)
}

@Composable
private fun TvField(value: String, hint: String, isPassword: Boolean, onChange: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth().padding(bottom = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AElev)
            .border(1.dp, ALine, RoundedCornerShape(14.dp))
            .tvFocus(RoundedCornerShape(14.dp))
            .clickable { editing = true }
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        val shown = when {
            value.isEmpty() -> hint
            isPassword -> "•".repeat(value.length.coerceAtMost(14))
            else -> value
        }
        Text(
            shown, color = if (value.isEmpty()) ALo else AHi, fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
    if (editing) TextEntryDialog(
        title = hint.ifBlank { stringResource(R.string.sources_enter_value) },
        initial = value,
        isPassword = isPassword,
        onDismiss = { editing = false },
        onOk = { onChange(it); editing = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneField(value: String, hint: String, isPassword: Boolean, onChange: (String) -> Unit) {
    var reveal by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange,
        placeholder = { Text(hint, color = ALo, fontSize = 14.sp) },
        singleLine = true, shape = RoundedCornerShape(14.dp),
        visualTransformation = if (isPassword && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (!isPassword) null else {
            {
                IconButton(onClick = { reveal = !reveal }) {
                    Icon(
                        if (reveal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        stringResource(if (reveal) R.string.sources_hide_password else R.string.sources_show_password),
                        tint = ALo, modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AHi, unfocusedTextColor = AHi,
            focusedBorderColor = AAccent, unfocusedBorderColor = ALine,
            unfocusedContainerColor = AElev, focusedContainerColor = AElev,
            cursorColor = AAccent
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
    )
}

private fun build(
    tab: Int, name: String, epg: String, m3uUrl: String,
    server: String, user: String, pass: String,
    portal: String, mac: String, ua: String,
    filePath: String, fileLabel: String, avatar: Int,
    defaultPlaylistName: String, defaultLocalName: String
): Playlist? = when (tab) {
    0 -> if (m3uUrl.isBlank()) null else Playlist(
        name = name.ifBlank { m3uUrl.substringAfterLast('/').ifBlank { defaultPlaylistName } },
        type = PlaylistType.M3U, source = m3uUrl.trim(), isUrl = true, epgUrl = epg.trim(), avatar = avatar
    )
    1 -> if (server.isBlank() || user.isBlank() || pass.isBlank()) null else Playlist(
        name = name.ifBlank { "Xtream $user" },
        type = PlaylistType.XTREAM, server = server.trim(),
        username = user.trim(), password = pass.trim(), epgUrl = epg.trim(), avatar = avatar
    )
    2 -> if (portal.isBlank() || mac.isBlank()) null else Playlist(
        name = name.ifBlank { "MAC ${mac.takeLast(8)}" },
        type = PlaylistType.STALKER, portal = portal.trim(),
        mac = mac.trim(), userAgent = ua.trim(), epgUrl = epg.trim(), avatar = avatar
    )
    else -> if (filePath.isBlank()) null else Playlist(
        name = name.ifBlank { fileLabel.ifBlank { defaultLocalName } },
        type = PlaylistType.M3U, source = filePath, isUrl = false, avatar = avatar
    )
}
