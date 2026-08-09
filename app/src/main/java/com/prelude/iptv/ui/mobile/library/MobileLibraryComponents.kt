package com.prelude.iptv.ui.mobile.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.library.LibraryArtwork
import com.prelude.iptv.ui.components.library.LibraryCardBadges
import com.prelude.iptv.ui.components.library.LibraryCardProgress
import com.prelude.iptv.ui.components.library.libraryDescription
import com.prelude.iptv.ui.components.library.libraryMetaLine
import com.prelude.iptv.ui.components.library.libraryTitle

@Composable
internal fun MobileLibraryHeroCopy(
    channel: Channel,
    meta: TmdbClient.Meta?,
    progress: Float?,
    favorite: Boolean,
    onPlay: () -> Unit,
    onInfo: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            stringResource(if (progress != null) R.string.library_eyebrow_continue else R.string.library_hero_eyebrow_from_library),
            color = if (progress != null) IptvColors.Success else IptvColors.Primary,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(7.dp))
        Text(
            libraryTitle(channel),
            color = Color.White,
            fontSize = 32.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            libraryMetaLine(channel, meta),
            color = Color(0xFFD5D8DC),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            libraryDescription(channel, meta) ?: stringResource(R.string.library_description_fallback),
            color = Color(0xFFC1C6CC),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPlay,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(if (progress != null) R.string.library_action_continue else R.string.library_action_play),
                    fontWeight = FontWeight.Black, fontSize = 12.sp
                )
            }
            LibraryHeroIconButton(Icons.Default.Info, stringResource(R.string.library_action_info), onInfo)
            LibraryHeroIconButton(
                icon = if (favorite) Icons.Default.Check else Icons.Default.Add,
                description = stringResource(if (favorite) R.string.library_in_my_list else R.string.library_action_add),
                onClick = onFavorite
            )
        }
    }
}

@Composable
private fun LibraryHeroIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = .11f))
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(21.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MobileLibraryCard(
    channel: Channel,
    poster: Boolean,
    progress: Float?,
    favorite: Boolean,
    managementMode: Boolean,
    selectedForRemoval: Boolean,
    onSelect: () -> Unit,
    onToggleRemoval: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.combinedClickable(
            onClick = { if (managementMode) onToggleRemoval() else onSelect() },
            onLongClick = { if (managementMode) onToggleRemoval() else onRemove() }
        )
    ) {
        Box(
            Modifier.fillMaxWidth().then(
                if (poster) Modifier.aspectRatio(2f / 3f) else Modifier.aspectRatio(16f / 9f)
            ).clip(RoundedCornerShape(11.dp))
        ) {
            LibraryArtwork(channel, modifier = Modifier.fillMaxSize())
            LibraryCardBadges(channel, favorite, selectedForRemoval)
            LibraryCardProgress(progress)
            if (managementMode && !selectedForRemoval) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp)
                        .clip(RoundedCornerShape(99.dp)).background(Color.Black.copy(alpha = .72f)),
                    contentAlignment = Alignment.Center
                ) { Text("○", color = Color.White, fontSize = 16.sp) }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            libraryTitle(channel),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = if (poster) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            libraryMetaLine(channel),
            color = IptvColors.TextSecondary,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
