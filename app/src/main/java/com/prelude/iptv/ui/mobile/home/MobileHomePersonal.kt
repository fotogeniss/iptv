package com.prelude.iptv.ui.mobile.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.icons.filled.Search
import com.prelude.iptv.ui.mobile.live.LiveContinueCard
import com.prelude.iptv.ui.mobile.live.LiveSectionLabel
import java.util.Calendar

/** Χαιρετισμός με το όνομα του προφίλ και τι έχει η πηγή — η κορυφή της προσωπικής Αρχικής. */
@Composable
internal fun MobileHomeGreeting(
    profileName: String,
    liveCount: Int,
    titleCount: Int,
    onSearch: () -> Unit,
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingRes = when (hour) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..19 -> R.string.home_greeting_afternoon
        else -> R.string.home_greeting_night
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF7A2C40), Color(0xFF2A1D3D)))),
            contentAlignment = Alignment.Center
        ) {
            Text(profileName.trim().take(1).uppercase().ifBlank { "P" }, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                stringResource(greetingRes, profileName),
                color = IptvColors.TextPrimary,
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 25.sp,
                lineHeight = 26.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(
                    R.string.home_library_summary,
                    pluralStringResource(R.plurals.live_channel_count, liveCount, liveCount),
                    pluralStringResource(R.plurals.search_title_count, titleCount, titleCount),
                ),
                color = IptvColors.TextTertiary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Default.Search, stringResource(R.string.search_title), tint = IptvColors.TextPrimary)
        }
    }
}

/** «Ζωντανά τώρα»: τα κανάλια που βλέπεις, με ό,τι παίζουν αυτή τη στιγμή. */
@Composable
internal fun MobileHomeLiveNow(
    channels: List<Channel>,
    nowMs: Long,
    onPlay: (Channel) -> Unit,
    onOpenLive: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LiveSectionLabel(stringResource(R.string.live_now_heading), Modifier.weight(1f))
        Text(
            stringResource(R.string.live_title),
            color = IptvColors.TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenLive).padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(channels, key = { "home-live:${PlaybackQueue.favKey(it)}" }) { channel ->
            LiveContinueCard(channel, nowMs) { onPlay(channel) }
        }
    }
}
