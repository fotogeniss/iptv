package com.prelude.iptv.ui.mobile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingRadius
import com.prelude.iptv.ui.components.settings.SettingsSourceUi
import com.prelude.iptv.ui.localization.labelRes

@Composable
internal fun MobileSettingsSourceCard(
    source: SettingsSourceUi,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: (() -> Unit)? = null
) {
    var menu by remember { mutableStateOf(false) }
    val displayName = source.name.ifBlank { stringResource(R.string.sources_fallback_name, source.index + 1) }
    val displayEndpoint = source.endpoint.ifBlank {
        stringResource(if (source.local) R.string.sources_local_file else R.string.sources_local_source)
    }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(StreamingRadius.Panel))
            .background(IptvColors.Surface)
            .border(1.dp, IptvColors.Divider, RoundedCornerShape(StreamingRadius.Panel))
            .clickable(onClick = onOpen)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(49.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(sourceColor(source.type)),
            contentAlignment = Alignment.Center
        ) {
            Text(source.typeLabel, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    displayName,
                    color = IptvColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (source.current) {
                    Spacer(Modifier.width(7.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(4.dp))
                            .background(IptvColors.Primary.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            stringResource(R.string.sources_status_active).uppercase(),
                            color = IptvColors.Primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(displayEndpoint, color = IptvColors.TextTertiary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            if (source.loading) {
                val downloadingLabel = stringResource(R.string.sources_downloading)
                val progressLabel = source.progressPercent?.let { "$it% · ${source.progressStage}" }
                    ?: source.progressStage.ifBlank { downloadingLabel }
                Text(progressLabel, color = IptvColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                val progressPercent = source.progressPercent
                if (progressPercent != null) {
                    LinearProgressIndicator(
                        progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = IptvColors.Primary,
                        trackColor = IptvColors.Divider
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = IptvColors.Primary,
                        trackColor = IptvColors.Divider
                    )
                }
            } else {
                val statusLabel = stringResource(source.status.labelRes())
                val sourceSummary = source.channelCount?.let {
                    pluralStringResource(R.plurals.sources_loaded_count, it, it)
                } ?: statusLabel
                Text(
                    sourceSummary,
                    color = IptvColors.TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.sources_options), tint = IptvColors.TextSecondary)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sources_open)) },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                    onClick = { menu = false; onOpen() }
                )
                if (onRefresh != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (source.loading) R.string.sources_refreshing else R.string.sources_refresh_playlist)) },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        enabled = !source.loading,
                        onClick = { menu = false; onRefresh() }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sources_edit)) },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = { menu = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sources_delete)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
}

private fun sourceColor(type: PlaylistType): Brush = when (type) {
    PlaylistType.M3U -> Brush.linearGradient(listOf(Color(0xFF176D8E), Color(0xFF12384B)))
    PlaylistType.XTREAM -> Brush.linearGradient(listOf(Color(0xFF6328BC), Color(0xFF321668)))
    PlaylistType.STALKER -> Brush.linearGradient(listOf(Color(0xFF7A3519), Color(0xFF38170B)))
}
