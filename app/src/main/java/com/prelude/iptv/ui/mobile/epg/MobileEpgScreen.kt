package com.prelude.iptv.ui.mobile.epg

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.epg.rememberEpgNow
import com.prelude.iptv.ui.epg.EpgFilter
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Πρόγραμμα (EPG) στο κινητό, Horizon (prototypes/redesign_horizon_screens.html, ενότητα 3).
 *
 * Κάθετη λίστα «Τώρα / Μετά» ανά κανάλι, χωρίς οριζόντιο σύρσιμο. Πάνω οι ημέρες
 * και τα φίλτρα, μετά η κάρτα του επιλεγμένου καναλιού. Από κάθε κανάλι ανοίγει
 * το πρόγραμμα όλης της ημέρας του.
 */
@Composable
fun MobileEpgScreen(
    channels: List<Channel>,
    onBack: () -> Unit,
    onProgramClick: (Channel, EpgManager.Prog) -> Unit,
    onChannelClick: (Channel) -> Unit,
    bottomContentPadding: Dp = 0.dp
) {
    val nowMs = rememberEpgNow()
    val todayStart = remember(nowMs / HOUR_MS) { startOfDay(nowMs) }
    var dayOffset by rememberSaveable { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf(EpgFilter.Now) }
    var dayChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    // Σε άλλη ημέρα κοιτάμε την ίδια ώρα του ρολογιού εκείνης της ημέρας.
    val anchorMs = todayStart + dayOffset * DAY_MS + (nowMs - todayStart)
    val rows = remember(channels, anchorMs / 60_000L, filter) {
        channels.mapNotNull { mobileEpgRow(it, anchorMs, filter) }
    }
    val selected = rows.firstOrNull { it.key == selectedKey }
        ?: rows.firstOrNull { it.primary != null && anchorMs in it.primary.startMs until it.primary.stopMs }
        ?: rows.firstOrNull()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    dayChannel?.let { channel ->
        BackHandler { dayChannel = null }
        MobileEpgChannelDay(
            channel = channel,
            todayStart = todayStart,
            nowMs = nowMs,
            initialDayOffset = dayOffset,
            onBack = { dayChannel = null },
            onWatch = { onChannelClick(channel) },
            onProgramClick = { programme -> onProgramClick(channel, programme) },
            bottomContentPadding = bottomContentPadding,
        )
        return
    }

    if (channels.isEmpty()) {
        MobileEpgEmptyState(onBack)
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(IptvColors.Background),
        contentPadding = PaddingValues(bottom = bottomContentPadding + 16.dp)
    ) {
        item(key = "epg-header") {
            MobileEpgTopBar(title = stringResource(R.string.epg_title), onBack = onBack)
        }
        item(key = "epg-days") {
            MobileEpgDayStrip(todayStart = todayStart, selectedOffset = dayOffset, onSelect = { dayOffset = it })
        }
        item(key = "epg-filters") {
            MobileEpgFilterChips(selected = filter, onSelect = { filter = it })
        }
        selected?.let { row ->
            item(key = "epg-card") {
                MobileEpgNowCard(
                    row = row,
                    nowMs = nowMs,
                    onWatch = { onChannelClick(row.channel) },
                    onFullDay = { dayChannel = row.channel },
                )
            }
        }
        item(key = "epg-all-label") {
            MobileEpgSectionLabel(stringResource(R.string.epg_all_channels))
        }
        if (rows.isEmpty()) {
            item(key = "epg-empty-filter") {
                Text(
                    stringResource(R.string.epg_no_program_filter),
                    color = IptvColors.TextTertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp)
                )
            }
        }
        items(rows, key = { "epg-row:${it.key}" }) { row ->
            MobileEpgChannelRow(
                row = row,
                selected = row.key == selected?.key,
                nowMs = nowMs,
                onSelect = {
                    selectedKey = row.key
                    // Η κάρτα είναι μετά την κεφαλίδα, τις ημέρες και τα φίλτρα.
                    scope.launch { listState.animateScrollToItem(3) }
                },
                onOpenDay = { dayChannel = row.channel },
            )
        }
    }
}

/** Ένα κανάλι στη λίστα: τι παίζει στο [anchor] (ή τι έρχεται) και τι ακολουθεί. */
internal class MobileEpgRow(
    val channel: Channel,
    val primary: EpgManager.Prog?,
    val next: EpgManager.Prog?,
) {
    val key: String = channel.tvgId + channel.url
}

internal fun mobileEpgRow(channel: Channel, anchorMs: Long, filter: EpgFilter): MobileEpgRow? {
    val programmes = if (channel.tvgId.isBlank()) emptyList()
    else EpgManager.programmes(channel.tvgId, anchorMs - 4 * HOUR_MS, anchorMs + 8 * HOUR_MS)
    val current = programmes.indexOfFirst { anchorMs in it.startMs until it.stopMs }
    val upcoming = programmes.indexOfFirst { it.startMs > anchorMs }
    val (primary, next) = when {
        filter == EpgFilter.Later -> programmes.getOrNull(upcoming) to programmes.getOrNull(upcoming + 1)
        current >= 0 -> programmes[current] to programmes.getOrNull(current + 1)
        else -> null to programmes.getOrNull(upcoming)
    }
    if (filter == EpgFilter.Movies || filter == EpgFilter.Sports) {
        val terms = if (filter == EpgFilter.Movies) movieTerms else sportsTerms
        val haystack = "${channel.name} ${channel.group} ${channel.genre} ${primary?.title.orEmpty()}"
            .lowercase(Locale.ROOT)
        if (terms.none(haystack::contains)) return null
    }
    if (filter == EpgFilter.Later && primary == null) return null
    return MobileEpgRow(channel, primary, next)
}

internal const val HOUR_MS = 3_600_000L
internal const val DAY_MS = 24 * HOUR_MS

internal fun startOfDay(ms: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ms
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
