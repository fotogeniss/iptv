package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.billing.PremiumTier
import com.prelude.iptv.ui.CatalogRailLabels
import com.prelude.iptv.ui.home.HomeLayoutPolicy
import com.prelude.iptv.ui.home.HomeSection

@StringRes
fun HomeSection.titleRes(): Int = when (id) {
    HomeLayoutPolicy.HEADER -> R.string.home_section_header
    HomeLayoutPolicy.HERO -> R.string.home_section_hero
    HomeLayoutPolicy.SUGGESTIONS -> R.string.home_section_suggestions
    HomeLayoutPolicy.CONTINUE -> R.string.home_section_continue
    HomeLayoutPolicy.RECENT_LIVE -> R.string.home_section_recent_live
    HomeLayoutPolicy.NEW_LIVE -> R.string.home_section_new_live
    HomeLayoutPolicy.NEW_MOVIES -> R.string.home_section_new_movies
    HomeLayoutPolicy.NEW_EPISODES -> R.string.home_section_new_episodes
    HomeLayoutPolicy.TOP_MOVIES -> R.string.home_section_top_movies
    HomeLayoutPolicy.TOP_SERIES -> R.string.home_section_top_series
    HomeLayoutPolicy.LIVE -> R.string.home_section_live
    HomeLayoutPolicy.MOVIES -> R.string.home_section_movies
    HomeLayoutPolicy.SERIES -> R.string.home_section_series
    else -> error("Unknown Home section id: $id")
}

@StringRes
fun PremiumTier.labelRes(): Int = when (this) {
    PremiumTier.FREE -> R.string.home_tier_free
    PremiumTier.PREMIUM -> R.string.home_tier_premium
}

@Composable
fun catalogRailLabels(): CatalogRailLabels = CatalogRailLabels(
    continueWatching = stringResource(R.string.home_section_continue),
    myList = stringResource(R.string.home_section_my_list),
    trending = stringResource(R.string.home_section_trending),
    newReleases = stringResource(R.string.home_section_new_releases),
)
