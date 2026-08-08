package com.prelude.iptv.ui.mobile.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.CatalogRailSection
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale
import androidx.compose.animation.core.tween

@Composable
internal fun MobilePremiumHomeRail(
    section: CatalogRailSection,
    favoriteKeys: Set<String>,
    onOpen: (Channel) -> Unit,
    onViewAll: (CatalogRailSection) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Αναγκάζει κάθετες αφίσες ή πλατιά πλακίδια, αντί να συμπεραίνεται από το id.
     *
     * Το συμπέρασμα από το id δούλευε όσο τα id τα έφτιαχνε ΕΝΑ σημείο. Τώρα που
     * η αρχική συνθέτει δικά της rails, ένα άγνωστο id θα έπαιρνε σιωπηλά λάθος
     * σχήμα — και τα ζωντανά κανάλια σε κάθετη αφίσα είναι μια λωρίδα κενού.
     */
    portraitOverride: Boolean? = null,
) {
    val portrait = portraitOverride
        ?: (section.id == "new" || section.id == "my-list" || section.id.startsWith("group:"))
    Column(modifier.fillMaxWidth().padding(top = 18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(section.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(
                "Προβολή όλων",
                color = IptvColors.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onViewAll(section) }.padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            itemsIndexed(
                section.items,
                key = { index, channel -> "premium-mobile:${section.id}:$index:${PlaybackQueue.favKey(channel)}" }
            ) { index, channel ->
                if (section.ranked) {
                    MobileTop10Card(
                        channel = channel,
                        rank = index + 1,
                        favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                        onClick = { onOpen(channel) }
                    )
                } else {
                    MobilePremiumHomeCard(
                        channel = channel,
                        portrait = portrait,
                        rank = null,
                        progress = section.progress[PlaybackQueue.favKey(channel)],
                        favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                        onClick = { onOpen(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MobilePremiumHomeCard(
    channel: Channel,
    portrait: Boolean,
    rank: Int?,
    progress: Float?,
    favorite: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressedScale = motionScale(Motion.MobilePressedScale)
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        tween(motionDuration(Motion.Fast), easing = Motion.StandardEasing),
        label = "mobilePremiumCardPress"
    )
    val width = if (portrait) 130.dp else 210.dp
    val height = if (portrait) 195.dp else 126.dp
    val shape = RoundedCornerShape(11.dp)

    Box(
        Modifier
            .width(width)
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(IptvColors.Surface)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        if (channel.logo.isNotBlank()) {
            AsyncImage(channel.logo, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Outlined.Movie, null, tint = IptvColors.TextTertiary, modifier = Modifier.align(Alignment.Center).size(34.dp))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, .58f to Color.Transparent, 1f to Color.Black.copy(alpha = .94f))))
        rank?.let {
            Text(
                it.toString(),
                color = Color.White,
                fontSize = if (portrait) 38.sp else 31.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 9.dp, bottom = if (progress != null) 31.dp else 26.dp)
            )
        }
        if (favorite) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(25.dp).clip(RoundedCornerShape(99.dp)).background(Color.Black.copy(alpha = .72f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Check, "Στη λίστα μου", tint = Color.White, modifier = Modifier.size(15.dp)) }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(channel.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = listOf(channel.year, channel.genre).filter(String::isNotBlank).joinToString(" · ")
            if (meta.isNotBlank() && !portrait) {
                Text(meta, color = IptvColors.TextSecondary, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            progress?.let {
                Spacer(Modifier.height(7.dp))
                StreamingProgress(it, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Premium «Προεπισκοπήσεις»: κυκλικά thumbnails με κόκκινο δαχτυλίδι. */
@Composable
internal fun MobilePreviewsRow(
    channels: List<Channel>,
    onOpen: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val previews = remember(channels) { channels.filter { it.logo.isNotBlank() }.take(14) }
    if (previews.isEmpty()) return
    Column(modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            "Προεπισκοπήσεις",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 18.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(previews) { ch ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(66.dp).clickable { onOpen(ch) }
                ) {
                    Box(
                        Modifier.size(62.dp).clip(CircleShape)
                            .border(2.dp, IptvColors.Primary, CircleShape).padding(3.dp)
                    ) {
                        AsyncImage(
                            ch.logo, ch.name,
                            Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        ch.name,
                        color = IptvColors.TextSecondary,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Premium «Top 10»: τεράστιο περιγραμμένο νούμερο δίπλα (πίσω) από κάθετη αφίσα. */
@Composable
private fun MobileTop10Card(
    channel: Channel,
    rank: Int,
    favorite: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .width(158.dp)
            .height(128.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            rank.toString(),
            style = TextStyle(
                fontSize = 92.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF5C5C62),
                drawStyle = Stroke(width = 5f)
            ),
            modifier = Modifier.align(Alignment.BottomStart)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .width(88.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(IptvColors.Surface)
        ) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(channel.logo, channel.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(
                    Icons.Outlined.Movie, null,
                    tint = IptvColors.TextTertiary,
                    modifier = Modifier.align(Alignment.Center).size(30.dp)
                )
            }
            if (favorite) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp)
                        .clip(RoundedCornerShape(99.dp)).background(Color.Black.copy(alpha = .72f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Check, "Στη λίστα μου", tint = Color.White, modifier = Modifier.size(13.dp)) }
            }
        }
    }
}
