package com.prelude.iptv.ui.mobile.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import java.text.NumberFormat
import java.util.Locale

internal data class MobileCategoryOption(
    val id: String,
    val label: String,
    val count: Int,
)

/**
 * Η κοινή, compact εξερεύνηση κατηγοριών για Live, Ταινίες και Σειρές.
 *
 * Η οριζόντια λωρίδα κρατά την αρχική μικρή και άμεσα χρήσιμη. Το «Όλες»
 * ανοίγει το πλήρες σύνολο σε bottom sheet, οπότε μια πηγή με 100+ groups δεν
 * μετατρέπει την κορυφή της οθόνης σε ατελείωτη σειρά από pills.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileCategoryExplorer(
    options: List<MobileCategoryOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    title: String = "Εξερεύνησε",
    hint: String = "Βρες γρήγορα αυτό που θέλεις",
    sheetTitle: String = "Κατηγορίες",
    modifier: Modifier = Modifier,
) {
    if (options.size <= 1) return
    var sheetOpen by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth().padding(top = 19.dp, bottom = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 14.dp, bottom = 11.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = IptvColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    hint,
                    color = IptvColors.TextTertiary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable { sheetOpen = true }
                    .padding(start = 10.dp, end = 2.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Όλες",
                    color = IptvColors.TextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.width(7.dp))
                CategoryGridIcon(IptvColors.TextSecondary)
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(options, key = MobileCategoryOption::id) { option ->
                CompactCategoryCard(
                    option = option,
                    selected = option.id == selectedId,
                    onClick = { onSelect(option.id) },
                )
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            containerColor = Color(0xFF131313),
            contentColor = IptvColors.TextPrimary,
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 14.dp)
                        .size(width = 42.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(IptvColors.TextTertiary.copy(alpha = .55f))
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            ) {
                item(key = "category-sheet-heading") {
                    Text(
                        sheetTitle,
                        color = IptvColors.TextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "Διάλεξε μία κατηγορία για άμεσο φιλτράρισμα",
                        color = IptvColors.TextTertiary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 3.dp, bottom = 16.dp),
                    )
                }
                items(
                    items = options.chunked(2),
                    key = { row -> row.joinToString("|", transform = MobileCategoryOption::id) },
                ) { rowOptions ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        rowOptions.forEach { option ->
                            SheetCategoryCard(
                                option = option,
                                selected = option.id == selectedId,
                                onClick = {
                                    onSelect(option.id)
                                    sheetOpen = false
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCategoryCard(
    option: MobileCategoryOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val background = if (selected) IptvColors.TextPrimary else IptvColors.Surface
    val foreground = if (selected) IptvColors.Background else IptvColors.TextPrimary
    Box(
        Modifier
            .width(112.dp)
            .height(66.dp)
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) Color.White else IptvColors.Divider,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Text(
            option.label,
            color = foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).padding(end = 10.dp),
        )
        Text(
            formatCount(option.count),
            color = if (selected) Color(0xFF505050) else IptvColors.TextTertiary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart),
        )
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(IptvColors.Primary)
            )
        } else {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = .045f))
            )
        }
    }
}

@Composable
private fun SheetCategoryCard(
    option: MobileCategoryOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .height(65.dp)
            .clip(shape)
            .background(if (selected) IptvColors.TextPrimary else IptvColors.Surface)
            .border(
                1.dp,
                if (selected) Color.White else IptvColors.Divider,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            option.label,
            color = if (selected) IptvColors.Background else IptvColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            formatCount(option.count),
            color = if (selected) Color(0xFF505050) else IptvColors.TextTertiary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CategoryGridIcon(color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(2) {
                    Box(
                        Modifier
                            .size(5.dp)
                            .border(1.dp, color, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

private fun formatCount(count: Int): String =
    NumberFormat.getIntegerInstance(Locale("el", "GR")).format(count)
