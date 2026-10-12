package com.prelude.iptv.ui.tv.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.category.CategoryCountryPolicy
import com.prelude.iptv.category.CategoryEditorState
import com.prelude.iptv.category.CategoryEntry
import com.prelude.iptv.category.CategoryLayout
import com.prelude.iptv.category.CategoryLayoutPolicy
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.mobile.settings.CATEGORY_EDITOR_TYPES
import com.prelude.iptv.ui.mobile.settings.categoryTypeLabel
import com.prelude.iptv.ui.requestFocusWithRetry
import com.prelude.iptv.ui.tvConfirm
import com.prelude.iptv.ui.tvFocus
import kotlinx.coroutines.launch

private enum class TvCategoryColumn { VISIBLE, HIDDEN }

/**
 * Επεξεργασία κατηγοριών στην τηλεόραση (prototypes/redesign_categories.html).
 *
 * Δύο στήλες, ορατές αριστερά και κρυφές δεξιά. OK μεταφέρει από τη μία στην
 * άλλη. Παρατεταμένο OK «σηκώνει» μια ορατή κατηγορία· ▲▼ τη μετακινούν, OK ή
 * Πίσω την αφήνουν. Η νέα σειρά αποθηκεύεται μία φορά, στο άφημα, ώστε να μην
 * ξαναχτίζεται ο κατάλογος σε κάθε βήμα.
 */
