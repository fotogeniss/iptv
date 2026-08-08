@file:android.annotation.SuppressLint("ProduceStateDoesNotAssignValue")

package com.prelude.iptv.ui.components.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration

@Composable
fun rememberSearchMeta(
    channel: Channel?,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?
): State<TmdbClient.Meta?> = produceState<TmdbClient.Meta?>(
    initialValue = null,
    key1 = channel
) {
    value = channel?.takeUnless { it.kind == "live" }?.let { tmdbFor(it) }
}

@Composable
fun SearchCinematicBackdrop(
    channel: Channel?,
    meta: TmdbClient.Meta?,
    mobile: Boolean,
    modifier: Modifier = Modifier
) {
    val image = meta?.backdrop?.takeIf(String::isNotBlank)
        ?: meta?.poster?.takeIf(String::isNotBlank)
        ?: channel?.logo.orEmpty()
    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        Crossfade(targetState = image, animationSpec = tween(motionDuration(Motion.Hero), easing = Motion.EmphasizedEasing), label = "searchBackdrop") { target ->
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
                        0f to Color.Black.copy(alpha = .22f),
                        .42f to Color.Black.copy(alpha = .42f),
                        .72f to Color.Black.copy(alpha = .92f),
                        1f to IptvColors.Background
                    )
                } else {
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = .98f),
                        .34f to Color.Black.copy(alpha = .90f),
                        .64f to Color.Black.copy(alpha = .42f),
                        1f to Color.Black.copy(alpha = .20f)
                    )
                }
            )
        )
        if (!mobile) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = .12f),
                        .60f to Color.Transparent,
                        .83f to IptvColors.Background.copy(alpha = .88f),
                        1f to IptvColors.Background
                    )
                )
            )
        }
    }
}

@Composable
fun PremiumSearchEmpty(
    query: String,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = IptvColors.TextTertiary,
                modifier = Modifier.size(54.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.search_no_results),
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (query.isBlank()) stringResource(R.string.search_empty_category)
                else stringResource(R.string.search_try_different, query.trim()),
                color = IptvColors.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
