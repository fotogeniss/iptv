package com.prelude.iptv.ui.tv.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.live.LiveCinematicBackdrop
import com.prelude.iptv.ui.components.live.LiveFilterOption
import com.prelude.iptv.ui.components.live.liveTime

@Composable
fun TvPremiumLiveScreen(
    channels: List<Channel>,
    selected: Channel,
    filters: List<LiveFilterOption>,
    selectedFilterId: String,
    favoriteKeys: Set<String>,
    nowMs: Long,
    keyOf: (Channel) -> String,
    onSelect: (Channel) -> Unit,
    onFilter: (String) -> Unit,
    onPlay: (Channel) -> Unit,
    onEpg: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    multiviewPrimaryKey: String?,
    onArmMultiview: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    // Ρητό focus target: το DOWN από τα group chips να πηγαίνει ΠΑΝΤΑ στην πρώτη
    // κάρτα καναλιού (πριν το focus search «κολλούσε» και δεν κατέβαινε στη λίστα).
    val channelRailFocus = remember { FocusRequester() }
    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        LiveCinematicBackdrop(selected, mobile = false, modifier = Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.background(IptvColors.Primary, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 11.dp)
                    )
                    Spacer(Modifier.padding(horizontal = 5.dp))
                    Text("PRELUDE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.padding(horizontal = 8.dp))
                    Box(
                        Modifier.background(IptvColors.Primary.copy(alpha = .18f), RoundedCornerShape(99.dp))
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Text(stringResource(R.string.live_tv_badge), color = Color(0xFFFF777D), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text(liveTime(nowMs), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(7.dp))
            Box(
                Modifier.background(
                    if (multiviewPrimaryKey != null) IptvColors.Primary.copy(alpha = .22f)
                    else Color.Black.copy(alpha = .28f),
                    RoundedCornerShape(9.dp)
                ).padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    if (multiviewPrimaryKey != null)
                        stringResource(R.string.live_multiview_select_second_short)
                    else
                        stringResource(R.string.live_multiview_long_press),
                    color = if (multiviewPrimaryKey != null) Color.White else IptvColors.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            TvLiveHero(
                channel = selected,
                nowMs = nowMs,
                favorite = keyOf(selected) in favoriteKeys,
                onPlay = { onPlay(selected) },
                onEpg = { onEpg(selected) },
                onFavorite = { onToggleFavorite(selected) },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            TvLiveFilterRow(
                filters = filters,
                selectedId = selectedFilterId,
                onSelect = onFilter,
                modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown)
                        runCatching { channelRailFocus.requestFocus() }.isSuccess
                    else false
                }
            )
            Spacer(Modifier.height(8.dp))
            TvLiveChannelRail(
                channels = channels,
                selected = selected,
                favoriteKeys = favoriteKeys,
                nowMs = nowMs,
                keyOf = keyOf,
                onFocused = onSelect,
                onOpen = onPlay,
                multiviewPrimaryKey = multiviewPrimaryKey,
                onLongOpen = onArmMultiview,
                firstCardFocus = channelRailFocus,
                modifier = Modifier.fillMaxWidth().height(166.dp)
            )
            Spacer(Modifier.height(8.dp))
            TvLiveNowNextStrip(selected, nowMs, Modifier.fillMaxWidth().height(82.dp))
        }
    }
}