@Composable
internal fun TvCategoryEditorScreen(
    state: CategoryEditorState,
    onLayoutChange: (type: String, layout: CategoryLayout) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var type by remember { mutableStateOf("live") }
    var liftedOrder by remember { mutableStateOf<List<CategoryEntry>?>(null) }
    var liftedId by remember { mutableStateOf<String?>(null) }
    var pendingFocus by remember { mutableStateOf<Pair<TvCategoryColumn, Int>?>(null) }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val firstTab = remember { FocusRequester() }
    val visibleList = rememberLazyListState()
    val hiddenList = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val section = state.section(type)
    val entries = section.entries
    val visibleEntries = liftedOrder ?: entries.filter { it.visible }
    val hiddenEntries = entries.filterNot { it.visible }.sortedBy { it.option.title.lowercase() }
    val countries = remember(section.available) { CategoryCountryPolicy.groups(section.available) }

    fun requester(id: String) = requesters.getOrPut(id) { FocusRequester() }

    fun drop() {
        val order = liftedOrder
        liftedOrder = null
        liftedId = null
        if (order != null) {
            onLayoutChange(type, CategoryLayoutPolicy.layoutOf(order + entries.filterNot { it.visible }, section.layout))
        }
    }

    BackHandler {
        if (liftedId != null) drop() else onBack()
    }

    LaunchedEffect(Unit) { firstTab.requestFocusWithRetry() }

    // Μετά από μεταφορά η γραμμή φεύγει από τη στήλη· το focus πάει στη διπλανή,
    // αλλιώς το τηλεχειριστήριο θα έμενε χωρίς σημείο εκκίνησης.
    LaunchedEffect(pendingFocus, visibleEntries.size, hiddenEntries.size) {
        val (column, index) = pendingFocus ?: return@LaunchedEffect
        val list = if (column == TvCategoryColumn.VISIBLE) visibleEntries else hiddenEntries
        val other = if (column == TvCategoryColumn.VISIBLE) hiddenEntries else visibleEntries
        val target = list.getOrNull(index.coerceAtMost(list.lastIndex)) ?: other.firstOrNull()
        pendingFocus = null
        if (target != null) requester(target.option.id).requestFocusWithRetry() else firstTab.requestFocusWithRetry()
    }

    fun transfer(entry: CategoryEntry, column: TvCategoryColumn, index: Int) {
        onLayoutChange(
            type,
            CategoryLayoutPolicy.layoutOf(
                CategoryLayoutPolicy.setVisible(entries, setOf(entry.option.id), !entry.visible),
                section.layout,
            ),
        )
        pendingFocus = column to index
    }

    fun step(delta: Int, listState: LazyListState) {
        val order = liftedOrder ?: return
        val id = liftedId ?: return
        val from = order.indexOfFirst { it.option.id == id }
        val to = (from + delta).coerceIn(order.indices)
        if (from < 0 || to == from) return
        liftedOrder = CategoryLayoutPolicy.move(order, from, to)
        scope.launch {
            val shown = listState.layoutInfo.visibleItemsInfo.map { it.index }
            if (shown.isNotEmpty() && (to <= shown.first() || to >= shown.last())) {
                listState.animateScrollToItem((to - 2).coerceAtLeast(0))
            }
            requester(id).requestFocusWithRetry()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(IptvColors.Background)
            .padding(horizontal = 48.dp, vertical = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.cat_editor_title),
                color = IptvColors.TextPrimary,
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
            )
            Spacer(Modifier.width(16.dp))
            CATEGORY_EDITOR_TYPES.forEachIndexed { index, item ->
                val itemEntries = state.section(item).entries
                val label = categoryTypeLabel(item) +
                    if (itemEntries.isEmpty()) "" else " · " + stringResource(
                        R.string.cat_editor_visible_of, itemEntries.count { it.visible }, itemEntries.size,
                    )
                TvEditorChip(
                    label = label,
                    selected = item == type,
                    modifier = if (index == 0) Modifier.focusRequester(firstTab) else Modifier,
                ) {
                    if (liftedId != null) drop()
                    type = item
                }
            }
        }

        if (countries.isNotEmpty() && !section.loading) {
            val visibleIds = entries.filter { it.visible }.map { it.option.id }.toSet()
            Row(
                Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                countries.forEach { group ->
                    val on = group.ids.count(visibleIds::contains)
                    val all = on == group.ids.size
                    val name = group.code ?: stringResource(R.string.cat_editor_others)
                    TvEditorChip(
                        label = "$name  " + stringResource(R.string.cat_editor_visible_of, on, group.ids.size),
                        selected = all,
                        partial = on in 1 until group.ids.size,
                    ) {
                        onLayoutChange(
                            type,
                            CategoryLayoutPolicy.layoutOf(
                                CategoryLayoutPolicy.setVisible(entries, group.ids.toSet(), !all),
                                section.layout,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        when {
            section.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IptvColors.Primary)
            }
            section.error != null -> TvEditorMessage(stringResource(R.string.settings_category_load_failed), Modifier.weight(1f))
            entries.isEmpty() -> TvEditorMessage(stringResource(R.string.cat_editor_empty), Modifier.weight(1f))
            else -> Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                TvCategoryColumnView(
                    title = stringResource(R.string.cat_editor_visible_section, visibleEntries.size),
                    empty = stringResource(R.string.cat_editor_tv_none_visible),
                    entries = visibleEntries,
                    counts = section.counts,
                    listState = visibleList,
                    liftedId = liftedId,
                    requester = { id -> requester(id) },
                    onConfirm = { entry, index ->
                        if (liftedId != null) drop() else transfer(entry, TvCategoryColumn.VISIBLE, index)
                    },
                    onLongConfirm = { entry, _ ->
                        if (liftedId == null) {
                            liftedOrder = visibleEntries
                            liftedId = entry.option.id
                        } else {
                            drop()
                        }
                    },
                    onStep = { delta -> step(delta, visibleList) },
                    modifier = Modifier.weight(1f),
                )
                TvCategoryColumnView(
                    title = stringResource(R.string.cat_editor_hidden_section, hiddenEntries.size),
                    empty = stringResource(R.string.cat_editor_tv_none_hidden),
                    entries = hiddenEntries,
                    counts = section.counts,
                    listState = hiddenList,
                    liftedId = null,
                    requester = { id -> requester(id) },
                    onConfirm = { entry, index ->
                        if (liftedId != null) drop() else transfer(entry, TvCategoryColumn.HIDDEN, index)
                    },
                    onLongConfirm = null,
                    onStep = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            stringResource(if (liftedId != null) R.string.cat_editor_tv_lifted else R.string.cat_editor_tv_hint),
            color = if (liftedId != null) IptvColors.Glow else IptvColors.TextTertiary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun TvEditorMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text, color = IptvColors.TextSecondary, fontSize = 18.sp)
    }
}

@Composable
private fun TvEditorChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    partial: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(99.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) IptvColors.TextPrimary else IptvColors.Surface)
            .border(
                1.dp,
                when {
                    selected -> IptvColors.TextPrimary
                    partial -> IptvColors.Primary
                    else -> IptvColors.DividerStrong
                },
                shape,
            )
            .tvFocus(shape, tint = false)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            color = if (selected) IptvColors.Background else IptvColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun TvCategoryColumnView(
    title: String,
    empty: String,
    entries: List<CategoryEntry>,
    counts: Map<String, Int>,
    listState: LazyListState,
    liftedId: String?,
    requester: (String) -> FocusRequester,
    onConfirm: (CategoryEntry, Int) -> Unit,
    onLongConfirm: ((CategoryEntry, Int) -> Unit)?,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(IptvColors.Surface)
            .padding(top = 18.dp),
    ) {
        Text(
            title,
            color = IptvColors.TextPrimary,
            fontFamily = AppFonts.Display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (entries.isEmpty()) {
            Text(
                empty,
                color = IptvColors.TextTertiary,
                fontSize = 15.sp,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(entries, key = { _, item -> item.option.id }) { index, entry ->
                val lifted = entry.option.id == liftedId
                val shape = RoundedCornerShape(12.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(requester(entry.option.id))
                        .onPreviewKeyEvent { event ->
                            if (!lifted || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionUp -> { onStep(-1); true }
                                Key.DirectionDown -> { onStep(1); true }
                                // Όσο είναι σηκωμένη, δεν φεύγει στη διπλανή στήλη.
                                Key.DirectionLeft, Key.DirectionRight -> true
                                else -> false
                            }
                        }
                        .tvConfirm(
                            onClick = { onConfirm(entry, index) },
                            onLongClick = onLongConfirm?.let { long -> { long(entry, index) } },
                        )
                        .clip(shape)
                        .background(if (lifted) IptvColors.SurfaceSelected else IptvColors.Surface)
                        .border(1.dp, if (lifted) IptvColors.Primary else IptvColors.Surface, shape)
                        .tvFocus(shape)
                        .clickable { onConfirm(entry, index) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        entry.option.title,
                        color = IptvColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    val trailing = when {
                        lifted -> "▲▼"
                        else -> counts[entry.option.id]?.takeIf { it > 0 }?.toString()
                    }
                    if (trailing != null) {
                        Text(
                            trailing,
                            color = if (lifted) IptvColors.Glow else IptvColors.TextTertiary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
