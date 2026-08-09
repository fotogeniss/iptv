package com.prelude.iptv.ui.tv.library

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.LibraryDestination
import com.prelude.iptv.ui.components.library.LibraryCinematicBackdrop
import com.prelude.iptv.ui.components.library.LibraryHubTab
import com.prelude.iptv.ui.components.library.LibrarySort
import com.prelude.iptv.ui.components.library.PremiumLibraryContent
import com.prelude.iptv.ui.components.library.initialLibraryTab
import com.prelude.iptv.ui.components.library.libraryKey
import com.prelude.iptv.ui.components.library.libraryProgress
import com.prelude.iptv.ui.components.library.libraryRails
import com.prelude.iptv.ui.components.library.rememberLibraryMeta
import com.prelude.iptv.ui.localization.libraryRailLabels

@Composable
fun TvPremiumLibraryScreen(
    initialDestination: LibraryDestination,
    content: PremiumLibraryContent,
    favoriteKeys: Set<String>,
    onBack: () -> Unit,
    onOpen: (Channel) -> Unit,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onRemove: (LibraryDestination, Channel) -> Unit,
    onDestinationChange: (LibraryDestination) -> Unit,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?,
    modifier: Modifier = Modifier
) {
    var tab by remember(initialDestination) { mutableStateOf(initialLibraryTab(initialDestination)) }
    var sort by remember { mutableStateOf(LibrarySort.RECENT) }
    var managementMode by remember { mutableStateOf(false) }
    val railLabels = libraryRailLabels(content)
    val rails = remember(content, tab, sort, railLabels) { libraryRails(content, tab, sort, railLabels) }
    var selected by remember(rails) { mutableStateOf(rails.firstOrNull()?.items?.firstOrNull()) }
    var selectedDestination by remember(rails) { mutableStateOf(rails.firstOrNull()?.destination ?: LibraryDestination.MY_LIST) }
    val meta by rememberLibraryMeta(selected, tmdbFor)

    LaunchedEffect(rails) {
        if (selected == null || rails.none { selected in it.items }) {
            selected = rails.firstOrNull()?.items?.firstOrNull()
            selectedDestination = rails.firstOrNull()?.destination ?: LibraryDestination.MY_LIST
        }
    }

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        LibraryCinematicBackdrop(selected, meta, mobile = false)
        Column(Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 22.dp)) {
            TvLibraryHeading(content, onBack)
            Spacer(Modifier.height(16.dp))
            TvLibraryControls(
                selected = tab,
                onSelect = {
                    tab = it
                    it.destination?.let(onDestinationChange)
                },
                sort = sort,
                onSort = { sort = if (sort == LibrarySort.RECENT) LibrarySort.TITLE else LibrarySort.RECENT },
                managementMode = managementMode,
                onManage = { managementMode = !managementMode }
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (rails.isEmpty()) {
                    TvLibraryEmpty(tab, Modifier.weight(1f).fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        rails.forEachIndexed { railIndex, rail ->
                            item(key = rail.id) {
                                Column {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                                        Text(rail.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                                        Text(rail.subtitle, color = IptvColors.TextSecondary, fontSize = 9.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
                                        itemsIndexed(rail.items, key = { _, item -> "${rail.id}:${libraryKey(item)}" }) { index, channel ->
                                            TvLibraryCard(
                                                channel = channel,
                                                poster = rail.poster,
                                                progress = libraryProgress(channel, content),
                                                favorite = libraryKey(channel) in favoriteKeys,
                                                managementMode = managementMode,
                                                initialFocus = railIndex == 0 && index == 0,
                                                onFocused = {
                                                    selected = channel
                                                    selectedDestination = rail.destination
                                                },
                                                onOpen = { onOpen(channel) },
                                                onRemove = { onRemove(rail.destination, channel) },
                                                modifier = Modifier.width(if (rail.poster) 158.dp else 218.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                TvLibraryInfoPanel(
                    channel = selected,
                    meta = meta,
                    progress = selected?.let { libraryProgress(it, content) },
                    destination = selectedDestination,
                    favorite = selected?.let { libraryKey(it) in favoriteKeys } == true,
                    managementMode = managementMode,
                    onOpen = { selected?.let(onPlay) },
                    onToggleFavorite = { selected?.let(onToggleFavorite) },
                    onRemove = { selected?.let { onRemove(selectedDestination, it) } },
                    modifier = Modifier.width(310.dp).fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun TvLibraryEmpty(tab: LibraryHubTab, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (tab == LibraryHubTab.HISTORY) Icons.Default.History else Icons.Default.Check, null, tint = IptvColors.TextTertiary, modifier = Modifier.width(56.dp).height(56.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.library_empty_title), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.library_empty_subtitle), color = IptvColors.TextSecondary, fontSize = 12.sp)
        }
    }
}
