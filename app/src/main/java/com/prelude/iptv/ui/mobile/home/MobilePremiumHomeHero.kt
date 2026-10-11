package com.prelude.iptv.ui.mobile.home

import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.design.HorizonLine
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import com.prelude.iptv.ui.icons.filled.Add
import com.prelude.iptv.ui.icons.filled.Check
import com.prelude.iptv.ui.icons.filled.Info
import com.prelude.iptv.ui.icons.filled.PlayArrow
import com.prelude.iptv.ui.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.R
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.ProviderMetadataPolicy
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.home.HomeCinematicBackdrop
import com.prelude.iptv.ui.components.home.homeHeroCandidates
import com.prelude.iptv.ui.components.home.rememberHomeMeta
import kotlinx.coroutines.delay

/** Πόσο μένει ακίνητη μια σελίδα του hero πριν περάσει μόνη της στην επόμενη. */
private const val HERO_AUTO_ADVANCE_MS = 5_000L

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
    // ΑΥΤΟΜΑΤΗ ΕΝΑΛΛΑΓΗ, ΜΟΝΟ ΟΣΟ ΔΕΝ ΤΟ ΑΓΓΙΖΕΙ Ο ΧΡΗΣΤΗΣ.
    //
    // Το κλειδί είναι το `dragged` και ΟΧΙ το `isScrollInProgress`: το
    // δεύτερο γίνεται true και από τη δική μας κίνηση, οπότε το effect θα
    // ακύρωνε το ίδιο του το animation στη μέση. Με το άγγιγμα το effect
    // ακυρώνεται· με το άφημα ξαναξεκινά από την αρχή, δηλαδή η επόμενη
    // αυτόματη αλλαγή έρχεται πέντε δευτερόλεπτα μετά το τελευταίο σύρσιμο.
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(pager, heroes.size, dragged) {
        if (dragged || heroes.size <= 1) return@LaunchedEffect
        while (true) {
            delay(HERO_AUTO_ADVANCE_MS)
            pager.animateScrollToPage((pager.currentPage + 1) % heroes.size)
        }
    }
    // HORIZON: το hero είναι πλέον ΚΑΡΤΑ 228dp μέσα στα περιθώρια, όχι εικόνα
    // ολόκληρης οθόνης. Έτσι το «Συνέχισε να βλέπεις» και το Live φαίνονται
    // στην πρώτη οθόνη χωρίς scroll (prototypes/redesign_horizon.html).
    Column(Modifier.fillMaxWidth()) {
        if (showChrome) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "PRELUDE+",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = AppFonts.Display,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.5.sp
                    )
                    HorizonLine(Modifier.width(96.dp).padding(top = 4.dp))
                }
                Spacer(Modifier.weight(1f))
                MobileHeroRoundButton(Icons.Default.Search, stringResource(R.string.home_search), onSearch)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF5B2A3A), Color(0xFF2A1D3D)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profileName.trim().firstOrNull()?.uppercase() ?: "K",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                MobileHeroTab(stringResource(R.string.home_section_series), selectedDestination == "series", onOpenSeries)
                MobileHeroTab(stringResource(R.string.home_section_movies), selectedDestination == "movies" || selectedDestination == "home", onOpenMovies)
                MobileHeroTab(stringResource(R.string.home_categories_dropdown), false, onOpenCategories)
            }
        }
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 18.dp),
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth().height(228.dp)
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
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
        ) {
            heroes.indices.forEach { index ->
                Box(
                    Modifier
                        .width(if (index == pager.currentPage) 18.dp else 5.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (index == pager.currentPage) IptvColors.TextPrimary else IptvColors.SurfaceSelected)
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
    // Το `channel.year` κρατάει ό,τι έστειλε ο πάροχος επειδή συμμετέχει σε
    // κλειδιά ταυτότητας· εδώ, στην οθόνη, διαβάζεται ως έτος. Χωρίς αυτό ένα
    // `1993-08-28` εμφανιζόταν αυτούσιο στη θέση του έτους.
    val year = ProviderMetadataPolicy.displayYear(
        meta?.year?.takeIf(String::isNotBlank) ?: channel.year
    )
    val genre = meta?.genres?.takeIf(String::isNotBlank) ?: channel.genre
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(IptvColors.Surface)
            .clickable(onClick = onDetails)
    ) {
        HomeCinematicBackdrop(channel, meta, mobile = true, modifier = Modifier.fillMaxSize())
        // Σκούρο κάτω μέρος ώστε τίτλος και κουμπιά να διαβάζονται σε κάθε εικόνα.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to Color.Transparent,
                    1f to IptvColors.Background.copy(alpha = .94f)
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.home_top_choice),
                color = IptvColors.TextPrimary,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
            Text(
                title,
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 30.sp,
                fontFamily = AppFonts.Display,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val genreTag = genre.split(",", "/", "·", "|", "&").map { it.trim() }.firstOrNull { it.isNotBlank() }
            val metaParts = buildList {
                if (year.isNotBlank()) add(year)
                if (genreTag != null) add(genreTag)
                meta?.rating?.takeIf(String::isNotBlank)?.let { add("★ $it") }
            }
            if (metaParts.isNotEmpty()) {
                Text(
                    metaParts.joinToString("  ·  "),
                    color = IptvColors.TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IptvColors.Primary)
                        .clickable(onClick = onPlay)
                        .padding(start = 14.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_play), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Row(
                    Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = .10f))
                        .clickable(onClick = onToggleFavorite)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (favorite) Icons.Default.Check else Icons.Default.Add,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_section_my_list), color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
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
