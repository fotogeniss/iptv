package com.prelude.iptv.ui.mobile.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.LibraryDestination
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding
import com.prelude.iptv.ui.components.library.LibraryCinematicBackdrop
import com.prelude.iptv.ui.components.library.LibraryHubTab
import com.prelude.iptv.ui.components.library.LibrarySort
import com.prelude.iptv.ui.components.library.PremiumLibraryContent
import com.prelude.iptv.ui.components.library.initialLibraryTab
import com.prelude.iptv.ui.components.library.libraryKey
import com.prelude.iptv.ui.components.library.libraryProgress
import com.prelude.iptv.ui.components.library.libraryRails
import com.prelude.iptv.ui.components.library.rememberLibraryMeta

@Composable
fun MobilePremiumLibraryScreen(
    initialDestination: LibraryDestination,
    content: PremiumLibraryContent,
    favoriteKeys: Set<String>,
    onBack: () -> Unit,
    onOpen: (Channel) -> Unit,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onRemove: (LibraryDestination, Channel) -> Unit,
    onDestinationChange: (LibraryDestination) -> Unit,
    onOpenHome: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenSettings: () -> Unit,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?,
    modifier: Modifier = Modifier
) {
    var tab by remember(initialDestination) { mutableStateOf(initialLibraryTab(initialDestination)) }
    var sort by remember { mutableStateOf(LibrarySort.RECENT) }
    var managementMode by remember { mutableStateOf(false) }
    var removalSelection by remember { mutableStateOf<Map<String, Pair<LibraryDestination, Channel>>>(emptyMap()) }
    val rails = remember(content, tab, sort) { libraryRails(content, tab, sort) }
    var selected by remember(rails) { mutableStateOf(rails.firstOrNull()?.items?.firstOrNull()) }
    val meta by rememberLibraryMeta(selected, tmdbFor)

    val navigationContentPadding = premiumMobileNavigationContentPadding()
    val listState = rememberLazyListState()

    LaunchedEffect(rails) {
        if (selected == null || rails.none { selected in it.items }) {
            selected = rails.firstOrNull()?.items?.firstOrNull()
        }
    }

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (managementMode) navigationContentPadding + 58.dp else navigationContentPadding)
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(430.dp)) {
                    LibraryCinematicBackdrop(selected, meta, mobile = true)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("PRELUDE", color = Color.White, fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black)
                        Text("+", color = IptvColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    selected?.let { channel ->
                        MobileLibraryHeroCopy(
                            channel = channel,
                            meta = meta,
                            progress = libraryProgress(channel, content),
                            favorite = libraryKey(channel) in favoriteKeys,
                            onPlay = { onPlay(channel) },
                            onInfo = { onOpen(channel) },
                            onFavorite = { onToggleFavorite(channel) },
                            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 18.dp)
                        )
                    }
                }
            }
            item { MobileLibrarySummary(content) }
            item {
                MobileLibraryTabs(
                    selected = tab,
                    onSelect = {
                        tab = it
                        it.destination?.let(onDestinationChange)
                    },
                    sort = sort,
                    onSort = { sort = if (sort == LibrarySort.RECENT) LibrarySort.TITLE else LibrarySort.RECENT },
                    managementMode = managementMode,
                    onManage = {
                        managementMode = !managementMode
                        if (!managementMode) removalSelection = emptyMap()
                    }
                )
            }
            if (rails.isEmpty()) {
                item { MobileLibraryEmpty(tab, Modifier.fillMaxWidth().height(290.dp)) }
            } else {
                rails.forEach { rail ->
                    item {
                        MobileLibraryRail(
                            rail = rail,
                            content = content,
                            favoriteKeys = favoriteKeys,
                            managementMode = managementMode,
                            removalSelection = removalSelection,
                            onSelected = { channel ->
                                selected = channel
                            },
                            onToggleRemoval = { channel ->
                                val key = libraryKey(channel)
                                removalSelection = if (key in removalSelection) removalSelection - key
                                else removalSelection + (key to (rail.destination to channel))
                            },
                            onRemove = { channel -> onRemove(rail.destination, channel) }
                        )
                    }
                }
            }
        }

        if (managementMode) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xF2111316))
                    .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${removalSelection.size} επιλεγμένα", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        removalSelection.values.forEach { (destination, channel) -> onRemove(destination, channel) }
                        removalSelection = emptyMap()
                        managementMode = false
                    },
                    enabled = removalSelection.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Αφαίρεση", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            MobileLibraryBottomNav(
                onHome = onOpenHome,
                onSearch = { onDestinationChange(LibraryDestination.SEARCH) },
                onMovies = onOpenMovies,
                onSeries = onOpenSeries,
                onLive = onOpenLive,
                onMyList = {
                    tab = LibraryHubTab.MY_LIST
                    onDestinationChange(LibraryDestination.MY_LIST)
                },
                onSettings = onOpenSettings,
                collapsed = listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 40,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
