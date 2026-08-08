package com.prelude.iptv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.mobile.search.MobilePremiumSearchScreen
import com.prelude.iptv.ui.tv.search.TvPremiumSearchScreen

/** Routes the shared search contract to independent touch-first and DPAD-first UIs. */
@Composable
fun AdaptiveSearchScreen(
    allItems: List<Channel>,
    results: List<Channel>,
    favoriteKeys: Set<String>,
    progress: Map<String, Float>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onVoiceSearch: () -> Unit = {},
    onOpenHome: () -> Unit = {},
    onOpenMovies: () -> Unit = {},
    onOpenSeries: () -> Unit = {},
    onOpenLive: () -> Unit = {},
    onOpenMyList: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    tmdbFor: suspend (Channel) -> TmdbClient.Meta? = { null },
    modifier: Modifier = Modifier
) {
    if (isTvDevice()) {
        TvPremiumSearchScreen(
            allItems = allItems,
            results = results,
            favoriteKeys = favoriteKeys,
            progress = progress,
            query = query,
            onQueryChange = onQueryChange,
            onBack = onBack,
            onOpen = onOpen,
            onToggleFavorite = onToggleFavorite,
            onVoiceSearch = onVoiceSearch,
            tmdbFor = tmdbFor,
            modifier = modifier
        )
    } else {
        MobilePremiumSearchScreen(
            allItems = allItems,
            results = results,
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
            onOpenMyList = onOpenMyList,
            onOpenSettings = onOpenSettings,
            tmdbFor = tmdbFor,
            modifier = modifier
        )
    }
}
