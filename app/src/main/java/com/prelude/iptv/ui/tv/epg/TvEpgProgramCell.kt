package com.prelude.iptv.ui.tv.epg

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.epg.EpgProgramVisualState
import com.prelude.iptv.ui.components.epg.EpgWindow
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.ui.components.epg.epgTime
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

@Composable
internal fun TvProgramCell(
    programme: EpgManager.Prog,
    nowMs: Long,
    state: EpgProgramVisualState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val focused = state == EpgProgramVisualState.Focused
    val live = nowMs in programme.startMs until programme.stopMs
    val past = programme.stopMs <= nowMs
    val focusedScale = motionScale(Motion.TvFocusScale)
    val scale by animateFloatAsState(
        if (focused) focusedScale else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "epgCellScale"
    )
    val background by animateColorAsState(
        when {
            focused -> Color(0xFF4A4A50)
            state == EpgProgramVisualState.Selected -> Color(0xFF353B37)
            live -> Color(0xFF3A2427)
            else -> Color(0xFF242428)
        },
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "epgCellColor"
    )

    Box(
        modifier
            .zIndex(if (focused) 5f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (past && !focused) 0.52f else 1f
            }
            .shadow(if (focused) 22.dp else 4.dp, RoundedCornerShape(10.dp), clip = false)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (focused) Color.White.copy(alpha = 0.14f) else IptvColors.Primary.copy(alpha = if (live) 0.12f else 0f),
                            Color.Transparent
                        ),
                        radius = 280f
                    )
                )
        )
        Column(Modifier.align(Alignment.CenterStart)) {
            Text(
                programme.title,
                color = Color.White,
                fontSize = if (focused) 13.sp else 12.sp,
                fontWeight = if (focused || live) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${epgTime(programme.startMs)} – ${epgTime(programme.stopMs)}",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 9.sp
                )
                if (live) {
                    Spacer(Modifier.width(8.dp))
                    Text("LIVE", color = IptvColors.Primary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            if (live) {
                Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(99.dp))) {
                    Box(
                        Modifier
                            .fillMaxWidth(epgProgress(programme, nowMs))
                            .fillMaxHeight()
                            .background(IptvColors.Primary, RoundedCornerShape(99.dp))
                    )
                }
            }
        }
        if (state == EpgProgramVisualState.Selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Επιλεγμένο",
                tint = IptvColors.Success,
                modifier = Modifier.align(Alignment.TopEnd).size(14.dp)
            )
        }
    }
}

@Composable
internal fun TvCurrentTimeIndicator(
    window: EpgWindow,
    nowMs: Long,
    height: Dp,
    showLabel: Boolean
) {
    val minute = ((nowMs - window.startMs) / 60_000L).toInt()
    if (minute !in 0..window.totalMinutes) return
    val lineX = (minute * TvMinuteWidth.value).dp
    if (showLabel) {
        Box(
            Modifier
                .absoluteOffset(x = lineX - 24.dp)
                .width(50.dp)
                .height(height)
                .zIndex(12f)
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .width(2.dp)
                    .fillMaxHeight()
                    .shadow(10.dp, RoundedCornerShape(99.dp))
                    .background(IptvColors.Primary)
            )
            Text(
                epgTime(nowMs),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(4.dp))
                    .background(IptvColors.Primary)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    } else {
        Box(
            Modifier
                .absoluteOffset(x = lineX)
                .width(2.dp)
                .height(height)
                .shadow(10.dp, RoundedCornerShape(99.dp))
                .background(IptvColors.Primary)
                .zIndex(12f)
        )
    }
}
