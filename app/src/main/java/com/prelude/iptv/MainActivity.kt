package com.prelude.iptv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgEntry
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.PlaybackQueuePolicy
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.data.Repository
import com.prelude.iptv.ui.CastMember
import com.prelude.iptv.ui.DetailScreen
import com.prelude.iptv.ui.EmptyWithArrow
import com.prelude.iptv.ui.AdaptiveCatalogHome
import com.prelude.iptv.ui.AdaptiveSettingsScreen
import com.prelude.iptv.ui.HeroShowcase
import com.prelude.iptv.ui.MainViewModel
import com.prelude.iptv.ui.PremiumTvHero
import com.prelude.iptv.ui.PremiumProfileGate
import com.prelude.iptv.ui.PremiumTvNavigationRail
import com.prelude.iptv.ui.StreamingSegment
import com.prelude.iptv.ui.StreamingSegmentedControl
import com.prelude.iptv.ui.StreamingScreenHeader
import com.prelude.iptv.ui.PremiumLibraryScreen
import com.prelude.iptv.ui.LibraryDestination
import com.prelude.iptv.ui.LibraryPolicy
import com.prelude.iptv.ui.components.library.PremiumLibraryContent
import com.prelude.iptv.ui.PremiumContentRail
import com.prelude.iptv.ui.buildCatalogRailSections
import com.prelude.iptv.ui.NotchedBottomBar
import com.prelude.iptv.ui.TvIconButton
import com.prelude.iptv.ui.TvDialogTextButton
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.isTvDevice
import com.prelude.iptv.ui.Tab
import com.prelude.iptv.ui.TextEntryDialog
import com.prelude.iptv.ui.IptvTheme
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.mobile.navigation.PremiumMobileBottomNavigation
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.tvFocus
import com.prelude.iptv.tvhome.TvHomeSyncScheduler
import com.prelude.iptv.tvhome.TvHomeDevice
import com.prelude.iptv.ui.route.*
import com.prelude.iptv.ui.splash.PreludeSplash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (TvHomeDevice.isTv(this)) TvHomeSyncScheduler.schedule(this)
        setContent {
            IptvTheme {
                Root()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.prelude.iptv.billing.PreludeBilling.repository(this).onAppResumed()
    }
}

