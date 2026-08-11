package com.prelude.iptv.ui.mobile.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.SubtitleSearchPolicy
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.details.DetailSection
import com.prelude.iptv.ui.components.details.DetailPresentation
import com.prelude.iptv.ui.components.details.PremiumCastCard
import com.prelude.iptv.ui.components.details.PremiumRelatedCard
import com.prelude.iptv.ui.localization.labelRes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobilePremiumDetailScreen(
    presentation: DetailPresentation,
    onBack: () -> Unit,
    onFav: () -> Unit,
    onShare: () -> Unit,
    onPlayMovie: () -> Unit,
    onRestartMovie: () -> Unit,
    onPlayEpisode: (Channel) -> Unit,
    onOpenRelated: (Channel) -> Unit,
    bottomContentPadding: Dp = 42.dp
) {
    var seasonIndex by remember(presentation.seasons) { mutableIntStateOf(0) }
    val tabs = remember(presentation.isSeries, presentation.cast, presentation.relatedItems) {
        buildList {
            if (presentation.isSeries) add(DetailSection.Episodes)
            add(DetailSection.About)
            if (presentation.cast.isNotEmpty()) add(DetailSection.Cast)
            if (presentation.relatedItems.isNotEmpty()) add(DetailSection.Similar)
        }
    }
    var activeSection by remember(tabs) { mutableStateOf(tabs.first()) }
    val episodes = presentation.seasons.getOrNull(seasonIndex)?.second.orEmpty()
    var episodesDescending by remember(presentation.title) { mutableStateOf(false) }
    val displayedEpisodes = remember(episodes, episodesDescending) {
        episodes.mapIndexed { sourceIndex, episode -> sourceIndex to episode }
            .let { indexed -> if (episodesDescending) indexed.asReversed() else indexed }
    }
    // Ίδια συμπεριφορά με την τηλεόραση: αν τα επεισόδια δεν έχουν φορτώσει
    // ακόμη, κρατάμε την πρόθεση και ξεκινάμε μόλις φτάσουν — αντί να μη κάνει
    // τίποτα το κουμπί και να πρέπει να διαλέξεις χειροκίνητα επεισόδιο.
    var pendingSeriesPlay by remember(presentation.title) { mutableStateOf(false) }
    val primaryPlay: () -> Unit = {
        if (presentation.isSeries) {
            val target = presentation.primaryEpisode
            if (target != null) onPlayEpisode(target) else pendingSeriesPlay = true
        } else {
            onPlayMovie()
        }
    }
    LaunchedEffect(presentation.primaryEpisode, pendingSeriesPlay) {
        if (pendingSeriesPlay) {
            presentation.primaryEpisode?.let {
                pendingSeriesPlay = false
                onPlayEpisode(it)
            }
        }
    }

    LaunchedEffect(presentation.resumeEpisode, presentation.seasons) {
        val target = presentation.resumeEpisode ?: return@LaunchedEffect
        val index = presentation.seasons.indexOfFirst { (_, list) -> target in list }
        if (index >= 0) seasonIndex = index
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(IptvColors.Background),
        contentPadding = PaddingValues(bottom = bottomContentPadding)
    ) {
        item(key = "mobile-detail-hero") {
            MobileDetailHero(
                presentation = presentation,
                onBack = onBack,
                onShare = onShare,
                onFav = onFav,
                onPlay = primaryPlay,
                onRestart = onRestartMovie
            )
        }
        stickyHeader(key = "mobile-detail-tabs") {
            MobileDetailTabs(tabs, activeSection) { activeSection = it }
        }

        when (activeSection) {
            DetailSection.Episodes -> {
                item(key = "mobile-season-header") {
                    MobileSeasonHeader(
                        seasons = presentation.seasons,
                        selected = seasonIndex,
                        descending = episodesDescending,
                        onSelected = { seasonIndex = it },
                        onToggleOrder = { episodesDescending = !episodesDescending }
                    )
                }
                if (presentation.loading && episodes.isEmpty()) {
                    item { MobileDetailSkeleton() }
                } else {
                    itemsIndexed(
                        displayedEpisodes,
                        key = { _, item ->
                            val (sourceIndex, episode) = item
                            "mobile-detail-episode:$seasonIndex:$sourceIndex:${PlaybackQueue.favKey(episode)}"
                        }
                    ) { _, item ->
                        val (sourceIndex, episode) = item
                        val seasonNumber = SubtitleSearchPolicy.seasonNumber(
                            presentation.seasons.getOrNull(seasonIndex)?.first.orEmpty(),
                            seasonIndex + 1,
                        ) ?: seasonIndex + 1
                        val episodeNumber = SubtitleSearchPolicy.episodeNumber(episode.name, sourceIndex + 1)
                            ?: sourceIndex + 1
                        MobileEpisodeCard(
                            episode = episode,
                            number = episodeNumber,
                            progress = presentation.episodeProgress[PlaybackQueue.favKey(episode)],
                            onClick = { onPlayEpisode(episode) },
                            seriesTitle = presentation.title,
                            seriesYear = presentation.year,
                            season = seasonNumber,
                            seriesTmdbId = presentation.tmdbId,
                        )
                    }
                }
            }
            DetailSection.About -> item(key = "mobile-detail-about") {
                MobileAboutSection(presentation)
            }
            DetailSection.Cast -> item(key = "mobile-detail-cast") {
                MobileCastSection(presentation)
            }
            DetailSection.Similar -> item(key = "mobile-detail-similar") {
                MobileSimilarSection(presentation, onOpenRelated)
            }
        }
    }
}

