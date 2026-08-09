package com.prelude.iptv.ui.mobile.epg

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.prelude.iptv.ui.components.epg.EpgCinematicBackdrop
import com.prelude.iptv.ui.components.epg.EpgProgressBar
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.ui.components.epg.epgRuntime
import com.prelude.iptv.ui.components.epg.epgTime
import com.prelude.iptv.ui.components.epg.epgTimeRange
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.localization.localizedUppercase

@Composable
internal fun MobileEpgHero(
    channel: Channel,
    programme: EpgManager.Prog?,
    nextProgramme: EpgManager.Prog?,
    nowMs: Long,
    onBack: () -> Unit,
    onWatch: () -> Unit,
    onChannelClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(530.dp)) {
        EpgCinematicBackdrop(channel)
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.34f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.epg_back), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.Black.copy(alpha = 0.34f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).background(IptvColors.Primary, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(localizedUppercase(stringResource(R.string.epg_title)), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            }
        }

        val enterDuration = motionDuration(Motion.Medium)
        val exitDuration = motionDuration(Motion.Fast)
        AnimatedContent(
            targetState = Triple(channel, programme, nextProgramme),
            transitionSpec = {
                fadeIn(tween(enterDuration, easing = Motion.EmphasizedEasing)) togetherWith
                    fadeOut(tween(exitDuration, easing = Motion.StandardEasing))
            },
            label = "mobileEpgHero",
            modifier = Modifier.align(Alignment.BottomStart)
        ) { (heroChannel, heroProgramme, heroNextProgramme) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 26.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EpgChannelLogo(heroChannel, Modifier.size(46.dp), selected = true)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(heroChannel.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(
                                if (heroProgramme != null && nowMs in heroProgramme.startMs until heroProgramme.stopMs) R.string.epg_playing_now
                                else R.string.epg_programme
                            ),
                            color = if (heroProgramme != null && nowMs in heroProgramme.startMs until heroProgramme.stopMs) IptvColors.Success else IptvColors.TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(epgTime(nowMs), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(17.dp))
                Text(
                    heroProgramme?.title ?: heroChannel.name,
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    heroProgramme?.let {
                        Text(epgTimeRange(it), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(epgRuntime(it), color = IptvColors.TextSecondary, fontSize = 11.sp)
                    }
                    if (heroChannel.genre.isNotBlank()) {
                        Text(heroChannel.genre, color = IptvColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                }
                if (!heroProgramme?.desc.isNullOrBlank()) {
                    Spacer(Modifier.height(11.dp))
                    Text(
                        heroProgramme?.desc.orEmpty(),
                        color = Color(0xFFD0D0D3),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (heroProgramme != null && nowMs in heroProgramme.startMs until heroProgramme.stopMs) {
                    Spacer(Modifier.height(13.dp))
                    EpgProgressBar(epgProgress(heroProgramme, nowMs), Modifier.fillMaxWidth())
                }
                if (heroNextProgramme != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.epg_next_with_title, epgTime(heroNextProgramme.startMs), heroNextProgramme.title),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(17.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = onWatch,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 17.dp),
                        modifier = Modifier.weight(1f).height(47.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(if (heroProgramme != null && heroProgramme.startMs > nowMs) R.string.epg_reminder else R.string.epg_watch),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    OutlinedButton(
                        onClick = onChannelClick,
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(47.dp)
                    ) {
                        Icon(Icons.Default.LiveTv, stringResource(R.string.epg_open_channel), tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MobileEpgEmptyState(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(IptvColors.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.epg_empty), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text(stringResource(R.string.epg_return)) }
        }
    }
}
