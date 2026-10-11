package com.prelude.iptv.ui.mobile.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.components.epg.EpgChannelLogo
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.ui.components.epg.epgTime
import com.prelude.iptv.ui.components.epg.epgTimeRange
import com.prelude.iptv.ui.epg.EpgFilter
import com.prelude.iptv.ui.icons.automirrored.filled.ArrowBack
import com.prelude.iptv.ui.icons.filled.ChevronRight
import com.prelude.iptv.ui.localization.labelRes
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Ταξινόμηση κειμένου του παρόχου (όχι κείμενο της εφαρμογής).
internal val movieTerms = listOf("movie", "cinema", "film", "ταιν")
internal val sportsTerms = listOf("sport", "sports", "αθλη")

/** Κεφαλίδα: πίσω και τίτλος με τη display γραμματοσειρά. */
@Composable
internal fun MobileEpgTopBar(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.epg_back), tint = IptvColors.TextPrimary)
        }
        Text(
            title,
            color = IptvColors.TextPrimary,
            fontFamily = AppFonts.Display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Κεφαλαία χωρίς τόνους (σωστή γραφή ελληνικών κεφαλαίων). */
internal fun epgUpper(text: String): String =
    Normalizer.normalize(text.uppercase(), Normalizer.Form.NFD).replace("́", "")

/** Ημέρες: χθες, σήμερα και οι επόμενες πέντε. */
@Composable
internal fun MobileEpgDayStrip(todayStart: Long, selectedOffset: Int, onSelect: (Int) -> Unit) {
    val weekday = SimpleDateFormat("EEE", Locale.getDefault())
    val dayOfMonth = SimpleDateFormat("d", Locale.getDefault())
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items((-1..5).toList()) { offset ->
            val dayMs = todayStart + offset * DAY_MS + 2 * HOUR_MS
            val label = when (offset) {
                -1 -> stringResource(R.string.epg_yesterday)
                0 -> stringResource(R.string.epg_today)
                else -> weekday.format(Date(dayMs)).trimEnd('.')
            }
            val on = offset == selectedOffset
            Column(
                Modifier
                    .width(58.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (on) IptvColors.TextPrimary else IptvColors.Surface)
                    .border(1.dp, if (on) IptvColors.TextPrimary else IptvColors.Divider, RoundedCornerShape(15.dp))
                    .clickable { onSelect(offset) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    epgUpper(label),
                    color = if (on) IptvColors.Background.copy(alpha = 0.7f) else IptvColors.TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    dayOfMonth.format(Date(dayMs)),
                    color = if (on) IptvColors.Background else IptvColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Φίλτρα ώρας/είδους. */
@Composable
internal fun MobileEpgFilterChips(selected: EpgFilter, onSelect: (EpgFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(EpgFilter.entries.filter { it != EpgFilter.All }) { filter ->
            val on = filter == selected
            Text(
                stringResource(filter.labelRes()),
                color = if (on) IptvColors.Background else IptvColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (on) IptvColors.TextPrimary else IptvColors.Surface)
                    .border(1.dp, if (on) IptvColors.TextPrimary else IptvColors.Divider, CircleShape)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
internal fun MobileEpgSectionLabel(title: String) {
    Text(
        epgUpper(title),
        color = IptvColors.TextTertiary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 6.dp)
    )
}

/** Γραμμή καναλιού: τι παίζει, πόσο έχει προχωρήσει, τι ακολουθεί. */
@Composable
internal fun MobileEpgChannelRow(
    row: MobileEpgRow,
    selected: Boolean,
    nowMs: Long,
    onSelect: () -> Unit,
    onOpenDay: () -> Unit,
) {
    val primary = row.primary
    val live = primary != null && nowMs in primary.startMs until primary.stopMs
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.background(
                    Brush.horizontalGradient(listOf(IptvColors.Primary.copy(alpha = 0.12f), Color.Transparent))
                ) else Modifier
            )
            .clickable(onClick = onSelect)
    ) {
        if (selected) {
            // Ο δείκτης επιλογής: η κόκκινη γραμμή, εδώ κάθετα στην αριστερή άκρη.
            Box(Modifier.matchParentSize().padding(vertical = 14.dp)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(IptvColors.Primary, RoundedCornerShape(2.dp)))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EpgChannelLogo(row.channel, Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.channel.name,
                        color = IptvColors.TextTertiary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (primary != null) {
                        Text(epgTimeRange(primary), color = IptvColors.TextTertiary, fontSize = 12.sp, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (live) {
                        LiveMark()
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        primary?.title ?: stringResource(R.string.epg_no_guide),
                        color = if (primary != null) IptvColors.TextPrimary else IptvColors.TextTertiary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (live && primary != null) {
                    Spacer(Modifier.height(6.dp))
                    StreamingProgress(epgProgress(primary, nowMs), Modifier.fillMaxWidth())
                }
                row.next?.let { next ->
                    Spacer(Modifier.height(5.dp))
                    Text(
                        stringResource(R.string.epg_then, epgTime(next.startMs), next.title),
                        color = IptvColors.TextTertiary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onOpenDay) {
                Icon(
                    Icons.Default.ChevronRight,
                    stringResource(R.string.epg_full_day),
                    tint = IptvColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** «● LIVE» με τη λάμψη του Horizon. */
@Composable
internal fun LiveMark(label: String = stringResource(R.string.epg_live)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(IptvColors.Primary, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, color = IptvColors.Glow, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
    }
}
