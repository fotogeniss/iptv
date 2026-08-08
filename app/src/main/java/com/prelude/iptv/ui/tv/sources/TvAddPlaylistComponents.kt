package com.prelude.iptv.ui.tv.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.sources.PlaylistSourceMethod
import com.prelude.iptv.ui.tvFocus

@Composable
internal fun TvPlaylistBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(IptvColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("PRELUDE", color = IptvColors.TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("+", color = IptvColors.Primary, fontSize = 27.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun TvPlaylistMethodCard(
    method: PlaylistSourceMethod,
    selected: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    val content = when (method) {
        PlaylistSourceMethod.URL -> TvMethodContent(Icons.Default.Link, "Έναν σύνδεσμο", "M3U", "M3U ή get.php URL")
        PlaylistSourceMethod.XTREAM -> TvMethodContent(Icons.Default.Lock, "Server και κωδικούς", "XTREAM", "Server, username, password")
        PlaylistSourceMethod.MAC -> TvMethodContent(Icons.Default.Dns, "Portal και MAC", "STALKER", "Portal URL και MAC address")
        PlaylistSourceMethod.FILE -> TvMethodContent(Icons.Default.FolderOpen, "Ένα αρχείο", "FILE", "M3U ή M3U8 στη συσκευή")
    }
    Row(
        modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) Color.White.copy(alpha = .08f) else IptvColors.Surface.copy(alpha = .76f))
            .border(
                if (selected) 1.dp else .5.dp,
                if (selected) Color.White.copy(alpha = .45f) else IptvColors.DividerStrong,
                RoundedCornerShape(15.dp),
            )
            .tvFocus(RoundedCornerShape(15.dp), tint = true)
            .clickable(onClick = onSelect)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                .background(if (selected) IptvColors.Primary.copy(alpha = .16f) else Color.White.copy(alpha = .06f))
                .border(1.dp, if (selected) IptvColors.Primary.copy(alpha = .35f) else Color.White.copy(alpha = .08f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(content.icon, null, tint = if (selected) Color.White else IptvColors.TextSecondary, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                content.title,
                color = if (selected) Color.White else IptvColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    content.tag,
                    color = IptvColors.TextSecondary,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = .07f))
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(content.subtitle, color = IptvColors.TextTertiary, fontSize = 8.5.sp, maxLines = 1)
            }
        }
        Box(
            Modifier.size(22.dp).border(2.dp, if (selected) Color.White else IptvColors.TextTertiary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).background(Color.White, CircleShape))
        }
    }
}

@Composable
internal fun TvPlaylistFormCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(IptvColors.Surface.copy(alpha = .82f))
            .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 17.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, color = IptvColors.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = IptvColors.TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text("ΒΗΜΑ 2/2", color = IptvColors.TextTertiary, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
internal fun TvPlaylistFieldButton(
    label: String,
    value: String,
    placeholder: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    password: Boolean = false,
    error: String? = null,
    onClick: () -> Unit,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = IptvColors.TextSecondary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            if (optional) Text("Προαιρετικό", color = IptvColors.TextTertiary, fontSize = 7.5.sp)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(49.dp)
                .focusRequester(focusRequester)
                .clip(RoundedCornerShape(11.dp))
                .background(IptvColors.SurfaceRaised)
                .border(1.dp, if (error == null) IptvColors.DividerStrong else IptvColors.Error, RoundedCornerShape(11.dp))
                .tvFocus(RoundedCornerShape(11.dp), tint = false)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val shown = when {
                value.isBlank() -> placeholder
                password -> "•".repeat(value.length.coerceAtMost(14))
                else -> value
            }
            Text(
                shown,
                color = if (value.isBlank()) IptvColors.TextTertiary else IptvColors.TextPrimary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        error?.let { message ->
            Text(message, color = IptvColors.Error, fontSize = 7.5.sp, modifier = Modifier.padding(start = 3.dp, top = 3.dp))
        }
    }
}

@Composable
internal fun TvM3uFileButton(
    fileLabel: String,
    importing: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .focusRequester(focusRequester)
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = .035f))
            .border(1.dp, if (fileLabel.isBlank()) IptvColors.DividerStrong else IptvColors.Success.copy(alpha = .55f), RoundedCornerShape(13.dp))
            .tvFocus(RoundedCornerShape(13.dp), tint = true)
            .clickable(enabled = !importing, onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(43.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = .06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = IptvColors.TextSecondary, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    importing -> "Εισαγωγή αρχείου…"
                    fileLabel.isBlank() -> "Επίλεξε αρχείο από τη συσκευή"
                    else -> fileLabel
                },
                color = if (fileLabel.isBlank()) IptvColors.TextSecondary else IptvColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (fileLabel.isBlank()) FontWeight.Normal else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Υποστηρίζονται .m3u και .m3u8", color = IptvColors.TextTertiary, fontSize = 8.sp, modifier = Modifier.padding(top = 3.dp))
        }
        if (fileLabel.isNotBlank()) Icon(Icons.Default.CheckCircle, null, tint = IptvColors.Success, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun TvPlaylistAdvancedButton(
    expanded: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .focusRequester(focusRequester)
            .clip(RoundedCornerShape(8.dp))
            .tvFocus(RoundedCornerShape(8.dp), tint = false, scale = false)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            null,
            tint = IptvColors.TextSecondary,
            modifier = Modifier.size(17.dp).rotate(if (expanded) 180f else 0f),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            if (expanded) "Λιγότερες επιλογές" else "Περισσότερες επιλογές",
            color = IptvColors.TextSecondary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun TvPlaylistAction(
    label: String,
    primary: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .height(48.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(11.dp))
            .background(
                when {
                    !enabled -> IptvColors.Surface.copy(alpha = .55f)
                    primary -> IptvColors.Primary
                    else -> IptvColors.Surface
                },
            )
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else IptvColors.DividerStrong, RoundedCornerShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else IptvColors.TextTertiary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                label,
                color = if (enabled) Color.White else IptvColors.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private data class TvMethodContent(
    val icon: ImageVector,
    val title: String,
    val tag: String,
    val subtitle: String,
)
