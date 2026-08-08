package com.prelude.iptv.ui.mobile.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.home.HomeEntry
import com.prelude.iptv.ui.home.HomeLayoutPolicy
import kotlin.math.roundToInt

/**
 * «Επεξεργασία αρχικής»: τι φαίνεται, με ποια σειρά, και από ποια κατηγορία.
 *
 * ΓΙΑΤΙ ΟΛΑ ΣΕ ΜΙΑ ΛΙΣΤΑ: η προηγούμενη εκδοχή είχε τις ίδιες αποφάσεις
 * σκορπισμένες σε τρία σημεία — chips κατηγοριών στην κορυφή της αρχικής, ένα
 * «Επεξεργασία» για τις ομάδες, και τίποτα για τη σειρά. Ο χρήστης δεν είχε πού
 * να πάει για να δει τι θα δει· έψαχνε.
 *
 * Η οθόνη δεν αποθηκεύει μόνη της. Κάθε αλλαγή φεύγει προς τα έξω αμέσως, γιατί
 * μια οθόνη ρυθμίσεων με «Αποθήκευση» είναι μια οθόνη που μπορείς να φύγεις από
 * πάνω της και να χάσεις τη δουλειά σου.
 */
@Composable
internal fun MobileEditHomeScreen(
    entries: List<HomeEntry>,
    /** Επιλεγμένη κατηγορία ανά ενότητα (μόνο για όσες το υποστηρίζουν). */
    categoryOf: (String) -> String,
    /** Διαθέσιμες κατηγορίες ανά ενότητα — άδεια σημαίνει «δεν έχει τι να διαλέξει». */
    categoriesFor: (String) -> List<String>,
    onToggleVisible: (String) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onPickCategory: (sectionId: String, group: String) -> Unit,
    onClear: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = true, onBack = onBack)

    // Ποια γραμμή σέρνεται τώρα, και πόσο έχει μετακινηθεί από τη θέση της.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }
    var categoryFor by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf<HomeEntry?>(null) }

    Column(modifier.fillMaxSize().background(IptvColors.Background)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "Πίσω",
                tint = IptvColors.TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(onClick = onBack)
                    .padding(4.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Επεξεργασία αρχικής",
                color = IptvColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
        }

        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(14.dp)
        ) {
            item(key = "edit-home-list") {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(IptvColors.Surface)
                ) {
                    entries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            Box(
                                Modifier.fillMaxWidth().height(1.dp)
                                    .background(IptvColors.Divider)
                            )
                        }
                        EditRow(
                            entry = entry,
                            index = index,
                            dragging = dragIndex == index,
                            dragOffset = if (dragIndex == index) dragOffset else 0f,
                            category = if (entry.section.categorised) categoryOf(entry.section.id) else "",
                            hasCategories = categoriesFor(entry.section.id).isNotEmpty(),
                            onToggleVisible = { onToggleVisible(entry.section.id) },
                            onPickCategory = { categoryFor = entry.section.id },
                            onClear = { confirmClear = entry },
                            onMeasured = { rowHeightPx = it },
                            onDragStart = { dragIndex = index; dragOffset = 0f },
                            onDrag = { delta ->
                                dragOffset += delta
                            },
                            onDragEnd = {
                                val from = dragIndex
                                val height = rowHeightPx.takeIf { it > 0f }
                                val target = if (from != null && height != null) {
                                    (from + (dragOffset / height).roundToInt()).coerceIn(entries.indices)
                                } else from
                                dragIndex = null
                                dragOffset = 0f
                                if (from != null && target != null && from != target) onMove(from, target)
                            }
                        )
                    }
                }
                Text(
                    "Το μάτι κρύβει την ενότητα από την αρχική. Κράτα πατημένο πάνω στις " +
                        "γραμμές δεξιά και σύρε για να αλλάξεις σειρά. Οι δύο πρώτες " +
                        "γραμμές είναι σταθερές.",
                    color = IptvColors.TextTertiary,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }

    categoryFor?.let { sectionId ->
        CategoryPicker(
            title = entries.firstOrNull { it.section.id == sectionId }?.section?.title.orEmpty(),
            options = categoriesFor(sectionId),
            selected = categoryOf(sectionId),
            onPick = { onPickCategory(sectionId, it); categoryFor = null },
            onDismiss = { categoryFor = null }
        )
    }

    confirmClear?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmClear = null },
            title = { Text(entry.section.title) },
            text = { Text("Να διαγραφεί το ιστορικό αυτής της ενότητας; Δεν αναιρείται.") },
            confirmButton = {
                TextButton(onClick = { onClear(entry.section.id); confirmClear = null }) {
                    Text("Καθάρισμα", color = IptvColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = null }) { Text("Άκυρο") }
            }
        )
    }
}

@Composable
private fun EditRow(
    entry: HomeEntry,
    index: Int,
    dragging: Boolean,
    dragOffset: Float,
    category: String,
    hasCategories: Boolean,
    onToggleVisible: () -> Unit,
    onPickCategory: () -> Unit,
    onClear: () -> Unit,
    onMeasured: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val fixed = entry.section.fixed
    Row(
        Modifier
            .fillMaxWidth()
            .zIndex(if (dragging) 1f else 0f)
            .offset { IntOffset(0, dragOffset.toInt()) }
            .background(if (dragging) IptvColors.SurfaceRaised else Color.Transparent)
            .onSizeChanged { if (index == 0) onMeasured(it.height.toFloat()) }
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Το μάτι λείπει από τις σταθερές αντί να είναι εκεί και ανενεργό: ένα
        // κουμπί που δεν κάνει τίποτα είναι πρόσκληση να το πατήσεις ξανά.
        if (fixed) {
            Spacer(Modifier.width(22.dp))
        } else {
            Icon(
                if (entry.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                if (entry.visible) "Κρύψε" else "Εμφάνισε",
                tint = IptvColors.TextPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(onClick = onToggleVisible)
            )
        }
        Spacer(Modifier.width(13.dp))
        Text(
            entry.section.title,
            color = if (fixed) IptvColors.TextTertiary else IptvColors.TextPrimary,
            fontSize = 14.5.sp,
            fontWeight = if (fixed) FontWeight.SemiBold else FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).alpha(if (!fixed && !entry.visible) .45f else 1f)
        )
        if (entry.section.categorised && hasCategories) {
            Text(
                category.ifBlank { "Επιλογή" },
                color = IptvColors.Info,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 130.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(onClick = onPickCategory)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        if (entry.section.clearable) {
            Text(
                "Καθάρισμα",
                color = IptvColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        if (fixed) {
            Spacer(Modifier.width(22.dp))
        } else {
            Icon(
                Icons.Default.DragHandle, "Αλλαγή σειράς",
                tint = IptvColors.TextSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .pointerInput(entry.section.id, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, amount -> change.consume(); onDrag(amount.y) }
                        )
                    }
            )
        }
    }
}

@Composable
private fun CategoryPicker(
    title: String,
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                itemsIndexed(options, key = { _, group -> group }) { _, group ->
                    val isSelected = group == selected
                    Text(
                        group,
                        color = if (isSelected) IptvColors.Info else IptvColors.TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(group) }
                            .padding(vertical = 12.dp, horizontal = 6.dp)
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Άκυρο") } },
        confirmButton = {}
    )
}
