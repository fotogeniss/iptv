package com.prelude.iptv.ui.tv.epg

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.components.epg.EpgCinematicBackdrop
import com.prelude.iptv.ui.components.epg.epgWindow
import com.prelude.iptv.ui.components.epg.programmesFor
import com.prelude.iptv.ui.components.epg.rememberEpgNow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TvEpgScreen(
    channels: List<Channel>,
    onBack: () -> Unit,
    onProgramClick: (Channel, EpgManager.Prog) -> Unit,
    onChannelClick: (Channel) -> Unit
) {
    if (channels.isEmpty()) {
        TvEpgEmptyState(onBack = onBack)
        return
    }

    val nowMs = rememberEpgNow()
    val window = remember(nowMs / 1_800_000L) { epgWindow(nowMs) }
    val programmes = remember(channels, window) {
        channels.map { programmesFor(it, window) }
    }
    val initialChannel = remember(channels, programmes, nowMs) {
        programmes.indexOfFirst { row -> row.any { nowMs in it.startMs until it.stopMs } }
            .takeIf { it >= 0 }
            ?: programmes.indexOfFirst { it.isNotEmpty() }.coerceAtLeast(0)
    }
    var channelIndex by remember(channels, window) { mutableStateOf(initialChannel) }
    var programmeIndex by remember(channels, window) {
        val row = programmes.getOrNull(initialChannel).orEmpty()
        mutableStateOf(row.indexOfFirst { nowMs in it.startMs until it.stopMs }.coerceAtLeast(0))
    }
    var activatedKey by remember { mutableStateOf<String?>(null) }

    val selectedChannel = channels.getOrNull(channelIndex) ?: channels.first()
    val selectedRow = programmes.getOrNull(channelIndex).orEmpty()
    val selectedProgramme = selectedRow.getOrNull(programmeIndex) ?: selectedRow.firstOrNull()
    val nextProgramme = selectedProgramme?.let { selectedRow.getOrNull(selectedRow.indexOf(it) + 1) }

    val horizontalState = rememberScrollState()
    val verticalState = rememberLazyListState()
    val focusRequester = rememberInitialFocus(key = channels.map { it.tvgId.ifBlank { it.name } })
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dpPerMinute = 6.dp

    fun moveChannel(delta: Int) {
        if (channels.isEmpty()) return
        val anchor = selectedProgramme?.startMs ?: nowMs
        var candidate = channelIndex
        repeat(channels.size) {
            candidate = (candidate + delta).coerceIn(0, channels.lastIndex)
            val row = programmes.getOrNull(candidate).orEmpty()
            if (row.isNotEmpty() || candidate == 0 || candidate == channels.lastIndex) {
                channelIndex = candidate
                programmeIndex = nearestProgrammeIndex(row, anchor)
                return
            }
        }
    }

    LaunchedEffect(channelIndex, programmeIndex, selectedProgramme, horizontalState.maxValue) {
        verticalState.animateScrollToItem(channelIndex.coerceAtLeast(0))
        val programme = selectedProgramme ?: return@LaunchedEffect
        delay(30)
        val minute = ((programme.startMs - window.startMs) / 60_000L).toInt().coerceAtLeast(0)
        val target = with(density) {
            ((minute * dpPerMinute.value).dp - 210.dp).roundToPx()
        }.coerceIn(0, horizontalState.maxValue)
        horizontalState.animateScrollTo(target)
    }


    Box(
        Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        programmeIndex = (programmeIndex - 1).coerceAtLeast(0)
                        true
                    }
                    Key.DirectionRight -> {
                        programmeIndex = (programmeIndex + 1).coerceAtMost(selectedRow.lastIndex.coerceAtLeast(0))
                        true
                    }
                    Key.DirectionUp -> {
                        moveChannel(-1)
                        true
                    }
                    Key.DirectionDown -> {
                        moveChannel(1)
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        selectedProgramme?.let {
                            activatedKey = programmeKey(selectedChannel, it)
                            onProgramClick(selectedChannel, it)
                        } ?: onChannelClick(selectedChannel)
                        true
                    }
                    else -> false
                }
            }
            .focusable()
    ) {
        EpgCinematicBackdrop(channel = selectedChannel)
        TvEpgTopBar(onBack = onBack, nowMs = nowMs)
        TvEpgHero(
            channel = selectedChannel,
            programme = selectedProgramme,
            nextProgramme = nextProgramme,
            nowMs = nowMs,
            onWatch = {
                selectedProgramme?.let { onProgramClick(selectedChannel, it) }
                    ?: onChannelClick(selectedChannel)
            }
        )
        TvEpgDock(
            channels = channels,
            programmes = programmes,
            window = window,
            nowMs = nowMs,
            selectedChannelIndex = channelIndex,
            selectedProgrammeIndex = programmeIndex,
            activatedKey = activatedKey,
            horizontalState = horizontalState,
            verticalState = verticalState,
            onProgramTap = { channel, programme ->
                val ci = channels.indexOf(channel).coerceAtLeast(0)
                channelIndex = ci
                programmeIndex = programmes.getOrNull(ci).orEmpty().indexOf(programme).coerceAtLeast(0)
                activatedKey = programmeKey(channel, programme)
                onProgramClick(channel, programme)
            },
            onChannelTap = onChannelClick,
            onJumpToNow = {
                val currentChannel = programmes.indexOfFirst { row -> row.any { nowMs in it.startMs until it.stopMs } }
                    .takeIf { it >= 0 } ?: channelIndex
                channelIndex = currentChannel
                programmeIndex = programmes.getOrNull(currentChannel).orEmpty()
                    .indexOfFirst { nowMs in it.startMs until it.stopMs }
                    .coerceAtLeast(0)
            },
            onShiftTimeline = { minutes ->
                scope.launch {
                    val px = with(density) { (minutes * dpPerMinute.value).dp.roundToPx() }
                    horizontalState.animateScrollTo((horizontalState.value + px).coerceIn(0, horizontalState.maxValue))
                }
            }
        )
    }
}

private fun nearestProgrammeIndex(programmes: List<EpgManager.Prog>, anchorMs: Long): Int {
    if (programmes.isEmpty()) return 0
    return programmes.indices.minByOrNull { index ->
        kotlin.math.abs(programmes[index].startMs - anchorMs)
    } ?: 0
}

internal fun programmeKey(channel: Channel, programme: EpgManager.Prog): String =
    "${channel.tvgId}|${channel.url}|${programme.startMs}"
