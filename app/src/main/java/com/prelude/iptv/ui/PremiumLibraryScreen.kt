package com.prelude.iptv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.components.library.PremiumLibraryContent
import com.prelude.iptv.ui.mobile.library.MobilePremiumLibraryScreen
import com.prelude.iptv.ui.tv.library.TvPremiumLibraryScreen

enum class LibraryDestination {
    SEARCH,
    MY_LIST,
    CONTINUE_WATCHING,
    HISTORY
}

/** Adaptive entry point for Search and the premium Library hub. */
@Composable
fun PremiumLibraryScreen(
    destination: LibraryDestination,
    items: List<Channel>,
    searchUniverse: List<Channel> = items,
    content: PremiumLibraryContent = PremiumLibraryContent(),
    favoriteKeys: Set<String>,
    progress: Map<String, Float>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (Channel) -> Unit,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onRemove: (LibraryDestination, Channel) -> Unit,
    onDestinationChange: (LibraryDestination) -> Unit = {},
    onOpenHome: () -> Unit = {},
    onOpenMovies: () -> Unit = {},
    onOpenSeries: () -> Unit = {},
    onOpenLive: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onVoiceSearch: () -> Unit = {},
    tmdbFor: suspend (Channel) -> TmdbClient.Meta? = { null },
    modifier: Modifier = Modifier
) {
    if (destination == LibraryDestination.SEARCH) {
        AdaptiveSearchScreen(
            allItems = searchUniverse,
            results = items,
            favoriteKeys = favoriteKeys,
            progress = progress,
            query = query,
            onQueryChange = onQueryChange,
            onBack = onBack,
            onOpen = onOpen,
            onToggleFavorite = onToggleFavorite,
            onVoiceSearch = onVoiceSearch,
            onOpenHome = onOpenHome,
            onOpenMovies = onOpenMovies,
            onOpenSeries = onOpenSeries,
            onOpenLive = onOpenLive,
            onOpenMyList = { onDestinationChange(LibraryDestination.MY_LIST) },
            onOpenSettings = onOpenSettings,
            tmdbFor = tmdbFor,
            modifier = modifier
        )
        return
    }

    if (isTvDevice()) {
        TvPremiumLibraryScreen(
            initialDestination = destination,
            content = content,
            favoriteKeys = favoriteKeys,
            onBack = onBack,
            onOpen = onOpen,
            onPlay = onPlay,
            onToggleFavorite = onToggleFavorite,
            onRemove = onRemove,
            onDestinationChange = onDestinationChange,
            tmdbFor = tmdbFor,
            modifier = modifier
        )
    } else {
        MobilePremiumLibraryScreen(
            initialDestination = destination,
            content = content,
            favoriteKeys = favoriteKeys,
            onBack = onBack,
            onOpen = onOpen,
            onPlay = onPlay,
            onToggleFavorite = onToggleFavorite,
            onRemove = onRemove,
            onDestinationChange = onDestinationChange,
            onOpenHome = onOpenHome,
            onOpenMovies = onOpenMovies,
            onOpenSeries = onOpenSeries,
            onOpenLive = onOpenLive,
            onOpenSettings = onOpenSettings,
            tmdbFor = tmdbFor,
            modifier = modifier
        )
    }
}
