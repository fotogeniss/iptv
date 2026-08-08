package com.prelude.iptv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.design.Motion
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

/**
 * Shared streaming design primitives used by both mobile and TV.
 * Platform-specific screens remain separate; only visual language is shared.
 */
object StreamingSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 48.dp
}

object StreamingRadius {
    val Button = 10.dp
    val Card = 12.dp
    val Panel = 16.dp
    val Sheet = 24.dp
    val Pill = 999.dp
}

object StreamingBorders {
    val Hairline = BorderStroke(1.dp, IptvColors.Divider)
    val Strong = BorderStroke(1.dp, IptvColors.DividerStrong)
    val Focus = BorderStroke(2.dp, IptvColors.Focus)
}

enum class StreamingButtonStyle { Primary, Secondary, Ghost }

@Composable
fun StreamingButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: StreamingButtonStyle = StreamingButtonStyle.Primary,
    minHeight: Dp = 48.dp,
    enabled: Boolean = true
) {
    val background = when (style) {
        StreamingButtonStyle.Primary -> Color.White
        StreamingButtonStyle.Secondary -> IptvColors.SurfaceRaised
        StreamingButtonStyle.Ghost -> Color.Transparent
    }
    val foreground = if (style == StreamingButtonStyle.Primary) Color.Black else Color.White
    val border = if (style == StreamingButtonStyle.Ghost) StreamingBorders.Strong else null

    Row(
        modifier
            .height(minHeight)
            .clip(RoundedCornerShape(StreamingRadius.Button))
            .background(background)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(StreamingRadius.Button)) else Modifier)
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = foreground, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            label,
            color = foreground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StreamingLiveBadge(modifier: Modifier = Modifier, text: String = "LIVE") {
    Text(
        text,
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(IptvColors.Primary)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
fun StreamingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = IptvColors.Primary
) {
    val safe = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(StreamingRadius.Pill))
            .background(Color.White.copy(alpha = 0.24f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(safe)
                .height(4.dp)
                .background(accent)
        )
    }
}

@Composable
fun TvFocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    radius: Dp = StreamingRadius.Card,
    content: @Composable () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvFocusScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "streamingFocusableScale"
    )
    Box(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(radius))
            .background(if (focused) IptvColors.SurfaceSelected else IptvColors.Surface)
            .border(
                if (focused) StreamingBorders.Focus else StreamingBorders.Hairline,
                RoundedCornerShape(radius)
            )
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .focusable(interactionSource = source),
        contentAlignment = Alignment.Center
    ) { content() }
}
