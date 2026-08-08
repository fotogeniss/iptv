package com.prelude.iptv.ui.mobile.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prelude.iptv.ui.mobile.navigation.PremiumMobileBottomNavigation

/** Backwards-compatible wrapper around the single shared mobile app navigation. */
@Composable
internal fun MobileHomeBottomNavigation(
    selected: String = "home",
    onHome: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onLive: () -> Unit,
    onSearch: () -> Unit,
    onMyList: () -> Unit,
    onSettings: () -> Unit,
    collapsed: Boolean = false,
    modifier: Modifier = Modifier
) {
    PremiumMobileBottomNavigation(
        selected = selected,
        onHome = onHome,
        onMovies = onMovies,
        onSeries = onSeries,
        onLive = onLive,
        onSearch = onSearch,
        onMyList = onMyList,
        onSettings = onSettings,
        collapsed = collapsed,
        modifier = modifier
    )
}
