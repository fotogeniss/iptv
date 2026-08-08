package com.prelude.iptv.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val PlaybackAccent = IptvColors.Primary
private val HeroBlack = IptvColors.Background

/**
 * TV-first cinematic hero. It deliberately avoids auto-play and auto-scroll:
 * both create noisy network/player churn and unpredictable DPAD focus on TV boxes.
 */
@Composable
fun PremiumTvHero(
    channel: Channel,
    favorite: Boolean,
    progress: WatchProgress? = null,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var meta by remember(channel) { mutableStateOf<TmdbClient.Meta?>(null) }
    LaunchedEffect(channel) { meta = tmdbFor(channel) }

    val tv = isTvDevice()
    Box(
        modifier
            .fillMaxWidth()
            .height(if (tv) 420.dp else 280.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(HeroBlack)
            .clickable(onClick = onDetails)
    ) {
        if (channel.logo.isNotBlank()) {
            AsyncImage(
                model = meta?.backdrop?.takeIf { it.isNotBlank() } ?: channel.logo,
                contentDescription = channel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color(0xF40B0B0D),
                    0.45f to Color(0xCC0B0B0D),
                    1f to Color.Transparent
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.68f to Color(0x2A000000),
                    1f to HeroBlack
                )
            )
        )

        Column(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(if (tv) 0.56f else 0.82f)
                .padding(
                    start = if (tv) AppDimens.TvHorizontal else AppDimens.MobileHorizontal,
                    end = 18.dp,
                    top = if (tv) 64.dp else 30.dp,
                    bottom = 22.dp
                ),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = TmdbClient.cleanTitle(channel.name).ifBlank { channel.name },
                color = Color.White,
                fontSize = if (tv) 34.sp else 25.sp,
                lineHeight = if (tv) 42.sp else 29.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp, bottom = 3.dp)
            )
            Spacer(Modifier.height(10.dp))

            val year = meta?.year?.takeIf { it.isNotBlank() } ?: channel.year
            val genres = meta?.genres?.takeIf { it.isNotBlank() } ?: channel.genre
            val rating = meta?.rating?.takeIf { it.isNotBlank() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                rating?.let {
                    Text("$it TMDB", color = Color(0xFF59D68A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                }
                if (year.isNotBlank()) Text(year, color = Color.White, fontSize = 14.sp)
                if (genres.isNotBlank()) {
                    Spacer(Modifier.width(12.dp))
                    Text(genres, color = Color(0xFFD0D0D6), fontSize = 14.sp, maxLines = 1)
                }
            }

            val overview = meta?.overview.orEmpty()
            if (overview.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    overview,
                    color = Color(0xFFD6D6DB),
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 150.dp)
                        .tvFocus(RoundedCornerShape(6.dp), tint = false)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (progress != null) "Συνέχεια · ${progress.percent}%" else "Αναπαραγωγή",
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onDetails,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x6634343A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 160.dp)
                        .tvFocus(RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Default.Info, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Πληροφορίες", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onFavorite,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x6634343A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(width = 48.dp, height = 48.dp)
                        .tvFocus(RoundedCornerShape(6.dp))
                ) {
                    Icon(if (favorite) Icons.Default.Check else Icons.Default.Add, "Η λίστα μου")
                }
            }
            progress?.let {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(0.72f).height(3.dp).background(Color(0xFF4A4A50))) {
                    Box(Modifier.fillMaxWidth(it.fraction).height(3.dp).background(PlaybackAccent))
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    WatchProgressPolicy.remainingLabel(it),
                    color = Color(0xFFB8B8BE),
                    fontSize = 11.sp
                )
            }
        }


    }
}
