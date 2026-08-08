package com.prelude.iptv.ui.tv.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.PremiumSearchFilter
import com.prelude.iptv.ui.SearchUiPolicy
import com.prelude.iptv.ui.TextEntryDialog
import com.prelude.iptv.ui.TvIconButton
import com.prelude.iptv.ui.components.search.PremiumSearchEmpty
import com.prelude.iptv.ui.components.search.SearchCinematicBackdrop
import com.prelude.iptv.ui.components.search.rememberSearchMeta
import com.prelude.iptv.ui.rememberInitialFocus

@Composable
fun TvPremiumSearchScreen(
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
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(PremiumSearchFilter.ALL) }
    var editorOpen by remember { mutableStateOf(false) }
    val recents = remember { mutableStateListOf<String>() }
    val suggestions = remember(allItems) { SearchUiPolicy.suggestions(allItems, 6) }
    val source = remember(allItems, results, query) { if (query.isBlank()) allItems else results }
    val filtered = remember(source, filter, query) {
        val matched = SearchUiPolicy.filter(source, filter)
        if (query.isBlank()) SearchUiPolicy.discovery(matched, 50) else matched
    }
    var selected by remember { mutableStateOf(filtered.firstOrNull()) }
    LaunchedEffect(filtered) {
        val selectedKey = selected?.let(PlaybackQueue::favKey)
        selected = filtered.firstOrNull { PlaybackQueue.favKey(it) == selectedKey } ?: filtered.firstOrNull()
    }
    val meta by rememberSearchMeta(selected, tmdbFor)
    val searchFocus = rememberInitialFocus(key = Unit)

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        SearchCinematicBackdrop(selected, meta, mobile = false)
        Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 20.dp)) {
            Row(Modifier.fillMaxWidth()) {
                TvIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", onClick = onBack)
                Spacer(Modifier.width(13.dp))
                Column {
                    Text("PRELUDE+ SEARCH", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("Αναζήτησε σε ολόκληρη τη βιβλιοθήκη", color = IptvColors.TextTertiary, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(15.dp))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(Modifier.fillMaxWidth(.31f)) {
                    TvSearchEntryRow(
                        query = query,
                        focusRequester = searchFocus,
                        onEdit = { editorOpen = true },
                        onVoice = onVoiceSearch
                    )
                    Spacer(Modifier.height(12.dp))
                    TvSearchKeyboard(
                        onCharacter = { onQueryChange(query + it) },
                        onBackspace = { onQueryChange(query.dropLast(1)) },
                        onClear = { onQueryChange("") }
                    )
                    TvSearchSuggestions(
                        labels = if (recents.isNotEmpty()) recents else suggestions,
                        onSelect = onQueryChange
                    )
                }

                Column(Modifier.weight(1f)) {
                    selected?.let { channel ->
                        TvSearchFeatured(
                            channel = channel,
                            meta = meta,
                            favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                            onOpen = {
                                rememberRecent(recents, query)
                                onOpen(channel)
                            },
                            onToggleFavorite = { onToggleFavorite(channel) }
                        )
                    }
                    Spacer(Modifier.height(13.dp))
                    TvSearchFilterRow(filter) { filter = it }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            SearchUiPolicy.title(query, filter),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${filtered.size} τίτλοι", color = IptvColors.TextTertiary, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (filtered.isEmpty()) {
                        PremiumSearchEmpty(query, Modifier.fillMaxSize())
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            contentPadding = PaddingValues(5.dp, 5.dp, 5.dp, 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                            verticalArrangement = Arrangement.spacedBy(13.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(filtered, key = { index, _ -> "tv-search:$index" }) { _, channel ->
                                TvSearchResultCard(
                                    channel = channel,
                                    favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                                    progress = progress[PlaybackQueue.favKey(channel)],
                                    onFocused = { selected = channel },
                                    onOpen = {
                                        selected = channel
                                        rememberRecent(recents, query)
                                        onOpen(channel)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editorOpen) {
        TextEntryDialog(
            title = "Αναζήτηση",
            initial = query,
            onDismiss = { editorOpen = false },
            onOk = {
                onQueryChange(it)
                rememberRecent(recents, it)
                editorOpen = false
            }
        )
    }
}

private fun rememberRecent(recents: MutableList<String>, query: String) {
    val value = query.trim()
    if (value.isBlank()) return
    recents.remove(value)
    recents.add(0, value)
    while (recents.size > 5) recents.removeAt(recents.lastIndex)
}
