package com.prelude.iptv.ui.mobile.live

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.category.CategoryLayoutPolicy
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.epg.rememberEpgNow
import com.prelude.iptv.ui.design.HorizonSearchField
import com.prelude.iptv.ui.design.foldForSearch
import com.prelude.iptv.ui.icons.automirrored.filled.ArrowBack
import com.prelude.iptv.ui.icons.filled.CalendarMonth
import com.prelude.iptv.ui.icons.filled.Category
import com.prelude.iptv.ui.icons.filled.ChevronRight
import com.prelude.iptv.ui.icons.filled.Search
import com.prelude.iptv.ui.icons.filled.Star
import com.prelude.iptv.ui.icons.filled.Tune
import com.prelude.iptv.ui.mobile.navigation.MobileSettingsAction
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding

private const val OTHER_LIVE_GROUP_ID = "synthetic:other"
private const val TAB_ALL = "all"
private const val TAB_FAVORITES = "favorites"

private data class MobileLiveGroup(
    val id: String,
    val title: String,
    val channels: List<Channel>,
)

/**
 * Live στο κινητό, ανακατασκευή κατά το prototypes/redesign_live.html.
 *
 * Οδηγός καναλιών αντί για βιτρίνα: πάνω «Συνέχισε» με τα τελευταία κανάλια,
 * καρτέλες κατηγοριών που μένουν κολλημένες στην κορυφή, και μία κάθετη λίστα
 * όπου κάθε κανάλι δείχνει τι παίζει, πρόοδο και τι ακολουθεί. Όλες οι
 * κατηγορίες ανοίγουν σε φύλλο με αναζήτηση, όχι σε νέα οθόνη.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MobileLiveChannelsScreen(
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    keyOf: (Channel) -> String,
    onPlay: (Channel) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCategories: () -> Unit,
    nowTextFor: (Channel) -> String? = { null },
    onOpenEpg: (() -> Unit)? = null,
    onNavigationCollapsedChange: (Boolean) -> Unit = {},
    categoryTitlesInOrder: List<String> = emptyList(),
    recentChannels: List<Channel> = emptyList(),
    playingKey: String? = null,
    onToggleFavorite: (Channel) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(TAB_ALL) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sheetOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val nowMs = rememberEpgNow()
    val otherGroupLabel = stringResource(R.string.live_other)

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
    val favorites = remember(channels, favoriteKeys) { channels.filter { keyOf(it) in favoriteKeys } }
    val tabChannels = when (tab) {
        TAB_ALL -> channels
        TAB_FAVORITES -> favorites
        else -> groups.firstOrNull { it.id == tab }?.channels ?: channels
    }
    val needle = foldForSearch(query.trim())
    val shown = remember(tabChannels, needle) {
        if (needle.isEmpty()) tabChannels else tabChannels.filter { foldForSearch(it.name).contains(needle) }
    }
    val recents = remember(recentChannels, channels) {
        val available = channels.associateBy(keyOf)
        recentChannels.mapNotNull { available[keyOf(it)] }.take(8)
    }

    LaunchedEffect(listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40) {
        onNavigationCollapsedChange(listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40)
    }
    LaunchedEffect(tab) {
        // Η λίστα ξεκινά από την αρχή όταν αλλάζει κατηγορία· οι καρτέλες μένουν ορατές.
        if (listState.firstVisibleItemIndex > 1) listState.scrollToItem(1)
    }
    // Αν χαθεί η κατηγορία (αλλαγή πηγής), γυρνάμε στο «Όλα».
    LaunchedEffect(groups) {
        if (tab != TAB_ALL && tab != TAB_FAVORITES && groups.none { it.id == tab }) tab = TAB_ALL
    }

    BackHandler(enabled = searchOpen || tab != TAB_ALL) {
        when {
            searchOpen -> { searchOpen = false; query = "" }
            else -> tab = TAB_ALL
        }
    }

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = premiumMobileNavigationContentPadding() + 8.dp),
        ) {
            item(key = "live-top") {
                LiveTopBar(
                    onBack = onBack,
                    onSearch = { searchOpen = !searchOpen; if (!searchOpen) query = "" },
                    onOpenEpg = onOpenEpg,
                    onSettings = onOpenSettings,
                )
                if (searchOpen) {
                    HorizonSearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = stringResource(R.string.live_search_hint),
                        clearLabel = stringResource(R.string.live_clear_search),
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 6.dp),
                    )
                }
            }
            if (recents.isNotEmpty() && needle.isEmpty()) {
                item(key = "live-continue") {
                    LiveSectionLabel(stringResource(R.string.live_continue))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recents, key = { "recent:${keyOf(it)}" }) { channel ->
                            LiveContinueCard(channel, nowMs) { onPlay(channel) }
                        }
                    }
                }
            }
            stickyHeader(key = "live-tabs") {
                LazyRow(
                    Modifier.fillMaxWidth().background(IptvColors.Background).padding(vertical = 10.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(key = "tab-all") {
                        LiveCategoryTab(stringResource(R.string.live_all), channels.size, tab == TAB_ALL) { tab = TAB_ALL }
                    }
                    if (favorites.isNotEmpty()) {
                        item(key = "tab-fav") {
                            LiveCategoryTab(
                                stringResource(R.string.live_favorites),
                                favorites.size,
                                tab == TAB_FAVORITES,
                                leading = {
                                    Icon(Icons.Default.Star, null, tint = IptvColors.Warning, modifier = Modifier.size(14.dp))
                                },
                            ) { tab = TAB_FAVORITES }
                        }
                    }
                    items(groups.take(12), key = { "tab:${it.id}" }) { group ->
                        LiveCategoryTab(group.title, group.channels.size, tab == group.id) { tab = group.id }
                    }
                    item(key = "tab-more") {
                        LiveCategoryTab(
                            stringResource(R.string.live_categories_more),
                            null,
                            selected = false,
                            dashed = true,
                            leading = {
                                Icon(Icons.Default.Category, null, tint = IptvColors.TextSecondary, modifier = Modifier.size(15.dp))
                            },
                        ) { sheetOpen = true }
                    }
                }
            }
            if (shown.isEmpty()) {
                item(key = "live-empty") {
                    Text(
                        if (needle.isEmpty()) stringResource(R.string.live_empty_category)
                        else stringResource(R.string.live_empty_search, query.trim()),
                        color = IptvColors.TextTertiary,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp)
                    )
                }
            }
            itemsIndexed(shown, key = { index, channel -> "live-row:${keyOf(channel)}:$index" }) { index, channel ->
                val key = keyOf(channel)
                LiveGuideRow(
                    number = index + 1,
                    channel = channel,
                    nowMs = nowMs,
                    playing = key == playingKey,
                    favorite = key in favoriteKeys,
                    onPlay = { onPlay(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) },
                )
            }
        }
    }

    if (sheetOpen) {
        LiveCategoriesSheet(
            allCount = channels.size,
            favoritesCount = favorites.size,
            groups = groups,
            selected = tab,
            onSelect = { tab = it; sheetOpen = false },
            onEditCategories = { sheetOpen = false; onOpenCategories() },
            onDismiss = { sheetOpen = false },
        )
    }
}

@Composable
private fun LiveTopBar(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenEpg: (() -> Unit)?,
    onSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 10.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.live_back), tint = IptvColors.TextPrimary)
        }
        Text(
            stringResource(R.string.live_title),
            color = IptvColors.TextPrimary,
            fontFamily = AppFonts.Display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearch) {
            Icon(Icons.Default.Search, stringResource(R.string.live_search), tint = IptvColors.TextPrimary)
        }
        if (onOpenEpg != null) {
            IconButton(onClick = onOpenEpg) {
                Icon(Icons.Default.CalendarMonth, stringResource(R.string.live_guide), tint = IptvColors.TextPrimary)
            }
        }
        MobileSettingsAction(onClick = onSettings, modifier = Modifier.size(40.dp))
    }
}

/** Όλες οι κατηγορίες σε φύλλο από κάτω, με αναζήτηση και πλήθος. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveCategoriesSheet(
    allCount: Int,
    favoritesCount: Int,
    groups: List<MobileLiveGroup>,
    selected: String,
    onSelect: (String) -> Unit,
    onEditCategories: () -> Unit,
    onDismiss: () -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    val needle = foldForSearch(filter.trim())
    val visible = remember(groups, needle) {
        if (needle.isEmpty()) groups else groups.filter { foldForSearch(it.title).contains(needle) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.Surface,
        contentColor = IptvColors.TextPrimary,
    ) {
        Text(
            stringResource(R.string.live_categories_title),
            fontFamily = AppFonts.Display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            color = IptvColors.TextPrimary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
        )
        HorizonSearchField(
            value = filter,
            onValueChange = { filter = it },
            placeholder = stringResource(R.string.live_find_category),
            clearLabel = stringResource(R.string.live_clear_search),
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (needle.isEmpty()) {
                item(key = "sheet-all") {
                    SheetCategoryRow(stringResource(R.string.live_all_channels), allCount, selected == TAB_ALL) { onSelect(TAB_ALL) }
                }
                if (favoritesCount > 0) {
                    item(key = "sheet-fav") {
                        SheetCategoryRow(stringResource(R.string.live_favorites), favoritesCount, selected == TAB_FAVORITES) {
                            onSelect(TAB_FAVORITES)
                        }
                    }
                }
            }
            items(visible, key = { "sheet:${it.id}" }) { group ->
                SheetCategoryRow(group.title, group.channels.size, selected == group.id) { onSelect(group.id) }
            }
            item(key = "sheet-edit") {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onEditCategories).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, null, tint = IptvColors.TextSecondary, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(
                        stringResource(R.string.settings_edit_categories),
                        color = IptvColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetCategoryRow(title: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = if (selected) IptvColors.Glow else IptvColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(count.toString(), color = IptvColors.TextTertiary, fontSize = 13.sp)
        Spacer(Modifier.size(6.dp))
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = IptvColors.TextTertiary,
            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
        )
    }
}
