package com.prelude.iptv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.components.live.liveFilterOptions
import com.prelude.iptv.ui.components.live.liveVisibleChannels
import com.prelude.iptv.ui.components.live.rememberLiveNow
import com.prelude.iptv.ui.mobile.live.MobilePremiumLiveScreen
import com.prelude.iptv.ui.tv.live.TvPremiumLiveScreen

/** Adaptive entry point for the independent touch-first and DPAD-first Live TV UIs. */
@Composable
fun PremiumLiveTvScreen(
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    recentKeys: Set<String>,
    keyOf: (Channel) -> String,
    onPlay: (Channel) -> Unit,
    onEpg: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onMultiview: (Channel, Channel) -> Unit = { _, _ -> },
    onOpenHome: () -> Unit = {},
    onOpenMovies: () -> Unit = {},
    onOpenSeries: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenMyList: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) return
    val isTv = isTvDevice()
    var filterId by remember { mutableStateOf("all") }
    var selectedKey by remember(channels) { mutableStateOf(keyOf(channels.first())) }
    var multiviewPrimaryKey by remember(channels) { mutableStateOf<String?>(null) }
    val nowMs = rememberLiveNow()
    val filters = remember(channels) { liveFilterOptions(channels) }
    val visible = remember(channels, favoriteKeys, recentKeys, filterId) {
        liveVisibleChannels(channels, favoriteKeys, recentKeys, keyOf, filterId)
    }
    val selected = visible.firstOrNull { keyOf(it) == selectedKey }
        ?: visible.firstOrNull()
        ?: channels.first()

    LaunchedEffect(visible, selectedKey) {
        if (visible.none { keyOf(it) == selectedKey } && visible.isNotEmpty()) {
            selectedKey = keyOf(visible.first())
        }
    }

    val onSelect: (Channel) -> Unit = { selectedKey = keyOf(it) }
    val onTvOpen: (Channel) -> Unit = { channel ->
        when (val decision = MultiviewSelectionPolicy.onOpen(multiviewPrimaryKey, keyOf(channel))) {
            MultiviewSelectionPolicy.OpenDecision.PlaySingle -> onPlay(channel)
            MultiviewSelectionPolicy.OpenDecision.KeepPrimaryArmed -> selectedKey = keyOf(channel)
            is MultiviewSelectionPolicy.OpenDecision.Launch -> {
                val primary = channels.firstOrNull { keyOf(it) == decision.primaryKey }
                multiviewPrimaryKey = null
                if (primary != null) onMultiview(primary, channel) else onPlay(channel)
            }
        }
    }
    val onArmMultiview: (Channel) -> Unit = { channel ->
        selectedKey = keyOf(channel)
        multiviewPrimaryKey = keyOf(channel)
    }

    BackHandler(enabled = isTv && multiviewPrimaryKey != null) {
        multiviewPrimaryKey = null
    }
    val onFilter: (String) -> Unit = { next ->
        filterId = next
        val filtered = liveVisibleChannels(channels, favoriteKeys, recentKeys, keyOf, next)
        filtered.firstOrNull()?.let { selectedKey = keyOf(it) }
    }

    if (isTv) {
        TvPremiumLiveScreen(
            channels = visible,
            selected = selected,
            filters = filters,
            selectedFilterId = filterId,
            favoriteKeys = favoriteKeys,
            nowMs = nowMs,
            keyOf = keyOf,
            onSelect = onSelect,
            onFilter = onFilter,
            onPlay = onTvOpen,
            onEpg = onEpg,
            onToggleFavorite = onToggleFavorite,
            multiviewPrimaryKey = multiviewPrimaryKey,
            onArmMultiview = onArmMultiview,
            modifier = modifier
        )
    } else {
        MobilePremiumLiveScreen(
            channels = visible,
            allChannels = channels,
            selected = selected,
            filters = filters,
            selectedFilterId = filterId,
            favoriteKeys = favoriteKeys,
            nowMs = nowMs,
            keyOf = keyOf,
            onSelect = { channel ->
                selectedKey = keyOf(channel)
                onPlay(channel)
            },
            onFilter = onFilter,
            onPlay = onPlay,
            onEpg = onEpg,
            onToggleFavorite = onToggleFavorite,
            onOpenHome = onOpenHome,
            onOpenMovies = onOpenMovies,
            onOpenSeries = onOpenSeries,
            onOpenSearch = onOpenSearch,
            onOpenMyList = onOpenMyList,
            onOpenSettings = onOpenSettings,
            modifier = modifier
        )
    }
}
