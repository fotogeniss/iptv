package com.prelude.iptv.ui.tv.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prelude.iptv.billing.PreludeBilling
import com.prelude.iptv.billing.PremiumTier
import com.prelude.iptv.billing.billingActivity
import com.prelude.iptv.billing.effectivePremiumTier
import com.prelude.iptv.billing.hasQaPremiumOverride
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.BuildConfig
import com.prelude.iptv.R
import com.prelude.iptv.localization.AppLanguage
import com.prelude.iptv.localization.AppLanguageController
import com.prelude.iptv.localization.LocalizationRolloutPolicy
import com.prelude.iptv.ui.components.settings.PremiumSettingsRow
import com.prelude.iptv.ui.components.settings.SettingsHealthCard
import com.prelude.iptv.ui.components.settings.SettingsPage
import com.prelude.iptv.ui.components.settings.SettingsSectionHeader
import com.prelude.iptv.ui.components.settings.SettingsSourceUi
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.TvDialogTextButton
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.localizedText
import com.prelude.iptv.ui.localization.localizedUppercase

@Composable
fun TvPremiumSettingsScreen(
    sources: List<SettingsSourceUi>,
    profileName: String,
    playerLabel: String,
    autoFrameRateLabel: String,
    bufferLabel: String,
    tvHomeEnabled: Boolean,
    tvHomeMyListEnabled: Boolean,
    fontScale: Float,
    epgEnabled: Boolean,
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
    onToggleTvHome: () -> Unit,
    onToggleTvHomeMyList: () -> Unit,
    onClearTmdbCache: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf(SettingsPage.Sources) }
    var addPicker by remember { mutableStateOf(false) }
    var languagePickerOpen by remember { mutableStateOf(false) }
    val languagePickerVisible = LocalizationRolloutPolicy.pickerVisible(
        ownerQaBuild = BuildConfig.PREMIUM_QA_OVERRIDE,
        translationParityComplete = BuildConfig.LOCALIZATION_PARITY_COMPLETE,
    )
    val selectedAppLanguage = AppLanguageController.selected()
    val firstFocus = rememberInitialFocus(enabled = true)
    val pages = listOf(
        SettingsPage.Sources to Icons.Default.Storage,
        SettingsPage.Playback to Icons.Default.PlayCircle,
        SettingsPage.Appearance to Icons.Default.Palette,
        SettingsPage.Account to Icons.Default.AccountCircle,
        SettingsPage.About to Icons.Default.Info
    )

    Row(
        modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF161821), IptvColors.Background),
                radius = 1200f
            )
        )
    ) {
        // Το αριστερό μενού δεν είναι μόνιμα ανοιχτό: μένει στενή μπάρα εικονιδίων
        // και «ανοίγει» (labels + κείμενα) μόνο όταν πάρει focus, δηλαδή όταν ο
        // χρήστης πάει τέρμα αριστερά. Κλείνει μόλις το focus γυρίσει στο περιεχόμενο.
        var railExpanded by remember { mutableStateOf(false) }
        val railWidth by animateDpAsState(
            if (railExpanded) 268.dp else 84.dp,
            tween(220),
            label = "settingsRailWidth"
        )
        Column(
            Modifier.width(railWidth).fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.34f))
                .onFocusChanged { railExpanded = it.hasFocus }
                // Scrollable ώστε σε μεγάλη γραμματοσειρά ή με TV overscan τα κάτω
                // items (Account/About) να μη μένουν κρυμμένα — το D-pad focus τα
                // φέρνει πάντα στην οθόνη.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (railExpanded) 24.dp else 12.dp, vertical = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(14.dp).height(30.dp).background(IptvColors.Primary))
                if (railExpanded) {
                    Spacer(Modifier.width(12.dp))
                    Text("PRELUDE", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth().then(
                    if (railExpanded)
                        Modifier.background(Color.White.copy(alpha = 0.045f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).padding(12.dp)
                    else Modifier
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(42.dp).height(42.dp).background(IptvColors.Primary, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text(profileName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                if (railExpanded) {
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(profileName, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1)
                        Text(stringResource(R.string.settings_primary_profile), color = IptvColors.TextTertiary, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            pages.forEachIndexed { index, (item, icon) ->
                TvSettingsNavItem(
                    page = item,
                    icon = icon,
                    selected = item == page,
                    expanded = railExpanded,
                    focusRequester = if (index == 0) firstFocus else null,
                    onClick = { page = item }
                )
                Spacer(Modifier.height(6.dp))
            }
            if (railExpanded) {
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.settings_version_environment, version, stringResource(R.string.settings_production)), color = IptvColors.TextTertiary, fontSize = 10.sp, maxLines = 1)
            }
        }

        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (page) {
                SettingsPage.Sources -> TvSourcesPage(
                    sources = sources,
                    onOpen = onOpenSource,
                    onEdit = onEditSource,
                    onDelete = onDeleteSource,
                    onRefreshCurrent = onRefreshCurrentSource,
                    onAdd = { addPicker = true }
                )
                SettingsPage.Playback -> TvPlaybackPage(playerLabel, autoFrameRateLabel, bufferLabel, tvHomeEnabled, tvHomeMyListEnabled, epgEnabled, tmdbConfigured, subtitlesConfigured, onDialog, onToggleEpg, onToggleTvHome, onToggleTvHomeMyList, onClearTmdbCache)
                SettingsPage.Appearance -> TvAppearancePage(
                    fontScale = fontScale,
                    selectedLanguage = selectedAppLanguage,
                    languagePickerVisible = languagePickerVisible,
                    onOpenLanguagePicker = { languagePickerOpen = true },
                    onDialog = onDialog,
                )
                SettingsPage.Account -> TvAccountPage(profileName, onDialog, onShare)
                SettingsPage.About -> TvAboutPage(version, sources.size, playerLabel, epgEnabled)
            }
        }
    }

    if (addPicker) {
        val addSourceFocus = rememberInitialFocus(key = "add-source-picker")
        AlertDialog(
            onDismissRequest = { addPicker = false },
            containerColor = IptvColors.SurfaceRaised,
            title = { Text(stringResource(R.string.settings_new_source), color = Color.White, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SourceTypeRow("M3U playlist", stringResource(R.string.sources_method_url_card_subtitle), Modifier.focusRequester(addSourceFocus)) { addPicker = false; onAddSource(0) }
                    SourceTypeRow("Xtream Codes", stringResource(R.string.sources_method_xtream_card_subtitle)) { addPicker = false; onAddSource(1) }
                    SourceTypeRow("Stalker Portal", stringResource(R.string.sources_method_mac_card_subtitle)) { addPicker = false; onAddSource(2) }
                }
            },
            confirmButton = {},
            dismissButton = { TvDialogTextButton(label = stringResource(R.string.settings_close), color = Color.White, onClick = { addPicker = false }) }
        )
    }

    if (languagePickerOpen && languagePickerVisible) {
        TvAppLanguageDialog(
            selected = selectedAppLanguage,
            onSelect = { language ->
                languagePickerOpen = false
                AppLanguageController.select(language)
            },
            onDismiss = { languagePickerOpen = false },
        )
    }
}

@Composable
private fun TvSourcesPage(
    sources: List<SettingsSourceUi>,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onRefreshCurrent: () -> Unit,
    onAdd: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(40.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(localizedUppercase(stringResource(R.string.settings_title)), color = IptvColors.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Text(stringResource(R.string.settings_sources_title), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.settings_sources_subtitle), color = IptvColors.TextSecondary, fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val current = sources.firstOrNull { it.current }
                    OutlinedButton(
                        onClick = onRefreshCurrent,
                        enabled = current != null && !current.loading,
                        border = androidx.compose.foundation.BorderStroke(1.dp, IptvColors.DividerStrong),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Text("  ${stringResource(R.string.settings_refresh)}", fontWeight = FontWeight.ExtraBold)
                    }
                    Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                        Icon(Icons.Default.Add, null)
                        Text("  ${stringResource(R.string.settings_new_source)}", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(sources, key = { it.index }) { source ->
                    TvSettingsSourceCard(
                        source = source,
                        onOpen = { onOpen(source.index) },
                        onEdit = { onEdit(source.index) },
                        onDelete = { onDelete(source.index) },
                        onRefresh = if (source.current) onRefreshCurrent else null
                    )
                }
                item { TvAddSourceCard(onAdd) }
            }
            Spacer(Modifier.height(28.dp))
        }
        item {
            SettingsSectionHeader(stringResource(R.string.settings_system_health), stringResource(R.string.settings_system_health_subtitle))
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsHealthCard(stringResource(R.string.settings_saved_sources), sources.size.toString(), Modifier.weight(1f))
                SettingsHealthCard(stringResource(R.string.settings_active_now), sources.count { it.current }.toString(), Modifier.weight(1f))
                SettingsHealthCard(stringResource(R.string.settings_loaded_items), sources.firstOrNull { it.current }?.channelCount?.toString() ?: "—", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TvPlaybackPage(
    player: String, autoFrameRate: String, buffer: String,
    tvHomeEnabled: Boolean, tvHomeMyListEnabled: Boolean,
    epg: Boolean, tmdb: Boolean, subs: Boolean,
    onDialog: (String) -> Unit, onToggleEpg: () -> Unit, onToggleTvHome: () -> Unit,
    onToggleTvHomeMyList: () -> Unit, onClearCache: () -> Unit
) = TvSettingsRows(
    title = stringResource(R.string.settings_playback_data_title),
    subtitle = stringResource(R.string.settings_playback_data_subtitle),
    rows = listOf(
        TvRowData(stringResource(R.string.settings_player_mode_title), stringResource(R.string.settings_player_mode_subtitle), Icons.Default.PlayCircle, player) { onDialog("player") },
        TvRowData("Auto Frame Rate", stringResource(R.string.settings_afr_subtitle), Icons.Default.Settings, autoFrameRate) { onDialog("afr") },
        TvRowData(stringResource(R.string.settings_buffer_title), stringResource(R.string.settings_buffer_subtitle), Icons.Default.Settings, buffer) { onDialog("buffer") },
        TvRowData(stringResource(R.string.settings_tv_home), stringResource(R.string.settings_tv_home_subtitle), Icons.Default.Movie, checked = tvHomeEnabled, action = onToggleTvHome),
        TvRowData(stringResource(R.string.settings_tv_my_list), stringResource(R.string.settings_tv_my_list_subtitle), Icons.Default.Favorite, checked = tvHomeMyListEnabled, action = onToggleTvHomeMyList),
        TvRowData(
            stringResource(R.string.epg_settings_programme_guide),
            stringResource(R.string.epg_settings_xmltv_matching),
            Icons.Default.CalendarMonth,
            checked = epg,
            action = onToggleEpg,
        ),
        TvRowData(stringResource(R.string.settings_tmdb_metadata), stringResource(R.string.settings_tmdb_metadata_subtitle), Icons.Default.Movie, stringResource(if (tmdb) R.string.settings_connected else R.string.settings_configure)) { onDialog("tmdb") },
        TvRowData("OpenSubtitles", stringResource(R.string.settings_opensubtitles_subtitle), Icons.Default.Subtitles, stringResource(if (subs) R.string.settings_connected else R.string.settings_configure)) { onDialog("subs") },
        TvRowData(stringResource(R.string.settings_clear_tmdb_cache), stringResource(R.string.settings_clear_tmdb_cache_subtitle), Icons.Default.CleaningServices, action = onClearCache)
    )
)

@Composable
private fun TvAppearancePage(
    fontScale: Float,
    selectedLanguage: AppLanguage,
    languagePickerVisible: Boolean,
    onOpenLanguagePicker: () -> Unit,
    onDialog: (String) -> Unit,
) {
    val rows = mutableListOf<TvRowData>()
    if (languagePickerVisible) {
        rows += TvRowData(
            title = stringResource(R.string.settings_app_language),
            subtitle = stringResource(R.string.settings_app_language_subtitle),
            icon = Icons.Default.Language,
            value = stringResource(selectedLanguage.labelRes()),
            action = onOpenLanguagePicker,
        )
    }
    rows += TvRowData(
        title = stringResource(R.string.settings_text_size),
        subtitle = stringResource(R.string.settings_text_size_subtitle),
        icon = Icons.Default.FormatSize,
        value = "${(fontScale * 100).toInt()}%",
    ) { onDialog("font") }

    TvSettingsRows(
        title = stringResource(R.string.settings_appearance_title),
        subtitle = stringResource(R.string.settings_appearance_subtitle),
        rows = rows,
    )
}

@Composable
private fun TvAccountPage(profile: String, onDialog: (String) -> Unit, onShare: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { PreludeBilling.repository(context) }
    val billing by repository.state.collectAsStateWithLifecycle()
    val qaAccess = hasQaPremiumOverride()
    val premiumActive = effectivePremiumTier(billing.entitlement.tier) == PremiumTier.PREMIUM
    val activity = context.billingActivity()
    val billingMessage = billing.message?.localizedText()

    TvSettingsRows(
        stringResource(R.string.settings_group_account_security),
        stringResource(R.string.settings_account_security_subtitle),
        listOf(
            TvRowData(
                stringResource(R.string.billing_premium_title),
                when {
                    qaAccess -> stringResource(R.string.settings_premium_owner_qa)
                    billingMessage != null -> billingMessage
                    premiumActive -> stringResource(R.string.settings_premium_purchase_active)
                    else -> stringResource(R.string.settings_premium_single_purchase)
                }.orEmpty(),
                Icons.Default.Star,
                if (qaAccess) stringResource(R.string.billing_qa_badge) else if (premiumActive) stringResource(R.string.settings_premium_active_badge) else billing.offer?.formattedPrice.orEmpty(),
            ) {
                if (!qaAccess) activity?.let(repository::launchPremiumPurchase) ?: repository.start()
            },
            TvRowData(
                stringResource(R.string.settings_restore_purchases),
                stringResource(R.string.settings_restore_purchases_subtitle),
                Icons.Default.Refresh,
                action = { if (!qaAccess) repository.restorePurchases() },
            ),
            TvRowData(stringResource(R.string.settings_profile_named, profile), stringResource(R.string.settings_profile_separate_data), Icons.Default.AccountCircle) { onDialog("profiles") },
            TvRowData(stringResource(R.string.settings_parental_control), stringResource(R.string.settings_parental_control_subtitle), Icons.Default.Lock) { onDialog("pin") },
            TvRowData(stringResource(R.string.settings_backup), stringResource(R.string.settings_backup_subtitle), Icons.Default.Backup) { onDialog("backup") },
            TvRowData(stringResource(R.string.settings_share_app), stringResource(R.string.settings_share_package_subtitle), Icons.Default.Share, action = onShare),
        )
    )
}

@Composable
private fun TvAboutPage(version: String, sourceCount: Int, player: String, epg: Boolean) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(40.dp)) {
        item {
            SettingsSectionHeader(stringResource(R.string.settings_about_prelude), stringResource(R.string.settings_about_prelude_subtitle))
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsHealthCard(stringResource(R.string.settings_version), version, Modifier.weight(1f))
                SettingsHealthCard(stringResource(R.string.settings_nav_sources), sourceCount.toString(), Modifier.weight(1f))
                SettingsHealthCard("Player", player, Modifier.weight(1f))
                SettingsHealthCard("EPG", if (epg) "ON" else "OFF", Modifier.weight(1f))
            }
        }
    }
}

private data class TvRowData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val value: String = "",
    val checked: Boolean? = null,
    val action: () -> Unit
)

@Composable
private fun TvSettingsRows(title: String, subtitle: String, rows: List<TvRowData>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SettingsSectionHeader(title, subtitle); Spacer(Modifier.height(14.dp)) }
        items(rows) { row ->
            PremiumSettingsRow(row.title, row.subtitle, row.icon, value = row.value, checked = row.checked, television = true, onClick = row.action)
        }
    }
}

@Composable
private fun SourceTypeRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PremiumSettingsRow(title, subtitle, Icons.Default.Add, modifier = modifier, television = true, onClick = onClick)
}
