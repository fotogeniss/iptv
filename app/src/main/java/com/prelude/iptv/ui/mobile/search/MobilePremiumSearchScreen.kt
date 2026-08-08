package com.prelude.iptv.ui.mobile.search

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.PremiumSearchFilter
import com.prelude.iptv.ui.SearchUiPolicy
import com.prelude.iptv.ui.components.search.PremiumSearchEmpty
import com.prelude.iptv.ui.components.search.rememberSearchMeta
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.localizedSearchHeading
import com.prelude.iptv.ui.mobile.navigation.PremiumMobileBottomNavigation
import com.prelude.iptv.ui.mobile.navigation.MobileSettingsAction
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding
import kotlinx.coroutines.launch

@Composable
fun MobilePremiumSearchScreen(
    allItems: List<Channel>,
    results: List<Channel>,
    favoriteKeys: Set<String>,
    progress: Map<String, Float>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onVoiceSearch: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenMyList: () -> Unit,
    onOpenSettings: () -> Unit,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(PremiumSearchFilter.ALL) }
    val recents = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val source = remember(allItems, results, query) { if (query.isBlank()) allItems else results }
    val filtered = remember(source, filter, query) {
        val matched = SearchUiPolicy.filter(source, filter)
        if (query.isBlank()) SearchUiPolicy.discovery(matched, 60) else matched
    }
    var selected by remember { mutableStateOf(filtered.firstOrNull()) }
    LaunchedEffect(filtered) {
        val key = selected?.let(PlaybackQueue::favKey)
        selected = filtered.firstOrNull { PlaybackQueue.favKey(it) == key } ?: filtered.firstOrNull()
    }
    val meta by rememberSearchMeta(selected, tmdbFor)
    val suggestions = remember(allItems) { SearchUiPolicy.suggestions(allItems, 8) }
    val navigationContentPadding = premiumMobileNavigationContentPadding()

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
    Column(Modifier.fillMaxSize()) {
        MobileSearchTopBar(
            query = query,
            onQueryChange = onQueryChange,
            onBack = onBack,
            onVoiceSearch = onVoiceSearch,
            onSettings = onOpenSettings,
            onCommit = {
                query.trim().takeIf(String::isNotBlank)?.let {
                    recents.remove(it); recents.add(0, it)
                    while (recents.size > 5) recents.removeAt(recents.lastIndex)
                }
            }
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PremiumSearchFilter.entries) { item ->
                MobileSearchFilterChip(item, selected = filter == item) { filter = item }
            }
        }

        if (filtered.isEmpty()) {
            PremiumSearchEmpty(query, Modifier.fillMaxSize())
            return@Column
        }

        val showRecent = query.isBlank() && (recents.isNotEmpty() || suggestions.isNotEmpty())
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 9.dp, bottom = navigationContentPadding),
            verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (showRecent) {
                item {
                    MobileRecentSearches(
                        labels = if (recents.isNotEmpty()) recents else suggestions.take(5),
                        title = if (recents.isNotEmpty()) {
                            stringResource(R.string.search_recent)
                        } else {
                            stringResource(R.string.search_popular)
                        },
                        onSelect = onQueryChange,
                        showClear = recents.isNotEmpty(),
                        onClear = { recents.clear() }
                    )
                }
            }
            selected?.let { channel ->
                item {
                    MobileSearchFeatured(
                        channel = channel,
                        meta = meta,
                        favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                        progress = progress[PlaybackQueue.favKey(channel)],
                        onOpen = {
                            query.trim().takeIf(String::isNotBlank)?.let {
                                recents.remove(it); recents.add(0, it)
                            }
                            onOpen(channel)
                        },
                        onToggleFavorite = { onToggleFavorite(channel) }
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(
                        localizedSearchHeading(SearchUiPolicy.heading(query, filter)),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        pluralStringResource(R.plurals.search_title_count, filtered.size, filtered.size),
                        color = IptvColors.TextTertiary,
                        fontSize = 11.sp,
                    )
                }
            }
            itemsIndexed(
                filtered.chunked(2),
                key = { index, _ -> "search-row:$index" }
            ) { _, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    row.forEach { channel ->
                        MobileSearchResultCard(
                            channel = channel,
                            favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                            progress = progress[PlaybackQueue.favKey(channel)],
                            onSelect = {
                                selected = channel
                                scope.launch { listState.animateScrollToItem(if (showRecent) 1 else 0) }
                            },
                            onToggleFavorite = { onToggleFavorite(channel) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
        PremiumMobileBottomNavigation(
            selected = "search",
            onHome = onOpenHome,
            onMovies = onOpenMovies,
            onSeries = onOpenSeries,
            onLive = onOpenLive,
            onSearch = {},
            onMyList = onOpenMyList,
            onSettings = onOpenSettings,
            collapsed = listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 40,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MobileSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onVoiceSearch: () -> Unit,
    onSettings: () -> Unit,
    onCommit: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(IptvColors.Background.copy(alpha = .96f))
            .padding(start = 8.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.search_back), tint = Color.White)
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = IptvColors.TextSecondary) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotBlank()) IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, stringResource(R.string.search_clear), tint = Color.White)
                    }
                    IconButton(onClick = onVoiceSearch) {
                        Icon(Icons.Default.Mic, stringResource(R.string.search_voice), tint = Color.White)
                    }
                }
            },
            placeholder = { Text(stringResource(R.string.search_field_hint), color = IptvColors.TextTertiary) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onCommit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = .78f),
                unfocusedBorderColor = IptvColors.DividerStrong,
                focusedContainerColor = IptvColors.Surface,
                unfocusedContainerColor = IptvColors.Surface,
                cursorColor = Color.White
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        MobileSettingsAction(onClick = onSettings)
    }
}

@Composable
private fun MobileSearchFilterChip(
    filter: PremiumSearchFilter,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        stringResource(filter.labelRes()),
        color = if (selected) Color.Black else IptvColors.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(CircleShape)
            .background(if (selected) Color.White else IptvColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun MobileRecentSearches(
    labels: List<String>,
    title: String,
    onSelect: (String) -> Unit,
    showClear: Boolean,
    onClear: () -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            if (showClear) {
                Text(
                    stringResource(R.string.search_clear),
                    color = IptvColors.TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable(onClick = onClear).padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(labels) { label ->
                Row(
                    Modifier.clip(CircleShape).background(IptvColors.SurfaceRaised)
                        .clickable { onSelect(label) }.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = IptvColors.TextTertiary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label, color = IptvColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
