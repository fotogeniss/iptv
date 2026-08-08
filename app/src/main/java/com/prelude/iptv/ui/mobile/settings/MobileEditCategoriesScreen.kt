package com.prelude.iptv.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.category.CategoryEditorState
import com.prelude.iptv.category.CategoryLayout
import com.prelude.iptv.category.CategoryLayoutPolicy
import kotlin.math.roundToInt

@Composable
internal fun MobileEditCategoriesScreen(
    state: CategoryEditorState,
    onLayoutChange: (type: String, layout: CategoryLayout) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var type by remember { mutableStateOf("live") }
    var addOpen by remember { mutableStateOf(false) }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    val section = state.section(type)
    val entries = section.entries

    Column(modifier.fillMaxSize().background(IptvColors.Background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = IptvColors.TextPrimary)
            }
            Text(
                "Επεξεργασία κατηγοριών",
                color = IptvColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf("live" to "Live TV", "vod" to "Ταινίες", "series" to "Σειρές").forEach { (id, label) ->
                FilterChip(
                    selected = type == id,
                    onClick = { type = id },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IptvColors.Primary,
                        selectedLabelColor = Color.White,
                        containerColor = IptvColors.Surface,
                        labelColor = IptvColors.TextPrimary,
                    ),
                )
            }
        }

        when {
            section.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IptvColors.Primary)
            }
            section.error != null -> Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(section.error, color = IptvColors.TextSecondary)
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                itemsIndexed(entries, key = { _, item -> item.option.id }) { index, entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(IptvColors.Surface)
                            .onSizeChanged { if (rowHeight == 0f) rowHeight = it.height.toFloat() }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            val changed = entries.toMutableList().apply {
                                this[index] = entry.copy(visible = !entry.visible)
                            }
                            onLayoutChange(type, CategoryLayoutPolicy.layoutOf(changed, section.layout))
                        }) {
                            Icon(
                                if (entry.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                if (entry.visible) "Απόκρυψη" else "Εμφάνιση",
                                tint = if (entry.visible) IptvColors.TextPrimary else IptvColors.TextTertiary,
                            )
                        }
                        Text(
                            entry.option.title,
                            color = if (entry.visible) IptvColors.TextPrimary else IptvColors.TextTertiary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            onLayoutChange(type, CategoryLayoutPolicy.delete(section.layout, entry.option.id))
                        }) {
                            Icon(Icons.Default.DeleteOutline, "Διαγραφή", tint = IptvColors.Primary)
                        }
                        Icon(
                            Icons.Default.DragHandle,
                            "Μετακίνηση",
                            tint = IptvColors.TextSecondary,
                            modifier = Modifier
                                .size(30.dp)
                                .pointerInput(type, entry.option.id, index, section.layout.order) {
                                    var totalDrag = 0f
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { totalDrag = 0f },
                                        onDragEnd = {
                                            val height = rowHeight.takeIf { it > 0f } ?: return@detectDragGesturesAfterLongPress
                                            val target = (index + (totalDrag / height).roundToInt())
                                                .coerceIn(entries.indices)
                                            if (target != index) {
                                                val moved = CategoryLayoutPolicy.move(entries, index, target)
                                                onLayoutChange(type, CategoryLayoutPolicy.layoutOf(moved, section.layout))
                                            }
                                        },
                                        onDragCancel = { totalDrag = 0f },
                                    ) { change, amount ->
                                        change.consume()
                                        totalDrag += amount.y
                                    }
                                },
                        )
                    }
                }

                item("add-$type") {
                    TextButton(
                        onClick = { addOpen = true },
                        enabled = section.deletedEntries.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Προσθήκη ομάδας")
                    }
                }
            }
        }

        Button(
            onClick = { onSave(); onBack() },
            enabled = state.sourceId.isNotBlank() && state.sections.values.none { it.loading },
            colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
        ) { Text("Αποθήκευση", fontWeight = FontWeight.Black) }
    }

    if (addOpen) {
        AlertDialog(
            onDismissRequest = { addOpen = false },
            title = { Text("Προσθήκη ομάδας") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(section.deletedEntries.size) { index ->
                        val option = section.deletedEntries[index]
                        Text(
                            option.title,
                            modifier = Modifier.fillMaxWidth().clickable {
                                onLayoutChange(type, CategoryLayoutPolicy.restore(section.layout, option.id))
                                addOpen = false
                            }.padding(vertical = 14.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { addOpen = false }) { Text("Κλείσιμο") } },
        )
    }
}
