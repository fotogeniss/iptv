package com.prelude.iptv.ui.tv.live

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.live.LiveChannelArtwork
import com.prelude.iptv.ui.components.live.LiveFilterOption
import com.prelude.iptv.ui.components.live.LiveProgressBar
import com.prelude.iptv.ui.components.live.liveNowNext
import com.prelude.iptv.ui.components.live.liveProgress
import com.prelude.iptv.ui.components.live.liveRemaining
import com.prelude.iptv.ui.components.live.liveTime
import com.prelude.iptv.ui.components.live.liveTimeRange
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun TvLiveFilterRow(
    filters: List<LiveFilterOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier,
        contentPadding = PaddingValues(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(filters, key = { _, item -> item.id }) { _, item ->
            var focused by remember { mutableStateOf(false) }
            val focusedScale = motionScale(Motion.TvActionScale)
            val scale by animateFloatAsState(
                if (focused) focusedScale else 1f,
                tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
                label = "filterScale"
            )
            val selected = item.id == selectedId
            Text(
                item.label,
                color = if (selected || focused) Color.Black else IptvColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier
                    .onFocusChanged { focused = it.isFocused || it.hasFocus }
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(
                        if (selected || focused) Color.White else Color.White.copy(alpha = .09f),
                        RoundedCornerShape(99.dp)
                    )
                    .clickable { onSelect(item.id) }
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
internal fun TvLiveChannelRail(
    channels: List<Channel>,
    selected: Channel,
    favoriteKeys: Set<String>,
    nowMs: Long,
    keyOf: (Channel) -> String,
    onFocused: (Channel) -> Unit,
    onOpen: (Channel) -> Unit,
    multiviewPrimaryKey: String?,
    onLongOpen: (Channel) -> Unit,
    firstCardFocus: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.CenterStart) {
            Text("Δεν υπάρχουν κανάλια σε αυτό το φίλτρο", color = IptvColors.TextSecondary, fontSize = 15.sp)
        }
        return
    }
    val initialFocus = rememberInitialFocus(key = channels.firstOrNull()?.let(keyOf))
    LazyRow(
        modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(channels, key = { _, channel -> keyOf(channel) }) { index, channel ->
            TvLiveChannelCard(
                channel = channel,
                nowMs = nowMs,
                selected = keyOf(channel) == keyOf(selected),
                favorite = keyOf(channel) in favoriteKeys,
                onFocused = { onFocused(channel) },
                onClick = { onOpen(channel) },
                onLongClick = { onLongOpen(channel) },
                multiviewPrimary = keyOf(channel) == multiviewPrimaryKey,
                modifier = if (index == 0) {
                    if (firstCardFocus != null)
                        Modifier.focusRequester(initialFocus).focusRequester(firstCardFocus)
                    else Modifier.focusRequester(initialFocus)
                } else Modifier
            )
        }
    }
}

@Composable
private fun TvLiveChannelCard(
    channel: Channel,
    nowMs: Long,
    selected: Boolean,
    favorite: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    multiviewPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    val (now, _) = remember(channel.tvgId, nowMs / 30_000L) { liveNowNext(channel, nowMs) }
    var focused by remember { mutableStateOf(false) }
    var dpadLongPressTriggered by remember(channel) { mutableStateOf(false) }
    var dpadLongPressJob by remember(channel) { mutableStateOf<Job?>(null) }
    val keyScope = rememberCoroutineScope()
    val focusedScale = motionScale(Motion.TvEmphasisScale)
    val scale by animateFloatAsState(
        if (focused) focusedScale else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "channelScale"
    )
    Box(
        modifier
            .width(235.dp).fillMaxHeight()
            .onFocusChanged {
                focused = it.isFocused || it.hasFocus
                if (focused) {
                    onFocused()
                } else {
                    dpadLongPressJob?.cancel()
                    dpadLongPressJob = null
                    dpadLongPressTriggered = false
                }
            }
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                shadowElevation = if (focused) 28.dp.toPx() else 7.dp.toPx()
                shape = RoundedCornerShape(15.dp)
                clip = true
            }
            .onPreviewKeyEvent { event ->
                val isConfirm = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter
                if (!isConfirm) return@onPreviewKeyEvent false

                when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (dpadLongPressJob == null) {
                            dpadLongPressTriggered = false
                            dpadLongPressJob = keyScope.launch {
                                delay(android.view.ViewConfiguration.getLongPressTimeout().toLong())
                                dpadLongPressTriggered = true
                                onLongClick()
                            }
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        dpadLongPressJob?.cancel()
                        dpadLongPressJob = null
                        if (!dpadLongPressTriggered) onClick()
                        dpadLongPressTriggered = false
                        true
                    }
                    else -> false
                }
            }
            .background(
                if (focused) Color.White.copy(alpha = .16f)
                else if (selected) Color.White.copy(alpha = .10f)
                else IptvColors.Surface.copy(alpha = .82f),
                RoundedCornerShape(15.dp)
            )
            .border(
                width = if (multiviewPrimary) 3.dp else 0.dp,
                color = if (multiviewPrimary) IptvColors.Primary else Color.Transparent,
                shape = RoundedCornerShape(15.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LiveChannelArtwork(channel, Modifier.size(width = 72.dp, height = 58.dp), selected = focused)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.background(IptvColors.Primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) { Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                    if (favorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Favorite, null, tint = IptvColors.Primary, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(channel.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(now?.title ?: channel.group.ifBlank { "Ζωντανή μετάδοση" }, color = Color.White.copy(alpha = .68f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                LiveProgressBar(liveProgress(now, nowMs), Modifier.fillMaxWidth(), height = 3)
            }
        }
        if (multiviewPrimary) {
            Box(
                Modifier.align(Alignment.TopEnd)
                    .padding(7.dp)
                    .background(IptvColors.Primary, RoundedCornerShape(99.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
internal fun TvLiveNowNextStrip(
    channel: Channel,
    nowMs: Long,
    modifier: Modifier = Modifier
) {
    val (now, next) = remember(channel.tvgId, nowMs / 30_000L) { liveNowNext(channel, nowMs) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LiveProgrammePanel(
            label = "ΤΩΡΑ",
            title = now?.title ?: "Ζωντανή μετάδοση",
            subtitle = if (now != null) "${liveTimeRange(now)} · ${liveRemaining(now, nowMs)}" else channel.name,
            modifier = Modifier.weight(1.35f)
        )
        LiveProgrammePanel(
            label = "ΕΠΟΜΕΝΟ",
            title = next?.title ?: "Δεν υπάρχουν διαθέσιμα στοιχεία",
            subtitle = next?.let { "${liveTime(it.startMs)} – ${liveTime(it.stopMs)}" }.orEmpty(),
            modifier = Modifier.weight(.8f)
        )
    }
}

@Composable
private fun LiveProgrammePanel(label: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .065f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = IptvColors.TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) Text(subtitle, color = IptvColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
