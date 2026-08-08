package com.prelude.iptv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.mobile.home.MobilePremiumHomeScreen
import com.prelude.iptv.ui.tv.home.TvPremiumHomeScreen
import com.prelude.iptv.ui.localization.catalogRailLabels

/**
 * Adaptive dispatcher for the premium Home experience.
 *
 * Mobile and Android TV intentionally own independent compositions. Only data,
 * catalog policy and callbacks are shared between the two surfaces.
 */
@Composable
fun AdaptiveCatalogHome(
    channels: List<Channel>,
    continueWatching: List<Pair<Channel, Float>>,
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
    /**
     * Ολόκληρος ο κατάλογος, χωρίς τα φίλτρα της τρέχουσας ενότητας.
     *
     * Η αρχική κινητού μετρά ζωντανά/ταινίες/σειρές και φτιάχνει rails ζωντανών —
     * δουλειές που χρειάζονται κανάλια τα οποία η ενότητα έχει ήδη πετάξει.
     */
    allChannels: List<Channel> = channels,
    /** Κανάλια που είδε πρόσφατα, νεότερο πρώτο. */
    recentLive: List<Channel> = emptyList(),
    onClearHistory: (String) -> Unit = {},
    onUpdateContents: () -> Unit = {},
    onExport: () -> Unit = {},
    categoryTitlesInOrder: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) return

    val railLabels = catalogRailLabels()
    val sections = remember(channels, favoriteKeys, continueWatching, railLabels) {
        buildCatalogRailSections(channels, favoriteKeys, continueWatching, railLabels).sortedBy { section ->
            when (section.id) {
                "continue" -> 0
                "trending" -> 1
                "new" -> 2
                "my-list" -> 3
                else -> 4
            }
        }
    }

    if (isTvDevice()) {
        TvPremiumHomeScreen(
            channels = channels,
            sections = sections,
            favoriteKeys = favoriteKeys,
            profileName = profileName,
            tmdbFor = tmdbFor,
            onPlay = onPlay,
            onDetails = onDetails,
            onToggleFavorite = onToggleFavorite,
            modifier = modifier
        )
    } else {
        MobilePremiumHomeScreen(
            channels = channels,
            sections = sections,
            favoriteKeys = favoriteKeys,
            profileName = profileName,
            tmdbFor = tmdbFor,
            onPlay = onPlay,
            onDetails = onDetails,
            onToggleFavorite = onToggleFavorite,
            selectedDestination = selectedDestination,
            onOpenHome = onOpenHome,
            onOpenMovies = onOpenMovies,
            onOpenSeries = onOpenSeries,
            onOpenLive = onOpenLive,
            onOpenSearch = onOpenSearch,
            onOpenMyList = onOpenMyList,
            onOpenSettings = onOpenSettings,
            onOpenCategories = onOpenCategories,
            allChannels = allChannels,
            recentLive = recentLive,
            onClearHistory = onClearHistory,
            onUpdateContents = onUpdateContents,
            onExport = onExport,
            categoryTitlesInOrder = categoryTitlesInOrder,
            modifier = modifier
        )
    }
}
