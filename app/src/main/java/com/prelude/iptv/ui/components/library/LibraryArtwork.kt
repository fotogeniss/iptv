package com.prelude.iptv.ui.components.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration

@Composable
fun LibraryCinematicBackdrop(
    channel: Channel?,
    meta: TmdbClient.Meta?,
    mobile: Boolean,
    modifier: Modifier = Modifier
) {
    val image = meta?.backdrop?.takeIf(String::isNotBlank)
        ?: meta?.poster?.takeIf(String::isNotBlank)
        ?: channel?.logo.orEmpty()
    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        Crossfade(image, animationSpec = tween(motionDuration(Motion.Hero), easing = Motion.EmphasizedEasing), label = "libraryBackdrop") { target ->
            if (target.isNotBlank()) {
                AsyncImage(
                    model = target,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                if (mobile) {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = .12f),
                        .48f to Color.Black.copy(alpha = .18f),
                        .76f to Color.Black.copy(alpha = .82f),
                        1f to IptvColors.Background
                    )
                } else {
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = .98f),
                        .36f to Color.Black.copy(alpha = .88f),
                        .68f to Color.Black.copy(alpha = .30f),
                        1f to Color.Black.copy(alpha = .42f)
                    )
                }
            )
        )
        if (!mobile) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        .58f to Color.Black.copy(alpha = .06f),
                        .82f to IptvColors.Background.copy(alpha = .88f),
                        1f to IptvColors.Background
                    )
                )
            )
        }
    }
}

@Composable
fun LibraryArtwork(
    channel: Channel,
    image: String = channel.logo,
    modifier: Modifier = Modifier
) {
    Box(modifier.background(IptvColors.Surface), contentAlignment = Alignment.Center) {
        if (image.isNotBlank()) {
            AsyncImage(
                model = image,
                contentDescription = channel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val icon = when (channel.kind) {
                "live" -> Icons.Outlined.LiveTv
                "series" -> Icons.Outlined.Tv
                else -> Icons.Outlined.Movie
            }
            Icon(icon, null, tint = IptvColors.TextTertiary, modifier = Modifier.size(42.dp))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    .58f to Color.Black.copy(alpha = .08f),
                    1f to Color.Black.copy(alpha = .88f)
                )
            )
        )
    }
}

@Composable
fun BoxScope.LibraryCardBadges(
    channel: Channel,
    favorite: Boolean,
    selectedForRemoval: Boolean = false
) {
    if (channel.kind == "live") {
        Text(
            "LIVE",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .clip(RoundedCornerShape(4.dp)).background(IptvColors.Primary)
                .padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
    if (favorite || selectedForRemoval) {
        Box(
            Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selectedForRemoval) Color.White else Color.Black.copy(alpha = .72f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = if (selectedForRemoval) Color.Black else Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
fun BoxScope.LibraryCardProgress(progress: Float?) {
    val safe = progress ?: return
    StreamingProgress(
        progress = safe,
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp)
    )
}
