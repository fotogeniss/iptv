package com.prelude.iptv.ui.mobile.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.LibraryDestination
import com.prelude.iptv.ui.components.library.LibraryHubTab
import com.prelude.iptv.ui.components.library.LibraryRail
import com.prelude.iptv.ui.components.library.LibrarySort
import com.prelude.iptv.ui.components.library.PremiumLibraryContent
import com.prelude.iptv.ui.components.library.libraryKey
import com.prelude.iptv.ui.components.library.libraryProgress

@Composable
internal fun MobileLibrarySummary(content: PremiumLibraryContent) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LibraryStat(content.myList.size.toString(), "Η λίστα μου", Modifier.weight(1f))
        LibraryStat(content.continueWatching.size.toString(), "Σε εξέλιξη", Modifier.weight(1f))
        LibraryStat(content.history.size.toString(), "Ιστορικό", Modifier.weight(1f))
    }
}

@Composable
private fun LibraryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.background(IptvColors.Surface, RoundedCornerShape(12.dp)).padding(11.dp)) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = IptvColors.TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun MobileLibraryTabs(
    selected: LibraryHubTab,
    onSelect: (LibraryHubTab) -> Unit,
    sort: LibrarySort,
    onSort: () -> Unit,
    managementMode: Boolean,
    onManage: () -> Unit
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(LibraryHubTab.entries) { tab ->
            Text(
                tab.label,
                color = if (tab == selected) Color.White else IptvColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.background(if (tab == selected) IptvColors.Primary else IptvColors.Surface, RoundedCornerShape(99.dp))
                    .clickable { onSelect(tab) }.padding(horizontal = 13.dp, vertical = 10.dp)
            )
        }
        item {
            Text("⇅ ${sort.label}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(IptvColors.Surface, RoundedCornerShape(99.dp)).clickable(onClick = onSort)
                    .padding(horizontal = 13.dp, vertical = 10.dp))
        }
        item {
            Text(if (managementMode) "Τέλος" else "✓ Διαχείριση", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(IptvColors.Surface, RoundedCornerShape(99.dp)).clickable(onClick = onManage)
                    .padding(horizontal = 13.dp, vertical = 10.dp))
        }
    }
}

@Composable
internal fun MobileLibraryRail(
    rail: LibraryRail,
    content: PremiumLibraryContent,
    favoriteKeys: Set<String>,
    managementMode: Boolean,
    removalSelection: Map<String, Pair<LibraryDestination, Channel>>,
    onSelected: (Channel) -> Unit,
    onToggleRemoval: (Channel) -> Unit,
    onRemove: (Channel) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
            Text(rail.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(rail.subtitle, color = IptvColors.TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(9.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rail.items, key = { "${rail.id}:${libraryKey(it)}" }) { channel ->
                MobileLibraryCard(
                    channel = channel,
                    poster = rail.poster,
                    progress = libraryProgress(channel, content),
                    favorite = libraryKey(channel) in favoriteKeys,
                    managementMode = managementMode,
                    selectedForRemoval = libraryKey(channel) in removalSelection,
                    onSelect = { onSelected(channel) },
                    onToggleRemoval = { onToggleRemoval(channel) },
                    onRemove = { onRemove(channel) },
                    modifier = Modifier.width(if (rail.poster) 132.dp else 248.dp)
                )
            }
        }
    }
}

@Composable
internal fun MobileLibraryEmpty(tab: LibraryHubTab, modifier: Modifier = Modifier) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (tab == LibraryHubTab.HISTORY) Icons.Default.History else Icons.Default.Check, null, tint = IptvColors.TextTertiary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Δεν υπάρχει περιεχόμενο εδώ", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("Πρόσθεσε τίτλους από Home, Search ή Details.", color = IptvColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun MobileLibraryBottomNav(
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onLive: () -> Unit,
    onMyList: () -> Unit,
    onSettings: () -> Unit,
    collapsed: Boolean = false,
    modifier: Modifier = Modifier
) {
    com.prelude.iptv.ui.mobile.navigation.PremiumMobileBottomNavigation(
        selected = "library",
        onHome = onHome,
        onMovies = onMovies,
        onSeries = onSeries,
        onLive = onLive,
        onSearch = onSearch,
        onMyList = onMyList,
        onSettings = onSettings,
        collapsed = collapsed,
        modifier = modifier
    )
}
