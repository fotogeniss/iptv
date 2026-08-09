package com.prelude.iptv.ui.mobile.epg

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.ui.components.epg.epgTimeRange
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

@Composable
internal fun MobileProgramCard(
    programme: EpgManager.Prog,
    nowMs: Long,
    selected: Boolean,
    onClick: () -> Unit,
    onAction: () -> Unit
) {
    val live = nowMs in programme.startMs until programme.stopMs
    val selectedScale = motionScale(1.02f)
    val scale by animateFloatAsState(
        if (selected) selectedScale else 1f,
        tween(motionDuration(Motion.Fast), easing = Motion.StandardEasing),
        label = "mobileEpgCardScale"
    )
    val surface by animateColorAsState(
        when {
            selected -> Color(0xFF343438)
            live -> Color(0xFF301C20)
            else -> IptvColors.Surface
        },
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "mobileEpgCardSurface"
    )
    Box(
        Modifier
            .width(218.dp)
            .height(121.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(13.dp))
            .background(surface)
            .border(
                if (live && !selected) 1.5.dp else 1.dp,
                when {
                    selected -> Color.White.copy(alpha = 0.48f)
                    live -> IptvColors.Primary
                    else -> IptvColors.Divider
                },
                RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick)
            .padding(13.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (live) IptvColors.Primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        radius = 270f
                    )
                )
        )
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    epgTimeRange(programme),
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 9.sp
                )
                Spacer(Modifier.weight(1f))
                if (live) Text(stringResource(R.string.epg_live), color = IptvColors.Primary, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                programme.title,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            if (live) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(99.dp))) {
                    Box(
                        Modifier
                            .fillMaxWidth(epgProgress(programme, nowMs))
                            .fillMaxHeight()
                            .background(IptvColors.Primary, RoundedCornerShape(99.dp))
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(30.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Color.White.copy(alpha = 0.10f))
                .clickable(onClick = onAction),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.epg_program_action),
                tint = if (selected) Color.Black else Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
