package com.prelude.iptv.ui.mobile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingRadius

@Composable
internal fun MobileSettingsTopBar(onNotifications: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .height(76.dp)
            .border(width = 0.5.dp, color = IptvColors.Divider)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.size(42.dp))
        Text(
            "Ρυθμίσεις",
            modifier = Modifier.weight(1f),
            color = IptvColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Box {
            IconButton(onClick = onNotifications, modifier = Modifier.size(42.dp)) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = "Ειδοποιήσεις",
                    tint = IptvColors.TextPrimary,
                    modifier = Modifier.size(25.dp)
                )
            }
            Box(
                Modifier.align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 6.dp)
                    .size(8.dp)
                    .background(IptvColors.Primary, CircleShape)
                    .border(2.dp, IptvColors.Background, CircleShape)
            )
        }
    }
}

@Composable
internal fun MobileSettingsAccountHero(
    profileName: String,
    onProfile: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(IptvColors.Background, Color(0xFF16090A), IptvColors.Background)
                )
            )
            .padding(horizontal = 20.dp, vertical = 23.dp)
    ) {
        Column(Modifier.fillMaxWidth(0.78f)) {
            Text(
                "Το PRELUDE+\nστα μέτρα σου",
                color = IptvColors.TextPrimary,
                fontSize = 27.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Προφίλ, ασφάλεια και προσωπικές επιλογές σε ένα σημείο.",
                color = IptvColors.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onProfile,
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    Modifier.size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Brush.linearGradient(listOf(IptvColors.Primary, Color(0xFF79070D)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profileName.take(1).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
                Text("Διαχείριση προφίλ", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun MobileSettingsPremiumCard(onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(StreamingRadius.Panel))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF271719), Color(0xFF171717), Color(0xFF211011))
                )
            )
            .border(1.dp, IptvColors.Divider, RoundedCornerShape(StreamingRadius.Panel))
            .padding(19.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, null, tint = IptvColors.Primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(7.dp))
            Text("PRELUDE+", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .background(IptvColors.Primary.copy(alpha = 0.17f))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text("PREMIUM", color = Color(0xFFFF666D), fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(15.dp))
        Text(
            "Όλες οι δυνατότητες.\nΧωρίς περιορισμούς.",
            color = Color.White,
            fontSize = 22.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Πολλαπλές πηγές, Multiview, online υπότιτλοι, προφίλ και εξατομίκευση.",
            color = IptvColors.TextSecondary,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary, contentColor = Color.White)
        ) {
            Text("Ανακάλυψε το Premium", fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun MobileSettingsGroupTitle(title: String) {
    Text(
        title,
        color = Color(0xFFC8C8C8),
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 27.dp, bottom = 10.dp)
    )
}

@Composable
internal fun MobileOverviewRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    value: String = "",
    checked: Boolean? = null,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(StreamingRadius.Card))
            .background(IptvColors.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(StreamingRadius.Card))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (primary) IptvColors.Primary.copy(alpha = 0.14f) else IptvColors.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (primary) Color(0xFFFF555D) else IptvColors.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = IptvColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = IptvColors.TextTertiary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (checked != null) {
            Switch(
                checked = checked,
                onCheckedChange = { onClick() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = IptvColors.Primary,
                    checkedTrackColor = IptvColors.Primary.copy(alpha = 0.35f),
                    uncheckedThumbColor = IptvColors.TextSecondary,
                    uncheckedTrackColor = IptvColors.SurfaceRaised
                ),
                modifier = Modifier.height(26.dp)
            )
        } else {
            if (value.isNotBlank()) {
                Text(value, color = IptvColors.TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(5.dp))
            }
            Icon(Icons.Default.ChevronRight, null, tint = IptvColors.TextTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun MobileSettingsRows(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content
    )
}

@Composable
internal fun MobileSettingsStatusCard(label: String, value: String, healthy: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(15.dp))
            .padding(13.dp)
    ) {
        Text(label, color = IptvColors.TextTertiary, fontSize = 9.sp)
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (healthy) {
                Box(Modifier.size(6.dp).background(Color(0xFF48C875), CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(value, color = IptvColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