@Composable
private fun Root(vm: MainViewModel = viewModel()) {
    val state by vm.appShellState.collectAsStateWithLifecycle()
    val catalog by vm.catalogState.collectAsStateWithLifecycle()

    // ---- ΕΙΣΑΓΩΓΗ ----
    //
    // Επίστρωση πάνω από τα πάντα, όχι ξεχωριστή οθόνη. Έτσι βλέπει την ΙΔΙΑ
    // πρόοδο με τον κατάλογο και φεύγει όταν εκείνος είναι έτοιμος — αντί για
    // ένα χρονόμετρο που μαντεύει.
    //
    // Το `rememberSaveable` κρατά το «τελείωσε» σε περιστροφή οθόνης: η εισαγωγή
    // παίζει μία φορά ανά άνοιγμα, όχι μία φορά ανά σύνθεση.
    var splashDone by rememberSaveable { mutableStateOf(false) }
    if (!splashDone) {
        val progress by vm.catalogProgressState.collectAsStateWithLifecycle()
        val active = progress.sourceProgress[vm.currentSourceId()]?.takeIf { it.active }
        PreludeSplash(
            realProgress = active?.percent?.let { it / 100f },
            stage = active?.stage.orEmpty(),
            // ΕΤΟΙΜΗ = Η ΕΦΑΡΜΟΓΗ ΕΧΕΙ ΚΑΤΙ ΝΑ ΔΕΙΞΕΙ, Η ΣΤΑΜΑΤΗΣΕ ΝΑ ΠΡΟΣΠΑΘΕΙ.
            //
            // Και τα τέσσερα σκέλη είναι απαραίτητα, και το καθένα αντιστοιχεί σε
            // μια οθόνη που ΘΑ φανεί από κάτω:
            //
            //  - καμία πηγή              -> οθόνη καλωσορίσματος
            //  - Xtream/Stalker          -> «διάλεξε ενότητα». ΔΕΝ κατεβάζει από
            //                               μόνο του, οπότε το «περίμενε κανάλια»
            //                               θα κρατούσε την εισαγωγή για πάντα.
            //  - υπάρχουν κανάλια        -> ο κατάλογος
            //  - σφάλμα                  -> το μήνυμα λάθους. Μια εισαγωγή που δεν
            //                               φεύγει επειδή έπεσε το δίκτυο κρύβει
            //                               ακριβώς την πληροφορία που χρειάζεται.
            //
            // Το `!loading` μπαίνει έξω από όλα: όσο κατεβάζει, μένουμε.
            finished = !catalog.loading && (
                state.playlists.isEmpty() ||
                    catalog.chooseContent ||
                    catalog.channels.isNotEmpty() ||
                    catalog.status.startsWith("Σφάλμα")
                ),
            onDismiss = { splashDone = true }
        )
        return
    }

    val ctx = androidx.compose.ui.platform.LocalContext.current
    val startupStore = remember(ctx) { com.prelude.iptv.data.PlaylistStore(ctx) }
    val profiles by vm.profilesState.collectAsStateWithLifecycle()
    val activity = ctx as? android.app.Activity
    val skipProfileGate = activity?.intent?.getBooleanExtra(AppRouteContract.EXTRA_SKIP_PROFILE_GATE, false) == true
    var profileGateOpen by rememberSaveable { mutableStateOf(profiles.size > 1 && !skipProfileGate) }
    LaunchedEffect(skipProfileGate) {
        if (skipProfileGate) activity?.intent?.removeExtra(AppRouteContract.EXTRA_SKIP_PROFILE_GATE)
    }
    var openProfileSettingsAfterGate by rememberSaveable { mutableStateOf(false) }

    if (profileGateOpen) {
        PremiumProfileGate(
            profiles = profiles,
            activeProfileId = vm.activeProfileId(),
            pinRequired = vm::profileNeedsPin,
            verifyPin = vm::checkPin,
            onOpenProfile = { profile ->
                if (profile.id == vm.activeProfileId()) {
                    profileGateOpen = false
                } else {
                    vm.setActiveProfile(profile.id)
                    activity?.intent?.putExtra(AppRouteContract.EXTRA_SKIP_PROFILE_GATE, true)
                    activity?.recreate()
                }
            },
            onManageProfiles = {
                profileGateOpen = false
                openProfileSettingsAfterGate = true
            }
        )
        return
    }

    // Χωρίς πηγή ανοίγει απευθείας η νέα, πλήρης add-playlist ροή σε mobile και TV.
    // Τα δύο παλιά ενδιάμεσα onboarding βήματα αφαιρέθηκαν: δεν πρόσθεταν
    // λειτουργικότητα και ανάγκαζαν τον χρήστη να επιλέξει τον ίδιο τύπο δύο φορές.
    var openFirstPlaylistAfterAdd by rememberSaveable { mutableStateOf(false) }
    if (state.playlists.isEmpty()) {
        AddPlaylistScreen(
            initialTab = 0,
            onDismiss = { activity?.finish() },
            onAdd = { playlist ->
                openFirstPlaylistAfterAdd = true
                vm.addPlaylist(playlist)
                vm.setContentType("live")
            },
        )
        return
    }

    // Τα αγαπημένα αλλάζουν και μέσα στον player (γράφει απευθείας στο store):
    // στο ON_RESUME ξαναδιαβάζουμε, αλλιώς η λίστα δείχνει μπαγιάτικο state.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refreshFavorites()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    var tab by rememberSaveable { mutableStateOf(if (openProfileSettingsAfterGate) Tab.SETTINGS else Tab.PLAYLIST) }
    var browsing by rememberSaveable { mutableStateOf(openFirstPlaylistAfterAdd) }
    var settingsNavigationCollapsed by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var addTab by remember { mutableStateOf(0) }
    var showXmltv by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf(-1) }
    var autoOpened by rememberSaveable { mutableStateOf(false) }
    var returnToBrowseAfterSettings by rememberSaveable { mutableStateOf(false) }
    var confirmSourceSelection by rememberSaveable { mutableStateOf(false) }
    var browseDestinationRequest by rememberSaveable { mutableStateOf<String?>(null) }

    // Άνοιγμα κατευθείαν στην τελευταία λίστα που έβλεπες (μία φορά ανά εκκίνηση).
    // Το ViewModel έχει ήδη επαναφέρει το currentIndex από το store.
    LaunchedEffect(state.playlists.size, startupStore.autoOpenPlaylist) {
        if (com.prelude.iptv.data.PlaylistPreferencePolicy.shouldAutoOpen(
                alreadyOpened = autoOpened,
                enabled = startupStore.autoOpenPlaylist,
                hasSources = state.playlists.isNotEmpty()
            )
        ) {
            autoOpened = true
            browsing = true
        }
    }

    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, state.fontScale)) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            if (browsing) {
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    // Το Back μέσα στο catalog δεν πετάει αμέσως στην επιλογή
                    // πηγής. Ζητά επιβεβαίωση, γιατί σε κινητό/TV πατιέται εύκολα
                    // κατά λάθος και η ενεργή πηγή πρέπει να παραμείνει φορτωμένη.
                    BackHandler(enabled = true) { confirmSourceSelection = true }
                    BrowseScreen(
                        vm = vm,
                        onBack = { confirmSourceSelection = true },
                        onOpenSettings = {
                            returnToBrowseAfterSettings = true
                            browsing = false
                            tab = Tab.SETTINGS
                        },
                        initialMobileDestination = browseDestinationRequest,
                        onMobileDestinationConsumed = { browseDestinationRequest = null }
                    )
                }
            } else {
                BackHandler(enabled = tab != Tab.PLAYLIST) {
                    if (tab == Tab.SETTINGS && returnToBrowseAfterSettings) {
                        returnToBrowseAfterSettings = false
                        browsing = true
                    } else {
                        tab = Tab.PLAYLIST
                    }
                }
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (tab) {
                                Tab.PLAYLIST -> PlaylistTab(
                                    state = state,
                                    vm = vm,
                                    onOpen = { i -> vm.selectPlaylist(i); browsing = true },
                                    onEdit = { i -> editIndex = i },
                                    onAdd = { type -> addTab = type; showAdd = true }
                                )
                                Tab.XTREAM -> XtreamTab(state, vm,
                                    onAdd = { addTab = 1; showAdd = true },
                                    onOpen = { i -> vm.selectPlaylist(i); browsing = true },
                                    onEdit = { i -> editIndex = i })
                                Tab.EPG -> EpgTabScreen { showXmltv = true }
                                Tab.SETTINGS -> SettingsTab(
                                    vm = vm,
                                    onAddSource = { type -> addTab = type; showAdd = true },
                                    onOpenSource = { index -> vm.selectPlaylist(index); browsing = true },
                                    onEditSource = { index -> editIndex = index },
                                    onNavigationCollapsedChange = { settingsNavigationCollapsed = it }
                                )
                            }
                        }
                        if (tab != Tab.SETTINGS) {
                            NotchedBottomBar(
                                current = tab,
                                onSelect = { selected ->
                                    returnToBrowseAfterSettings = false
                                    tab = selected
                                },
                                // ΕΝΑΣ ΠΡΟΟΡΙΣΜΟΣ, ΧΩΡΙΣ ΕΝΔΙΑΜΕΣΗ ΕΠΙΛΟΓΗ.
                                //
                                // Πριν άνοιγε το AddMenuSheet, που ρωτούσε «από
                                // πού;» και μετά άνοιγε το AddPlaylistScreen με
                                // προεπιλεγμένη καρτέλα. Οι τέσσερις πρώτες
                                // επιλογές του ΕΙΝΑΙ ήδη καρτέλες εκείνης της
                                // οθόνης, οπότε το φύλλο πρόσθετε ένα βήμα για
                                // να διαλέξεις κάτι που μπορείς να διαλέξεις και
                                // μέσα. Ίδιος προορισμός με το πρώην κουμπί
                                // «＋ Νέα πηγή» της κεφαλίδας.
                                onAdd = { addTab = 1; showAdd = true }
                            )
                        }
                    }
                    val catalogOverlayOpen = catalog.chooseContent || catalog.pickCategories ||
                        catalog.askLoadMode || catalog.askRefreshMode
                    if (!isTvDevice() && tab == Tab.SETTINGS && !catalogOverlayOpen) {
                        fun openDestination(destination: String) {
                            returnToBrowseAfterSettings = false
                            browseDestinationRequest = destination
                            browsing = true
                        }
                        PremiumMobileBottomNavigation(
                            selected = "settings",
                            onHome = { openDestination("home") },
                            onMovies = { openDestination("movies") },
                            onSeries = { openDestination("series") },
                            onLive = { openDestination("live") },
                            onSearch = { openDestination("search") },
                            onMyList = { openDestination("library") },
                            onSettings = {},
                            collapsed = settingsNavigationCollapsed,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        Box(Modifier.fillMaxSize()) {
            BackHandler(enabled = true) { showAdd = false }
            AddPlaylistScreen(
                initialTab = addTab,
                onDismiss = { showAdd = false },
                onAdd = {
                    showAdd = false
                    vm.addPlaylist(it)
                    vm.setContentType("live")
                    browsing = true
                }
            )
        }
    }
    if (editIndex >= 0) {
        val idx = editIndex
        val pl = state.playlists.getOrNull(idx)
        if (pl == null) editIndex = -1
        else Box(Modifier.fillMaxSize()) {
            BackHandler(enabled = true) { editIndex = -1 }
            AddPlaylistScreen(
                existing = pl,
                onDismiss = { editIndex = -1 },
                onAdd = { updated -> editIndex = -1; vm.updatePlaylist(idx, updated) }
            )
        }
    }

    if (confirmSourceSelection) {
        val confirmFocus = rememberInitialFocus(key = "confirm-source-selection")
        AlertDialog(
            onDismissRequest = { confirmSourceSelection = false },
            title = { Text("Αλλαγή πηγής;") },
            text = {
                Text("Θα φύγεις από την τρέχουσα πηγή περιεχομένου και θα επιστρέψεις στην επιλογή πηγής. Θέλεις να συνεχίσεις;")
            },
            confirmButton = {
                TvDialogTextButton(
                    label = "Συνέχεια",
                    modifier = Modifier.focusRequester(confirmFocus),
                    onClick = {
                        confirmSourceSelection = false
                        returnToBrowseAfterSettings = false
                        browsing = false
                        tab = Tab.PLAYLIST
                    }
                )
            },
            dismissButton = {
                TvDialogTextButton(label = "Ακύρωση", onClick = { confirmSourceSelection = false })
            }
        )
    }

    // Η «Εισαγωγή EPG» ΔΕΝ έχασε πόρτα μαζί με το AddMenuSheet: υπάρχει και στην
    // καρτέλα EPG. Η «Αναπαραγωγή μεμονωμένου stream» είχε ΜΟΝΟ εκείνη, και
    // αφαιρέθηκε μαζί της — δες τη σημείωση πάνω από τον [SingleStreamDialog].
    if (showXmltv) XmltvDialog(vm, onDismiss = { showXmltv = false })
    // Η μετάβαση στην επιλογή πηγής προστατεύεται από confirm dialog.
}

/* =============================== tab: Λίστες =============================== */
