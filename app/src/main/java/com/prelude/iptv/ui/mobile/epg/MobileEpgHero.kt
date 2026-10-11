package com.prelude.iptv.ui.mobile.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.components.epg.epgPalette
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.ui.components.epg.epgTimeRange
import com.prelude.iptv.ui.icons.filled.CalendarMonth
import com.prelude.iptv.ui.icons.filled.PlayArrow

/** Η κάρτα του επιλεγμένου καναλιού: τι παίζει, πρόοδος, Παρακολούθηση / Όλη η μέρα. */
@Composable
internal fun MobileEpgNowCard(
    row: MobileEpgRow,
    nowMs: Long,
    onWatch: () -> Unit,
    onFullDay: () -> Unit,
) {
    val palette = epgPalette(row.channel)
    val programme = row.primary
    val live = programme != null && nowMs in programme.startMs until programme.stopMs
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .padding(start = 18.dp, end = 18.dp, top = 14.dp)
            .fillMaxWidth()
            .heightIn(min = 190.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(palette.primary.copy(alpha = 0.9f), palette.surface, IptvColors.Background))
            )
    ) {
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0.15f to Color.Transparent, 1f to IptvColors.Background.copy(alpha = 0.93f))
            )
        )
        Column(
            Modifier.fillMaxWidth().padding(16.dp).align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (programme != null) {
                if (live) {
                    LiveMark(epgUpper(stringResource(R.string.epg_now_range, epgTimeRange(programme))))
                } else {
                    Text(epgTimeRange(programme), color = IptvColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                programme?.title ?: row.channel.name,
                color = Color.White,
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 27.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (programme != null) row.channel.name else stringResource(R.string.epg_no_guide),
                color = IptvColors.TextSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (live && programme != null) {
                StreamingProgress(epgProgress(programme, nowMs), Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EpgCardButton(stringResource(R.string.epg_watch), Icons.Default.PlayArrow, primary = true, onClick = onWatch)
                EpgCardButton(stringResource(R.string.epg_full_day), Icons.Default.CalendarMonth, primary = false, onClick = onFullDay)
            }
        }
    }
}

@Composable
internal fun EpgCardButton(label: String, icon: ImageVector, primary: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (primary) IptvColors.Primary else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = Color.White, fontSize = 13.5.sp, fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
internal fun MobileEpgEmptyState(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(IptvColors.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                stringResource(R.string.epg_empty),
                color = IptvColors.TextPrimary,
                fontFamily = AppFonts.Display,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            EpgCardButton(stringResource(R.string.epg_return), Icons.Default.PlayArrow, primary = true, onClick = onBack)
        }
    }
}
