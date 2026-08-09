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
import com.prelude.iptv.R
import com.prelude.iptv.data.*
import com.prelude.iptv.ui.*
import com.prelude.iptv.ui.components.library.*
import com.prelude.iptv.ui.design.*
import com.prelude.iptv.ui.localization.localizedBackupFailure
import com.prelude.iptv.ui.localization.localizedBackupRestoreSuccess
import com.prelude.iptv.ui.localization.localizedProfileName
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTab(
    vm: MainViewModel,
    onAddSource: (Int) -> Unit,
    onOpenSource: (Int) -> Unit,
    onEditSource: (Int) -> Unit,
    onNavigationCollapsedChange: (Boolean) -> Unit = {}
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val activeProfileName = localizedProfileName(vm.activeProfileDisplayName())
    val st by vm.settingsState.collectAsStateWithLifecycle()
    val catalog by vm.catalogState.collectAsStateWithLifecycle()
    val epg by vm.epgState.collectAsStateWithLifecycle()
    val categoryEditor by vm.categoryEditorState.collectAsStateWithLifecycle()
    val dialogState = remember { mutableStateOf("") }
    var dialog by dialogState

    val store = remember { com.prelude.iptv.data.PlaylistStore(ctx) }
    val playerModeState = remember { mutableStateOf(store.playerMode) }
    var playerMode by playerModeState
    val autoFrameRateModeState = remember { mutableStateOf(store.autoFrameRateMode) }
    var autoFrameRateMode by autoFrameRateModeState
    val bufferProfileState = remember { mutableStateOf(store.bufferProfile) }
    var bufferProfile by bufferProfileState
    var tvHomeEnabled by remember { mutableStateOf(store.tvHomeEnabled) }
    var tvHomeMyListEnabled by remember { mutableStateOf(store.tvHomeMyListEnabled) }
    var epgOn by remember { mutableStateOf(vm.epgEnabled()) }
    val version = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" }
        catch (e: Exception) { "" }
    }
    var pendingBackupPassword by remember { mutableStateOf("") }

    // ---- Αντίγραφο ασφαλείας: SAF (ο χρήστης διαλέγει πού) — καμία άδεια,
    // δουλεύει σε Drive/USB/τοπικά, και σε Android TV αν υπάρχει file manager.
    val exportBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) runCatching {
            val output = ctx.contentResolver.openOutputStream(uri)
                ?: throw BackupException(BackupFailure.DestinationUnavailable)
            output.use {
                it.write(com.prelude.iptv.data.Backup.export(ctx, pendingBackupPassword).toByteArray())
            }
        }.onSuccess { toast(ctx, ctx.getString(R.string.account_backup_exported)) }
            .onFailure { toast(ctx, ctx.localizedBackupFailure(it)) }
        pendingBackupPassword = ""
    }
    val importBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) runCatching {
            val input = ctx.contentResolver.openInputStream(uri)
                ?: throw BackupException(BackupFailure.SourceUnavailable)
            val txt = input.bufferedReader().use { it.readText() }
            com.prelude.iptv.data.Backup.import(ctx, txt, pendingBackupPassword)
        }.onSuccess { n ->
            // Το ViewModel διαβάζει τα prefs στο init: χωρίς restart θα έδειχνε
            // ΤΑ ΠΑΛΙΑ δεδομένα ενώ ο δίσκος έχει τα νέα (σιωπηλή ασυνέπεια).
            toast(ctx, ctx.localizedBackupRestoreSuccess(n))
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                if (launch != null) ctx.startActivity(launch)
            }, 700)
        }.onFailure { toast(ctx, ctx.localizedBackupFailure(it)) }
        pendingBackupPassword = ""
    }

    BackHandler(enabled = catalog.askRefreshMode || catalog.pickCategories || catalog.askLoadMode) {
        when {
            catalog.askRefreshMode -> vm.cancelRefreshChoice()
            catalog.pickCategories -> vm.cancelCategoryPicker()
            catalog.askLoadMode -> vm.cancelLoadMode()
        }
    }

    AdaptiveSettingsScreen(
        playlists = st.playlists,
        currentIndex = st.currentIndex,
        currentChannelCount = st.currentChannelCount,
        sourceProgress = st.sourceProgress,
        profileName = activeProfileName,
        playerMode = playerMode,
        autoFrameRateMode = autoFrameRateMode,
        bufferProfile = bufferProfile,
        tvHomeEnabled = tvHomeEnabled,
        tvHomeMyListEnabled = tvHomeMyListEnabled,
        fontScale = st.fontScale,
        epgEnabled = epgOn,
        epgLoaded = epg.loaded,
        epgStatus = epg.status,
        epgSources = epg.sources,
        tmdbConfigured = vm.loadTmdbKey().isNotBlank(),
        subtitlesConfigured = vm.loadSubSettings().first.isNotBlank(),
        version = version,
        onAddSource = onAddSource,
        onOpenSource = onOpenSource,
        onEditSource = onEditSource,
        onDeleteSource = vm::deletePlaylist,
        onRefreshCurrentSource = {
            if (vm.canRefreshCurrentSection()) vm.requestRefresh()
            else toast(ctx, "Δεν υπάρχει ενεργή πηγή για ανανέωση")
        },
        onDialog = { dialog = it },
        onToggleEpg = { epgOn = !epgOn; vm.setEpgEnabled(epgOn) },
        onSearchEpg = vm::searchEpg,
        onUseEpgSource = vm::useEpgSource,
        onCloseEpgSearch = vm::closeEpgSearch,
        categoryEditorState = categoryEditor,
        onEditCategories = vm::openCategoryEditor,
        onCategoryLayoutChange = vm::updateCategoryEditorLayout,
        onSaveCategories = vm::saveCategoryEditor,
        onClearHomeHistory = vm::clearHomeHistory,
        onToggleTvHome = {
            tvHomeEnabled = !tvHomeEnabled
            store.tvHomeEnabled = tvHomeEnabled
            com.prelude.iptv.tvhome.TvHomeSyncScheduler.schedule(ctx)
            toast(ctx, if (tvHomeEnabled) "Η αρχική TV θα δείχνει το Συνέχισε να βλέπεις" else "Η σειρά αφαιρέθηκε από την αρχική TV")
        },
        onToggleTvHomeMyList = {
            tvHomeMyListEnabled = !tvHomeMyListEnabled
            store.tvHomeMyListEnabled = tvHomeMyListEnabled
            if (tvHomeMyListEnabled) {
                com.prelude.iptv.tvhome.TvHomeChannelManager.enableMyList(ctx)
                toast(ctx, "Ενεργοποιήθηκε το κανάλι Η λίστα μου")
            } else {
                com.prelude.iptv.tvhome.TvHomeSyncScheduler.schedule(ctx)
                toast(ctx, "Το κανάλι Η λίστα μου αφαιρέθηκε")
            }
        },
        onClearTmdbCache = {
            com.prelude.iptv.data.TmdbClient.clearCache()
            toast(ctx, "Το cache καθαρίστηκε")
        },
        onShare = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "IPTV Player — ${ctx.packageName}")
            }
            ctx.startActivity(Intent.createChooser(intent, "Κοινοποίηση"))
        },
        onNavigationCollapsedChange = onNavigationCollapsedChange
    )

    SettingsAccountDialogs(
        dialogState = dialogState,
        vm = vm,
        context = ctx,
        onExportBackup = { password ->
            pendingBackupPassword = password
            exportBackup.launch(com.prelude.iptv.data.Backup.suggestedFileName())
        },
        onImportBackup = { password ->
            pendingBackupPassword = password
            importBackup.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    )
    SettingsPlaybackDialogs(
        dialogState = dialogState,
        playerModeState = playerModeState,
        autoFrameRateModeState = autoFrameRateModeState,
        bufferProfileState = bufferProfileState,
        vm = vm,
        state = st,
        store = store,
        context = ctx
    )

    if (catalog.askRefreshMode) RefreshModeDialog(
        contentType = catalog.contentType,
        onExisting = { vm.refreshExistingSelection() },
        onChooseGroups = { vm.refreshAndChooseGroups() },
        onCancel = { vm.cancelRefreshChoice() }
    )
    if (catalog.askLoadMode) LoadModeDialog(
        count = catalog.categories.size,
        onAll = { vm.loadEverything() },
        onChoose = { vm.chooseCategories() },
        onCancel = { vm.cancelLoadMode() }
    )
    if (catalog.pickCategories) CategoryPicker(
        categories = catalog.categories,
        initialSelectedIds = catalog.categorySelectionIds,
        onCancel = { vm.cancelCategoryPicker() },
        onLoad = { vm.loadSelectedCategories(it) }
    )
}
