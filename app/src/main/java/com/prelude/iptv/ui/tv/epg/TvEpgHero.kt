package com.prelude.iptv.ui.tv.epg

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.greekUppercase
import com.prelude.iptv.ui.components.epg.EpgChannelLogo
import com.prelude.iptv.ui.components.epg.EpgProgressBar
import com.prelude.iptv.ui.components.epg.epgPalette
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.ui.components.epg.epgRuntime
import com.prelude.iptv.ui.components.epg.epgTime
import com.prelude.iptv.ui.components.epg.epgTimeRange
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration

@Composable
internal fun TvEpgTopBar(
    onBack: () -> Unit,
    nowMs: Long
) {
    Row(
        Modifier.padding(start = 34.dp, end = 46.dp, top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "PRELUDE",
            color = IptvColors.Primary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp
        )
        Spacer(Modifier.width(34.dp))
        Text("Αρχική", color = IptvColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(24.dp))
        Text("Live TV", color = IptvColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(24.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Οδηγός TV", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(52.dp).height(2.dp).background(Color.White, RoundedCornerShape(99.dp)))
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .shadow(8.dp, RoundedCornerShape(99.dp))
                    .background(IptvColors.Primary, RoundedCornerShape(99.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text("EPG LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(22.dp))
            Text(epgTime(nowMs), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun TvEpgHero(
    channel: Channel,
    programme: EpgManager.Prog?,
    nextProgramme: EpgManager.Prog?,
    nowMs: Long,
    onWatch: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        val enterDuration = motionDuration(Motion.Medium)
        val exitDuration = motionDuration(Motion.Fast)
        AnimatedContent(
            targetState = Triple(channel, programme, nextProgramme),
            transitionSpec = {
                fadeIn(tween(enterDuration, easing = Motion.EmphasizedEasing)) togetherWith
                    fadeOut(tween(exitDuration, easing = Motion.StandardEasing))
            },
            label = "tvEpgHero",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 54.dp, top = 104.dp)
        ) { (heroChannel, heroProgramme, heroNextProgramme) ->
            Column(Modifier.width(620.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(22.dp).height(3.dp).background(IptvColors.Primary, RoundedCornerShape(99.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (heroProgramme != null && nowMs in heroProgramme.startMs until heroProgramme.stopMs) {
                            "ΤΩΡΑ · ${heroChannel.name}"
                        } else {
                            "ΟΔΗΓΟΣ TV · ${heroChannel.name}"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    heroProgramme?.title ?: heroChannel.name,
                    color = Color.White,
                    fontSize = 46.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.6).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(15.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLive = heroProgramme != null && nowMs in heroProgramme.startMs until heroProgramme.stopMs
                    if (isLive) {
                        Text("LIVE", color = IptvColors.Success, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    heroProgramme?.let {
                        Text(epgTimeRange(it), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(epgRuntime(it), color = IptvColors.TextSecondary, fontSize = 12.sp)
                    }
                    if (heroChannel.genre.isNotBlank()) {
                        Text(heroChannel.genre, color = IptvColors.TextSecondary, fontSize = 12.sp)
                    }
                }
                if (!heroProgramme?.desc.isNullOrBlank()) {
                    Spacer(Modifier.height(15.dp))
                    Text(
                        heroProgramme?.desc.orEmpty(),
                        color = Color(0xFFD1D1D5),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (heroProgramme != null && nowMs in heroProgramme.startMs until heroProgramme.stopMs) {
                    Spacer(Modifier.height(14.dp))
                    EpgProgressBar(epgProgress(heroProgramme, nowMs), Modifier.width(360.dp))
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onWatch,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (heroProgramme != null && heroProgramme.startMs > nowMs) "Υπενθύμιση" else "Παρακολούθηση", fontWeight = FontWeight.Bold)
                    }
                    if (heroNextProgramme != null) {
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text("ΕΠΟΜΕΝΟ · ${epgTime(heroNextProgramme.startMs)}", color = IptvColors.TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(heroNextProgramme.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }

        TvEpgPosterCard(
            channel = channel,
            title = programme?.title ?: channel.name,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 110.dp, end = 70.dp)
        )

        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp, bottom = 92.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).background(IptvColors.Success, RoundedCornerShape(99.dp)))
            Spacer(Modifier.width(8.dp))
            Text("CINEMATIC PREVIEW · EPG OVERLAY", color = Color.White.copy(alpha = 0.76f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TvEpgPosterCard(
    channel: Channel,
    title: String,
    modifier: Modifier = Modifier
) {
    val palette = epgPalette(channel)
    Box(
        modifier
            .size(width = 164.dp, height = 232.dp)
            .shadow(34.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(palette.primary, palette.surface, palette.secondary)))
    ) {
        EpgChannelLogo(
            channel = channel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .size(82.dp),
            selected = true
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
        )
        Text(
            title.greekUppercase(),
            color = Color.White,
            fontSize = 17.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Black,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
        )
    }
}

@Composable
internal fun TvEpgEmptyState(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(IptvColors.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Δεν υπάρχει διαθέσιμο πρόγραμμα EPG", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Επιστροφή") }
        }
    }
}
