package com.prelude.iptv.ui.tv.epg

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
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
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.epg.EpgChannelLogo
import com.prelude.iptv.ui.components.epg.EpgProgramVisualState
import com.prelude.iptv.ui.components.epg.EpgWindow
import com.prelude.iptv.ui.components.epg.epgTime

private val TvChannelWidth = 188.dp
private val TvRowHeight = 92.dp
internal val TvMinuteWidth = 6.dp

@Composable
internal fun TvEpgDock(
    channels: List<Channel>,
    programmes: List<List<EpgManager.Prog>>,
    window: EpgWindow,
    nowMs: Long,
    selectedChannelIndex: Int,
    selectedProgrammeIndex: Int,
    activatedKey: String?,
    horizontalState: ScrollState,
    verticalState: LazyListState,
    onProgramTap: (Channel, EpgManager.Prog) -> Unit,
    onChannelTap: (Channel) -> Unit,
    onJumpToNow: () -> Unit,
    onShiftTimeline: (Int) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(452.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            IptvColors.Background.copy(alpha = 0.90f),
                            IptvColors.Background
                        )
                    )
                )
                .padding(top = 34.dp)
        ) {
            TvEpgDockHeader(onJumpToNow, onShiftTimeline)
            TvTimelineHeader(window, nowMs, horizontalState)
            LazyColumn(
                state = verticalState,
                contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 28.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = channels,
                    key = { _, channel -> channel.tvgId + channel.url }
                ) { channelIndex, channel ->
                    TvEpgChannelRow(
                        channel = channel,
                        programmes = programmes.getOrNull(channelIndex).orEmpty(),
                        window = window,
                        nowMs = nowMs,
                        selected = channelIndex == selectedChannelIndex,
                        selectedProgrammeIndex = if (channelIndex == selectedChannelIndex) selectedProgrammeIndex else -1,
                        activatedKey = activatedKey,
                        horizontalState = horizontalState,
                        onProgramTap = { onProgramTap(channel, it) },
                        onChannelTap = { onChannelTap(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvEpgDockHeader(
    onJumpToNow: () -> Unit,
    onShiftTimeline: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 46.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(stringResource(R.string.epg_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.epg_dpad_instruction), color = IptvColors.TextTertiary, fontSize = 9.sp, letterSpacing = 0.7.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            TvDockTool(onClick = { onShiftTimeline(-30) }) {
                Icon(Icons.Default.ChevronLeft, stringResource(R.string.epg_shift_back_30), tint = Color.White, modifier = Modifier.size(18.dp))
            }
            TvDockTool(onClick = onJumpToNow) {
                Icon(Icons.Default.MyLocation, stringResource(R.string.epg_tab_now), tint = IptvColors.Primary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.epg_tab_now), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            TvDockTool(onClick = { onShiftTimeline(30) }) {
                Icon(Icons.Default.ChevronRight, stringResource(R.string.epg_shift_forward_30), tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TvDockTool(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier
            .height(31.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun TvTimelineHeader(
    window: EpgWindow,
    nowMs: Long,
    horizontalState: ScrollState
) {
    Row(Modifier.fillMaxWidth().height(36.dp)) {
        Spacer(Modifier.width(TvChannelWidth))
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 36.dp)
                .horizontalScroll(horizontalState, enabled = false)
        ) {
            Box(Modifier.width((window.totalMinutes * TvMinuteWidth.value).dp).fillMaxHeight()) {
                Row {
                    for (minute in 0..window.totalMinutes step 30) {
                        val major = minute % 60 == 0
                        Box(Modifier.width((30 * TvMinuteWidth.value).dp).fillMaxHeight()) {
                            Text(
                                epgTime(window.startMs + minute * 60_000L),
                                color = if (major) Color.White.copy(alpha = 0.82f) else IptvColors.TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = if (major) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                            Box(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .width(1.dp)
                                    .height(if (major) 11.dp else 7.dp)
                                    .background(Color.White.copy(alpha = if (major) 0.20f else 0.10f))
                            )
                        }
                    }
                }
                TvCurrentTimeIndicator(window, nowMs, height = 36.dp, showLabel = true)
            }
        }
    }
}

@Composable
private fun TvEpgChannelRow(
    channel: Channel,
    programmes: List<EpgManager.Prog>,
    window: EpgWindow,
    nowMs: Long,
    selected: Boolean,
    selectedProgrammeIndex: Int,
    activatedKey: String?,
    horizontalState: ScrollState,
    onProgramTap: (EpgManager.Prog) -> Unit,
    onChannelTap: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(TvRowHeight)
            .background(if (selected) Color.White.copy(alpha = 0.035f) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .width(TvChannelWidth)
                .fillMaxHeight()
                .clickable(onClick = onChannelTap)
                .padding(start = 36.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EpgChannelLogo(channel, Modifier.size(51.dp), selected = selected)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    channel.name,
                    color = if (selected) Color.White else IptvColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.group.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(channel.group, color = IptvColors.TextTertiary, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 36.dp)
                .horizontalScroll(horizontalState, enabled = false)
        ) {
            Box(Modifier.width((window.totalMinutes * TvMinuteWidth.value).dp).fillMaxHeight()) {
                programmes.forEachIndexed { programmeIndex, programme ->
                    val startMinute = ((programme.startMs - window.startMs) / 60_000L).toInt().coerceAtLeast(0)
                    val endMinute = ((programme.stopMs - window.startMs) / 60_000L).toInt().coerceAtMost(window.totalMinutes)
                    val durationMinutes = (endMinute - startMinute).coerceAtLeast(1)
                    val isFocused = selected && programmeIndex == selectedProgrammeIndex
                    val isLive = nowMs in programme.startMs until programme.stopMs
                    val key = programmeKey(channel, programme)
                    TvProgramCell(
                        programme = programme,
                        nowMs = nowMs,
                        state = when {
                            isFocused -> EpgProgramVisualState.Focused
                            activatedKey == key -> EpgProgramVisualState.Selected
                            isLive -> EpgProgramVisualState.Live
                            else -> EpgProgramVisualState.Default
                        },
                        modifier = Modifier
                            .absoluteOffset(x = (startMinute * TvMinuteWidth.value).dp, y = 8.dp)
                            .width(((durationMinutes * TvMinuteWidth.value).dp - 7.dp).coerceAtLeast(48.dp))
                            .height(72.dp),
                        onClick = { onProgramTap(programme) }
                    )
                }
                TvCurrentTimeIndicator(window, nowMs, height = TvRowHeight, showLabel = false)
            }
        }
    }
}
