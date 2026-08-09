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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.prelude.iptv.ui.components.epg.EpgWindow
import com.prelude.iptv.ui.components.epg.epgTime
import com.prelude.iptv.ui.epg.EpgFilter
import com.prelude.iptv.ui.localization.labelRes
import java.util.Calendar
import java.util.Locale

@Immutable
internal data class MobileIndexedProgramme(
    val index: Int,
    val programme: EpgManager.Prog
)

@Composable
internal fun MobileEpgGuideHeader(
    window: EpgWindow,
    nowMs: Long,
    selectedTab: EpgFilter,
    selectedTimeMs: Long,
    onTabSelected: (EpgFilter) -> Unit,
    onTimeSelected: (Long) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(IptvColors.BackgroundRaised)
            .padding(top = 10.dp, bottom = 14.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(38.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.24f), RoundedCornerShape(99.dp))
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stringResource(R.string.epg_title), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                Text(stringResource(R.string.epg_touch_guide), color = IptvColors.TextTertiary, fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.epg_today),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(EpgFilter.entries) { _, tab ->
                val selected = tab == selectedTab
                Text(
                    stringResource(tab.labelRes()),
                    color = if (selected) Color.Black else IptvColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.07f))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val times = remember(window) { timelineTimes(window) }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(times) { _, timeMs ->
                val selected = kotlin.math.abs(timeMs - selectedTimeMs) < 15 * 60_000L
                val isNow = kotlin.math.abs(timeMs - nowMs) < 30 * 60_000L
                Column(
                    Modifier
                        .width(69.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) IptvColors.SurfaceSelected else IptvColors.Surface)
                        .border(
                            1.dp,
                            if (selected) Color.White.copy(alpha = 0.45f) else IptvColors.Divider,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onTimeSelected(timeMs) }
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(epgTime(timeMs), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(if (isNow) stringResource(R.string.epg_now_badge) else "", color = IptvColors.Primary, fontSize = 7.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
internal fun MobileEpgChannelCard(
    channel: Channel,
    channelIndex: Int,
    programmes: List<MobileIndexedProgramme>,
    nowMs: Long,
    selectedChannelIndex: Int,
    selectedProgrammeIndex: Int,
    onChannelSelected: () -> Unit,
    onOpenChannel: () -> Unit,
    onProgramSelected: (Int) -> Unit,
    onProgramAction: (Int) -> Unit
) {
    val channelSelected = channelIndex == selectedChannelIndex
    Column(
        Modifier
            .fillMaxWidth()
            .background(IptvColors.BackgroundRaised)
            .padding(top = 9.dp, bottom = 15.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EpgChannelLogo(
                channel = channel,
                modifier = Modifier.size(46.dp).clickable(onClick = onOpenChannel),
                selected = channelSelected
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f).clickable(onClick = onChannelSelected)) {
                Text(
                    channel.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    channel.group.ifBlank { stringResource(R.string.epg_live_tv) },
                    color = IptvColors.TextTertiary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (channelSelected) {
                Box(Modifier.size(7.dp).background(IptvColors.Primary, CircleShape))
            }
        }
        Spacer(Modifier.height(10.dp))
        if (programmes.isEmpty()) {
            Text(
                stringResource(R.string.epg_no_program_filter),
                color = IptvColors.TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 74.dp, vertical = 18.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(start = 74.dp, top = 0.dp, end = 17.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                itemsIndexed(programmes, key = { _, item -> item.programme.startMs }) { _, item ->
                    MobileProgramCard(
                        programme = item.programme,
                        nowMs = nowMs,
                        selected = channelSelected && item.index == selectedProgrammeIndex,
                        onClick = { onProgramSelected(item.index) },
                        onAction = { onProgramAction(item.index) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun MobileEpgGuideFooter() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(IptvColors.BackgroundRaised)
            .navigationBarsPadding()
            .height(28.dp)
    )
}

internal fun filteredMobileProgrammes(
    channel: Channel,
    programmes: List<EpgManager.Prog>,
    tab: EpgFilter,
    nowMs: Long
): List<MobileIndexedProgramme> {
    val descriptor = "${channel.name} ${channel.group} ${channel.genre}".lowercase(Locale.ROOT)
    val currentIndex = programmes.indexOfFirst { nowMs in it.startMs until it.stopMs }
    val nextIndex = when {
        currentIndex >= 0 -> currentIndex + 1
        else -> programmes.indexOfFirst { it.startMs >= nowMs }
    }
    return programmes.mapIndexedNotNull { index, programme ->
        val include = when (tab) {
            EpgFilter.Now -> index == currentIndex || index == nextIndex
            EpgFilter.Later -> programme.startMs > nowMs
            EpgFilter.All -> true
            EpgFilter.Movies -> listOf("movie", "cinema", "film", "ταιν").any { descriptor.contains(it) }
            EpgFilter.Sports -> listOf("sport", "sports", "αθλη").any { descriptor.contains(it) }
        }
        if (include) MobileIndexedProgramme(index, programme) else null
    }
}

private fun timelineTimes(window: EpgWindow): List<Long> {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = window.startMs
        set(Calendar.MINUTE, if (get(Calendar.MINUTE) < 30) 0 else 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return List(9) { index -> calendar.timeInMillis + index * 30 * 60_000L }
}
