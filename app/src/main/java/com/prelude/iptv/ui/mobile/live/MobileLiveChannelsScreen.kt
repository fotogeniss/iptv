package com.prelude.iptv.ui.mobile.live

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prelude.iptv.R
import com.prelude.iptv.category.CategoryLayoutPolicy
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.mobile.home.MobileCategoryOption
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding

private const val OTHER_LIVE_GROUP_ID = "synthetic:other"

private data class MobileLiveGroup(
    val id: String,
    val title: String,
    val channels: List<Channel>,
)

/**
 * Mobile Live TV orchestration: owns screen state and selects the appropriate
 * category, search, list or grid content. Rendering lives in focused sibling
 * files so this screen remains readable as the feature grows.
 */
@Composable
fun MobileLiveChannelsScreen(
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    keyOf: (Channel) -> String,
    onPlay: (Channel) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    nowTextFor: (Channel) -> String? = { null },
    onOpenEpg: (() -> Unit)? = null,
    onNavigationCollapsedChange: (Boolean) -> Unit = {},
    categoryTitlesInOrder: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var openGroup by remember { mutableStateOf<String?>(null) }
    var categoryLayout by remember { mutableStateOf(LiveChannelLayout.LIST) }
    val railListState = rememberLazyListState()
    val channelListState = rememberLazyListState()
    val channelGridState = rememberLazyGridState()
    val otherGroupLabel = stringResource(R.string.live_other)
    val allChannelsLabel = stringResource(R.string.live_all)

    val groups = remember(channels, categoryTitlesInOrder, otherGroupLabel) {
        val grouped = channels.groupBy { it.group.trim() }
            .map { (providerTitle, items) ->
                MobileLiveGroup(
                    id = if (providerTitle.isBlank()) OTHER_LIVE_GROUP_ID else "provider:$providerTitle",
                    title = providerTitle.ifBlank { otherGroupLabel },
                    channels = items,
                )
            }
            .sortedByDescending { it.channels.size }
        CategoryLayoutPolicy.orderByTitle(grouped, categoryTitlesInOrder) { it.title }
    }
    val categoryOptions = remember(groups, channels.size, allChannelsLabel) {
        listOf(MobileCategoryOption("all", allChannelsLabel, channels.size)) +
            groups.map { group ->
                MobileCategoryOption("group:${group.id}", group.title, group.channels.size)
            }
    }
    val selectedGroup = remember(groups, openGroup) {
        openGroup?.let { selected -> groups.firstOrNull { it.id == selected } }
    }
    val selectedGroupChannels = selectedGroup?.channels
    val searchableChannels = selectedGroupChannels ?: channels
    val results = remember(searchableChannels, query) {
        if (query.isBlank()) emptyList()
        else searchableChannels.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    val navigationCollapsed = when {
        openGroup != null && categoryLayout == LiveChannelLayout.LIST ->
            channelListState.firstVisibleItemIndex > 0 || channelListState.firstVisibleItemScrollOffset > 40
        openGroup != null || query.isNotBlank() ->
            channelGridState.firstVisibleItemIndex > 0 || channelGridState.firstVisibleItemScrollOffset > 40
        else -> railListState.firstVisibleItemIndex > 0 || railListState.firstVisibleItemScrollOffset > 40
    }

    LaunchedEffect(navigationCollapsed) {
        onNavigationCollapsedChange(navigationCollapsed)
    }
    LaunchedEffect(openGroup) {
        query = ""
        if (openGroup != null) categoryLayout = LiveChannelLayout.LIST
        railListState.scrollToItem(0)
        channelListState.scrollToItem(0)
        channelGridState.scrollToItem(0)
    }
    LaunchedEffect(categoryLayout) {
        if (openGroup != null) {
            channelListState.scrollToItem(0)
            channelGridState.scrollToItem(0)
        }
    }

    BackHandler(enabled = openGroup != null || query.isNotBlank()) {
        when {
            query.isNotBlank() -> query = ""
            else -> openGroup = null
        }
    }

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            LiveHeader(
                title = selectedGroup?.title ?: stringResource(R.string.live_title),
                onBack = { if (openGroup != null) openGroup = null else onBack() },
                onOpenEpg = onOpenEpg,
                onSettings = onOpenSettings,
            )
            LiveSearchField(
                value = query,
                onChange = { query = it },
                onClear = { query = "" },
                categoryLayout = categoryLayout.takeIf { openGroup != null },
                onToggleLayout = {
                    categoryLayout = if (categoryLayout == LiveChannelLayout.LIST) {
                        LiveChannelLayout.GRID
                    } else {
                        LiveChannelLayout.LIST
                    }
                },
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(IptvColors.Divider))

            val bottomPadding = premiumMobileNavigationContentPadding()
            when {
                openGroup != null -> {
                    val categoryChannels = if (query.isBlank()) selectedGroupChannels.orEmpty() else results
                    if (categoryChannels.isEmpty()) {
                        EmptyLive(
                            if (query.isBlank()) stringResource(R.string.live_empty_category)
                            else stringResource(R.string.live_empty_search, query.trim())
                        )
                    } else if (categoryLayout == LiveChannelLayout.LIST) {
                        ChannelList(
                            channels = categoryChannels,
                            favoriteKeys = favoriteKeys,
                            keyOf = keyOf,
                            onPlay = onPlay,
                            nowTextFor = nowTextFor,
                            bottomPadding = bottomPadding,
                            state = channelListState,
                        )
                    } else {
                        ChannelGrid(
                            channels = categoryChannels,
                            favoriteKeys = favoriteKeys,
                            keyOf = keyOf,
                            onPlay = onPlay,
                            nowTextFor = nowTextFor,
                            bottomPadding = bottomPadding,
                            state = channelGridState,
                        )
                    }
                }

                query.isNotBlank() -> {
                    if (results.isEmpty()) {
                        EmptyLive(stringResource(R.string.live_empty_search, query.trim()))
                    } else {
                        ChannelGrid(
                            channels = results,
                            favoriteKeys = favoriteKeys,
                            keyOf = keyOf,
                            onPlay = onPlay,
                            nowTextFor = nowTextFor,
                            bottomPadding = bottomPadding,
                            state = channelGridState
                        )
                    }
                }

                else -> {
                    if (groups.isEmpty()) {
                        EmptyLive(stringResource(R.string.live_empty_categories))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = railListState,
                            contentPadding = PaddingValues(bottom = bottomPadding),
                        ) {
                            item(key = "live-category-explorer") {
                                LiveCategoryExplorer(
                                    options = categoryOptions,
                                    selectedGroup = openGroup,
                                    onSelectGroup = { openGroup = it },
                                )
                            }
                            items(groups, key = { "live-group:${it.id}" }) { group ->
                                LiveCategorySection(
                                    title = group.title,
                                    channels = group.channels,
                                    favoriteKeys = favoriteKeys,
                                    keyOf = keyOf,
                                    nowTextFor = nowTextFor,
                                    onPlay = onPlay,
                                    onSeeAll = { openGroup = group.id },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
