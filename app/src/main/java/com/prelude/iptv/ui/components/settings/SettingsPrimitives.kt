package com.prelude.iptv.ui.components.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingRadius
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale

@Composable
fun PremiumSettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    value: String = "",
    checked: Boolean? = null,
    television: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        if (focused && television) motionScale(1.018f) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing), label = "settingsRowScale"
    )
    val background = if (focused && television) Color.White else IptvColors.Surface
    val foreground = if (focused && television) Color.Black else IptvColors.TextPrimary
    val secondary = if (focused && television) Color(0xFF55555D) else IptvColors.TextTertiary

    Row(
        modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(RoundedCornerShape(StreamingRadius.Card))
            .background(background)
            .border(1.dp, if (focused && television) Color.Transparent else IptvColors.Divider, RoundedCornerShape(StreamingRadius.Card))
            // ΣΗΜΑΝΤΙΚΟ: μόνο .clickable — είναι ήδη focusable και μετατρέπει το
            // OK/DPAD-center σε click. Ένα επιπλέον .focusable() εδώ «έκλεβε» το
            // focus σε κόμβο χωρίς click, οπότε σε TV το πάτημα δεν ενεργοποιούσε
            // το item («δεν άνοιγε τίποτα»).
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(if (television) 42.dp else 38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (focused && television) Color.Black.copy(alpha = 0.08f) else IptvColors.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = foreground, modifier = Modifier.size(if (television) 22.dp else 20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = foreground, fontSize = if (television) 15.sp else 14.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = secondary, fontSize = if (television) 11.sp else 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (checked != null) {
            Switch(
                checked = checked,
                onCheckedChange = { onClick() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = IptvColors.Primary,
                    uncheckedThumbColor = IptvColors.TextSecondary,
                    uncheckedTrackColor = IptvColors.SurfaceRaised
                )
            )
        } else if (value.isNotBlank()) {
            Text(value, color = if (focused && television) Color.Black else IptvColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, subtitle: String = "", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(title, color = IptvColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = IptvColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
fun SettingsHealthCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(StreamingRadius.Card))
            .background(IptvColors.Surface)
            .border(1.dp, IptvColors.Divider, RoundedCornerShape(StreamingRadius.Card))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(value, color = IptvColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = IptvColors.TextTertiary, fontSize = 10.sp)
    }
}
