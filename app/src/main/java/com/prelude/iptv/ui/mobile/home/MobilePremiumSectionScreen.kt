package com.prelude.iptv.ui.mobile.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.CatalogRailSection
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding
import com.prelude.iptv.ui.mobile.navigation.MobileSettingsAction
import com.prelude.iptv.ui.mobile.search.MobileSearchResultCard

@Composable
internal fun MobilePremiumSectionScreen(
    section: CatalogRailSection,
    favoriteKeys: Set<String>,
    selectedDestination: String,
    onBack: () -> Unit,
    onOpen: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onOpenHome: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMyList: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val gridState = rememberLazyGridState()
    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(section.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("${section.allItems.size} τίτλοι", color = IptvColors.TextTertiary, fontSize = 11.sp)
                }
                MobileSettingsAction(onClick = onOpenSettings)
            }
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 15.dp,
                    end = 15.dp,
                    top = 8.dp,
                    bottom = premiumMobileNavigationContentPadding()
                ),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = section.allItems,
                    key = { channel -> "view-all:${section.id}:${PlaybackQueue.favKey(channel)}" }
                ) { channel ->
                    MobileSearchResultCard(
                        channel = channel,
                        favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                        progress = section.progress[PlaybackQueue.favKey(channel)],
                        onSelect = { onOpen(channel) },
                        onToggleFavorite = { onToggleFavorite(channel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        MobileHomeBottomNavigation(
            selected = selectedDestination,
            onHome = onOpenHome,
            onMovies = onOpenMovies,
            onSeries = onOpenSeries,
            onLive = onOpenLive,
            onSearch = onOpenSearch,
            onMyList = onOpenMyList,
            onSettings = onOpenSettings,
            collapsed = gridState.firstVisibleItemIndex > 0 ||
                gridState.firstVisibleItemScrollOffset > 40,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
