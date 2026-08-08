package com.prelude.iptv.ui.mobile.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding
import com.prelude.iptv.ui.components.live.LiveFilterOption
import com.prelude.iptv.ui.mobile.home.MobileHomeBottomNavigation

@Composable
fun MobilePremiumLiveScreen(
    channels: List<Channel>,
    allChannels: List<Channel>,
    selected: Channel,
    filters: List<LiveFilterOption>,
    selectedFilterId: String,
    favoriteKeys: Set<String>,
    nowMs: Long,
    keyOf: (Channel) -> String,
    onSelect: (Channel) -> Unit,
    onFilter: (String) -> Unit,
    onPlay: (Channel) -> Unit,
    onEpg: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onOpenHome: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMyList: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = premiumMobileNavigationContentPadding())
        ) {
            item(key = "mobile-live-hero") {
                MobileLiveHero(
                    channel = selected,
                    nowMs = nowMs,
                    favorite = keyOf(selected) in favoriteKeys,
                    onPlay = { onPlay(selected) },
                    onEpg = { onEpg(selected) },
                    onFavorite = { onToggleFavorite(selected) },
                    onSearch = onOpenSearch
                )
            }
            item(key = "mobile-live-filters") {
                MobileLiveFilterRow(filters, selectedFilterId, onFilter)
            }
            item(key = "mobile-live-now") {
                MobileLiveRailSection(
                    title = "Ζωντανά τώρα",
                    subtitle = "${channels.size} κανάλια",
                    channels = channels,
                    selected = selected,
                    favoriteKeys = favoriteKeys,
                    nowMs = nowMs,
                    keyOf = keyOf,
                    onSelect = onSelect
                )
            }
            item(key = "mobile-live-sports") {
                MobileSportsRail(
                    channels = allChannels,
                    selected = selected,
                    favoriteKeys = favoriteKeys,
                    nowMs = nowMs,
                    keyOf = keyOf,
                    onSelect = onSelect
                )
            }
            item(key = "mobile-live-guide-header") {
                MobileQuickGuideHeader(
                    channelCount = channels.size,
                    onOpenEpg = { onEpg(selected) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                )
            }
            itemsIndexed(
                items = channels,
                key = { index, channel -> "mobile-live-guide:${keyOf(channel)}:$index" }
            ) { index, channel ->
                MobileQuickGuideRow(
                    channel = channel,
                    nowMs = nowMs,
                    selected = keyOf(channel) == keyOf(selected),
                    last = index == channels.lastIndex,
                    onSelect = { onSelect(channel) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            MobileHomeBottomNavigation(
                selected = "live",
                onHome = onOpenHome,
                onMovies = onOpenMovies,
                onSeries = onOpenSeries,
                onLive = {},
                onSearch = onOpenSearch,
                onMyList = onOpenMyList,
                onSettings = onOpenSettings,
                collapsed = listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 40
            )
        }
    }
}
