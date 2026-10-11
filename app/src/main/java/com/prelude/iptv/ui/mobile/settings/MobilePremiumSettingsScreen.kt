package com.prelude.iptv.ui.mobile.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import com.prelude.iptv.ui.icons.filled.AccountCircle
import com.prelude.iptv.ui.icons.filled.Backup
import com.prelude.iptv.ui.icons.filled.BugReport
import com.prelude.iptv.ui.icons.filled.CalendarMonth
import com.prelude.iptv.ui.icons.filled.Category
import com.prelude.iptv.ui.icons.filled.CleaningServices
import com.prelude.iptv.ui.icons.filled.Description
import com.prelude.iptv.ui.icons.filled.FormatSize
import com.prelude.iptv.ui.icons.filled.Info
import com.prelude.iptv.ui.icons.filled.Language
import com.prelude.iptv.ui.icons.filled.Lock
import com.prelude.iptv.ui.icons.filled.Movie
import com.prelude.iptv.ui.icons.filled.PlayCircle
import com.prelude.iptv.ui.icons.filled.Share
import com.prelude.iptv.ui.icons.filled.Star
import com.prelude.iptv.ui.icons.filled.Storage
import com.prelude.iptv.ui.icons.filled.Subtitles
import com.prelude.iptv.ui.icons.filled.Tune
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.BuildConfig
import com.prelude.iptv.R
import com.prelude.iptv.localization.AppLanguageController
import com.prelude.iptv.localization.LocalizationRolloutPolicy
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.design.HorizonSearchField
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
    var query by rememberSaveable { mutableStateOf("") }
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

    val activeSource = sources.firstOrNull { it.current }
    val groupPlayback = stringResource(R.string.settings_group_playback)
    val groupContent = stringResource(R.string.settings_group_content)
    val groupAccount = stringResource(R.string.settings_group_account_security)
    val groupApp = stringResource(R.string.settings_group_app)
    val ready = stringResource(R.string.settings_ready)
    val configure = stringResource(R.string.settings_configure)
    val enabledLabel = stringResource(R.string.settings_enabled)
    val disabledLabel = stringResource(R.string.settings_disabled)
    val sourcesValue = sourceSummary(sources)
    val languageValue = stringResource(selectedAppLanguage.labelRes())

    // Όλες οι ρυθμίσεις ως μία λίστα: η ίδια πηγή τροφοδοτεί και τις ομάδες
    // και την αναζήτηση, ώστε να μην ξεχνιέται κάποια γραμμή στο ένα από τα δύο.
    val entries = buildList {
        add(SettingsEntry(groupPlayback, stringResource(R.string.settings_player), Icons.Default.PlayCircle,
            stringResource(R.string.settings_player_subtitle), value = playerLabel) { playerSettingsOpen = true })
        add(SettingsEntry(groupPlayback, stringResource(R.string.settings_subtitles), Icons.Default.Subtitles,
            stringResource(R.string.settings_subtitles_subtitle),
            value = if (subtitlesConfigured) ready else configure) { onDialog("subs") })
        add(SettingsEntry(groupPlayback, stringResource(R.string.settings_appearance_title), Icons.Default.FormatSize,
            stringResource(R.string.settings_appearance_subtitle), value = "${(fontScale * 100).toInt()}%") { onDialog("font") })
        if (languagePickerVisible) {
            add(SettingsEntry(groupPlayback, stringResource(R.string.settings_app_language), Icons.Default.Language,
                stringResource(R.string.settings_app_language_subtitle), value = languageValue) { languagePickerOpen = true })
        }
        add(SettingsEntry(groupContent, stringResource(R.string.settings_my_sources), Icons.Default.Storage,
            stringResource(R.string.settings_my_sources_subtitle), value = sourcesValue) {
            val active = sources.firstOrNull { it.current } ?: sources.firstOrNull()
            if (active != null) managerSourceIndex = active.index else addPicker = true
        })
        add(SettingsEntry(groupContent, stringResource(R.string.settings_edit_categories), Icons.Default.Category,
            stringResource(R.string.settings_edit_categories_subtitle)) { onEditCategories(); editingCategories = true })
        add(SettingsEntry(groupContent, stringResource(R.string.settings_edit_home), Icons.Default.Tune,
            stringResource(R.string.settings_edit_home_subtitle)) { onEditCategories(); editingHome = true })
        add(SettingsEntry(groupContent, stringResource(R.string.epg_settings_programme_guide), Icons.Default.CalendarMonth,
            stringResource(R.string.settings_epg_subtitle),
            value = if (epgEnabled) enabledLabel else disabledLabel) { epgSettingsOpen = true })
        add(SettingsEntry(groupContent, stringResource(R.string.settings_content_info), Icons.Default.Movie,
            stringResource(R.string.settings_content_info_subtitle),
            value = if (tmdbConfigured) ready else configure) { onDialog("tmdb") })
        add(SettingsEntry(groupAccount, stringResource(R.string.settings_profile), Icons.Default.AccountCircle,
            stringResource(R.string.settings_profile_subtitle), value = profileName) { onDialog("profiles") })
        add(SettingsEntry(groupAccount, stringResource(R.string.settings_parental_control), Icons.Default.Lock,
            stringResource(R.string.settings_parental_control_subtitle)) { onDialog("pin") })
        add(SettingsEntry(groupAccount, stringResource(R.string.settings_backup_restore), Icons.Default.Backup,
            stringResource(R.string.settings_backup_restore_subtitle)) { onDialog("backup") })
        add(SettingsEntry(groupApp, stringResource(R.string.settings_diagnostics), Icons.Default.BugReport,
            stringResource(R.string.settings_diagnostics_subtitle)) { diagnosticsOpen = true })
        add(SettingsEntry(groupApp, stringResource(R.string.settings_clear_metadata_cache), Icons.Default.CleaningServices,
            stringResource(R.string.settings_clear_metadata_cache_subtitle), showSubtitle = true, chevron = false,
            onClick = onClearTmdbCache))
        add(SettingsEntry(groupApp, stringResource(R.string.settings_help_center), Icons.Default.Info,
            stringResource(R.string.settings_help_center_subtitle)) { infoSheet = MobileSettingsInfo.Help })
        add(SettingsEntry(groupApp, stringResource(R.string.settings_rate_app), Icons.Default.Star,
            stringResource(R.string.settings_rate_app_subtitle)) { openStoreReview(context) })
        add(SettingsEntry(groupApp, stringResource(R.string.settings_share_app), Icons.Default.Share,
            stringResource(R.string.settings_share_app_subtitle), onClick = onShare))
        add(SettingsEntry(groupApp, stringResource(R.string.settings_legal_privacy), Icons.Default.Description,
            stringResource(R.string.settings_legal_privacy_subtitle)) { legalPrivacyOpen = true })
    }
    val needle = searchKey(query.trim())
    val matches = if (needle.isEmpty()) emptyList() else entries.filter {
        searchKey("${it.title} ${it.subtitle} ${it.group}").contains(needle)
    }

    BackHandler(enabled = query.isNotEmpty()) { query = "" }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().background(IptvColors.Background),
        contentPadding = PaddingValues(bottom = premiumMobileNavigationContentPadding())
    ) {
        item(key = "settings-title") {
            HorizonSettingsTitle(stringResource(R.string.settings_title))
        }
        item(key = "settings-profile") {
            HorizonProfileCard(
                profileName = profileName,
                sourceName = activeSource?.name,
                onProfile = { accountFlow = true },
                onPremium = { premiumSheet = true },
            )
        }
        item(key = "settings-search") {
            HorizonSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.settings_search_hint),
                clearLabel = stringResource(R.string.settings_search_clear),
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp),
            )
        }
        if (needle.isEmpty()) {
            entries.groupBy { it.group }.forEach { (group, rows) ->
                item(key = "settings-group-$group") {
                    HorizonGroupLabel(group)
                    HorizonSettingsCard {
                        rows.forEach { SettingsEntryRow(it) }
                    }
                }
            }
        } else if (matches.isEmpty()) {
            item(key = "settings-search-empty") {
                Text(
                    stringResource(R.string.settings_search_empty, query.trim()),
                    color = IptvColors.TextTertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp),
                )
            }
        } else {
            item(key = "settings-search-results") {
                Spacer(Modifier.height(14.dp))
                HorizonSettingsCard {
                    matches.forEach { SettingsEntryRow(it, showGroup = true) }
                }
            }
        }

        item(key = "settings-footer") {
            Column(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.settings_using_version, version.ifBlank { "—" }),
                    color = IptvColors.TextTertiary,
                    fontSize = 12.sp,
                )
                TextButton(onClick = { premiumSheet = true }) {
                    Text(stringResource(R.string.settings_restore_purchases), color = IptvColors.TextSecondary, fontSize = 12.5.sp)
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

/** Γραμμή χωρίς βελάκι: η ενέργεια γίνεται επιτόπου. */
private val NoTrailing: @Composable () -> Unit = {}

/** Μία ρύθμιση της επισκόπησης· [subtitle] χρησιμοποιείται και για την αναζήτηση. */
private class SettingsEntry(
    val group: String,
    val title: String,
    val icon: ImageVector,
    val subtitle: String,
    val value: String? = null,
    val showSubtitle: Boolean = false,
    val chevron: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsEntryRow(entry: SettingsEntry, showGroup: Boolean = false) {
    HorizonSettingsRow(
        title = entry.title,
        icon = entry.icon,
        subtitle = when {
            showGroup -> entry.group
            entry.showSubtitle -> entry.subtitle
            else -> null
        },
        value = entry.value,
        onClick = entry.onClick,
        trailing = if (entry.chevron) null else NoTrailing,
    )
}
