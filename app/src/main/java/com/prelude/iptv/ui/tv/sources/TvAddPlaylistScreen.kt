package com.prelude.iptv.ui.tv.sources

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.TextEntryDialog
import com.prelude.iptv.ui.TvDialogTextButton
import com.prelude.iptv.ui.mobile.sources.MobilePlaylistDraft
import com.prelude.iptv.ui.mobile.sources.MobilePlaylistDraftPolicy
import com.prelude.iptv.ui.mobile.sources.MobilePlaylistMethod
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.requestFocusWithRetry
import com.prelude.iptv.ui.sources.importM3uFile
import com.prelude.iptv.ui.sources.PlaylistConnectionTestResult
import com.prelude.iptv.ui.sources.testPlaylistConnection
import kotlinx.coroutines.launch

@Composable
fun TvAddPlaylistScreen(
    initialTab: Int,
    onDismiss: () -> Unit,
    onAdd: (Playlist) -> Unit,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
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
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    var importingFile by rememberSaveable { mutableStateOf(false) }
    var editingInputName by rememberSaveable { mutableStateOf<String?>(null) }
    var showQuickTip by rememberSaveable { mutableStateOf(false) }
    var testingConnection by remember { mutableStateOf(false) }
    var testedDraft by remember { mutableStateOf<MobilePlaylistDraft?>(null) }
    var connectionResult by remember { mutableStateOf<PlaylistConnectionTestResult?>(null) }

    val methodFocus = remember { MobilePlaylistMethod.entries.associateWith { FocusRequester() } }
    val inputFocus = remember { TvPlaylistInput.entries.associateWith { FocusRequester() } }
    val advancedFocus = remember { FocusRequester() }
    val saveFocus = remember { FocusRequester() }
    val testFocus = remember { FocusRequester() }
    val laterFocus = remember { FocusRequester() }
    val helpFocus = remember { FocusRequester() }
    val loginFocus = remember { FocusRequester() }

    fun firstInputFor(target: MobilePlaylistMethod): TvPlaylistInput = when (target) {
        MobilePlaylistMethod.URL -> TvPlaylistInput.PLAYLIST_URL
        MobilePlaylistMethod.XTREAM -> TvPlaylistInput.SERVER
        MobilePlaylistMethod.MAC -> TvPlaylistInput.PORTAL
        MobilePlaylistMethod.FILE -> TvPlaylistInput.FILE
    }
    val firstFieldFocus = inputFocus.getValue(firstInputFor(method))

    LaunchedEffect(Unit) {
        methodFocus.getValue(method).requestFocusWithRetry()
    }

    fun restoreInputFocus(input: TvPlaylistInput) {
        scope.launch { inputFocus.getValue(input).requestFocusWithRetry() }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            restoreInputFocus(TvPlaylistInput.FILE)
            return@rememberLauncherForActivityResult
        }
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
            inputFocus.getValue(TvPlaylistInput.FILE).requestFocusWithRetry()
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

    val formTitle = when (method) {
        MobilePlaylistMethod.URL -> "Σύνδεση με URL"
        MobilePlaylistMethod.XTREAM -> "Σύνδεση Xtream"
        MobilePlaylistMethod.MAC -> "Σύνδεση MAC / Stalker"
        MobilePlaylistMethod.FILE -> "Τοπικό αρχείο M3U"
    }
    val formSubtitle = when (method) {
        MobilePlaylistMethod.URL -> "Συμπλήρωσε τον σύνδεσμο της λίστας σου."
        MobilePlaylistMethod.XTREAM -> "Συμπλήρωσε τα στοιχεία πρόσβασης του παρόχου."
        MobilePlaylistMethod.MAC -> "Συμπλήρωσε το Portal URL και τη MAC address."
        MobilePlaylistMethod.FILE -> "Επίλεξε ένα αρχείο από τη συσκευή."
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF030304), Color(0xFF090304), Color(0xFF030304)),
                ),
            )
            .systemBarsPadding()
            .padding(horizontal = 38.dp, vertical = 16.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                TvPlaylistAction(
                    label = "Γρήγορη βοήθεια",
                    primary = false,
                    focusRequester = helpFocus,
                    modifier = Modifier.align(Alignment.CenterStart).width(150.dp),
                    onClick = { showQuickTip = true },
                )
                TvPlaylistBrand()
                TvPlaylistAction(
                    label = "Σύνδεση",
                    primary = false,
                    focusRequester = loginFocus,
                    modifier = Modifier.align(Alignment.CenterEnd).width(105.dp),
                    onClick = {
                        Toast.makeText(context, "Η σύνδεση λογαριασμού θα προστεθεί αργότερα.", Toast.LENGTH_SHORT).show()
                    },
                )
            }

            Column(
                Modifier.fillMaxWidth().height(82.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Προσθήκη λίστας", color = IptvColors.Primary, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(
                    "Το PRELUDE+ είναι media player και δεν παρέχει περιεχόμενο ή συνδρομές IPTV.",
                    color = IptvColors.TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }

            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column(Modifier.weight(.88f).fillMaxHeight()) {
                    Text("1. Επίλεξε τρόπο σύνδεσης", color = IptvColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MobilePlaylistMethod.entries.chunked(2).forEachIndexed { rowIndex, rowMethods ->
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowMethods.forEachIndexed { columnIndex, candidate ->
                                    val index = rowIndex * 2 + columnIndex
                                    TvPlaylistMethodCard(
                                        method = candidate,
                                        selected = method == candidate,
                                        focusRequester = methodFocus.getValue(candidate),
                                        modifier = Modifier.weight(1f).focusProperties {
                                            if (index % 2 == 0) right = methodFocus.getValue(MobilePlaylistMethod.entries[index + 1])
                                            else {
                                                left = methodFocus.getValue(MobilePlaylistMethod.entries[index - 1])
                                                right = firstFieldFocus
                                            }
                                            if (index >= 2) up = methodFocus.getValue(MobilePlaylistMethod.entries[index - 2])
                                            if (index <= 1) down = methodFocus.getValue(MobilePlaylistMethod.entries[index + 2])
                                        },
                                        onSelect = {
                                            methodName = candidate.name
                                            formError = null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                TvPlaylistFormCard(
                    title = formTitle,
                    subtitle = formSubtitle,
                    modifier = Modifier.weight(1.12f).fillMaxHeight(),
                ) {
                    when (method) {
                        MobilePlaylistMethod.URL -> TvPlaylistFieldButton(
                            label = "URL λίστας",
                            value = playlistUrl,
                            placeholder = "http://example.com/playlist.m3u",
                            focusRequester = inputFocus.getValue(TvPlaylistInput.PLAYLIST_URL),
                            onClick = { editingInputName = TvPlaylistInput.PLAYLIST_URL.name },
                        )

                        MobilePlaylistMethod.XTREAM -> {
                            TvPlaylistFieldButton(
                                label = "Server URL",
                                value = server,
                                placeholder = "http://example.com:port",
                                focusRequester = inputFocus.getValue(TvPlaylistInput.SERVER),
                                onClick = { editingInputName = TvPlaylistInput.SERVER.name },
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TvPlaylistFieldButton(
                                    label = "Username",
                                    value = username,
                                    placeholder = "Username",
                                    focusRequester = inputFocus.getValue(TvPlaylistInput.USERNAME),
                                    modifier = Modifier.weight(1f),
                                    onClick = { editingInputName = TvPlaylistInput.USERNAME.name },
                                )
                                TvPlaylistFieldButton(
                                    label = "Password",
                                    value = password,
                                    placeholder = "Password",
                                    focusRequester = inputFocus.getValue(TvPlaylistInput.PASSWORD),
                                    modifier = Modifier.weight(1f),
                                    password = true,
                                    onClick = { editingInputName = TvPlaylistInput.PASSWORD.name },
                                )
                            }
                        }

                        MobilePlaylistMethod.MAC -> {
                            TvPlaylistFieldButton(
                                label = "Portal URL",
                                value = portal,
                                placeholder = "http://portal.example.com/c/",
                                focusRequester = inputFocus.getValue(TvPlaylistInput.PORTAL),
                                onClick = { editingInputName = TvPlaylistInput.PORTAL.name },
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TvPlaylistFieldButton(
                                    label = "MAC address",
                                    value = macAddress,
                                    placeholder = "00:1A:79:00:00:00",
                                    focusRequester = inputFocus.getValue(TvPlaylistInput.MAC),
                                    modifier = Modifier.weight(1f),
                                    onClick = { editingInputName = TvPlaylistInput.MAC.name },
                                )
                                TvPlaylistFieldButton(
                                    label = "User-Agent",
                                    value = userAgent,
                                    placeholder = "MAG250 / User-Agent",
                                    focusRequester = inputFocus.getValue(TvPlaylistInput.USER_AGENT),
                                    modifier = Modifier.weight(1f),
                                    optional = true,
                                    onClick = { editingInputName = TvPlaylistInput.USER_AGENT.name },
                                )
                            }
                        }

                        MobilePlaylistMethod.FILE -> TvM3uFileButton(
                            fileLabel = fileLabel,
                            importing = importingFile,
                            focusRequester = inputFocus.getValue(TvPlaylistInput.FILE),
                            onClick = {
                                filePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/plain", "*/*"))
                            },
                        )
                    }

                    Spacer(Modifier.height(5.dp))
                    TvPlaylistAdvancedButton(
                        expanded = advancedExpanded,
                        focusRequester = advancedFocus,
                        modifier = Modifier.focusProperties { down = testFocus },
                        onClick = { advancedExpanded = !advancedExpanded },
                    )
                    AnimatedVisibility(visible = advancedExpanded, enter = fadeIn(), exit = fadeOut()) {
                        Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TvPlaylistFieldButton(
                                label = "Όνομα λίστας",
                                value = name,
                                placeholder = "π.χ. Η λίστα μου",
                                focusRequester = inputFocus.getValue(TvPlaylistInput.NAME),
                                modifier = Modifier.weight(1f),
                                optional = true,
                                onClick = { editingInputName = TvPlaylistInput.NAME.name },
                            )
                            if (method != MobilePlaylistMethod.FILE) {
                                TvPlaylistFieldButton(
                                    label = "EPG URL",
                                    value = epgUrl,
                                    placeholder = "http://example.com/epg.xml",
                                    focusRequester = inputFocus.getValue(TvPlaylistInput.EPG),
                                    modifier = Modifier.weight(1f),
                                    optional = true,
                                    onClick = { editingInputName = TvPlaylistInput.EPG.name },
                                )
                            }
                        }
                    }
                    formError?.let {
                        Text(it, color = IptvColors.Error, fontSize = 9.sp, modifier = Modifier.padding(top = 7.dp))
                    }
                    if (testedDraft == draft) {
                        connectionResult?.let { result ->
                            Text(
                                result.message,
                                color = if (result.successful) IptvColors.Success else IptvColors.Error,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 7.dp),
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().height(62.dp).padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvPlaylistAction(
                    label = "Ίσως αργότερα",
                    primary = false,
                    focusRequester = laterFocus,
                    modifier = Modifier.width(170.dp).focusProperties {
                        right = testFocus
                        up = advancedFocus
                    },
                    onClick = onDismiss,
                )
                Spacer(Modifier.width(12.dp))
                TvPlaylistAction(
                    label = if (testingConnection) "Έλεγχος…" else "Δοκιμή σύνδεσης",
                    primary = false,
                    focusRequester = testFocus,
                    enabled = !importingFile && !testingConnection,
                    modifier = Modifier.width(190.dp).focusProperties {
                        left = laterFocus
                        right = saveFocus
                        up = advancedFocus
                    },
                    onClick = {
                        testingConnection = true
                        connectionResult = null
                        scope.launch {
                            val snapshot = draft
                            val result = testPlaylistConnection(snapshot)
                            testedDraft = snapshot
                            connectionResult = result
                            testingConnection = false
                            testFocus.requestFocusWithRetry()
                        }
                    },
                )
                Spacer(Modifier.width(12.dp))
                TvPlaylistAction(
                    label = "Αποθήκευση λίστας",
                    primary = true,
                    focusRequester = saveFocus,
                    modifier = Modifier.width(225.dp).focusProperties {
                        left = testFocus
                        up = advancedFocus
                    },
                    onClick = {
                        val validation = MobilePlaylistDraftPolicy.validationMessage(draft)
                        val playlist = if (validation == null) MobilePlaylistDraftPolicy.build(draft) else null
                        if (playlist != null) onAdd(playlist)
                        else {
                            formError = validation ?: "Δεν ήταν δυνατή η δημιουργία της λίστας."
                            val target = firstInvalidInput(draft, validation.orEmpty())
                            scope.launch { inputFocus.getValue(target).requestFocusWithRetry() }
                        }
                    },
                )
            }
        }
    }

    val editingInput = editingInputName?.let { nameValue ->
        runCatching { TvPlaylistInput.valueOf(nameValue) }.getOrNull()
    }
    if (editingInput != null) {
        TextEntryDialog(
            title = editingInput.title,
            initial = inputValue(
                editingInput, playlistUrl, server, username, password, portal, macAddress, userAgent, name, epgUrl,
            ),
            isPassword = editingInput == TvPlaylistInput.PASSWORD,
            onDismiss = {
                editingInputName = null
                restoreInputFocus(editingInput)
            },
            onOk = { newValue ->
                when (editingInput) {
                    TvPlaylistInput.PLAYLIST_URL -> playlistUrl = newValue
                    TvPlaylistInput.SERVER -> server = newValue
                    TvPlaylistInput.USERNAME -> username = newValue
                    TvPlaylistInput.PASSWORD -> password = newValue
                    TvPlaylistInput.PORTAL -> portal = newValue
                    TvPlaylistInput.MAC -> macAddress = MobilePlaylistDraftPolicy.formatMac(newValue)
                    TvPlaylistInput.USER_AGENT -> userAgent = newValue
                    TvPlaylistInput.NAME -> name = newValue
                    TvPlaylistInput.EPG -> epgUrl = newValue
                    TvPlaylistInput.FILE -> Unit
                }
                formError = null
                editingInputName = null
                restoreInputFocus(editingInput)
            },
        )
    }

    if (showQuickTip) {
        TvPlaylistQuickTipDialog(
            onDismiss = {
                showQuickTip = false
                scope.launch { helpFocus.requestFocusWithRetry() }
            },
        )
    }
}

@Composable
private fun TvPlaylistQuickTipDialog(onDismiss: () -> Unit) {
    val closeFocus = rememberInitialFocus(key = "tv-add-playlist-tip")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.Surface,
        title = { Text("Πού βρίσκω τα στοιχεία;", color = IptvColors.TextPrimary, fontWeight = FontWeight.Black) },
        text = {
            Text(
                "Χρησιμοποίησε τα στοιχεία που σου έχει δώσει ο νόμιμος πάροχος περιεχομένου. " +
                    "Για M3U χρειάζεσαι URL, για Xtream server, username και password, ενώ για " +
                    "MAC/Stalker χρειάζεσαι Portal URL και MAC address.",
                color = IptvColors.TextSecondary,
                lineHeight = 19.sp,
            )
        },
        confirmButton = {
            TvDialogTextButton(
                label = "Κατάλαβα",
                color = IptvColors.Primary,
                modifier = Modifier.focusRequester(closeFocus),
                onClick = onDismiss,
            )
        },
    )
}

private enum class TvPlaylistInput(val title: String) {
    PLAYLIST_URL("URL λίστας"),
    SERVER("Server URL"),
    USERNAME("Username"),
    PASSWORD("Password"),
    PORTAL("Portal URL"),
    MAC("MAC address"),
    USER_AGENT("User-Agent"),
    FILE("Αρχείο M3U"),
    NAME("Όνομα λίστας"),
    EPG("EPG URL"),
}

private fun firstInvalidInput(draft: MobilePlaylistDraft, validation: String): TvPlaylistInput = when (draft.method) {
    MobilePlaylistMethod.URL -> TvPlaylistInput.PLAYLIST_URL
    MobilePlaylistMethod.XTREAM -> when {
        "Server URL" in validation -> TvPlaylistInput.SERVER
        draft.username.isBlank() -> TvPlaylistInput.USERNAME
        draft.password.isBlank() -> TvPlaylistInput.PASSWORD
        else -> TvPlaylistInput.SERVER
    }
    MobilePlaylistMethod.MAC -> if ("Portal URL" in validation) TvPlaylistInput.PORTAL else TvPlaylistInput.MAC
    MobilePlaylistMethod.FILE -> TvPlaylistInput.FILE
}

private fun inputValue(
    input: TvPlaylistInput,
    playlistUrl: String,
    server: String,
    username: String,
    password: String,
    portal: String,
    macAddress: String,
    userAgent: String,
    name: String,
    epgUrl: String,
): String = when (input) {
    TvPlaylistInput.PLAYLIST_URL -> playlistUrl
    TvPlaylistInput.SERVER -> server
    TvPlaylistInput.USERNAME -> username
    TvPlaylistInput.PASSWORD -> password
    TvPlaylistInput.PORTAL -> portal
    TvPlaylistInput.MAC -> macAddress
    TvPlaylistInput.USER_AGENT -> userAgent
    TvPlaylistInput.NAME -> name
    TvPlaylistInput.EPG -> epgUrl
    TvPlaylistInput.FILE -> ""
}
