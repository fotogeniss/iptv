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
import com.prelude.iptv.ui.icons.filled.Add
import com.prelude.iptv.ui.icons.filled.Check
import com.prelude.iptv.ui.icons.filled.PlayArrow
import com.prelude.iptv.ui.icons.outlined.Movie
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.design.highlightMatch
import com.prelude.iptv.ui.components.epg.epgProgress
import com.prelude.iptv.data.EpgManager
import androidx.compose.ui.res.pluralStringResource
import com.prelude.iptv.ui.icons.filled.Search
import com.prelude.iptv.ui.icons.filled.History
import com.prelude.iptv.ui.icons.filled.Close
import coil.compose.AsyncImage
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.SearchUiPolicy
import com.prelude.iptv.ui.StreamingProgress
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale
import com.prelude.iptv.ui.localization.localizedSearchCategory
import com.prelude.iptv.ui.localization.localizedUppercase
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
                stringResource(
                    R.string.search_featured,
                    localizedUppercase(localizedSearchCategory(SearchUiPolicy.category(channel))),
                ),
                color = IptvColors.Glow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(5.dp))
            Text(
                channel.name,
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 29.sp,
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.SemiBold,
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
                    Modifier.height(42.dp).clip(RoundedCornerShape(14.dp)).background(IptvColors.Primary)
                        .clickable(onClick = onOpen).padding(start = 12.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.search_open), color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = .62f))
                ) {
                    Icon(
                        if (favorite) Icons.Default.Check else Icons.Default.Add,
                        if (favorite) stringResource(R.string.search_remove_from_list)
                        else stringResource(R.string.search_add_to_list),
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
                    if (favorite) stringResource(R.string.search_remove_from_list)
                    else stringResource(R.string.search_add_to_list),
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

/** Επικεφαλίδα ομάδας: τίτλος, προαιρετικός μετρητής και ενέργεια δεξιά. */
@Composable
internal fun MobileSearchSectionHeader(
    title: String,
    count: Int? = null,
    seeAll: Int? = null,
    onSeeAll: () -> Unit = {},
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            upperNoAccents(title),
            color = IptvColors.TextTertiary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        when {
            seeAll != null -> Text(
                stringResource(R.string.search_see_all, seeAll),
                color = IptvColors.TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSeeAll).padding(10.dp)
            )
            actionLabel != null -> Text(
                actionLabel,
                color = IptvColors.TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(10.dp)
            )
            count != null -> Text(
                pluralStringResource(R.plurals.search_title_count, count, count),
                color = IptvColors.TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

private fun upperNoAccents(text: String): String =
    java.text.Normalizer.normalize(text.uppercase(), java.text.Normalizer.Form.NFD).replace("́", "")

/** Γραμμή αποτελέσματος: εικόνα, τίτλος με τονισμένο τον όρο, τι παίζει τώρα ή στοιχεία. */
@Composable
internal fun MobileSearchResultRow(
    channel: Channel,
    query: String,
    nowMs: Long,
    favorite: Boolean,
    progress: Float?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val live = channel.kind == "live"
    val now = remember(channel.tvgId, live, nowMs) {
        if (live && channel.tvgId.isNotBlank()) EpgManager.nowNext(channel.tvgId, nowMs).first else null
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .then(if (live) Modifier.size(52.dp) else Modifier.size(width = 56.dp, height = 80.dp))
                .clip(RoundedCornerShape(if (live) 14.dp else 12.dp))
                .background(IptvColors.SurfaceSelected),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo.isNotBlank()) {
                AsyncImage(
                    channel.logo,
                    channel.name,
                    Modifier.fillMaxSize().then(if (live) Modifier.padding(6.dp) else Modifier),
                    contentScale = if (live) ContentScale.Fit else ContentScale.Crop
                )
            } else {
                Text(
                    channel.name.take(3).uppercase(),
                    color = IptvColors.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                highlightMatch(channel.name, query),
                color = IptvColors.TextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            if (now != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(IptvColors.Primary, CircleShape))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        stringResource(R.string.live_label),
                        color = IptvColors.Glow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(now.title, color = IptvColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                StreamingProgress(epgProgress(now, nowMs), Modifier.fillMaxWidth(0.9f))
            } else {
                val metaLine = SearchUiPolicy.metaLine(channel)
                Text(
                    metaLine.ifBlank { localizedSearchCategory(SearchUiPolicy.category(channel)) },
                    color = IptvColors.TextTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                progress?.let {
                    Spacer(Modifier.height(6.dp))
                    StreamingProgress(it, Modifier.fillMaxWidth(0.9f))
                }
            }
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
            Icon(
                if (favorite) Icons.Default.Check else Icons.Default.Add,
                if (favorite) stringResource(R.string.search_remove_from_list)
                else stringResource(R.string.search_add_to_list),
                tint = if (favorite) IptvColors.TextPrimary else IptvColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Πρόσφατη (ή δημοφιλής) αναζήτηση: πατάς και ξαναψάχνει, το Χ τη σβήνει. */
@Composable
internal fun MobileRecentSearchRow(
    label: String,
    recent: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(start = 22.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (recent) Icons.Default.History else Icons.Default.Search,
            null,
            tint = IptvColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = IptvColors.TextPrimary,
            fontSize = 14.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
        )
        if (recent) {
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Close, stringResource(R.string.search_remove_recent), tint = IptvColors.TextTertiary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Τέσσερα πλακίδια για να πας κατευθείαν σε μια ενότητα, με το πλήθος τους. */
@Composable
internal fun MobileQuickAccessGrid(
    liveCount: Int,
    movieCount: Int,
    seriesCount: Int,
    myListCount: Int,
    onLive: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onMyList: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(stringResource(R.string.search_filter_live), liveCount, Color(0xFF6B1F2C), Modifier.weight(1f), onLive)
            QuickTile(stringResource(R.string.search_filter_movies), movieCount, Color(0xFF5B3A1F), Modifier.weight(1f), onMovies)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(stringResource(R.string.search_filter_series), seriesCount, Color(0xFF2C1F6B), Modifier.weight(1f), onSeries)
            QuickTile(stringResource(R.string.library_tab_my_list), myListCount, Color(0xFF1F3A6B), Modifier.weight(1f), onMyList)
        }
    }
}

@Composable
private fun QuickTile(title: String, count: Int, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(tint, IptvColors.Surface)))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            pluralStringResource(R.plurals.search_title_count, count, count),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            maxLines = 1
        )
    }
}
