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
import androidx.compose.material.icons.Icons
import com.prelude.iptv.ui.icons.automirrored.filled.ArrowBack
import com.prelude.iptv.ui.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.components.epg.rememberEpgNow
import com.prelude.iptv.ui.design.HorizonSearchField
import androidx.compose.foundation.border
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
    val typing = query.isNotBlank()
    val source = remember(allItems, results, query) { if (typing) results else allItems }
    val filtered = remember(source, filter, query) {
        val matched = SearchUiPolicy.filter(source, filter)
        if (typing) matched else SearchUiPolicy.discovery(matched, 60)
    }
    // Μετρητές ανά φίλτρο: φαίνονται πάνω στα chips όσο γράφεις.
    val counts = remember(results, typing) {
        if (!typing) emptyMap() else PremiumSearchFilter.entries.associateWith { SearchUiPolicy.filter(results, it).size }
    }
    val groups = remember(results, typing) { if (typing) SearchUiPolicy.group(results) else null }
    val libraryGroups = remember(allItems) { SearchUiPolicy.group(allItems) }
    var selected by remember { mutableStateOf(filtered.firstOrNull()) }
    LaunchedEffect(filtered) {
        val key = selected?.let(PlaybackQueue::favKey)
        selected = filtered.firstOrNull { PlaybackQueue.favKey(it) == key } ?: filtered.firstOrNull()
    }
    val meta by rememberSearchMeta(if (typing) null else selected, tmdbFor)
    val suggestions = remember(allItems) { SearchUiPolicy.suggestions(allItems, 8) }
    val navigationContentPadding = premiumMobileNavigationContentPadding()
    val nowMs = rememberEpgNow()

    fun rememberTerm(term: String) {
        term.trim().takeIf(String::isNotBlank)?.let {
            recents.remove(it); recents.add(0, it)
            while (recents.size > 6) recents.removeAt(recents.lastIndex)
        }
    }
    val openChannel: (Channel) -> Unit = { channel -> rememberTerm(query); onOpen(channel) }

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.search_back), tint = IptvColors.TextPrimary)
            }
            HorizonSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search_field_hint),
                clearLabel = stringResource(R.string.search_clear),
                onSearch = { rememberTerm(query) },
                modifier = Modifier.weight(1f),
                trailing = {
                    IconButton(onClick = onVoiceSearch) {
                        Icon(Icons.Default.Mic, stringResource(R.string.search_voice), tint = IptvColors.TextSecondary)
                    }
                }
            )
            Spacer(Modifier.width(6.dp))
            MobileSettingsAction(onClick = onOpenSettings)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PremiumSearchFilter.entries) { item ->
                MobileSearchFilterChip(item, counts[item], selected = filter == item) { filter = item }
            }
        }

        if (typing && filtered.isEmpty()) {
            PremiumSearchEmpty(query, Modifier.fillMaxSize())
            return@Column
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 6.dp, bottom = navigationContentPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            if (typing && filter == PremiumSearchFilter.ALL && groups != null) {
                // Αποτελέσματα ομαδοποιημένα ανά τύπο: λίγα από το καθένα, «Όλα» για τα υπόλοιπα.
                val sections = listOf(
                    Triple(PremiumSearchFilter.LIVE, groups.live, R.string.search_filter_live),
                    Triple(PremiumSearchFilter.MOVIES, groups.movies, R.string.search_filter_movies),
                    Triple(PremiumSearchFilter.SERIES, groups.series, R.string.search_filter_series),
                    Triple(null, groups.other, R.string.search_category_content),
                )
                sections.filter { it.second.isNotEmpty() }.forEach { (sectionFilter, items, titleRes) ->
                    item(key = "search-section:$titleRes") {
                        MobileSearchSectionHeader(
                            title = stringResource(titleRes),
                            seeAll = if (sectionFilter != null && items.size > SECTION_PREVIEW) items.size else null,
                            onSeeAll = { sectionFilter?.let { filter = it } }
                        )
                    }
                    items(
                        items.take(if (sectionFilter == null) items.size else SECTION_PREVIEW),
                        key = { "search-result:${titleRes}:${PlaybackQueue.favKey(it)}" }
                    ) { channel ->
                        MobileSearchResultRow(
                            channel = channel,
                            query = query,
                            nowMs = nowMs,
                            favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                            progress = progress[PlaybackQueue.favKey(channel)],
                            onOpen = { openChannel(channel) },
                            onToggleFavorite = { onToggleFavorite(channel) }
                        )
                    }
                }
            } else if (typing) {
                item(key = "search-filter-header") {
                    MobileSearchSectionHeader(
                        title = stringResource(filter.labelRes()),
                        count = filtered.size,
                    )
                }
                items(filtered, key = { "search-result:${PlaybackQueue.favKey(it)}" }) { channel ->
                    MobileSearchResultRow(
                        channel = channel,
                        query = query,
                        nowMs = nowMs,
                        favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                        progress = progress[PlaybackQueue.favKey(channel)],
                        onOpen = { openChannel(channel) },
                        onToggleFavorite = { onToggleFavorite(channel) }
                    )
                }
            } else {
                // Πριν γράψεις: πρόσφατες (ή δημοφιλείς), γρήγορη πρόσβαση, προτάσεις.
                val labels = if (recents.isNotEmpty()) recents.toList() else suggestions.take(5)
                if (labels.isNotEmpty() && filter == PremiumSearchFilter.ALL) {
                    item(key = "search-recents-header") {
                        MobileSearchSectionHeader(
                            title = stringResource(if (recents.isNotEmpty()) R.string.search_recent else R.string.search_popular),
                            actionLabel = if (recents.isNotEmpty()) stringResource(R.string.search_clear) else null,
                            onAction = { recents.clear() }
                        )
                    }
                    items(labels, key = { "search-recent:$it" }) { label ->
                        MobileRecentSearchRow(
                            label = label,
                            recent = recents.isNotEmpty(),
                            onSelect = { onQueryChange(label) },
                            onRemove = { recents.remove(label) }
                        )
                    }
                }
                if (filter == PremiumSearchFilter.ALL) {
                    item(key = "search-quick-access") {
                        MobileSearchSectionHeader(title = stringResource(R.string.search_quick_access))
                        MobileQuickAccessGrid(
                            liveCount = libraryGroups.live.size,
                            movieCount = libraryGroups.movies.size,
                            seriesCount = libraryGroups.series.size,
                            myListCount = favoriteKeys.size,
                            onLive = onOpenLive,
                            onMovies = onOpenMovies,
                            onSeries = onOpenSeries,
                            onMyList = onOpenMyList,
                        )
                    }
                }
                selected?.let { channel ->
                    item(key = "search-featured") {
                        Spacer(Modifier.height(22.dp))
                        MobileSearchFeatured(
                            channel = channel,
                            meta = meta,
                            favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                            progress = progress[PlaybackQueue.favKey(channel)],
                            onOpen = { openChannel(channel) },
                            onToggleFavorite = { onToggleFavorite(channel) },
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }
                item(key = "search-discover-header") {
                    MobileSearchSectionHeader(
                        title = localizedSearchHeading(SearchUiPolicy.heading(query, filter)),
                        count = filtered.size,
                    )
                }
                itemsIndexed(
                    filtered.chunked(2),
                    key = { index, _ -> "search-row:$index" }
                ) { _, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        row.forEach { channel ->
                            MobileSearchResultCard(
                                channel = channel,
                                favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                                progress = progress[PlaybackQueue.favKey(channel)],
                                onSelect = {
                                    selected = channel
                                    // Η κάρτα «προτεινόμενο» είναι μετά τις πρόσφατες και τη γρήγορη πρόσβαση.
                                    val featuredIndex = if (filter == PremiumSearchFilter.ALL) {
                                        (if (labels.isNotEmpty()) 1 + labels.size else 0) + 1
                                    } else 0
                                    scope.launch { listState.animateScrollToItem(featuredIndex) }
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

/** Πόσα αποτελέσματα δείχνει κάθε ομάδα πριν το «Όλα». */
private const val SECTION_PREVIEW = 4

@Composable
private fun MobileSearchFilterChip(
    filter: PremiumSearchFilter,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = stringResource(filter.labelRes())
    Text(
        if (count != null) "$label · $count" else label,
        color = if (selected) IptvColors.Background else IptvColors.TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(CircleShape)
            .background(if (selected) IptvColors.TextPrimary else IptvColors.Surface)
            .border(1.dp, if (selected) IptvColors.TextPrimary else IptvColors.Divider, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
