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
import com.prelude.iptv.ui.icons.filled.Check
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.design.drawHorizon
import com.prelude.iptv.ui.design.Horizon
import com.prelude.iptv.ui.StreamingProgress
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.ui.zIndex
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.epg.EpgProgramVisualState
import com.prelude.iptv.ui.components.epg.EpgWindow
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.ui.components.epg.epgTime
import com.prelude.iptv.ui.components.epg.epgTimeRange
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
            focused -> IptvColors.SurfacePressed
            state == EpgProgramVisualState.Selected -> IptvColors.SurfaceSelected
            live -> Color(0xFF2A1A20)
            else -> IptvColors.SurfaceRaised
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
            .shadow(if (focused) 22.dp else 0.dp, RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, if (focused) IptvColors.DividerStrong else IptvColors.Divider, RoundedCornerShape(12.dp))
            // Horizon: το πρόγραμμα με focus παίρνει τη γραμμή-ορίζοντα στο κάτω μέρος.
            .drawWithContent {
                drawContent()
                if (focused) drawHorizon(size.width * 0.08f, size.width * 0.92f, size.height - 2.dp.toPx())
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            IptvColors.Primary.copy(alpha = if (live) 0.14f else 0f),
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
                fontSize = if (focused) 15.sp else 14.sp,
                fontWeight = if (focused || live) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    epgTimeRange(programme),
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 11.sp
                )
                if (live) {
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.epg_live), color = IptvColors.Glow, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            if (live) {
                Spacer(Modifier.height(7.dp))
                StreamingProgress(epgProgress(programme, nowMs), Modifier.fillMaxWidth())
            }
        }
        if (state == EpgProgramVisualState.Selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.epg_selected),
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
                .absoluteOffset(x = lineX - 45.dp)
                .width(92.dp)
                .height(height)
                .zIndex(12f)
        ) {
            NowLine(Modifier.align(Alignment.TopCenter).fillMaxHeight())
            Text(
                epgUpper(stringResource(R.string.epg_now_marker, epgTime(nowMs))),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(4.dp))
                    .background(IptvColors.Primary)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    } else {
        NowLine(
            Modifier
                .absoluteOffset(x = lineX - 4.dp)
                .height(height)
                .zIndex(12f)
        )
    }
}

/** Η γραμμή-ορίζοντας κάθετα: η τρέχουσα ώρα, κόκκινη με λάμψη. */
@Composable
private fun NowLine(modifier: Modifier = Modifier) {
    Canvas(modifier.width(10.dp)) {
        val x = size.width / 2f
        drawLine(
            brush = Brush.verticalGradient(listOf(Color.Transparent, IptvColors.Primary.copy(alpha = 0.45f), Color.Transparent)),
            start = androidx.compose.ui.geometry.Offset(x, 0f),
            end = androidx.compose.ui.geometry.Offset(x, size.height),
            strokeWidth = Horizon.Glow.toPx(),
        )
        drawLine(
            brush = Brush.verticalGradient(listOf(IptvColors.Primary, IptvColors.Glow, IptvColors.Primary)),
            start = androidx.compose.ui.geometry.Offset(x, 0f),
            end = androidx.compose.ui.geometry.Offset(x, size.height),
            strokeWidth = Horizon.Line.toPx(),
        )
    }
}

private fun epgUpper(text: String): String =
    java.text.Normalizer.normalize(text.uppercase(), java.text.Normalizer.Form.NFD).replace("\u0301", "")
