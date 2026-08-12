package com.prelude.iptv.ui.mobile.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.BuildConfig
import com.prelude.iptv.R
import com.prelude.iptv.localization.AppLanguageController
import com.prelude.iptv.localization.LocalizationRolloutPolicy
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.settings.SettingsSourceUi
import com.prelude.iptv.ui.home.HomeLayoutPolicy
import com.prelude.iptv.category.CategoryEditorState
import com.prelude.iptv.category.CategoryLayout
import com.prelude.iptv.ui.mobile.home.MobileEditHomeScreen
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.epg.EpgSourceOption
import com.prelude.iptv.ui.epg.EpgStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobilePremiumSettingsScreen(
    sources: List<SettingsSourceUi>,
    profileName: String,
    playerLabel: String,
    fontScale: Float,
    epgEnabled: Boolean,
    epgLoaded: Boolean,
    epgStatus: EpgStatus,
    epgSources: List<EpgSourceOption>,
    currentEpgUrl: String,
    currentSourceType: String,
    tmdbConfigured: Boolean,
    subtitlesConfigured: Boolean,
    version: String,
    onAddSource: (Int) -> Unit,
    onOpenSource: (Int) -> Unit,
    onEditSource: (Int) -> Unit,
    onDeleteSource: (Int) -> Unit,
    onRefreshCurrentSource: () -> Unit,
    onDialog: (String) -> Unit,
    onToggleEpg: () -> Unit,
    onSearchEpg: () -> Unit,
    onUseEpgSource: (String) -> Unit,
    onCloseEpgSearch: () -> Unit,
    categoryEditorState: CategoryEditorState,
    onEditCategories: () -> Unit,
    onCategoryLayoutChange: (String, CategoryLayout) -> Unit,
    onSaveCategories: () -> Unit,
    onClearHomeHistory: (String) -> Unit,
    onClearTmdbCache: () -> Unit,
    onShare: () -> Unit,
    onNavigationCollapsedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { PlaylistStore(context) }
    val listState = rememberLazyListState()
    val navigationCollapsed =
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40

    var addPicker by remember { mutableStateOf(false) }
    var sourcesSheet by remember { mutableStateOf(false) }
    var premiumSheet by remember { mutableStateOf(false) }
    var infoSheet by remember { mutableStateOf<MobileSettingsInfo?>(null) }
    var editingHome by remember { mutableStateOf(false) }
    var editingCategories by remember { mutableStateOf(false) }
    var accountFlow by remember { mutableStateOf(false) }
    var managerSourceIndex by remember { mutableStateOf<Int?>(null) }
    var playerSettingsOpen by remember { mutableStateOf(false) }
    var epgSettingsOpen by remember { mutableStateOf(false) }
    var legalPrivacyOpen by remember { mutableStateOf(false) }
    var diagnosticsOpen by remember { mutableStateOf(false) }
    var languagePickerOpen by remember { mutableStateOf(false) }
    var autoOpenPlaylist by remember { mutableStateOf(store.autoOpenPlaylist) }
    var playlistRefreshDays by remember { mutableStateOf(store.playlistRefreshDays) }
    var mobileBufferProfile by remember { mutableStateOf(store.bufferProfile) }
    var startWithSubtitles by remember { mutableStateOf(store.startWithSubtitles) }
    var subtitleSize by remember { mutableStateOf(store.subtitleSizePercent) }
    var subtitleLanguage by remember { mutableStateOf(store.preferredSubtitleLanguage) }
    var audioLanguage by remember { mutableStateOf(store.preferredAudioLanguage) }
    // Από τις Ρυθμίσεις ο επεξεργαστής ανοίγει στην Αρχική και αλλάζει προορισμό
    // με τα chips — εδώ δεν υπάρχει «οθόνη που κοιτάς» για να τον προεπιλέξει.
    var homeDestination by remember { mutableStateOf(HomeLayoutPolicy.DEST_HOME) }
    var homeEntries by remember {
        mutableStateOf(
            HomeLayoutPolicy.resolve(
                savedOrder = store.homeSectionOrder(HomeLayoutPolicy.DEST_HOME),
                hidden = store.homeHiddenSections(HomeLayoutPolicy.DEST_HOME),
                destination = HomeLayoutPolicy.DEST_HOME,
            )
        )
    }
    fun reloadHomeEntries() {
        homeEntries = HomeLayoutPolicy.resolve(
            savedOrder = store.homeSectionOrder(homeDestination),
            hidden = store.homeHiddenSections(homeDestination),
            destination = homeDestination,
        )
    }
    val languagePickerVisible = LocalizationRolloutPolicy.pickerVisible(
        ownerQaBuild = BuildConfig.PREMIUM_QA_OVERRIDE,
        translationParityComplete = BuildConfig.LOCALIZATION_PARITY_COMPLETE,
    )
    val selectedAppLanguage = AppLanguageController.selected()

    LaunchedEffect(navigationCollapsed, editingHome, editingCategories, accountFlow, managerSourceIndex, playerSettingsOpen, epgSettingsOpen, legalPrivacyOpen, diagnosticsOpen, languagePickerOpen) {
        onNavigationCollapsedChange(
            navigationCollapsed || editingHome || editingCategories || accountFlow ||
                managerSourceIndex != null || playerSettingsOpen || epgSettingsOpen || legalPrivacyOpen ||
                diagnosticsOpen || languagePickerOpen
        )
    }

    if (editingHome) {
        MobileEditHomeScreen(
            entries = homeEntries,
            categoryOf = store::homeRailCategory,
            categoriesFor = { sectionId ->
                val type = when (sectionId) {
                    HomeLayoutPolicy.LIVE -> "live"
                    HomeLayoutPolicy.MOVIES -> "vod"
                    HomeLayoutPolicy.SERIES -> "series"
                    else -> ""
                }
                categoryEditorState.section(type).entries.filter { it.visible }.map { it.option.title }
            },
            destination = homeDestination,
            onDestinationChange = { homeDestination = it; reloadHomeEntries() },
            onToggleVisible = { id ->
                store.setHomeHiddenSections(
                    homeDestination,
                    HomeLayoutPolicy.toggle(store.homeHiddenSections(homeDestination), id),
                )
                reloadHomeEntries()
            },
            onMove = { from, to ->
                store.setHomeSectionOrder(
                    homeDestination,
                    HomeLayoutPolicy.move(HomeLayoutPolicy.idsOf(homeEntries), from, to),
                )
                reloadHomeEntries()
            },
            selectedCategoriesOf = { id -> store.homeRailCategories(homeDestination, id) },
            onPickCategories = { id, groups ->
                store.setHomeRailCategories(homeDestination, id, groups)
                reloadHomeEntries()
            },
            onClear = onClearHomeHistory,
            onBack = { editingHome = false },
            modifier = modifier
        )
        return
    }

    if (editingCategories) {
        MobileEditCategoriesScreen(
            state = categoryEditorState,
            onLayoutChange = onCategoryLayoutChange,
            onSave = onSaveCategories,
            onBack = { editingCategories = false },
            modifier = modifier,
        )
        return
    }

    if (accountFlow) {
        MobileAccountSyncScreen(
            profileName = profileName,
            onManageProfiles = { accountFlow = false; onDialog("profiles") },
            onBack = { accountFlow = false },
            modifier = modifier
        )
        return
    }

    if (playerSettingsOpen) {
        MobilePlayerSettingsScreen(
            bufferProfile = mobileBufferProfile,
            startWithSubtitles = startWithSubtitles,
            subtitleSize = subtitleSize,
            subtitleLanguage = subtitleLanguage,
            audioLanguage = audioLanguage,
            onBufferProfile = { value -> mobileBufferProfile = value; store.bufferProfile = value },
            onStartWithSubtitles = { value -> startWithSubtitles = value; store.startWithSubtitles = value },
            onSubtitleSize = { value -> subtitleSize = value; store.subtitleSizePercent = value },
            onSubtitleLanguage = { value -> subtitleLanguage = value; store.preferredSubtitleLanguage = value },
            onAudioLanguage = { value -> audioLanguage = value; store.preferredAudioLanguage = value },
            onOpenSubtitlesAccount = { playerSettingsOpen = false; onDialog("subs") },
            onBack = { playerSettingsOpen = false },
            modifier = modifier
        )
        return
    }

    if (epgSettingsOpen) {
        MobileEpgSettingsScreen(
            enabled = epgEnabled,
            loaded = epgLoaded,
            sourceType = currentSourceType,
            currentUrl = currentEpgUrl,
            status = epgStatus,
            discoveredSources = epgSources,
            onEnabledChange = { enabled -> if (enabled != epgEnabled) onToggleEpg() },
            onDiscover = onSearchEpg,
            onUseUrl = onUseEpgSource,
            onCloseDiscovery = onCloseEpgSearch,
            onBack = { epgSettingsOpen = false },
            modifier = modifier
        )
        return
    }

    if (legalPrivacyOpen) {
        MobileLegalPrivacyScreen(
            version = version,
            onBack = { legalPrivacyOpen = false },
            modifier = modifier,
        )
        return
    }

    if (diagnosticsOpen) {
        MobileDiagnosticsScreen(
            onBack = { diagnosticsOpen = false },
            modifier = modifier,
        )
        return
    }

    managerSourceIndex?.let { selectedIndex ->
        val source = sources.firstOrNull { it.index == selectedIndex }
        if (source != null) {
            MobilePlaylistManagerScreen(
                source = source,
                autoOpen = autoOpenPlaylist,
                refreshDays = playlistRefreshDays,
                onAutoOpenChange = { enabled ->
                    autoOpenPlaylist = enabled
                    store.autoOpenPlaylist = enabled
                },
                onRefreshDaysChange = { days ->
                    playlistRefreshDays = days
                    store.playlistRefreshDays = days
                },
                onOpen = { managerSourceIndex = null; onOpenSource(source.index) },
                onEdit = { managerSourceIndex = null; onEditSource(source.index) },
                onDelete = { managerSourceIndex = null; onDeleteSource(source.index) },
                onRefresh = onRefreshCurrentSource,
                onManageAll = { managerSourceIndex = null; sourcesSheet = true },
                onBack = { managerSourceIndex = null },
                modifier = modifier
            )
            return
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().background(IptvColors.Background),
        contentPadding = PaddingValues(bottom = premiumMobileNavigationContentPadding())
    ) {
        item(key = "settings-top") {
            MobileSettingsTopBar {
                Toast.makeText(context, context.getString(R.string.settings_no_new_notifications), Toast.LENGTH_SHORT).show()
            }
        }
        item(key = "settings-account") {
            MobileSettingsAccountHero(profileName) { accountFlow = true }
        }
        item(key = "settings-premium") {
            MobileSettingsPremiumCard { premiumSheet = true }
        }

        item(key = "settings-general") {
            MobileSettingsGroupTitle(stringResource(R.string.settings_group_general))
            MobileSettingsRows {
                MobileOverviewRow(
                    title = stringResource(R.string.settings_my_sources),
                    subtitle = stringResource(R.string.settings_my_sources_subtitle),
                    icon = Icons.Default.Storage,
                    value = sourceSummary(sources),
                    primary = true,
                    onClick = {
                        val active = sources.firstOrNull { it.current } ?: sources.firstOrNull()
                        if (active != null) managerSourceIndex = active.index else addPicker = true
                    }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_player),
                    subtitle = stringResource(R.string.settings_player_subtitle),
                    icon = Icons.Default.PlayCircle,
                    value = playerLabel,
                    onClick = { playerSettingsOpen = true }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.epg_settings_programme_guide),
                    subtitle = stringResource(R.string.settings_epg_subtitle),
                    icon = Icons.Default.CalendarMonth,
                    value = stringResource(if (epgEnabled) R.string.settings_enabled else R.string.settings_disabled),
                    onClick = { epgSettingsOpen = true }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_subtitles),
                    subtitle = stringResource(R.string.settings_subtitles_subtitle),
                    icon = Icons.Default.Subtitles,
                    value = stringResource(if (subtitlesConfigured) R.string.settings_ready else R.string.settings_configure),
                    onClick = { onDialog("subs") }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_content_info),
                    subtitle = stringResource(R.string.settings_content_info_subtitle),
                    icon = Icons.Default.Movie,
                    value = stringResource(if (tmdbConfigured) R.string.settings_ready else R.string.settings_configure),
                    onClick = { onDialog("tmdb") }
                )
            }
        }

        item(key = "settings-customize") {
            MobileSettingsGroupTitle(stringResource(R.string.settings_group_personalization))
            MobileSettingsRows {
                if (languagePickerVisible) {
                    MobileOverviewRow(
                        title = stringResource(R.string.settings_app_language),
                        subtitle = stringResource(R.string.settings_app_language_subtitle),
                        icon = Icons.Default.Language,
                        value = stringResource(selectedAppLanguage.labelRes()),
                        onClick = { languagePickerOpen = true },
                    )
                }
                MobileOverviewRow(
                    title = stringResource(R.string.settings_edit_home),
                    subtitle = stringResource(R.string.settings_edit_home_subtitle),
                    icon = Icons.Default.Tune,
                    onClick = { onEditCategories(); editingHome = true }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_edit_categories),
                    subtitle = stringResource(R.string.settings_edit_categories_subtitle),
                    icon = Icons.Default.Category,
                    onClick = { onEditCategories(); editingCategories = true }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_appearance_title),
                    subtitle = stringResource(R.string.settings_appearance_subtitle),
                    icon = Icons.Default.FormatSize,
                    value = "${(fontScale * 100).toInt()}%",
                    onClick = { onDialog("font") }
                )
            }
        }

        item(key = "settings-security") {
            MobileSettingsGroupTitle(stringResource(R.string.settings_group_account_security))
            MobileSettingsRows {
                MobileOverviewRow(
                    title = stringResource(R.string.settings_profile),
                    subtitle = stringResource(R.string.settings_profile_subtitle),
                    icon = Icons.Default.AccountCircle,
                    value = profileName,
                    onClick = { onDialog("profiles") }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_parental_control),
                    subtitle = stringResource(R.string.settings_parental_control_subtitle),
                    icon = Icons.Default.Lock,
                    onClick = { onDialog("pin") }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_backup_restore),
                    subtitle = stringResource(R.string.settings_backup_restore_subtitle),
                    icon = Icons.Default.Backup,
                    onClick = { onDialog("backup") }
                )
            }
        }

        item(key = "settings-support") {
            MobileSettingsGroupTitle(stringResource(R.string.settings_group_support))
            MobileSettingsRows {
                MobileOverviewRow(
                    title = stringResource(R.string.settings_help_center),
                    subtitle = stringResource(R.string.settings_help_center_subtitle),
                    icon = Icons.Default.Info,
                    onClick = { infoSheet = MobileSettingsInfo.Help }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_rate_app),
                    subtitle = stringResource(R.string.settings_rate_app_subtitle),
                    icon = Icons.Default.Star,
                    onClick = { openStoreReview(context) }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_diagnostics),
                    subtitle = stringResource(R.string.settings_diagnostics_subtitle),
                    icon = Icons.Default.BugReport,
                    onClick = { diagnosticsOpen = true }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_legal_privacy),
                    subtitle = stringResource(R.string.settings_legal_privacy_subtitle),
                    icon = Icons.Default.Description,
                    onClick = { legalPrivacyOpen = true }
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_share_app),
                    subtitle = stringResource(R.string.settings_share_app_subtitle),
                    icon = Icons.Default.Share,
                    onClick = onShare
                )
                MobileOverviewRow(
                    title = stringResource(R.string.settings_clear_metadata_cache),
                    subtitle = stringResource(R.string.settings_clear_metadata_cache_subtitle),
                    icon = Icons.Default.CleaningServices,
                    onClick = onClearTmdbCache
                )
            }
        }

        item(key = "settings-health") {
            MobileSettingsGroupTitle(stringResource(R.string.settings_group_app_status))
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MobileSettingsStatusCard(
                        stringResource(R.string.settings_source_status),
                        stringResource(if (sources.any { it.current }) R.string.settings_connected_feminine else R.string.settings_no_source),
                        sources.any { it.current },
                        Modifier.weight(1f)
                    )
                    MobileSettingsStatusCard("EPG", stringResource(if (epgEnabled) R.string.settings_enabled else R.string.settings_disabled), epgEnabled, Modifier.weight(1f))
                }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MobileSettingsStatusCard("Player", playerLabel, true, Modifier.weight(1f))
                    MobileSettingsStatusCard(stringResource(R.string.settings_version), version.ifBlank { "—" }, false, Modifier.weight(1f))
                }
            }
        }

        item(key = "settings-footer") {
            Column(Modifier.fillMaxWidth().padding(top = 31.dp, bottom = 12.dp)) {
                Text(
                    stringResource(R.string.settings_using_version, version.ifBlank { "—" }),
                    color = IptvColors.TextTertiary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                TextButton(
                    onClick = { premiumSheet = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.settings_restore_purchases), color = IptvColors.TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }

    if (sourcesSheet) {
        MobileSourcesSheet(
            sources = sources,
            onDismiss = { sourcesSheet = false },
            onOpen = { index -> sourcesSheet = false; onOpenSource(index) },
            onEdit = { index -> sourcesSheet = false; onEditSource(index) },
            onDelete = { index -> sourcesSheet = false; onDeleteSource(index) },
            onRefresh = onRefreshCurrentSource,
            onAdd = { sourcesSheet = false; addPicker = true }
        )
    }

    if (addPicker) {
        MobileAddSourceSheet(
            onDismiss = { addPicker = false },
            onSelectType = { type -> addPicker = false; onAddSource(type) }
        )
    }

    if (premiumSheet) {
        MobilePremiumSheet(onDismiss = { premiumSheet = false })
    }

    if (languagePickerOpen && languagePickerVisible) {
        MobileAppLanguageSheet(
            selected = selectedAppLanguage,
            onSelect = { language ->
                languagePickerOpen = false
                AppLanguageController.select(language)
            },
            onDismiss = { languagePickerOpen = false },
        )
    }

    infoSheet?.let { info ->
        MobileInfoSheet(info = info, onDismiss = { infoSheet = null })
    }
}

@Composable
private fun sourceSummary(sources: List<SettingsSourceUi>): String = if (sources.isEmpty()) {
    stringResource(R.string.settings_source_none)
} else {
    pluralStringResource(R.plurals.settings_source_summary, sources.size, sources.size)
}

private fun openStoreReview(context: Context) {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(marketIntent) }
        .onFailure { runCatching { context.startActivity(webIntent) } }
}
