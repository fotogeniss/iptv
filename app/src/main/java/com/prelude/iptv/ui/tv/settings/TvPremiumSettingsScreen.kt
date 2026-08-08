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
                        Text("Κύριο προφίλ", color = IptvColors.TextTertiary, fontSize = 10.sp)
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
                Text("v$version · Production", color = IptvColors.TextTertiary, fontSize = 10.sp, maxLines = 1)
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
            title = { Text("Νέα πηγή", color = Color.White, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SourceTypeRow("M3U playlist", "URL ή τοπικό αρχείο", Modifier.focusRequester(addSourceFocus)) { addPicker = false; onAddSource(0) }
                    SourceTypeRow("Xtream Codes", "Server, username και password") { addPicker = false; onAddSource(1) }
                    SourceTypeRow("Stalker Portal", "Portal URL και MAC address") { addPicker = false; onAddSource(2) }
                }
            },
            confirmButton = {},
            dismissButton = { TvDialogTextButton(label = "Κλείσιμο", color = Color.White, onClick = { addPicker = false }) }
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
                    Text("ΡΥΘΜΙΣΕΙΣ", color = IptvColors.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Text("Πηγές περιεχομένου", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text("M3U, Xtream και Stalker με ασφαλή απόκρυψη credentials.", color = IptvColors.TextSecondary, fontSize = 13.sp)
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
                        Text("  Ανανέωση", fontWeight = FontWeight.ExtraBold)
                    }
                    Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                        Icon(Icons.Default.Add, null)
                        Text("  Νέα πηγή", fontWeight = FontWeight.ExtraBold)
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
            SettingsSectionHeader("Υγεία συστήματος", "Πραγματική κατάσταση από το αποθηκευμένο app state")
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsHealthCard("Αποθηκευμένες πηγές", sources.size.toString(), Modifier.weight(1f))
                SettingsHealthCard("Ενεργές τώρα", sources.count { it.current }.toString(), Modifier.weight(1f))
                SettingsHealthCard("Φορτωμένα στοιχεία", sources.firstOrNull { it.current }?.channelCount?.toString() ?: "—", Modifier.weight(1f))
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
    title = "Αναπαραγωγή & δεδομένα",
    subtitle = "Player engines, EPG και metadata services",
    rows = listOf(
        TvRowData("Player αναπαραγωγής", "Αυτόματο fallback μεταξύ ExoPlayer και VLC", Icons.Default.PlayCircle, player) { onDialog("player") },
        TvRowData("Auto Frame Rate", "Αντιστοίχιση 24/25/30/50/60 Hz στη συχνότητα του περιεχομένου", Icons.Default.Settings, autoFrameRate) { onDialog("afr") },
        TvRowData("Απόθεμα αναπαραγωγής", "Πόση εικόνα κρατά ο player μπροστά — ταχύτητα ή σταθερότητα", Icons.Default.Settings, buffer) { onDialog("buffer") },
        TvRowData("Αρχική Android TV", "Δημοσίευση του Συνέχισε να βλέπεις χωρίς URLs ή credentials", Icons.Default.Movie, checked = tvHomeEnabled, action = onToggleTvHome),
        TvRowData("Κανάλι Η λίστα μου", "Source-scoped αγαπημένα με ασφαλή launcher links", Icons.Default.Favorite, checked = tvHomeMyListEnabled, action = onToggleTvHomeMyList),
        TvRowData("Οδηγός προγράμματος", "XMLTV και αντιστοίχιση καναλιών", Icons.Default.CalendarMonth, checked = epg, action = onToggleEpg),
        TvRowData("TMDB metadata", "Αφίσες, backdrops και βαθμολογίες", Icons.Default.Movie, if (tmdb) "Συνδεδεμένο" else "Ρύθμιση") { onDialog("tmdb") },
        TvRowData("OpenSubtitles", "API key και λογαριασμός", Icons.Default.Subtitles, if (subs) "Συνδεδεμένο" else "Ρύθμιση") { onDialog("subs") },
        TvRowData("Εκκαθάριση TMDB cache", "Δεν επηρεάζει λίστες ή ιστορικό", Icons.Default.CleaningServices, action = onClearCache)
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

    TvSettingsRows(
        "Λογαριασμός & ασφάλεια",
        "Προφίλ, Premium, parental control και αντίγραφα ασφαλείας",
        listOf(
            TvRowData(
                "PRELUDE+ Premium",
                when {
                    qaAccess -> "Πλήρης πρόσβαση ιδιοκτήτη · QA build"
                    billing.message != null -> billing.message
                    premiumActive -> "Η αγορά είναι ενεργή"
                    else -> "Μία αγορά μέσω Google Play"
                }.orEmpty(),
                Icons.Default.Star,
                if (qaAccess) "QA" else if (premiumActive) "ΕΝΕΡΓΟ" else billing.offer?.formattedPrice.orEmpty(),
            ) {
                if (!qaAccess) activity?.let(repository::launchPremiumPurchase) ?: repository.start()
            },
            TvRowData(
                "Επαναφορά αγορών",
                "Έλεγχος των αγορών του ενεργού λογαριασμού Google Play",
                Icons.Default.Refresh,
                action = { if (!qaAccess) repository.restorePurchases() },
            ),
            TvRowData("Προφίλ: $profile", "Ξεχωριστά αγαπημένα και ιστορικό", Icons.Default.AccountCircle) { onDialog("profiles") },
            TvRowData("Γονικός έλεγχος", "PIN και κλειδωμένες κατηγορίες", Icons.Default.Lock) { onDialog("pin") },
            TvRowData("Αντίγραφο ασφαλείας", "Εξαγωγή ή επαναφορά JSON", Icons.Default.Backup) { onDialog("backup") },
            TvRowData("Κοινοποίηση εφαρμογής", "Αποστολή package identifier", Icons.Default.Share, action = onShare),
        )
    )
}

@Composable
private fun TvAboutPage(version: String, sourceCount: Int, player: String, epg: Boolean) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(40.dp)) {
        item {
            SettingsSectionHeader("Σχετικά με το Prelude", "Premium streaming UI πάνω στο υπάρχον playback και data layer")
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsHealthCard("Έκδοση", version, Modifier.weight(1f))
                SettingsHealthCard("Πηγές", sourceCount.toString(), Modifier.weight(1f))
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
