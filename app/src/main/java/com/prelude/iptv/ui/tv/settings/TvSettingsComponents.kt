package com.prelude.iptv.ui.tv.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingRadius
import com.prelude.iptv.ui.components.settings.SettingsPage
import com.prelude.iptv.ui.components.settings.SettingsSourceUi
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

@Composable
internal fun TvSettingsNavItem(
    page: SettingsPage,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean = true,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) motionScale(1.025f) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "settingsNav"
    )
    val bg = when {
        focused -> Color.White
        selected -> Color.White.copy(alpha = 0.09f)
        else -> Color.Transparent
    }
    val fg = if (focused) Color.Black else if (selected) Color.White else IptvColors.TextSecondary

    Row(
        Modifier.fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .scale(scale)
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .focusable(interactionSource = source)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Text(page.label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun TvSettingsSourceCard(
    source: SettingsSourceUi,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvFocusScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "sourceScale"
    )

    Column(
        Modifier.width(278.dp).height(214.dp)
            .scale(scale)
            .clip(RoundedCornerShape(StreamingRadius.Panel))
            .background(
                Brush.linearGradient(
                    listOf(
                        if (focused) Color(0xFF303037) else IptvColors.SurfaceRaised,
                        IptvColors.Surface
                    )
                )
            )
            .border(1.dp, if (focused) Color.White.copy(alpha = 0.28f) else IptvColors.Divider, RoundedCornerShape(StreamingRadius.Panel))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .focusable(interactionSource = interaction)
            .padding(17.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(sourceBrush(source.type)),
                contentAlignment = Alignment.Center
            ) {
                Text(source.typeLabel, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                if (source.current) "● ΕΝΕΡΓΗ" else source.statusLabel.uppercase(),
                color = if (source.current) IptvColors.Primary else IptvColors.TextTertiary,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(source.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(source.endpoint, color = IptvColors.TextTertiary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(13.dp))
        if (source.loading) {
            val label = source.progressPercent?.let { "$it% · ${source.progressStage}" }
                ?: source.progressStage.ifBlank { "Λήψη από την πηγή…" }
            Text(label, color = IptvColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp))
            val progressPercent = source.progressPercent
            if (progressPercent != null) {
                LinearProgressIndicator(
                    progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = IptvColors.Primary,
                    trackColor = IptvColors.Divider
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = IptvColors.Primary,
                    trackColor = IptvColors.Divider
                )
            }
        } else {
            Text(
                source.channelCount?.let { "$it φορτωμένα στοιχεία" } ?: "Τα στοιχεία σύνδεσης είναι αποθηκευμένα τοπικά",
                color = IptvColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 2
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvSourceAction(Icons.Default.PlayArrow, "Άνοιγμα", onOpen)
            if (onRefresh != null) TvSourceAction(Icons.Default.Refresh, "Ανανέωση", onRefresh, enabled = !source.loading)
            TvSourceAction(Icons.Default.Edit, "Επεξεργασία", onEdit)
            TvSourceAction(Icons.Default.Delete, "Διαγραφή", onDelete)
        }
    }
}

@Composable
internal fun TvAddSourceCard(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvFocusScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "addSource"
    )
    Column(
        Modifier.width(250.dp).height(214.dp)
            .scale(scale)
            .clip(RoundedCornerShape(StreamingRadius.Panel))
            .background(Color.White.copy(alpha = if (focused) 0.1f else 0.035f))
            .border(1.dp, if (focused) Color.White.copy(alpha = 0.35f) else IptvColors.DividerStrong, RoundedCornerShape(StreamingRadius.Panel))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .focusable(interactionSource = interaction)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(27.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(13.dp))
        Text("Νέα πηγή", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text("M3U, Xtream ή Stalker", color = IptvColors.TextTertiary, fontSize = 11.sp)
    }
}

@Composable
private fun TvSourceAction(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        Modifier.clip(RoundedCornerShape(9.dp))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.075f))
            .clickable(enabled = enabled, interactionSource = source, indication = null, onClick = onClick)
            .focusable(enabled = enabled, interactionSource = source)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (!enabled) IptvColors.TextTertiary else if (focused) Color.Black else Color.White, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = if (!enabled) IptvColors.TextTertiary else if (focused) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun sourceBrush(type: PlaylistType): Brush = when (type) {
    PlaylistType.M3U -> Brush.linearGradient(listOf(Color(0xFF176D8E), Color(0xFF12384B)))
    PlaylistType.XTREAM -> Brush.linearGradient(listOf(Color(0xFF6328BC), Color(0xFF321668)))
    PlaylistType.STALKER -> Brush.linearGradient(listOf(Color(0xFF7A3519), Color(0xFF38170B)))
}
