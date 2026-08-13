package com.prelude.iptv.ui.mobile.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prelude.iptv.billing.PremiumFeature
import com.prelude.iptv.R
import com.prelude.iptv.billing.PremiumRequiredDialog
import com.prelude.iptv.billing.isUnlocked
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.CatalogRailSection
import com.prelude.iptv.ui.home.HomeLayoutPolicy
import com.prelude.iptv.ui.home.HomeRailContentPolicy
import com.prelude.iptv.ui.localization.titleRes
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding
import com.prelude.iptv.category.CategoryLayoutPolicy
import kotlin.random.Random

/**
 * Η αρχική του κινητού.
 *
 * ΤΙ ΑΛΛΑΞΕ ΚΑΙ ΓΙΑΤΙ: πριν, η σειρά των ενοτήτων ήταν γραμμένη μέσα στον κώδικα
 * και ο χρήστης είχε τρία σκόρπια κουμπιά για να την επηρεάσει έμμεσα — chips
 * κατηγοριών, «Οι ομάδες σου», και τίποτα για το τι κρύβεται. Τώρα υπάρχει ΕΝΑ
 * σημείο ([MobileEditHomeScreen]) και η οθόνη απλώς εκτελεί ό,τι λέει.
 *
 * Η διάταξη διαβάζεται και γράφεται εδώ, απευθείας από τον [PlaylistStore], και
 * όχι μέσα από το ViewModel: αφορά ΜΟΝΟ αυτή την οθόνη, δεν επηρεάζει τίποτα
 * άλλο, και δεν αξίζει να περάσει μέσα από τρία επίπεδα παραμέτρων για να
 * καταλήξει στο ίδιο αρχείο ρυθμίσεων. Το ίδιο κάνει ο player με το μέγεθος των
 * υποτίτλων.
 */
