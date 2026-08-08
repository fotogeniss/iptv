package com.prelude.iptv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Mobile-only primitives. TV screens intentionally do not use these components. */
object MobileV2 {
    val ScreenPadding = 16.dp
    val SectionGap = 24.dp
    val CardRadius = 12.dp
    val ControlRadius = 12.dp
    val Hairline = BorderStroke(1.dp, IptvColors.Divider)
}

@Composable
fun MobileV2Header(
    title: String,
    subtitle: String? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = MobileV2.ScreenPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = IptvColors.TextPrimary,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = IptvColors.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actionIcon != null && onAction != null) {
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = onAction,
                modifier = Modifier
                    .size(44.dp)
                    .background(IptvColors.Surface, RoundedCornerShape(MobileV2.ControlRadius))
                    .border(MobileV2.Hairline, RoundedCornerShape(MobileV2.ControlRadius))
            ) {
                Icon(actionIcon, actionDescription, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun MobileV2ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(46.dp)
                .background(
                    if (selected) Color.White else IptvColors.SurfaceRaised,
                    RoundedCornerShape(MobileV2.ControlRadius)
                )
                .border(MobileV2.Hairline, RoundedCornerShape(MobileV2.ControlRadius)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (selected) Color.White else IptvColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MobileV2SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        color = IptvColors.TextPrimary,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier.padding(horizontal = MobileV2.ScreenPadding)
    )
}


@Composable
fun MobileV2BackHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .background(IptvColors.Surface, RoundedCornerShape(MobileV2.ControlRadius))
                .border(MobileV2.Hairline, RoundedCornerShape(MobileV2.ControlRadius))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = IptvColors.TextPrimary,
                fontSize = 25.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = IptvColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actionIcon != null && onAction != null) {
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = onAction,
                modifier = Modifier
                    .size(44.dp)
                    .background(IptvColors.Surface, RoundedCornerShape(MobileV2.ControlRadius))
                    .border(MobileV2.Hairline, RoundedCornerShape(MobileV2.ControlRadius))
            ) { Icon(actionIcon, actionDescription, tint = Color.White) }
        }
    }
}

@Composable
fun MobileV2SettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    value: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MobileV2.CardRadius))
            .background(IptvColors.Surface)
            .border(MobileV2.Hairline, RoundedCornerShape(MobileV2.CardRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp)
                .background(IptvColors.SurfaceRaised, RoundedCornerShape(11.dp))
                .border(MobileV2.Hairline, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = IptvColors.TextSecondary, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!value.isNullOrBlank()) {
            Text(value, color = IptvColors.TextSecondary, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(7.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = IptvColors.TextTertiary, modifier = Modifier.size(20.dp))
    }
}
