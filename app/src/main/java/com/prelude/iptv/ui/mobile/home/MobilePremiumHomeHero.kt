package com.prelude.iptv.ui.mobile.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.home.HomeCinematicBackdrop
import com.prelude.iptv.ui.components.home.homeHeroCandidates
import com.prelude.iptv.ui.components.home.rememberHomeMeta

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MobilePremiumHomeHero(
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    profileName: String,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?,
    onPlay: (Channel) -> Unit,
    onDetails: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onSearch: () -> Unit,
    selectedDestination: String = "home",
    onOpenMovies: () -> Unit = {},
    onOpenSeries: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    /**
     * Ζωγραφίζει το δικό του λογότυπο, αναζήτηση, προφίλ και καρτέλες από πάνω.
     *
     * false όταν η οθόνη έχει ήδη δική της κεφαλίδα. Δύο κεφαλίδες η μία πάνω
     * στην άλλη δεν είναι θέμα γούστου — το λογότυπο εμφανίζεται δύο φορές.
     */
    showChrome: Boolean = true,
) {
    val heroes = homeHeroCandidates(channels)
    val pager = rememberPagerState(pageCount = { heroes.size })
    Box(Modifier.fillMaxWidth().height(515.dp)) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(0.dp),
            pageSpacing = 0.dp,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val channel = heroes[page]
            val meta by rememberHomeMeta(channel, tmdbFor)
            MobileHeroPage(
                channel = channel,
                meta = meta,
                favorite = PlaybackQueue.favKey(channel) in favoriteKeys,
                onPlay = { onPlay(channel) },
                onDetails = { onDetails(channel) },
                onToggleFavorite = { onToggleFavorite(channel) }
            )
        }
        if (showChrome) Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PRELUDE", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("+", color = IptvColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                MobileHeroRoundButton(Icons.Default.Search, "Αναζήτηση", onSearch)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xAA2D3035)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profileName.trim().firstOrNull()?.uppercase() ?: "K",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                MobileHeroTab("Σειρές", selectedDestination == "series", onOpenSeries)
                MobileHeroTab("Ταινίες", selectedDestination == "movies" || selectedDestination == "home", onOpenMovies)
                MobileHeroTab("Κατηγορίες ▾", false, onOpenCategories)
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            heroes.indices.forEach { index ->
                Box(
                    Modifier
                        .width(if (index == pager.currentPage) 18.dp else 5.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (index == pager.currentPage) Color.White else Color.White.copy(alpha = .28f))
                )
            }
        }
    }
}

@Composable
private fun MobileHeroPage(
    channel: Channel,
    meta: TmdbClient.Meta?,
    favorite: Boolean,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val title = TmdbClient.cleanTitle(channel.name).ifBlank { channel.name }
    val year = meta?.year?.takeIf(String::isNotBlank) ?: channel.year
    val overview = meta?.overview?.takeIf(String::isNotBlank) ?: channel.plot
    val genre = meta?.genres?.takeIf(String::isNotBlank) ?: channel.genre
    Box(Modifier.fillMaxSize().clickable(onClick = onDetails)) {
        HomeCinematicBackdrop(channel, meta, mobile = true, modifier = Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 19.dp, vertical = 31.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "#1 ΣΤΙΣ ΕΠΙΛΟΓΕΣ ΣΟΥ",
                color = Color.White,
                fontSize = 9.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(9.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val genreTags = genre.split(",", "/", "·", "|", "&").map { it.trim() }.filter { it.isNotBlank() }.take(3)
            if (genreTags.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    genreTags.joinToString("   ·   "),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                meta?.rating?.takeIf(String::isNotBlank)?.let {
                    Text("$it Match", color = IptvColors.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(" · ", color = IptvColors.TextSecondary, fontSize = 11.sp)
                }
                if (year.isNotBlank()) Text(year, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (overview.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    overview,
                    color = Color(0xFFCDD0D4),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).clickable(onClick = onPlay),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Αναπαραγωγή", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                MobileHeroSquareButton(
                    icon = if (favorite) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = "Η λίστα μου",
                    onClick = onToggleFavorite
                )
                MobileHeroSquareButton(Icons.Default.Info, "Πληροφορίες", onDetails)
            }
        }
    }
}

@Composable
private fun MobileHeroTab(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (active) Color.White else Color(0xFFCFCFCF),
        fontSize = 13.sp,
        fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp)
    )
}

@Composable
private fun MobileHeroRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = .42f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(19.dp)) }
}

@Composable
private fun MobileHeroSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = .14f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(21.dp)) }
}
