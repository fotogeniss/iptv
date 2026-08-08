package com.prelude.iptv.ui.mobile.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.epg.epgWindow
import com.prelude.iptv.ui.components.epg.programmesFor
import com.prelude.iptv.ui.components.epg.rememberEpgNow

@Composable
fun MobileEpgScreen(
    channels: List<Channel>,
    onBack: () -> Unit,
    onProgramClick: (Channel, EpgManager.Prog) -> Unit,
    onChannelClick: (Channel) -> Unit,
    bottomContentPadding: Dp = 0.dp
) {
    val nowMs = rememberEpgNow()
    val window = remember(nowMs / 1_800_000L) { epgWindow(nowMs) }
    val programmes = remember(channels, window) { channels.map { programmesFor(it, window) } }
    val initialChannel = remember(channels, programmes, nowMs) {
        programmes.indexOfFirst { row -> row.any { nowMs in it.startMs until it.stopMs } }
            .takeIf { it >= 0 }
            ?: programmes.indexOfFirst { it.isNotEmpty() }.coerceAtLeast(0)
    }
    val initialProgramme = remember(initialChannel, programmes, nowMs) {
        programmes.getOrNull(initialChannel).orEmpty()
            .indexOfFirst { nowMs in it.startMs until it.stopMs }
            .coerceAtLeast(0)
    }

    var selectedChannelIndex by remember(channels, window) { mutableStateOf(initialChannel) }
    var selectedProgrammeIndex by remember(channels, window) { mutableStateOf(initialProgramme) }
    var selectedTab by remember { mutableStateOf(MobileEpgTab.Now) }
    var selectedTimeMs by remember(window) { mutableStateOf(nowMs) }

    val selectedChannel = channels.getOrNull(selectedChannelIndex) ?: channels.firstOrNull()
    val selectedRow = programmes.getOrNull(selectedChannelIndex).orEmpty()
    val selectedProgramme = selectedRow.getOrNull(selectedProgrammeIndex) ?: selectedRow.firstOrNull()
    val nextProgramme = selectedProgramme?.let { selectedRow.getOrNull(selectedRow.indexOf(it) + 1) }
    val listState = rememberLazyListState()

    LaunchedEffect(channels.size, selectedChannelIndex) {
        if (channels.isNotEmpty()) {
            selectedChannelIndex = selectedChannelIndex.coerceIn(0, channels.lastIndex)
        }
    }

    if (selectedChannel == null) {
        MobileEpgEmptyState(onBack)
        return
    }

    Box(Modifier.fillMaxSize().background(IptvColors.Background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomContentPadding)
        ) {
            item(key = "hero") {
                MobileEpgHero(
                    channel = selectedChannel,
                    programme = selectedProgramme,
                    nextProgramme = nextProgramme,
                    nowMs = nowMs,
                    onBack = onBack,
                    onWatch = {
                        selectedProgramme?.let { onProgramClick(selectedChannel, it) }
                            ?: onChannelClick(selectedChannel)
                    },
                    onChannelClick = { onChannelClick(selectedChannel) }
                )
            }
            item(key = "guideHeader") {
                MobileEpgGuideHeader(
                    window = window,
                    nowMs = nowMs,
                    selectedTab = selectedTab,
                    selectedTimeMs = selectedTimeMs,
                    onTabSelected = { selectedTab = it },
                    onTimeSelected = { timeMs ->
                        selectedTimeMs = timeMs
                        selectedProgrammeIndex = nearestMobileProgramme(selectedRow, timeMs)
                    }
                )
            }
            itemsIndexed(
                items = channels,
                key = { _, channel -> channel.tvgId + channel.url }
            ) { channelIndex, channel ->
                MobileEpgChannelCard(
                    channel = channel,
                    channelIndex = channelIndex,
                    programmes = filteredMobileProgrammes(
                        channel = channel,
                        programmes = programmes.getOrNull(channelIndex).orEmpty(),
                        tab = selectedTab,
                        nowMs = nowMs
                    ),
                    nowMs = nowMs,
                    selectedChannelIndex = selectedChannelIndex,
                    selectedProgrammeIndex = selectedProgrammeIndex,
                    onChannelSelected = {
                        selectedChannelIndex = channelIndex
                        selectedProgrammeIndex = nearestMobileProgramme(
                            programmes.getOrNull(channelIndex).orEmpty(),
                            selectedTimeMs
                        )
                    },
                    onOpenChannel = { onChannelClick(channel) },
                    onProgramSelected = { programmeIndex ->
                        selectedChannelIndex = channelIndex
                        selectedProgrammeIndex = programmeIndex
                    },
                    onProgramAction = { programmeIndex ->
                        programmes.getOrNull(channelIndex)?.getOrNull(programmeIndex)?.let { programme ->
                            onProgramClick(channel, programme)
                        }
                    }
                )
            }
            item(key = "guideFooter") { MobileEpgGuideFooter() }
        }
    }
}

private fun nearestMobileProgramme(programmes: List<EpgManager.Prog>, anchorMs: Long): Int {
    if (programmes.isEmpty()) return 0
    val containing = programmes.indexOfFirst { anchorMs in it.startMs until it.stopMs }
    if (containing >= 0) return containing
    return programmes.indices.minByOrNull { index ->
        kotlin.math.abs(programmes[index].startMs - anchorMs)
    } ?: 0
}
