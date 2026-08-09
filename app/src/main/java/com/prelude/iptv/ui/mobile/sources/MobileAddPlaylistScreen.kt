package com.prelude.iptv.ui.mobile.sources

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.R
import com.prelude.iptv.ui.localization.messageRes
import com.prelude.iptv.ui.sources.M3uImportException
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
fun MobileAddPlaylistScreen(
    initialTab: Int,
    onDismiss: () -> Unit,
    onAdd: (Playlist) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stepName by remember { mutableStateOf(MobileSourceOnboardingStep.CHOOSE.name) }
    val step = runCatching { MobileSourceOnboardingStep.valueOf(stepName) }
        .getOrDefault(MobileSourceOnboardingStep.CHOOSE)
    var draft by remember {
        mutableStateOf(
            PlaylistSourceDraft(method = PlaylistSourceDraftPolicy.methodForInitialTab(initialTab)),
        )
    }
    var smartInput by remember { mutableStateOf("") }
    var detectionError by remember { mutableStateOf<String?>(null) }
    var validation by remember { mutableStateOf<PlaylistSourceValidation?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var importingFile by remember { mutableStateOf(false) }
    var showQuickTip by remember { mutableStateOf(false) }
    var submissionStage by remember { mutableStateOf(PlaylistSourceSubmissionStage.VALIDATING) }
    var verifiedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var submissionJob by remember { mutableStateOf<Job?>(null) }
    val defaultPlaylistName = stringResource(R.string.sources_default_playlist_name)
    val defaultLocalName = stringResource(R.string.sources_default_local_name)

    fun showStep(target: MobileSourceOnboardingStep) {
        stepName = target.name
    }

    fun updateDraft(value: PlaylistSourceDraft) {
        draft = value
        validation = null
        generalError = null
        verifiedPlaylist = null
    }

    fun goBack() {
        when (step) {
            MobileSourceOnboardingStep.CHOOSE -> onDismiss()
            MobileSourceOnboardingStep.DETAILS -> showStep(MobileSourceOnboardingStep.CHOOSE)
            MobileSourceOnboardingStep.CHECKING -> {
                submissionJob?.cancel()
                submissionJob = null
                showStep(MobileSourceOnboardingStep.DETAILS)
            }
            MobileSourceOnboardingStep.SUCCESS -> showStep(MobileSourceOnboardingStep.DETAILS)
        }
    }
    BackHandler(onBack = ::goBack)

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importingFile = true
        scope.launch {
            val result = importM3uFile(context, uri)
            importingFile = false
            result.fold(
                onSuccess = { imported ->
                    updateDraft(draft.copy(filePath = imported.path, fileLabel = imported.label))
                },
                onFailure = { error ->
                    val messageRes = (error as? M3uImportException)?.reason?.messageRes()
                        ?: R.string.sources_file_open_failed
                    generalError = context.getString(messageRes)
                },
            )
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF030304), Color(0xFF090304), Color(0xFF030304)),
            ),
        ),
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        ) {
            MobileSourceOnboardingTopBar(
                step = step,
                onBack = ::goBack,
                onHelp = { showQuickTip = true },
            )
            MobileSourceOnboardingProgress(step)

            when (step) {
                MobileSourceOnboardingStep.CHOOSE -> MobileSourceChooseStep(
                    smartInput = smartInput,
                    detectionError = detectionError,
                    onSmartInputChange = {
                        smartInput = it
                        detectionError = null
                    },
                    onDetect = {
                        val detection = PlaylistSourceDraftPolicy.detect(smartInput)
                        if (detection == null) {
                            detectionError = context.getString(R.string.sources_unrecognized_credentials)
                        } else {
                            updateDraft(detection.draft)
                            detectionError = null
                            showStep(MobileSourceOnboardingStep.DETAILS)
                        }
                    },
                    onMethod = { method ->
                        updateDraft(draft.copy(method = method))
                        showStep(MobileSourceOnboardingStep.DETAILS)
                    },
                )

                MobileSourceOnboardingStep.DETAILS -> MobileSourceDetailsStep(
                    draft = draft,
                    validation = validation,
                    generalError = generalError,
                    advancedExpanded = advancedExpanded,
                    passwordVisible = passwordVisible,
                    importingFile = importingFile,
                    onDraftChange = ::updateDraft,
                    onAdvancedChange = { advancedExpanded = it },
                    onTogglePassword = { passwordVisible = !passwordVisible },
                    onChangeMethod = { showStep(MobileSourceOnboardingStep.CHOOSE) },
                    onPickFile = {
                        filePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/plain", "*/*"))
                    },
                    onSubmit = {
                        val immediateValidation = PlaylistSourceDraftPolicy.validation(draft)
                        if (immediateValidation != null) {
                            validation = immediateValidation
                            generalError = null
                        } else {
                            val snapshot = PlaylistSourceDraftPolicy.normalized(draft)
                            draft = snapshot
                            submissionStage = PlaylistSourceSubmissionStage.VALIDATING
                            showStep(MobileSourceOnboardingStep.CHECKING)
                            submissionJob = scope.launch {
                                val result = submitPlaylistSource(
                                    snapshot,
                                    if (snapshot.method == PlaylistSourceMethod.FILE) defaultLocalName else defaultPlaylistName,
                                    onStage = { submissionStage = it },
                                )
                                submissionJob = null
                                if (result.successful) {
                                    verifiedPlaylist = result.playlist
                                    showStep(MobileSourceOnboardingStep.SUCCESS)
                                } else {
                                    validation = result.validation
                                    generalError = result.failure?.let { context.getString(it.messageRes()) }
                                    showStep(MobileSourceOnboardingStep.DETAILS)
                                }
                            }
                        }
                    },
                    onExit = onDismiss,
                )

                MobileSourceOnboardingStep.CHECKING -> MobileSourceCheckingStep(submissionStage)
                MobileSourceOnboardingStep.SUCCESS -> MobileSourceSuccessStep(
                    onComplete = { verifiedPlaylist?.let(onAdd) },
                )
            }
        }
    }

    if (showQuickTip) MobilePlaylistQuickTip(onDismiss = { showQuickTip = false })
}
