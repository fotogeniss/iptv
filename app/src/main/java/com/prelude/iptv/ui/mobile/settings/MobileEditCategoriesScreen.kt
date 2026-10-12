package com.prelude.iptv.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.prelude.iptv.R
import com.prelude.iptv.category.CategoryCountryPolicy
import com.prelude.iptv.category.CategoryEditorSection
import com.prelude.iptv.category.CategoryEditorState
import com.prelude.iptv.category.CategoryEntry
import com.prelude.iptv.category.CategoryLayout
import com.prelude.iptv.category.CategoryLayoutPolicy
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.design.HorizonSearchField
import com.prelude.iptv.ui.design.foldForSearch
import com.prelude.iptv.ui.design.highlightMatch
import com.prelude.iptv.ui.icons.automirrored.filled.ArrowBack
import com.prelude.iptv.ui.icons.filled.DragHandle
import com.prelude.iptv.ui.icons.filled.ExpandLess
import com.prelude.iptv.ui.icons.filled.ExpandMore
import com.prelude.iptv.ui.icons.filled.Sort
import com.prelude.iptv.ui.icons.filled.Visibility
import com.prelude.iptv.ui.icons.filled.VisibilityOff
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Οι τρεις ενότητες με τη σειρά που εμφανίζονται. */
internal val CATEGORY_EDITOR_TYPES = listOf("live", "vod", "series")

@Composable
internal fun categoryTypeLabel(type: String): String = stringResource(
    when (type) {
        "vod" -> R.string.settings_categories_movies
        "series" -> R.string.settings_categories_series
        else -> R.string.settings_categories_live
    }
)

@Composable
internal fun categoryCountLabel(type: String, count: Int): String =
    if (type == "live") stringResource(R.string.cat_editor_channels, count)
    else stringResource(R.string.cat_editor_titles, count)

/**
 * Επεξεργασία κατηγοριών (prototypes/redesign_categories.html).
 *
 * Από πάνω προς τα κάτω: ενότητα με «48 από 212», χώρες (ένα πάτημα για όλη τη
 * χώρα), Δείξε/Κρύψε όλες και Α–Ω, αναζήτηση, ορατές με σύρσιμο, κρυφές
 * μαζεμένες. Κάθε αλλαγή αποθηκεύεται αμέσως· το snackbar δίνει «Αναίρεση».
 */
