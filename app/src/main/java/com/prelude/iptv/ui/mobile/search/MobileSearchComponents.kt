package com.prelude.iptv.ui.mobile.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.greekUppercase
import com.prelude.iptv.ui.SearchUiPolicy
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale
import androidx.compose.animation.core.tween

@Composable
internal fun MobileSearchFeatured(
    channel: Channel,
    meta: TmdbClient.Meta?,
    favorite: Boolean,
    progress: Float?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val image = meta?.backdrop?.takeIf(String::isNotBlank) ?: channel.logo
    Box(
        modifier
            .fillMaxWidth()
            .height(218.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(IptvColors.Surface)
    ) {
        if (image.isNotBlank()) {
            AsyncImage(image, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = .92f),
                    .62f to Color.Black.copy(alpha = .36f),
                    1f to Color.Black.copy(alpha = .18f)
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(17.dp)
        ) {
            Text(
                "${SearchUiPolicy.category(channel).greekUppercase()} · ΠΡΟΤΕΙΝΟΜΕΝΟ",
                color = IptvColors.Success,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(5.dp))
            Text(
                channel.name,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val metaLine = SearchUiPolicy.metaLine(channel, meta)
            if (metaLine.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(metaLine, color = Color.White.copy(alpha = .84f), fontSize = 11.sp, maxLines = 1)
            }
            progress?.let {
                Spacer(Modifier.height(10.dp))
                StreamingProgress(it, Modifier.fillMaxWidth(.66f))
            }
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    Modifier.height(42.dp).clip(RoundedCornerShape(9.dp)).background(Color.White)
                        .clickable(onClick = onOpen).padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Άνοιγμα", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = .62f))
                ) {
                    Icon(
                        if (favorite) Icons.Default.Check else Icons.Default.Add,
                        if (favorite) "Αφαίρεση από τη λίστα" else "Προσθήκη στη λίστα",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
internal fun MobileSearchResultCard(
    channel: Channel,
    favorite: Boolean,
    progress: Float?,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressedScale = motionScale(Motion.MobilePressedScale)
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        tween(motionDuration(Motion.Fast), easing = Motion.StandardEasing),
        label = "mobileSearchCardPress"
    )
    Column(modifier.scale(scale).clickable(interactionSource = interaction, indication = null, onClick = onSelect)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(.70f).clip(RoundedCornerShape(11.dp))
                .background(IptvColors.SurfaceRaised)
        ) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(channel.logo, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Outlined.Movie, null, tint = IptvColors.TextTertiary, modifier = Modifier.align(Alignment.Center).size(38.dp))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(.46f to Color.Transparent, 1f to Color.Black.copy(alpha = .90f))))
            if (channel.kind == "live") {
                Text(
                    "LIVE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(4.dp)).background(IptvColors.Primary)
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(34.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = .58f))
            ) {
                Icon(
                    if (favorite) Icons.Default.Check else Icons.Default.Add,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            progress?.let { StreamingProgress(it, Modifier.align(Alignment.BottomCenter)) }
        }
        Spacer(Modifier.height(7.dp))
        Text(channel.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            SearchUiPolicy.metaLine(channel),
            color = IptvColors.TextTertiary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
