package com.prelude.iptv.ui.mobile.sources

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.sources.importM3uFile
import com.prelude.iptv.ui.sources.PlaylistConnectionTestResult
import com.prelude.iptv.ui.sources.testPlaylistConnection
import kotlinx.coroutines.launch

@Composable
fun MobileAddPlaylistScreen(
    initialTab: Int,
    onDismiss: () -> Unit,
    onAdd: (Playlist) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var methodName by rememberSaveable { mutableStateOf(MobilePlaylistDraftPolicy.methodForInitialTab(initialTab).name) }
    val method = runCatching { MobilePlaylistMethod.valueOf(methodName) }.getOrDefault(MobilePlaylistMethod.URL)
    var playlistUrl by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var portal by rememberSaveable { mutableStateOf("") }
    var macAddress by rememberSaveable { mutableStateOf("") }
    var userAgent by rememberSaveable { mutableStateOf("") }
    var filePath by rememberSaveable { mutableStateOf("") }
    var fileLabel by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var epgUrl by rememberSaveable { mutableStateOf("") }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var showQuickTip by rememberSaveable { mutableStateOf(false) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    var importingFile by rememberSaveable { mutableStateOf(false) }
    var testingConnection by remember { mutableStateOf(false) }
    var testedDraft by remember { mutableStateOf<MobilePlaylistDraft?>(null) }
    var connectionResult by remember { mutableStateOf<PlaylistConnectionTestResult?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importingFile = true
        scope.launch {
            val result = importM3uFile(context, uri)
            importingFile = false
            result.fold(
                onSuccess = {
                    filePath = it.path
                    fileLabel = it.label
                    formError = null
                },
                onFailure = { formError = it.message ?: "Δεν άνοιξε το αρχείο M3U." },
            )
        }
    }

    val draft = MobilePlaylistDraft(
        method = method,
        playlistUrl = playlistUrl,
        server = server,
        username = username,
        password = password,
        portal = portal,
        macAddress = macAddress,
        userAgent = userAgent,
        filePath = filePath,
        fileLabel = fileLabel,
        name = name,
        epgUrl = epgUrl,
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF030304), Color(0xFF090304), Color(0xFF030304)),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            MobilePlaylistTopBar(
                onQuickTip = { showQuickTip = true },
                onLogin = {
                    Toast.makeText(context, "Η σύνδεση λογαριασμού θα προστεθεί αργότερα.", Toast.LENGTH_SHORT).show()
                },
            )

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Προσθήκη λίστας",
                    color = IptvColors.Primary,
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Το PRELUDE+ είναι media player και δεν παρέχει περιεχόμενο ή συνδρομές IPTV.",
                    color = IptvColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Text("Τρόπος σύνδεσης", color = IptvColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                MobilePlaylistMethod.entries.forEach { candidate ->
                    MobilePlaylistMethodCard(
                        method = candidate,
                        selected = method == candidate,
                        onSelect = {
                            methodName = candidate.name
                            formError = null
                        },
                    )
                }
            }

            Spacer(Modifier.height(19.dp))
            when (method) {
                MobilePlaylistMethod.URL -> MobilePlaylistField(
                    label = "URL λίστας",
                    value = playlistUrl,
                    placeholder = "http://example.com/playlist.m3u",
                    onValueChange = { playlistUrl = it; formError = null },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                )

                MobilePlaylistMethod.XTREAM -> {
                    MobilePlaylistField(
                        label = "Server URL",
                        value = server,
                        placeholder = "http://example.com:port",
                        onValueChange = { server = it; formError = null },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(13.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MobilePlaylistField(
                            label = "Username",
                            value = username,
                            placeholder = "Username",
                            onValueChange = { username = it; formError = null },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                        MobilePlaylistField(
                            label = "Password",
                            value = password,
                            placeholder = "Password",
                            onValueChange = { password = it; formError = null },
                            modifier = Modifier.weight(1f),
                            password = true,
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        )
                    }
                }

                MobilePlaylistMethod.MAC -> {
                    MobilePlaylistField(
                        label = "Portal URL",
                        value = portal,
                        placeholder = "http://portal.example.com/c/",
                        onValueChange = { portal = it; formError = null },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(13.dp))
                    MobilePlaylistField(
                        label = "MAC address",
                        value = macAddress,
                        placeholder = "00:1A:79:00:00:00",
                        onValueChange = {
                            macAddress = MobilePlaylistDraftPolicy.formatMac(it)
                            formError = null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(13.dp))
                    MobilePlaylistField(
                        label = "User-Agent",
                        value = userAgent,
                        placeholder = "MAG250 / custom User-Agent",
                        onValueChange = { userAgent = it },
                        optional = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }

                MobilePlaylistMethod.FILE -> MobileM3uFilePicker(
                    fileLabel = fileLabel,
                    importing = importingFile,
                    onPick = { filePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/plain", "*/*")) },
                )
            }

            Spacer(Modifier.height(5.dp))
            MobilePlaylistAdvancedToggle(
                expanded = advancedExpanded,
                onClick = { advancedExpanded = !advancedExpanded },
            )
            AnimatedVisibility(
                visible = advancedExpanded,
                enter = fadeIn() + slideInVertically { -it / 5 },
                exit = fadeOut() + slideOutVertically { -it / 5 },
            ) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    MobilePlaylistField(
                        label = "Όνομα λίστας",
                        value = name,
                        placeholder = "π.χ. Η λίστα μου",
                        onValueChange = { name = it },
                        optional = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    if (method != MobilePlaylistMethod.FILE) {
                        Spacer(Modifier.height(13.dp))
                        MobilePlaylistField(
                            label = "EPG URL",
                            value = epgUrl,
                            placeholder = "http://example.com/epg.xml",
                            onValueChange = { epgUrl = it },
                            optional = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        )
                    }
                }
            }

            formError?.let {
                Text(
                    it,
                    color = IptvColors.Error,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 12.dp, start = 2.dp, end = 2.dp),
                )
            }

            Spacer(Modifier.height(22.dp))
            OutlinedButton(
                onClick = {
                    testingConnection = true
                    connectionResult = null
                    scope.launch {
                        val snapshot = draft
                        val result = testPlaylistConnection(snapshot)
                        testedDraft = snapshot
                        connectionResult = result
                        testingConnection = false
                    }
                },
                enabled = !importingFile && !testingConnection,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = IptvColors.TextPrimary,
                    disabledContentColor = IptvColors.TextTertiary,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (testingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = IptvColors.TextPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(9.dp))
                    Text("Έλεγχος σύνδεσης…", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Δοκιμή σύνδεσης", fontWeight = FontWeight.Bold)
                }
            }

            if (testedDraft == draft) {
                connectionResult?.let { result ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp, start = 3.dp, end = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (result.successful) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (result.successful) IptvColors.Success else IptvColors.Error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            result.message,
                            color = if (result.successful) IptvColors.Success else IptvColors.Error,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(13.dp))
            Button(
                onClick = {
                    val validation = MobilePlaylistDraftPolicy.validationMessage(draft)
                    val playlist = if (validation == null) MobilePlaylistDraftPolicy.build(draft) else null
                    if (playlist == null) formError = validation ?: "Δεν ήταν δυνατή η δημιουργία της λίστας."
                    else onAdd(playlist)
                },
                enabled = !importingFile,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IptvColors.Primary,
                    contentColor = Color.White,
                    disabledContainerColor = IptvColors.SurfaceRaised,
                    disabledContentColor = IptvColors.TextTertiary,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Αποθήκευση λίστας", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(49.dp)) {
                Text("Ίσως αργότερα", color = IptvColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showQuickTip) MobilePlaylistQuickTip(onDismiss = { showQuickTip = false })
}

@Composable
private fun MobilePlaylistTopBar(onQuickTip: () -> Unit, onLogin: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) {
        OutlinedButton(
            onClick = onQuickTip,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = IptvColors.TextPrimary),
            modifier = Modifier.align(Alignment.CenterStart).height(38.dp),
        ) {
            Text("Βοήθεια", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        MobilePlaylistBrand()
        TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterEnd)) {
            Text("Σύνδεση", color = IptvColors.TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MobileM3uFilePicker(fileLabel: String, importing: Boolean, onPick: () -> Unit) {
    Column {
        Text("Αρχείο M3U", color = IptvColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(IptvColors.SurfaceRaised)
                .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(13.dp))
                .clickable(enabled = !importing, onClick = onPick)
                .padding(horizontal = 15.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.InsertDriveFile,
                null,
                tint = IptvColors.TextSecondary,
                modifier = Modifier.size(23.dp),
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        importing -> "Εισαγωγή αρχείου…"
                        fileLabel.isNotBlank() -> fileLabel
                        else -> "Επίλεξε αρχείο από τη συσκευή"
                    },
                    color = if (fileLabel.isBlank()) IptvColors.TextTertiary else IptvColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (fileLabel.isBlank()) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Υποστηρίζονται .m3u και .m3u8", color = IptvColors.TextTertiary, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (fileLabel.isNotBlank()) {
                Icon(Icons.Default.CheckCircle, null, tint = IptvColors.Success, modifier = Modifier.size(20.dp))
            }
        }
    }
}
