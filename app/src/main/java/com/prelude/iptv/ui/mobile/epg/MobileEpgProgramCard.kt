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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.ui.components.epg.epgTime
import com.prelude.iptv.ui.components.epg.epgTimeRange
import com.prelude.iptv.ui.icons.filled.PlayArrow

/**
 * Όλη η ημέρα ενός καναλιού. Τα περασμένα θαμπώνουν και η λίστα ανοίγει στο «τώρα»,
 * ώστε να βρίσκεις αμέσως πού είσαι. Πάτημα σε πρόγραμμα: παίζει αν είναι τώρα,
 * catch-up αν πέρασε, υπενθύμιση αν έρχεται.
 */
@Composable
internal fun MobileEpgChannelDay(
    channel: Channel,
    todayStart: Long,
    nowMs: Long,
    initialDayOffset: Int,
    onBack: () -> Unit,
    onWatch: () -> Unit,
    onProgramClick: (EpgManager.Prog) -> Unit,
    bottomContentPadding: Dp,
) {
    var dayOffset by remember { mutableIntStateOf(initialDayOffset) }
    val dayStart = todayStart + dayOffset * DAY_MS
    val programmes = remember(channel, dayStart) {
        if (channel.tvgId.isBlank()) emptyList()
        else EpgManager.programmes(channel.tvgId, dayStart, dayStart + DAY_MS)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(programmes) {
        val current = programmes.indexOfFirst { nowMs in it.startMs until it.stopMs }
        // +2: κεφαλίδα και ημέρες· αφήνουμε ένα περασμένο από πάνω για συνέχεια.
        if (current > 0) listState.scrollToItem(current + 1)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(IptvColors.Background),
        contentPadding = PaddingValues(bottom = bottomContentPadding + 16.dp)
    ) {
        item(key = "day-header") {
            MobileEpgTopBar(title = channel.name, onBack = onBack) {
                IconButton(onClick = onWatch) {
                    Icon(Icons.Default.PlayArrow, stringResource(R.string.epg_watch), tint = IptvColors.TextPrimary)
                }
            }
        }
        item(key = "day-strip") {
            MobileEpgDayStrip(todayStart = todayStart, selectedOffset = dayOffset, onSelect = { dayOffset = it })
            Spacer(Modifier.height(14.dp))
        }
        if (programmes.isEmpty()) {
            item(key = "day-empty") {
                Text(
                    stringResource(R.string.epg_no_guide),
                    color = IptvColors.TextTertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp)
                )
            }
        }
        itemsIndexed(programmes, key = { _, programme -> "day:${programme.startMs}" }) { index, programme ->
            MobileEpgDayRow(
                programme = programme,
                nowMs = nowMs,
                first = index == 0,
                last = index == programmes.lastIndex,
                onClick = { onProgramClick(programme) },
            )
        }
    }
}

@Composable
private fun MobileEpgDayRow(
    programme: EpgManager.Prog,
    nowMs: Long,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
) {
    val live = nowMs in programme.startMs until programme.stopMs
    val past = programme.stopMs <= nowMs
    val top = if (first) 18.dp else 0.dp
    val bottom = if (last) 18.dp else 0.dp
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    Box(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .background(IptvColors.Surface, shape)
            .border(0.5.dp, IptvColors.Divider, shape)
            .then(
                if (live) Modifier.background(
                    Brush.horizontalGradient(listOf(IptvColors.Primary.copy(alpha = 0.12f), Color.Transparent)),
                    shape
                ) else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        if (live) {
            Box(Modifier.matchParentSize().padding(vertical = 14.dp)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(IptvColors.Primary, RoundedCornerShape(2.dp)))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp).alpha(if (past) 0.5f else 1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                epgTime(programme.startMs),
                color = if (live) IptvColors.Glow else IptvColors.TextTertiary,
                fontSize = 12.5.sp,
                fontWeight = if (live) FontWeight.ExtraBold else FontWeight.SemiBold,
                modifier = Modifier.width(46.dp).padding(top = 2.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (live) {
                        LiveMark(stringResource(R.string.epg_now_badge))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        programme.title,
                        color = IptvColors.TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (live) StreamingProgress(epgProgress(programme, nowMs), Modifier.fillMaxWidth().padding(vertical = 3.dp))
                Text(epgTimeRange(programme), color = IptvColors.TextTertiary, fontSize = 12.sp)
            }
        }
    }
}