@Composable
fun MobilePremiumHomeScreen(
    /** Ό,τι φιλτράρει η τρέχουσα ενότητα — τροφοδοτεί hero και προτάσεις. */
    channels: List<Channel>,
    sections: List<CatalogRailSection>,
    favoriteKeys: Set<String>,
    profileName: String,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?,
    onPlay: (Channel) -> Unit,
    onDetails: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    selectedDestination: String,
    onOpenHome: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMyList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCategories: () -> Unit = {},
    onSort: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onSectionBack: () -> Unit = {},
    downloadedItemCount: Int = channels.size,
    downloadedCategoryCount: Int = 0,
    /**
     * ΟΛΟΚΛΗΡΟΣ ο κατάλογος, χωρίς φίλτρα.
     *
     * Τα πλακίδια και τα rails ζωντανών χρειάζονται κανάλια που η τρέχουσα
     * ενότητα έχει ήδη πετάξει. Χωρίς αυτό, ο μετρητής «Ζωντανά» θα έγραφε
     * πάντα μηδέν ενώ η λίστα έχει χιλιάδες.
     */
    allChannels: List<Channel> = channels,
    /** Κανάλια που είδε πρόσφατα, νεότερο πρώτο. */
    recentLive: List<Channel> = emptyList(),
    /** Σβήσιμο ιστορικού μιας ενότητας (id του [HomeLayoutPolicy]). */
    onClearHistory: (String) -> Unit = {},
    onUpdateContents: () -> Unit = {},
    onExport: () -> Unit = {},
    categoryTitlesInOrder: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    if (channels.isEmpty()) return

    val context = LocalContext.current
    val appContext = context.applicationContext
    val store = remember(appContext) { PlaylistStore(appContext) }
    // Οι αλλαγές γράφονται αμέσως στον δίσκο, αλλά η οθόνη πρέπει να τις δει
    // ΤΩΡΑ: το SharedPreferences δεν ειδοποιεί το Compose. Ο μετρητής είναι το
    // σήμα «ξαναδιάβασε».
    var layoutVersion by remember { mutableIntStateOf(0) }
    // Κάθε προορισμός έχει τη ΔΙΚΗ ΤΟΥ διάταξη. Πριν, μία ρύθμιση κυβερνούσε και
    // τις τέσσερις οθόνες, γι' αυτό ο επεξεργαστής απαριθμούσε ενότητες που δεν
    // μπορούσαν να εμφανιστούν εκεί που κοιτούσε ο χρήστης.
    val entries = remember(layoutVersion, selectedDestination) {
        HomeLayoutPolicy.resolve(
            savedOrder = store.homeSectionOrder(selectedDestination),
            hidden = store.homeHiddenSections(selectedDestination),
            destination = selectedDestination,
        )
    }

    DisposableEffect(store) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key?.contains("home_order") == true ||
                key?.contains("home_hidden") == true ||
                key?.contains("home_cat_") == true
            ) layoutVersion++
        }
        val preferences = appContext.getSharedPreferences(
            PlaylistStore.PREFS,
            android.content.Context.MODE_PRIVATE,
        )
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val canEditHome = isUnlocked(PremiumFeature.EDIT_HOME)
    val canSuggest = isUnlocked(PremiumFeature.SUGGESTIONS)

    /**
     * Ποια κλειδωμένη δυνατότητα ζήτησε ο χρήστης, ώστε να του εξηγηθεί.
     *
     * Πριν, το πάτημα έθετε `editing = true` και ένα `if (editing && canEditHome)`
     * το κατάπινε σιωπηλά. Ένα κουμπί που δεν κάνει τίποτα δεν διαβάζεται ως
     * «κλειδωμένο» — διαβάζεται ως «χαλασμένο».
     */
    var lockedFeature by remember { mutableStateOf<PremiumFeature?>(null) }

    var editing by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<CatalogRailSection?>(null) }

    val live = remember(allChannels) { HomeRailContentPolicy.liveOf(allChannels) }
    val movies = remember(allChannels) { HomeRailContentPolicy.moviesOf(allChannels) }
    val series = remember(allChannels) { HomeRailContentPolicy.seriesOf(allChannels) }
    val showsCatalogCategories =
        selectedDestination == "movies" || selectedDestination == "series"
    val catalogGroups = remember(channels, categoryTitlesInOrder) {
        val counted = channels.asSequence()
            .map { it.group.trim() }
            .filter(String::isNotBlank)
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        CategoryLayoutPolicy.orderByTitle(counted, categoryTitlesInOrder) { it.key }
    }
    var selectedCatalogGroup by remember(selectedDestination, catalogGroups, layoutVersion) {
        val sectionId = when (selectedDestination) {
            "movies" -> HomeLayoutPolicy.MOVIES
            "series" -> HomeLayoutPolicy.SERIES
            else -> ""
        }
        val saved = if (sectionId.isBlank()) "" else store.homeRailCategory(sectionId)
        mutableStateOf(
            HomeRailContentPolicy.resolveCategory(saved, catalogGroups.map { it.key })
                .takeIf { it.isNotBlank() }
        )
    }
    LaunchedEffect(catalogGroups) {
        if (selectedCatalogGroup != null &&
            catalogGroups.none { it.key == selectedCatalogGroup }
        ) {
            selectedCatalogGroup = null
        }
    }
    val catalogCategoryOptions = remember(catalogGroups, channels.size) {
        listOf(MobileCategoryOption("all", context.getString(R.string.home_all_categories), channels.size)) +
            catalogGroups.map { entry ->
                MobileCategoryOption("group:${entry.key}", entry.key, entry.value)
            }
    }
    val displayedChannels = remember(channels, selectedCatalogGroup) {
        selectedCatalogGroup?.let { selected ->
            channels.filter { it.group.trim() == selected }
        } ?: channels
    }
    val displayedSections = remember(sections, selectedCatalogGroup) {
        selectedCatalogGroup?.let { selected ->
            sections.mapNotNull { section ->
                val all = section.allItems.filter { it.group.trim() == selected }
                if (all.isEmpty()) null
                else section.copy(
                    items = section.items.filter { it.group.trim() == selected },
                    allItems = all,
                )
            }
        } ?: sections
    }
    // ΠΑΝΤΑ ΦΙΛΤΡΟ ΕΙΔΟΥΣ, ΑΚΟΜΗ ΚΑΙ ΜΕΣΑ ΣΤΗΝ ΙΔΙΑ ΤΗΝ ΕΝΟΤΗΤΑ.
    //
    // Πριν, στον προορισμό «Ταινίες» έπαιρνε το `displayedChannels` αυτούσιο,
    // εμπιστευόμενο ότι εκεί υπάρχουν μόνο ταινίες. Δεν ισχύει ΟΣΟ ΦΟΡΤΩΝΕΙ: το
    // `state.channels` κρατά την ενότητα που κατέβηκε πρώτη, οπότε αν προλάβαινε
    // η Live, οι Ταινίες και οι Σειρές γέμιζαν κανάλια. Το φίλτρο κοστίζει ένα
    // πέρασμα και κάνει την υπόθεση περιττή.
    val displayedMovies = remember(displayedChannels, selectedDestination, movies) {
        if (selectedDestination == "movies") {
            HomeRailContentPolicy.moviesOf(displayedChannels)
        } else movies
    }
    val displayedSeries = remember(displayedChannels, selectedDestination, series) {
        if (selectedDestination == "series") {
            HomeRailContentPolicy.seriesOf(displayedChannels)
        } else series
    }
    // Οι προτάσεις δεν περνούν από το selectedCatalogGroup. Είναι ανεξάρτητο,
    // τυχαίο μείγμα του συνολικού catalog του τρέχοντος προορισμού.
    val suggestionItems = remember(channels, allChannels, selectedDestination) {
        val pool = when (selectedDestination) {
            "movies" -> HomeRailContentPolicy.moviesOf(channels)
            "series" -> channels.filter { it.kind == "series" }.ifEmpty {
                channels.filter { it.kind == "series_ep" }
                    .distinctBy { it.seriesId.ifBlank { it.name } }
            }
            else -> allChannels.filter { it.kind == "vod" || it.kind == "series" }
        }
        HomeRailContentPolicy.suggestions(
            items = pool,
            limit = pool.size,
            seed = Random.nextInt(),
        )
    }

    val categoriesFor: (String) -> List<String> = { id ->
        when (id) {
            HomeLayoutPolicy.LIVE -> CategoryLayoutPolicy.orderByTitle(
                HomeRailContentPolicy.categoriesOf(live), categoryTitlesInOrder
            ) { it }
            HomeLayoutPolicy.MOVIES -> CategoryLayoutPolicy.orderByTitle(
                HomeRailContentPolicy.categoriesOf(movies), categoryTitlesInOrder
            ) { it }
            HomeLayoutPolicy.SERIES -> CategoryLayoutPolicy.orderByTitle(
                HomeRailContentPolicy.categoriesOf(series), categoryTitlesInOrder
            ) { it }
            else -> emptyList()
        }
    }
    val categoryOf: (String) -> String = { id ->
        HomeRailContentPolicy.resolveCategory(store.homeRailCategory(id), categoriesFor(id))
    }

    lockedFeature?.let { feature ->
        PremiumRequiredDialog(feature = feature, onDismiss = { lockedFeature = null })
    }

    // Ο επεξεργαστής ανοίγει στην οθόνη που κοιτάς, αλλά μπορείς να αλλάξεις
    // προορισμό μέσα από αυτόν. Ξαναρχίζει από τον τρέχοντα κάθε φορά που
    // ανοίγει, ώστε να μη «θυμάται» ότι την προηγούμενη φορά ρύθμιζες αλλού.
    var editingDestination by remember(editing) { mutableStateOf(selectedDestination) }
    val editorEntries = remember(layoutVersion, editingDestination) {
        HomeLayoutPolicy.resolve(
            savedOrder = store.homeSectionOrder(editingDestination),
            hidden = store.homeHiddenSections(editingDestination),
            destination = editingDestination,
        )
    }

    if (editing) {
        MobileEditHomeScreen(
            entries = editorEntries,
            destination = editingDestination,
            onDestinationChange = { editingDestination = it },
            categoryOf = categoryOf,
            categoriesFor = categoriesFor,
            onToggleVisible = { id ->
                store.setHomeHiddenSections(
                    editingDestination,
                    HomeLayoutPolicy.toggle(store.homeHiddenSections(editingDestination), id),
                )
                layoutVersion++
            },
            onMove = { from, to ->
                store.setHomeSectionOrder(
                    editingDestination,
                    HomeLayoutPolicy.move(HomeLayoutPolicy.idsOf(editorEntries), from, to),
                )
                layoutVersion++
            },
            selectedCategoriesOf = { id -> store.homeRailCategories(editingDestination, id) },
            onPickCategories = { id, groups ->
                store.setHomeRailCategories(editingDestination, id, groups)
                layoutVersion++
            },
            onClear = { id -> onClearHistory(id); layoutVersion++ },
            onBack = { editing = false },
            modifier = modifier
        )
        return
    }

    expanded?.let { section ->
        MobilePremiumSectionScreen(
            section = section,
            favoriteKeys = favoriteKeys,
            selectedDestination = selectedDestination,
            onBack = { expanded = null },
            onOpen = onDetails,
            onToggleFavorite = onToggleFavorite,
            onOpenHome = { expanded = null; onOpenHome() },
            onOpenMovies = { expanded = null; onOpenMovies() },
            onOpenSeries = { expanded = null; onOpenSeries() },
            onOpenLive = { expanded = null; onOpenLive() },
            onOpenSearch = { expanded = null; onOpenSearch() },
            onOpenMyList = { expanded = null; onOpenMyList() },
            onOpenSettings = { expanded = null; onOpenSettings() },
            modifier = modifier
        )
        return
    }

    val railResolver = MobileHomeRailResolver(
        canSuggest = canSuggest,
        suggestions = suggestionItems,
        sections = displayedSections,
        recentLive = recentLive,
        live = live,
        movies = movies,
        series = series,
        displayedMovies = displayedMovies,
        displayedSeries = displayedSeries,
        selectedDestination = selectedDestination,
        selectedCatalogGroup = selectedCatalogGroup,
        categoryOf = categoryOf,
        // Οι επιλεγμένες κατηγορίες είναι ανά προορισμό: η ίδια ενότητα
        // «Ταινίες» μπορεί να δείχνει άλλες κατηγορίες στην Αρχική και άλλες
        // μέσα στις Ταινίες. Το `layoutVersion` είναι μέσα στο key ώστε μια
        // αλλαγή στον επεξεργαστή να φαίνεται αμέσως.
        selectedCategoriesOf = remember(layoutVersion, selectedDestination) {
            { sectionId -> store.homeRailCategories(selectedDestination, sectionId) }
        },
        sectionTitle = { id ->
            val section = HomeLayoutPolicy.DEFAULT.first { it.id == id }
            context.getString(section.titleRes())
        },
        categoryTitle = { label, category ->
            context.getString(R.string.home_section_with_category, label, category)
        },
    )

    val heroVisible = entries.any { it.section.id == HomeLayoutPolicy.HERO && it.visible }

    val listState = rememberLazyListState()
    val navigationCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
        }
    }
    // Η κεφαλίδα γίνεται συμπαγής μόλις το hero φύγει από την οθόνη — δηλαδή
    // μόλις περάσουμε το πρώτο item, ή μόλις το πρώτο item κυλήσει αρκετά.
    val headerSolid by remember(heroVisible) {
        derivedStateOf {
            !heroVisible ||
                listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 320
        }
    }

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Η κεφαλίδα πλέει ΠΑΝΩ από το hero — εκεί υπάρχει σκούρα εικόνα από
            // κάτω και το κείμενο διαβάζεται. Αν όμως ο χρήστης κρύψει το hero,
            // η επόμενη ενότητα ξεκινά από την κορυφή και μπαίνει από κάτω της:
            // τότε χρειάζεται κανονικό περιθώριο.
            contentPadding = PaddingValues(
                top = when {
                    heroVisible -> 0.dp
                    selectedDestination != "home" -> 126.dp
                    else -> 56.dp
                },
                bottom = premiumMobileNavigationContentPadding()
            )
        ) {
            if (showsCatalogCategories && !heroVisible) {
                item(key = "catalog-category-explorer") {
                    CatalogCategoryExplorer(
                        selectedDestination = selectedDestination,
                        options = catalogCategoryOptions,
                        selectedGroup = selectedCatalogGroup,
                        onSelectGroup = { selectedCatalogGroup = it },
                    )
                }
            }
            entries.filter { it.visible }.forEach { entry ->
                when (entry.section.id) {
                    // Η κεφαλίδα δεν είναι στοιχείο της λίστας: μένει από πάνω.
                    HomeLayoutPolicy.HEADER -> Unit

                    HomeLayoutPolicy.HERO -> {
                        item(key = "home-hero") {
                            MobilePremiumHomeHero(
                                channels = displayedChannels,
                                favoriteKeys = favoriteKeys,
                                profileName = profileName,
                                tmdbFor = tmdbFor,
                                onPlay = onPlay,
                                onDetails = onDetails,
                                onToggleFavorite = onToggleFavorite,
                                onSearch = onOpenSearch,
                                selectedDestination = selectedDestination,
                                onOpenMovies = onOpenMovies,
                                onOpenSeries = onOpenSeries,
                                onOpenCategories = onOpenCategories,
                                showChrome = false
                            )
                        }
                        if (showsCatalogCategories) {
                            item(key = "catalog-category-explorer") {
                                CatalogCategoryExplorer(
                                    selectedDestination = selectedDestination,
                                    options = catalogCategoryOptions,
                                    selectedGroup = selectedCatalogGroup,
                                    onSelectGroup = { selectedCatalogGroup = it },
                                )
                            }
                        }
                    }

                    else -> {
                        // Μία ενότητα μπορεί να δώσει ΠΟΛΛΕΣ ράγες: οι ενότητες
                        // κατηγοριών παράγουν μία ανά επιλεγμένη κατηγορία.
                        val rails = railResolver.railsFor(entry.section.id)
                        if (rails.isNotEmpty()) {
                            rails.forEach { rail ->
                                item(key = "home-rail:${rail.id}") {
                                    MobileHomeRail(
                                        rail = rail,
                                        favoriteKeys = favoriteKeys,
                                        onOpen = onDetails,
                                        onViewAll = { expanded = it.toCatalogSection() }
                                    )
                                }
                            }
                        } else if (entry.section.id == HomeLayoutPolicy.SUGGESTIONS) {
                            // ΜΟΝΟ οι προτάσεις κρατούν θέση όταν είναι άδειες: το
                            // κενό τους σημαίνει «δεν σε ξέρω ακόμη», και αυτό
                            // πρέπει να ειπωθεί. Ένα άδειο «Νέες ταινίες» δεν
                            // σημαίνει τίποτα — απλώς φεύγει.
                            item(key = "home-suggestions-empty") { SuggestionsEmptyState() }
                        }
                    }
                }
            }
        }

        MobileHomeHeader(
            solid = headerSolid,
            selectedDestination = selectedDestination,
            itemCount = downloadedItemCount,
            categoryCount = downloadedCategoryCount,
            onSectionBack = onSectionBack,
            onSort = onSort,
            onFavorites = onFavorites,
            onUpdateContents = onUpdateContents,
            // Η απόφαση παίρνεται ΕΔΩ, τη στιγμή του πατήματος, και όχι με μια
            // σιωπηλή συνθήκη παρακάτω: ή ανοίγει η οθόνη, ή εξηγείται γιατί όχι.
            onEditHome = {
                if (canEditHome) editing = true
                else lockedFeature = PremiumFeature.EDIT_HOME
            },
            onCategories = onOpenCategories,
            onExport = onExport,
            onSettings = onOpenSettings,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            MobileHomeBottomNavigation(
                selected = selectedDestination,
                onHome = onOpenHome,
                onMovies = onOpenMovies,
                onSeries = onOpenSeries,
                onLive = onOpenLive,
                onSearch = onOpenSearch,
                onMyList = onOpenMyList,
                onSettings = onOpenSettings,
                collapsed = navigationCollapsed
            )
        }
    }
}
