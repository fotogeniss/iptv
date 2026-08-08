package com.prelude.iptv.ui.tv.sources

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.TextEntryDialog
import com.prelude.iptv.ui.TvDialogTextButton
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.requestFocusWithRetry
import com.prelude.iptv.ui.sources.PlaylistSourceDraft
import com.prelude.iptv.ui.sources.PlaylistSourceDraftPolicy
import com.prelude.iptv.ui.sources.PlaylistSourceMethod
import com.prelude.iptv.ui.sources.PlaylistSourceSubmissionStage
import com.prelude.iptv.ui.sources.PlaylistSourceValidation
import com.prelude.iptv.ui.sources.importM3uFile
import com.prelude.iptv.ui.sources.submitPlaylistSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun TvAddPlaylistScreen(
    initialTab: Int,
    onDismiss: () -> Unit,
    onAdd: (Playlist) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stepName by remember { mutableStateOf(TvSourceOnboardingStep.CHOOSE.name) }
    val step = runCatching { TvSourceOnboardingStep.valueOf(stepName) }.getOrDefault(TvSourceOnboardingStep.CHOOSE)
    var draft by remember {
        mutableStateOf(PlaylistSourceDraft(method = PlaylistSourceDraftPolicy.methodForInitialTab(initialTab)))
    }
    var validation by remember { mutableStateOf<PlaylistSourceValidation?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var importingFile by remember { mutableStateOf(false) }
    var editingInputName by remember { mutableStateOf<String?>(null) }
    var showQuickTip by remember { mutableStateOf(false) }
    var submissionStage by remember { mutableStateOf(PlaylistSourceSubmissionStage.VALIDATING) }
    var verifiedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var successMessage by remember { mutableStateOf("") }
    var submissionJob by remember { mutableStateOf<Job?>(null) }

    val methodFocus = remember { PlaylistSourceMethod.entries.associateWith { FocusRequester() } }
    val inputFocus = remember { TvPlaylistInput.entries.associateWith { FocusRequester() } }
    val changeFocus = remember { FocusRequester() }
    val advancedFocus = remember { FocusRequester() }
    val exitFocus = remember { FocusRequester() }
    val submitFocus = remember { FocusRequester() }
    val completeFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val helpFocus = remember { FocusRequester() }

    fun showStep(target: TvSourceOnboardingStep) {
        stepName = target.name
    }

    fun updateDraft(value: PlaylistSourceDraft) {
        draft = value
        validation = null
        generalError = null
        verifiedPlaylist = null
    }

    fun restoreInputFocus(input: TvPlaylistInput) {
        scope.launch { inputFocus.getValue(input).requestFocusWithRetry() }
    }

    fun goBack() {
        when (step) {
            TvSourceOnboardingStep.CHOOSE -> onDismiss()
            TvSourceOnboardingStep.DETAILS -> showStep(TvSourceOnboardingStep.CHOOSE)
            TvSourceOnboardingStep.CHECKING -> {
                submissionJob?.cancel()
                submissionJob = null
                showStep(TvSourceOnboardingStep.DETAILS)
            }
            TvSourceOnboardingStep.SUCCESS -> showStep(TvSourceOnboardingStep.DETAILS)
        }
    }
    BackHandler(onBack = ::goBack)

    LaunchedEffect(step, draft.method, validation?.field) {
        when (step) {
            TvSourceOnboardingStep.CHOOSE -> methodFocus.getValue(draft.method).requestFocusWithRetry()
            TvSourceOnboardingStep.DETAILS -> {
                val target = invalidInput(validation) ?: firstInputFor(draft.method)
                inputFocus.getValue(target).requestFocusWithRetry()
            }
            TvSourceOnboardingStep.SUCCESS -> completeFocus.requestFocusWithRetry()
            TvSourceOnboardingStep.CHECKING -> Unit
        }
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
                onSuccess = { imported -> updateDraft(draft.copy(filePath = imported.path, fileLabel = imported.label)) },
                onFailure = { error -> generalError = error.message ?: "Δεν άνοιξε το αρχείο M3U." },
            )
            inputFocus.getValue(TvPlaylistInput.FILE).requestFocusWithRetry()
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF030304), Color(0xFF090304), Color(0xFF030304))),
        ).systemBarsPadding().padding(horizontal = 42.dp, vertical = 18.dp),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TvSourceOnboardingTopBar(
                step = step,
                backFocus = backFocus,
                helpFocus = helpFocus,
                onBack = ::goBack,
                onHelp = { showQuickTip = true },
            )
            TvSourceOnboardingProgress(step)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (step) {
                    TvSourceOnboardingStep.CHOOSE -> TvSourceChooseStep(
                        selectedMethod = draft.method,
                        methodFocus = methodFocus,
                        onMethod = { method ->
                            updateDraft(draft.copy(method = method))
                            showStep(TvSourceOnboardingStep.DETAILS)
                        },
                    )

                    TvSourceOnboardingStep.DETAILS -> TvSourceDetailsStep(
                        draft = draft,
                        validation = validation,
                        generalError = generalError,
                        advancedExpanded = advancedExpanded,
                        importingFile = importingFile,
                        inputFocus = inputFocus,
                        changeFocus = changeFocus,
                        advancedFocus = advancedFocus,
                        exitFocus = exitFocus,
                        submitFocus = submitFocus,
                        onChangeMethod = { showStep(TvSourceOnboardingStep.CHOOSE) },
                        onEditInput = { editingInputName = it.name },
                        onAdvancedChange = { advancedExpanded = it },
                        onPickFile = {
                            filePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/plain", "*/*"))
                        },
                        onExit = onDismiss,
                        onSubmit = {
                            val immediateValidation = PlaylistSourceDraftPolicy.validation(draft)
                            if (immediateValidation != null) {
                                validation = immediateValidation
                                generalError = null
                                restoreInputFocus(invalidInput(immediateValidation) ?: firstInputFor(draft.method))
                            } else {
                                val snapshot = PlaylistSourceDraftPolicy.normalized(draft)
                                draft = snapshot
                                submissionStage = PlaylistSourceSubmissionStage.VALIDATING
                                showStep(TvSourceOnboardingStep.CHECKING)
                                submissionJob = scope.launch {
                                    val result = submitPlaylistSource(snapshot, onStage = { submissionStage = it })
                                    submissionJob = null
                                    if (result.successful) {
                                        verifiedPlaylist = result.playlist
                                        successMessage = result.message
                                        showStep(TvSourceOnboardingStep.SUCCESS)
                                    } else {
                                        validation = result.validation
                                        generalError = result.message.takeIf { result.validation == null }
                                        showStep(TvSourceOnboardingStep.DETAILS)
                                    }
                                }
                            }
                        },
                    )

                    TvSourceOnboardingStep.CHECKING -> TvSourceCheckingStep(submissionStage)
                    TvSourceOnboardingStep.SUCCESS -> TvSourceSuccessStep(
                        providerMessage = successMessage,
                        completeFocus = completeFocus,
                        onComplete = { verifiedPlaylist?.let(onAdd) },
                    )
                }
            }
        }
    }

    val editingInput = editingInputName?.let { runCatching { TvPlaylistInput.valueOf(it) }.getOrNull() }
    if (editingInput != null) {
        TextEntryDialog(
            title = editingInput.title,
            initial = inputValue(editingInput, draft),
            isPassword = editingInput == TvPlaylistInput.PASSWORD,
            onDismiss = {
                editingInputName = null
                restoreInputFocus(editingInput)
            },
            onOk = { newValue ->
                updateDraft(
                    when (editingInput) {
                        TvPlaylistInput.PLAYLIST_URL -> draft.copy(playlistUrl = newValue)
                        TvPlaylistInput.SERVER -> draft.copy(server = newValue)
                        TvPlaylistInput.USERNAME -> draft.copy(username = newValue)
                        TvPlaylistInput.PASSWORD -> draft.copy(password = newValue)
                        TvPlaylistInput.PORTAL -> draft.copy(portal = newValue)
                        TvPlaylistInput.MAC -> draft.copy(macAddress = PlaylistSourceDraftPolicy.formatMac(newValue))
                        TvPlaylistInput.USER_AGENT -> draft.copy(userAgent = newValue)
                        TvPlaylistInput.NAME -> draft.copy(name = newValue)
                        TvPlaylistInput.EPG -> draft.copy(epgUrl = newValue)
                        TvPlaylistInput.FILE -> draft
                    },
                )
                editingInputName = null
                restoreInputFocus(editingInput)
            },
        )
    }

    if (showQuickTip) {
        TvSourceQuickTipDialog(
            onDismiss = {
                showQuickTip = false
                scope.launch { helpFocus.requestFocusWithRetry() }
            },
        )
    }
}

@Composable
private fun TvSourceQuickTipDialog(onDismiss: () -> Unit) {
    val closeFocus = rememberInitialFocus(key = "tv-source-onboarding-help")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.Surface,
        title = { Text("Πού βρίσκω τα στοιχεία;", color = IptvColors.TextPrimary, fontWeight = FontWeight.Black) },
        text = {
            Text(
                "Χρησιμοποίησε τα στοιχεία που σου έδωσε ο νόμιμος πάροχος περιεχομένου. " +
                    "Διάλεξε σύνδεσμο, server και κωδικούς, Portal και MAC ή ένα αρχείο M3U.",
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
