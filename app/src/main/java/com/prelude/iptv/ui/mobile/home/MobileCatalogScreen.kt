package com.prelude.iptv.ui.mobile.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.CatalogRankingPolicy
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.design.HorizonSearchField
import com.prelude.iptv.ui.design.foldForSearch
import com.prelude.iptv.ui.icons.filled.Category
import com.prelude.iptv.ui.icons.filled.ChevronRight
import com.prelude.iptv.ui.icons.filled.Search
import com.prelude.iptv.ui.icons.filled.Sort
import com.prelude.iptv.ui.mobile.live.LiveCategoryTab
import com.prelude.iptv.ui.mobile.navigation.MobileSettingsAction
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding
import com.prelude.iptv.ui.mobile.search.MobileSearchResultCard

/** Ταξινόμηση του καταλόγου. Η σειρά εδώ είναι η σειρά στο chip. */
internal enum class CatalogSort { NEWEST, RATING, YEAR, TITLE }

/**
 * Ταινίες / Σειρές ως κατάλογος (prototypes/redesign_pages.html, ενότητα 1).
 *
 * Καρτέλες κατηγοριών κολλημένες πάνω (ίδιες με τη Live), ταξινόμηση και πλέγμα
 * τριών στηλών που φορτώνει όσο κατεβαίνεις — αντί για σειρές καρτών ανά
 * κατηγορία που σέρνεις στο πλάι και «Δες όλα» για να δεις περισσότερα.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileCatalogScreen(
    title: String,
    items: List<Channel>,
    categories: List<Pair<String, Int>>,
    favoriteKeys: Set<String>,
    progress: Map<String, Float>,
    onOpen: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigationCollapsedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var category by rememberSaveable(title) { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(CatalogSort.NEWEST) }
    var sortOpen by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val inCategory = remember(items, category) {
        category?.let { selected -> items.filter { it.group.trim() == selected } } ?: items
    }
    val shown = remember(inCategory, sort) { sorted(inCategory, sort) }
    val collapsed = gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 40
    LaunchedEffect(collapsed) { onNavigationCollapsedChange(collapsed) }
    LaunchedEffect(category, sort) { if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0) }
    LaunchedEffect(categories) {
        if (category != null && categories.none { it.first == category }) category = null
    }

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = premiumMobileNavigationContentPadding() + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "catalog-top", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        color = IptvColors.TextPrimary,
                        fontFamily = AppFonts.Display,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 32.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, stringResource(R.string.catalog_search), tint = IptvColors.TextPrimary)
                    }
                    MobileSettingsAction(onClick = onOpenSettings, modifier = Modifier.size(40.dp))
                }
            }
            item(key = "catalog-tabs", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    // Οι καρτέλες βγαίνουν από τα περιθώρια του πλέγματος ώστε να
                    // σκρολάρουν μέχρι την άκρη της οθόνης.
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(key = "all") {
                        LiveCategoryTab(stringResource(R.string.live_all), items.size, category == null) { category = null }
                    }
                    items(categories.take(12), key = { "cat:${it.first}" }) { (name, count) ->
                        LiveCategoryTab(name, count, category == name) { category = name }
                    }
                    item(key = "more") {
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
            item(key = "catalog-sort", span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveCategoryTab(
                        stringResource(sort.labelRes()),
                        null,
                        selected = false,
                        leading = {
                            Icon(Icons.Default.Sort, null, tint = IptvColors.TextSecondary, modifier = Modifier.size(15.dp))
                        },
                    ) { sortOpen = true }
                    Box(Modifier.weight(1f))
                    Text(
                        pluralStringResource(R.plurals.search_title_count, shown.size, shown.size),
                        color = IptvColors.TextTertiary,
                        fontSize = 12.5.sp,
                    )
                }
            }
            items(shown, key = { "catalog:${PlaybackQueue.favKey(it)}" }) { channel ->
                MobileSearchResultCard(
                    channel = channel,
                    favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                    progress = progress[PlaybackQueue.favKey(channel)],
                    onSelect = { onOpen(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (sortOpen) {
        ModalBottomSheet(
            onDismissRequest = { sortOpen = false },
            containerColor = IptvColors.Surface,
            contentColor = IptvColors.TextPrimary,
        ) {
            Text(
                stringResource(R.string.catalog_sort),
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                color = IptvColors.TextPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            )
            CatalogSort.entries.forEach { option ->
                SheetRow(stringResource(option.labelRes()), null, option == sort) { sort = option; sortOpen = false }
            }
            Box(Modifier.padding(bottom = 24.dp))
        }
    }

    if (sheetOpen) {
        var filter by remember { mutableStateOf("") }
        val needle = foldForSearch(filter.trim())
        val visible = remember(categories, needle) {
            if (needle.isEmpty()) categories else categories.filter { foldForSearch(it.first).contains(needle) }
        }
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            containerColor = IptvColors.Surface,
            contentColor = IptvColors.TextPrimary,
        ) {
            Text(
                title,
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
                clearLabel = stringResource(R.string.catalog_clear_search),
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                if (needle.isEmpty()) {
                    item(key = "all") {
                        SheetRow(stringResource(R.string.live_all), items.size, category == null) { category = null; sheetOpen = false }
                    }
                }
                items(visible, key = { "sheet:${it.first}" }) { (name, count) ->
                    SheetRow(name, count, category == name) { category = name; sheetOpen = false }
                }
            }
        }
    }
}

@Composable
private fun SheetRow(title: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
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
        if (count != null) Text(count.toString(), color = IptvColors.TextTertiary, fontSize = 13.sp)
        Icon(Icons.Default.ChevronRight, null, tint = IptvColors.TextTertiary, modifier = Modifier.padding(start = 6.dp).size(18.dp))
    }
}

private fun CatalogSort.labelRes(): Int = when (this) {
    CatalogSort.NEWEST -> R.string.catalog_sort_newest
    CatalogSort.RATING -> R.string.catalog_sort_rating
    CatalogSort.YEAR -> R.string.catalog_sort_year_desc
    CatalogSort.TITLE -> R.string.catalog_sort_ascending
}

/** Πλήρης ταξινόμηση: όσα δεν έχουν το κριτήριο μένουν στο τέλος, με τη σειρά του παρόχου. */
private fun sorted(items: List<Channel>, sort: CatalogSort): List<Channel> = when (sort) {
    CatalogSort.NEWEST -> {
        val dated = CatalogRankingPolicy.newestFirst(items)
        if (dated.isEmpty()) items.asReversed() else dated + (items - dated.toSet())
    }
    CatalogSort.RATING -> {
        val rated = CatalogRankingPolicy.topRatedFirst(items)
        rated + (items - rated.toSet())
    }
    CatalogSort.YEAR -> items.sortedByDescending { it.year.take(4).toIntOrNull() ?: Int.MIN_VALUE }
    CatalogSort.TITLE -> items.sortedBy { foldForSearch(it.name) }
}
