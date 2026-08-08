package com.prelude.iptv.ui.tv.search

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Movie
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.PremiumSearchFilter
import com.prelude.iptv.ui.SearchUiPolicy
import com.prelude.iptv.ui.StreamingProgress
import androidx.compose.animation.core.tween
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.localizedSearchCategory
import com.prelude.iptv.ui.localization.localizedUppercase

@Composable
internal fun TvSearchFeatured(
    channel: Channel,
    meta: TmdbClient.Meta?,
    favorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val image = meta?.backdrop?.takeIf(String::isNotBlank) ?: channel.logo
    Box(
        modifier.fillMaxWidth().height(184.dp).shadow(20.dp, RoundedCornerShape(17.dp))
            .clip(RoundedCornerShape(17.dp)).background(IptvColors.Surface)
    ) {
        if (image.isNotBlank()) AsyncImage(image, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = .96f),
                    .62f to Color.Black.copy(alpha = .40f),
                    1f to Color.Black.copy(alpha = .12f)
                )
            )
        )
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(.78f).padding(23.dp)) {
            Text(
                stringResource(
                    R.string.search_featured,
                    localizedUppercase(localizedSearchCategory(SearchUiPolicy.category(channel))),
                ),
                color = IptvColors.Success,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(5.dp))
            Text(channel.name, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = SearchUiPolicy.metaLine(channel, meta)
            if (line.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(line, color = Color.White.copy(alpha = .82f), fontSize = 10.sp, maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                SearchUiPolicy.description(channel, meta)
                    ?: stringResource(R.string.search_discover_more, channel.name),
                color = IptvColors.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            TvHeroAction(Icons.Default.PlayArrow, stringResource(R.string.search_open), primary = true, onClick = onOpen)
            TvHeroAction(if (favorite) Icons.Default.Check else Icons.Default.Add, stringResource(R.string.search_list), primary = false, onClick = onToggleFavorite)
        }
    }
}

@Composable
private fun TvHeroAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvActionScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "tvSearchHeroAction"
    )
    Row(
        Modifier.height(38.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary || focused) Color.White else Color.Black.copy(alpha = .62f))
            .onFocusChanged { focused = it.isFocused || it.hasFocus }.clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (primary || focused) Color.Black else Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = if (primary || focused) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun TvSearchFilterRow(
    selected: PremiumSearchFilter,
    onSelect: (PremiumSearchFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PremiumSearchFilter.entries.forEach { filter ->
            TvSearchFilterChip(filter, selected == filter) { onSelect(filter) }
        }
    }
}

@Composable
private fun TvSearchFilterChip(
    filter: PremiumSearchFilter,
    selected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvActionScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "tvSearchFilterScale"
    )
    Text(
        stringResource(filter.labelRes()),
        color = if (focused) Color.Black else if (selected) Color.White else IptvColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.height(34.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                when {
                    focused -> Color.White
                    selected -> Color.White.copy(alpha = .14f)
                    else -> IptvColors.Surface.copy(alpha = .78f)
                }
            )
            .onFocusChanged { focused = it.isFocused || it.hasFocus }.clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp)
    )
}

@Composable
internal fun TvSearchResultCard(
    channel: Channel,
    favorite: Boolean,
    progress: Float?,
    onFocused: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artwork = com.prelude.iptv.ui.components.rememberPosterArtwork(channel)
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) motionScale(Motion.TvEmphasisScale) else 1f,
        tween(motionDuration(Motion.Focus), easing = Motion.StandardEasing),
        label = "tvSearchResultScale"
    )
    Box(
        modifier.height(182.dp).zIndex(if (focused) 3f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 24.dp else 5.dp, RoundedCornerShape(9.dp))
            .clip(RoundedCornerShape(9.dp)).background(IptvColors.SurfaceRaised)
            .onFocusChanged {
                focused = it.isFocused || it.hasFocus
                if (it.isFocused) onFocused()
            }
            .clickable(onClick = onOpen)
    ) {
        if (artwork.isNotBlank()) {
            AsyncImage(artwork, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Outlined.Movie, null, tint = IptvColors.TextTertiary, modifier = Modifier.align(Alignment.Center).size(34.dp))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(.42f to Color.Transparent, 1f to Color.Black.copy(alpha = .95f))))
        if (focused) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .045f)))
        if (channel.kind == "live") {
            Text(
                "LIVE",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(7.dp).clip(RoundedCornerShape(4.dp)).background(IptvColors.Primary)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        if (favorite) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(7.dp).size(25.dp).clip(CircleShape).background(Color.Black.copy(alpha = .68f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp)) }
        }
        if (focused) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(9.dp).size(30.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp)) }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(9.dp)) {
            Text(channel.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(SearchUiPolicy.metaLine(channel), color = IptvColors.TextSecondary, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            progress?.let {
                Spacer(Modifier.height(6.dp))
                StreamingProgress(it, Modifier.fillMaxWidth())
            }
        }
    }
}