@Composable
internal fun MobileEditCategoriesScreen(
    state: CategoryEditorState,
    onLayoutChange: (type: String, layout: CategoryLayout) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var type by remember { mutableStateOf("live") }
    var query by remember { mutableStateOf("") }
    var hiddenExpanded by remember { mutableStateOf(false) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.cat_editor_undo)

    val section = state.section(type)
    val entries = section.entries
    val visibleEntries = entries.filter { it.visible }
    val hiddenEntries = entries.filterNot { it.visible }.sortedBy { it.option.title.lowercase() }
    val countries = remember(section.available) { CategoryCountryPolicy.groups(section.available) }
    val needle = foldForSearch(query.trim())
    val matches = if (needle.isEmpty()) emptyList()
    else entries.filter { foldForSearch(it.option.title).contains(needle) }

    // Κάθε αλλαγή: εφαρμογή αμέσως + snackbar με «Αναίρεση» που ξαναστέλνει το προηγούμενο.
    fun commit(next: List<CategoryEntry>, message: String) {
        val editedType = type
        val previous = section.layout
        onLayoutChange(editedType, CategoryLayoutPolicy.layoutOf(next, previous))
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onLayoutChange(editedType, previous)
        }
    }

    fun toggle(entry: CategoryEntry) {
        commit(
            CategoryLayoutPolicy.setVisible(entries, setOf(entry.option.id), !entry.visible),
            context.getString(
                if (entry.visible) R.string.cat_editor_snack_hidden else R.string.cat_editor_snack_shown,
                entry.option.title,
            ),
        )
    }

    fun bulk(ids: Set<String>, visible: Boolean) {
        val changed = entries.count { it.option.id in ids && it.visible != visible }
        if (changed == 0) return
        commit(
            CategoryLayoutPolicy.setVisible(entries, ids, visible),
            context.getString(R.string.cat_editor_snack_bulk, changed),
        )
    }

    fun moveToEdge(entry: CategoryEntry, top: Boolean) {
        commit(
            CategoryLayoutPolicy.moveToEdge(entries, entry.option.id, top),
            context.getString(R.string.cat_editor_snack_moved, entry.option.title),
        )
    }

    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back), tint = IptvColors.TextPrimary)
                }
                Text(
                    stringResource(R.string.cat_editor_title),
                    color = IptvColors.TextPrimary,
                    fontFamily = AppFonts.Display,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                )
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 104.dp),
            ) {
                item("types") {
                    CategoryTypeTabs(
                        state = state,
                        selected = type,
                        onSelect = { type = it; dragId = null },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                when {
                    section.loading -> item("loading") {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = IptvColors.Primary)
                        }
                    }
                    section.error != null -> item("error") {
                        CategoryEditorMessage(stringResource(R.string.settings_category_load_failed))
                    }
                    entries.isEmpty() -> item("empty") {
                        CategoryEditorMessage(stringResource(R.string.cat_editor_empty))
                    }
                    else -> {
                        if (countries.isNotEmpty() && needle.isEmpty()) {
                            item("countries-label") {
                                CategoryLabelRow(
                                    stringResource(R.string.cat_editor_countries),
                                    stringResource(R.string.cat_editor_countries_hint),
                                    Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                                )
                            }
                            item("countries") {
                                CountryGrid(
                                    section = section,
                                    groups = countries,
                                    onToggle = { group, allOn -> bulk(group.ids.toSet(), !allOn) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                        item("quick") {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                QuickChip(stringResource(R.string.cat_editor_show_all), Icons.Default.Visibility) {
                                    bulk(entries.map { it.option.id }.toSet(), true)
                                }
                                QuickChip(stringResource(R.string.cat_editor_hide_all), Icons.Default.VisibilityOff) {
                                    bulk(entries.map { it.option.id }.toSet(), false)
                                }
                                QuickChip(stringResource(R.string.cat_editor_sort_az), Icons.Default.Sort) {
                                    commit(
                                        CategoryLayoutPolicy.sortedByTitle(entries),
                                        context.getString(R.string.cat_editor_snack_sorted),
                                    )
                                }
                            }
                        }
                        item("search") {
                            HorizonSearchField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = stringResource(R.string.cat_editor_search),
                                clearLabel = stringResource(R.string.settings_search_clear),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
                            )
                        }

                        if (needle.isNotEmpty()) {
                            item("results-label") {
                                CategoryLabelRow(
                                    stringResource(R.string.cat_editor_results, matches.size),
                                    null,
                                    Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                                )
                            }
                            if (matches.isEmpty()) item("no-results") {
                                CategoryEditorMessage(stringResource(R.string.cat_editor_no_results, query.trim()))
                            }
                            itemsIndexed(matches, key = { _, item -> "m-" + item.option.id }) { index, entry ->
                                CategoryEditorRow(
                                    type = type,
                                    entry = entry,
                                    count = section.counts[entry.option.id],
                                    query = query,
                                    shape = cardShape(index, matches.size),
                                    showState = true,
                                    onToggle = { toggle(entry) },
                                    onMoveTop = { moveToEdge(entry, true) },
                                    onMoveBottom = { moveToEdge(entry, false) },
                                )
                            }
                        } else {
                            item("visible-label") {
                                CategoryLabelRow(
                                    stringResource(R.string.cat_editor_visible_section, visibleEntries.size),
                                    stringResource(R.string.cat_editor_drag_hint).takeIf { visibleEntries.size > 1 },
                                    Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                                )
                            }
                            itemsIndexed(visibleEntries, key = { _, item -> "v-" + item.option.id }) { index, entry ->
                                val dragging = dragId == entry.option.id
                                CategoryEditorRow(
                                    type = type,
                                    entry = entry,
                                    count = section.counts[entry.option.id],
                                    query = "",
                                    shape = cardShape(index, visibleEntries.size),
                                    showState = false,
                                    onToggle = { toggle(entry) },
                                    onMoveTop = { moveToEdge(entry, true) },
                                    onMoveBottom = { moveToEdge(entry, false) },
                                    modifier = Modifier
                                        .zIndex(if (dragging) 1f else 0f)
                                        .graphicsLayer {
                                            translationY = if (dragging) dragOffset else 0f
                                            shadowElevation = if (dragging) 18f else 0f
                                        }
                                        .onSizeChanged { if (rowHeight == 0f) rowHeight = it.height.toFloat() },
                                    dragHandle = Modifier.pointerInput(type, entry.option.id, index, visibleEntries.size) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { dragId = entry.option.id; dragOffset = 0f },
                                            onDragEnd = {
                                                val height = rowHeight.takeIf { it > 0f }
                                                val steps = if (height == null) 0 else (dragOffset / height).roundToInt()
                                                val target = (index + steps).coerceIn(visibleEntries.indices)
                                                dragId = null
                                                dragOffset = 0f
                                                if (target != index) {
                                                    // Μετακινούνται οι ορατές· οι κρυφές μένουν μετά από αυτές.
                                                    val reordered = CategoryLayoutPolicy.move(visibleEntries, index, target)
                                                    onLayoutChange(
                                                        type,
                                                        CategoryLayoutPolicy.layoutOf(
                                                            reordered + entries.filterNot { it.visible },
                                                            section.layout,
                                                        ),
                                                    )
                                                }
                                            },
                                            onDragCancel = { dragId = null; dragOffset = 0f },
                                        ) { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                        }
                                    },
                                )
                            }
                            if (hiddenEntries.isNotEmpty()) {
                                item("hidden-header") {
                                    HiddenHeader(
                                        count = hiddenEntries.size,
                                        expanded = hiddenExpanded,
                                        onClick = { hiddenExpanded = !hiddenExpanded },
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
                                    )
                                }
                                if (hiddenExpanded) {
                                    itemsIndexed(hiddenEntries, key = { _, item -> "h-" + item.option.id }) { index, entry ->
                                        CategoryEditorRow(
                                            type = type,
                                            entry = entry,
                                            count = section.counts[entry.option.id],
                                            query = "",
                                            shape = cardShape(index, hiddenEntries.size),
                                            showState = false,
                                            onToggle = { toggle(entry) },
                                            onMoveTop = null,
                                            onMoveBottom = null,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(16.dp),
                containerColor = IptvColors.SurfaceRaised,
                contentColor = IptvColors.TextPrimary,
                actionColor = IptvColors.Glow,
            )
        }
    }
}

private fun cardShape(index: Int, size: Int): Shape {
    val top = if (index == 0) 18.dp else 0.dp
    val bottom = if (index == size - 1) 18.dp else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun CategoryTypeTabs(
    state: CategoryEditorState,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(IptvColors.SurfaceSelected)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        CATEGORY_EDITOR_TYPES.forEach { type ->
            val on = type == selected
            val section = state.section(type)
            val entries = section.entries
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) IptvColors.TextPrimary else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(type) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    categoryTypeLabel(type),
                    color = if (on) IptvColors.Background else IptvColors.TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                if (!section.loading && entries.isNotEmpty()) {
                    Text(
                        stringResource(R.string.cat_editor_visible_of, entries.count { it.visible }, entries.size),
                        color = if (on) IptvColors.Background.copy(alpha = .7f) else IptvColors.TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryLabelRow(title: String, hint: String?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizonGroupLabel(title, Modifier.weight(1f))
        if (hint != null) Text(hint, color = IptvColors.TextTertiary, fontSize = 12.sp)
    }
}

@Composable
private fun CategoryEditorMessage(text: String) {
    Text(
        text,
        color = IptvColors.TextSecondary,
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
    )
}

@Composable
private fun CountryGrid(
    section: CategoryEditorSection,
    groups: List<CategoryCountryPolicy.CountryGroup>,
    onToggle: (CategoryCountryPolicy.CountryGroup, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleIds = section.entries.filter { it.visible }.map { it.option.id }.toSet()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { group ->
                    val on = group.ids.count(visibleIds::contains)
                    val all = on == group.ids.size
                    val some = on in 1 until group.ids.size
                    val shape = RoundedCornerShape(14.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (all) IptvColors.Primary.copy(alpha = .16f) else IptvColors.Surface)
                            .border(
                                1.dp,
                                when {
                                    all -> IptvColors.Primary
                                    some -> IptvColors.Primary.copy(alpha = .45f)
                                    else -> IptvColors.Divider
                                },
                                shape,
                            )
                            .clickable(role = Role.Switch) { onToggle(group, all) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            group.code ?: stringResource(R.string.cat_editor_others),
                            color = if (on > 0) IptvColors.TextPrimary else IptvColors.TextSecondary,
                            fontFamily = AppFonts.Display,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            maxLines = 1,
                        )
                        Text(
                            stringResource(R.string.cat_editor_visible_of, on, group.ids.size),
                            color = if (all) IptvColors.Glow else IptvColors.TextTertiary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun QuickChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(99.dp)
    Row(
        Modifier
            .clip(shape)
            .border(1.dp, IptvColors.DividerStrong, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = IptvColors.TextSecondary, modifier = Modifier.size(16.dp))
        Text(label, color = IptvColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun HiddenHeader(count: Int, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, IptvColors.Divider, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.VisibilityOff, null, tint = IptvColors.TextTertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.cat_editor_hidden_section, count),
            color = IptvColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = IptvColors.TextTertiary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryEditorRow(
    type: String,
    entry: CategoryEntry,
    count: Int?,
    query: String,
    shape: Shape,
    showState: Boolean,
    onToggle: () -> Unit,
    onMoveTop: (() -> Unit)?,
    onMoveBottom: (() -> Unit)?,
    modifier: Modifier = Modifier,
    dragHandle: Modifier? = null,
) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier.padding(horizontal = 16.dp).padding(bottom = 1.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(IptvColors.Surface)
                .combinedClickable(onClick = onToggle, onLongClick = { menu = true })
                .padding(start = if (dragHandle != null) 4.dp else 16.dp, end = 10.dp)
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dragHandle != null) {
                Box(Modifier.size(40.dp).then(dragHandle), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DragHandle,
                        stringResource(R.string.settings_category_move),
                        tint = IptvColors.TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    highlightMatch(entry.option.title, query),
                    color = if (entry.visible) IptvColors.TextPrimary else IptvColors.TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val stateLabel = if (showState) stringResource(
                    if (entry.visible) R.string.cat_editor_state_visible else R.string.cat_editor_state_hidden
                ) else null
                val countLabel = count?.takeIf { it > 0 }?.let { categoryCountLabel(type, it) }
                val parts = listOfNotNull(stateLabel, countLabel)
                if (parts.isNotEmpty()) {
                    Text(parts.joinToString(" · "), color = IptvColors.TextTertiary, fontSize = 12.sp, maxLines = 1)
                }
            }
            HorizonSwitch(checked = entry.visible, onCheckedChange = { onToggle() })
        }
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            containerColor = IptvColors.SurfaceRaised,
        ) {
            if (entry.visible && onMoveTop != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.cat_editor_move_top), color = IptvColors.TextPrimary) },
                    onClick = { menu = false; onMoveTop() },
                )
            }
            if (entry.visible && onMoveBottom != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.cat_editor_move_bottom), color = IptvColors.TextPrimary) },
                    onClick = { menu = false; onMoveBottom() },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(if (entry.visible) R.string.settings_category_hide else R.string.settings_category_show),
                        color = IptvColors.TextPrimary,
                    )
                },
                leadingIcon = {
                    Icon(
                        if (entry.visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null,
                        tint = IptvColors.TextSecondary,
                    )
                },
                onClick = { menu = false; onToggle() },
            )
        }
    }
}
