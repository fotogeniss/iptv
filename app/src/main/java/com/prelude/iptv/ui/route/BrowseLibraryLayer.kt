package com.prelude.iptv.ui.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.LibraryPolicy
import com.prelude.iptv.ui.MainViewModel
import com.prelude.iptv.ui.LibraryDestination
import com.prelude.iptv.ui.PremiumLibraryScreen
import com.prelude.iptv.ui.components.library.PremiumLibraryContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Οι οθόνες της βιβλιοθήκης: αναζήτηση, Η λίστα μου, Συνέχισε, Ιστορικό.
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΟ: μέσα στο [BrowseRoute] ήταν 130 γραμμές που δεν είχαν σχέση
 * με την περιήγηση καταλόγου — υπολογισμοί λιστών, πρόοδος, αναζήτηση εκτός
 * κύριου νήματος. Ζούσαν εκεί μόνο επειδή εκεί βρισκόταν η μεταβλητή που κρατά
 * ποια οθόνη βιβλιοθήκης είναι ανοιχτή.
 *
 * Οι βαριές λίστες υπολογίζονται εδώ, όχι στον καλούντα: έτσι δεν ξαναχτίζονται
 * όταν αλλάξει κάτι άσχετο στην περιήγηση.
 */
@Composable
internal fun BrowseLibraryLayer(
    destination: LibraryDestination,
    vm: MainViewModel,
    /** Η ορατή λίστα καταλόγου — κλειδί ακυρότητας, όχι περιεχόμενο προβολής. */
    visibleChannels: List<Channel>,
    sessionChannels: List<Channel>,
    favoriteKeys: Set<String>,
    recentsVersion: Int,
    query: String,
    debouncedQuery: String,
    isTv: Boolean,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onOpenDetails: (Channel) -> Unit,
    onPlay: (Channel) -> Unit,
    onDestinationChange: (LibraryDestination) -> Unit,
    onOpenSection: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val continuePairs = remember(recentsVersion) { vm.continueWatching() }
    val myListItems = remember(favoriteKeys, sessionChannels, recentsVersion) {
        vm.favoriteLibraryItems()
    }
    val continueItems = remember(continuePairs) { continuePairs.map { it.first } }
    val historyItems = remember(recentsVersion, sessionChannels) { vm.historyItems() }
    val allLibraryItems = remember(myListItems, continueItems, historyItems) {
        LibraryPolicy.unique(myListItems + continueItems + historyItems)
    }
    val allLibraryProgress = remember(allLibraryItems, continuePairs, recentsVersion) {
        vm.watchProgress(allLibraryItems).mapValues { it.value.fraction } +
            continuePairs.associate { vm.favKey(it.first) to it.second }
    }
    val libraryContent = remember(myListItems, continueItems, historyItems, allLibraryProgress) {
        PremiumLibraryContent(
            myList = myListItems,
            continueWatching = continueItems,
            history = historyItems,
            progress = allLibraryProgress
        )
    }

    // Το ακριβό μέρος της αναζήτησης —ξαναχτίσιμο υποψηφίων, κανονικοποίηση,
    // ανάγνωση δίσκου— ΔΕΝ εξαρτάται από το τι πληκτρολογείς: υπολογίζεται μία
    // φορά, και ΕΚΤΟΣ κύριου νήματος. Πριν έτρεχε σε κάθε recomposition πάνω στο
    // main thread και η πληκτρολόγηση σερνόταν.
    val searchUniverseItems by produceState(
        initialValue = allLibraryItems,
        destination, visibleChannels, recentsVersion, allLibraryItems
    ) {
        value = if (destination == LibraryDestination.SEARCH) {
            withContext(Dispatchers.Default) { vm.searchUniverse() }
        } else allLibraryItems
    }

    // Και το φιλτράρισμα ανά ερώτημα τρέχει εκτός κύριου νήματος, μετά το
    // debounce — ώστε να μη σέρνεται ούτε σε τεράστιους καταλόγους.
    var searchResults by remember { mutableStateOf<List<Channel>>(emptyList()) }
    LaunchedEffect(destination, debouncedQuery, searchUniverseItems) {
        searchResults = if (destination == LibraryDestination.SEARCH) {
            withContext(Dispatchers.Default) {
                vm.searchInUniverse(searchUniverseItems, debouncedQuery)
            }
        } else emptyList()
    }

    val libraryItems = remember(destination, searchResults, myListItems, continueItems, historyItems) {
        when (destination) {
            LibraryDestination.SEARCH -> searchResults
            LibraryDestination.MY_LIST -> myListItems
            LibraryDestination.CONTINUE_WATCHING -> continueItems
            LibraryDestination.HISTORY -> historyItems
        }
    }
    val libraryProgress = remember(destination, libraryItems, allLibraryProgress, recentsVersion) {
        if (destination == LibraryDestination.SEARCH) {
            vm.watchProgress(libraryItems).mapValues { it.value.fraction }
        } else allLibraryProgress
    }

    PremiumLibraryScreen(
        destination = destination,
        items = libraryItems,
        searchUniverse = searchUniverseItems,
        content = libraryContent,
        favoriteKeys = favoriteKeys,
        progress = libraryProgress,
        query = query,
        onQueryChange = onQueryChange,
        onBack = onClose,
        onOpen = { ch ->
            when (ch.kind) {
                "series", "vod", "movie" -> onOpenDetails(ch)
                else -> onPlay(ch)
            }
        },
        onPlay = { ch ->
            // Μια σειρά δεν «παίζει»: ανοίγει, για να διαλέξεις επεισόδιο.
            if (ch.kind == "series") onOpenDetails(ch) else onPlay(ch)
        },
        onToggleFavorite = vm::toggleFavorite,
        onVoiceSearch = onVoiceSearch,
        tmdbFor = vm::tmdb,
        onDestinationChange = onDestinationChange,
        onOpenHome = { onOpenSection("home") },
        onOpenMovies = { onOpenSection("movies") },
        onOpenSeries = { onOpenSection("series") },
        onOpenLive = { onOpenSection("live") },
        onOpenSettings = onOpenSettings,
        onRemove = { target, ch ->
            when (target) {
                LibraryDestination.MY_LIST -> vm.toggleFavorite(ch)
                LibraryDestination.CONTINUE_WATCHING -> vm.clearWatchProgress(ch)
                LibraryDestination.HISTORY -> vm.removeHistoryItem(ch)
                LibraryDestination.SEARCH -> Unit
            }
        },
        modifier = Modifier.fillMaxSize().padding(start = if (isTv) 74.dp else 0.dp)
    )
}