@Composable
private fun MobileDetailTabs(
    tabs: List<DetailSection>,
    active: DetailSection,
    onSelect: (DetailSection) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(IptvColors.Background.copy(alpha = 0.97f)),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        items(tabs, key = { it.name }) { tab ->
            Column(
                Modifier.clickable { onSelect(tab) }.padding(top = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(tab.labelRes()),
                    color = if (tab == active) Color.White else IptvColors.TextTertiary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.height(3.dp).fillMaxWidth()
                        .background(if (tab == active) IptvColors.Primary else Color.Transparent, RoundedCornerShape(99.dp))
                )
            }
        }
    }
}

@Composable
private fun MobileAboutSection(presentation: DetailPresentation) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp)) {
        Text(stringResource(R.string.details_about), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        val description = if (presentation.plot.isBlank()) {
            stringResource(R.string.details_no_description)
        } else presentation.plot
        Text(
            description,
            color = IptvColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        val creatorCredit = if (presentation.director.isNotBlank()) {
            stringResource(R.string.details_creator, presentation.director)
        } else null
        val castCredit = if (presentation.cast.isNotEmpty()) {
            stringResource(R.string.details_starring, presentation.cast.take(4).joinToString { it.name })
        } else null
        val genreCredit = if (presentation.genre.isNotBlank()) {
            stringResource(R.string.details_genres, presentation.genre)
        } else null
        val credits = listOfNotNull(creatorCredit, castCredit, genreCredit)
        if (credits.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            credits.forEach { line ->
                Text(line, color = IptvColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
            }
        }
        if (presentation.showTmdbNotice) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.details_tmdb_notice), color = IptvColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MobileCastSection(presentation: DetailPresentation) {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        Text(
            stringResource(R.string.details_cast),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(presentation.cast, key = { it.name + it.role }) { PremiumCastCard(it, 92.dp) }
        }
    }
}

@Composable
private fun MobileSimilarSection(
    presentation: DetailPresentation,
    onOpenRelated: (Channel) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        Text(
            stringResource(R.string.details_similar_for_you),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            items(presentation.relatedItems, key = { PlaybackQueue.favKey(it) }) { item ->
                PremiumRelatedCard(item, 122.dp, onClick = { onOpenRelated(item) })
            }
        }
    }
}
