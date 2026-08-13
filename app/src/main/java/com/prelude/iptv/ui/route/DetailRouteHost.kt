package com.prelude.iptv.ui.route

import android.content.*
import android.os.*
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.contract.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.*
import coil.compose.*
import com.prelude.iptv.*
import com.prelude.iptv.R
import com.prelude.iptv.data.*
import com.prelude.iptv.ui.*
import com.prelude.iptv.ui.components.library.*
import com.prelude.iptv.ui.design.*
import kotlinx.coroutines.*

@Composable
internal fun DetailHost(
    ch: Channel,
    state: CatalogUiState,
    vm: MainViewModel,
    onBack: () -> Unit,
    onPlay: (Channel, List<Channel>?, SubtitleSearchRequest?, Map<String, SubtitleSearchRequest>) -> Unit,
    onOpenRelated: (Channel) -> Unit,
    mobileBottomPadding: Dp = 42.dp,
    /**
     * true όσο παίζει κάτι από πάνω. Η οθόνη μένει συντεθειμένη ώστε το BACK να
     * επιστρέφει εκεί με τη θέση της ανέπαφη — αλλά πρέπει να ξέρει πότε ξαναήρθε
     * μπροστά, για να ξαναπάρει το focus.
     */
    obscuredByPlayer: Boolean = false
) {
    var info by remember(ch) { mutableStateOf<Map<String, String>?>(null) }
    var tmdb by remember(ch) { mutableStateOf<com.prelude.iptv.data.TmdbClient.Meta?>(null) }
    LaunchedEffect(ch) {
        info = try {
            vm.vodInfo(ch)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyMap()
        }
    }
    // ΞΕΧΩΡΙΣΤΗ ΣΗΜΑΙΑ ΚΑΙ ΟΧΙ «tmdb == null».
    //
    // Το null σημαίνει δύο πράγματα: «δεν έχει έρθει» και «δεν βρέθηκε». Το φόντο
    // πρέπει να τα ξεχωρίζει — στο πρώτο περιμένει, στο δεύτερο δείχνει την
    // αφίσα. Χωρίς αυτό, εμφανιζόταν πρώτα η αφίσα και μετά το backdrop.
    var tmdbSettled by remember(ch) { mutableStateOf(false) }
    LaunchedEffect(ch) {
        tmdb = vm.tmdb(ch)
        tmdbSettled = true
    }

    val i = info ?: emptyMap()
    val isSeries = ch.kind == "series"
    val context = androidx.compose.ui.platform.LocalContext.current
    val allEpisodes = remember(state.seriesSeasons) { state.seriesSeasons.flatMap { it.second } }
    val episodeProgress = remember(allEpisodes, state.recentsVersion) { vm.watchProgress(allEpisodes) }
    val movieProgress = remember(ch, state.recentsVersion) { if (isSeries) null else vm.watchProgress(ch) }
    val resumeEpisode = remember(allEpisodes, state.recentsVersion) {
        if (!isSeries || allEpisodes.isEmpty()) null
        else {
            val episodeKeys = allEpisodes.associateBy { vm.favKey(it) }
            vm.recents().firstNotNullOfOrNull { recent ->
                val match = episodeKeys[vm.favKey(recent)] ?: return@firstNotNullOfOrNull null
                if (vm.watchProgress(match) != null) match else null
            }
        }
    }

    // ηθοποιοί: προτίμησε TMDB (έχουν φωτογραφίες), αλλιώς σκέτα ονόματα από τον πάροχο
    val castStr = (i["cast"]?.ifBlank { ch.cast } ?: ch.cast)
    val cast = tmdb?.cast?.takeIf { it.isNotEmpty() }?.map { CastMember(it.name, it.role, it.photo) }
        ?: castStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { CastMember(it) }
    val relatedItems = remember(ch, state.channels) {
        state.channels.asSequence()
            .filter { candidate -> vm.favKey(candidate) != vm.favKey(ch) && candidate.kind == ch.kind }
            .distinctBy(vm::favKey)
            .sortedByDescending { candidate -> candidate.group.isNotBlank() && candidate.group == ch.group }
            .take(14)
            .toList()
    }
    val resolvedYear = (tmdb?.year?.ifBlank { null }) ?: (i["year"]?.ifBlank { ch.year } ?: ch.year)
    val resolvedSeriesTitle = SubtitleSearchPolicy.cleanTitle(ch.name)
    val movieSubtitleRequest = remember(ch, resolvedYear) {
        SubtitleSearchPolicy.movie(ch.name, resolvedYear)
    }
    val episodeSubtitleRequests = remember(ch, state.seriesSeasons, resolvedYear) {
        buildMap {
            state.seriesSeasons.forEachIndexed { seasonIndex, (label, episodes) ->
                val seasonNumber = SubtitleSearchPolicy.seasonNumber(label, seasonIndex + 1) ?: seasonIndex + 1
                episodes.forEachIndexed { episodeIndex, episode ->
                    val episodeNumber = SubtitleSearchPolicy.episodeNumber(episode.name, episodeIndex + 1)
                    put(
                        PlaybackQueue.favKey(episode),
                        SubtitleSearchPolicy.episode(
                            seriesTitle = resolvedSeriesTitle,
                            yearHint = resolvedYear,
                            season = seasonNumber,
                            episode = episodeNumber
                        )
                    )
                }
            }
        }
    }

    DetailScreen(
        title = com.prelude.iptv.data.TmdbClient.cleanTitle(ch.name).ifBlank { ch.name },
        tmdbId = ch.tmdbId,
        year = resolvedYear,
        rating = tmdb?.rating ?: "",
        duration = (i["duration"]?.ifBlank { ch.duration } ?: ch.duration),
        quality = com.prelude.iptv.data.ContentQualityPolicy.label(ch.name, ch.group),
        genre = (i["genre"]?.ifBlank { ch.genre } ?: ch.genre),
        director = (i["director"]?.ifBlank { ch.director } ?: ch.director),
        plot = (tmdb?.overview?.ifBlank { null }) ?: (i["plot"]?.ifBlank { ch.plot } ?: ch.plot),
        posterUrl = ch.logo.ifBlank { tmdb?.poster ?: "" },
        backdropUrl = tmdb?.backdrop ?: "",
        backdropPending = !tmdbSettled,
        cast = cast,
        showTmdbNotice = !com.prelude.iptv.data.TmdbClient.hasKey(),
        contentIsSeries = isSeries,
        seasons = if (isSeries) state.seriesSeasons else emptyList(),
        relatedItems = relatedItems,
        loading = if (isSeries) state.seriesLoading else (info == null),
        isFav = vm.favKey(ch) in state.favorites,
        movieProgress = movieProgress,
        episodeProgress = episodeProgress,
        resumeEpisode = resumeEpisode,
        mobileBottomPadding = mobileBottomPadding,
        obscuredByPlayer = obscuredByPlayer,
        onBack = onBack,
        onFav = { vm.toggleFavorite(ch) },
        onShare = {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, ch.name)
                putExtra(Intent.EXTRA_TEXT, ch.name)
            }
            context.startActivity(Intent.createChooser(share, context.getString(R.string.details_share)))
        },
        onPlayMovie = { onPlay(ch, null, movieSubtitleRequest, emptyMap()) },
        onRestartMovie = {
            vm.clearWatchProgress(ch)
            onPlay(ch, null, movieSubtitleRequest, emptyMap())
        },
        onPlayEpisode = { ep ->
            onPlay(
                ep,
                allEpisodes,
                episodeSubtitleRequests[PlaybackQueue.favKey(ep)],
                episodeSubtitleRequests
            )
        },
        onOpenRelated = onOpenRelated,
        // ΕΠΑΝΑΦΟΡΑ ΠΡΟΟΔΟΥ.
        //
        // Για ταινία σβήνει τη δική της θέση. Για σειρά σβήνει ΟΛΩΝ των
        // επεισοδίων: το «πού είχα μείνει» σε μια σειρά δεν είναι ένα σημείο αλλά
        // πολλά, και μια μισή επαναφορά θα άφηνε τη σειρά να συνεχίζει από κάποιο
        // επεισόδιο που ο χρήστης νόμιζε ότι μηδένισε.
        onClearProgress = {
            if (isSeries) allEpisodes.forEach(vm::clearWatchProgress)
            else vm.clearWatchProgress(ch)
        }
    )
}

/* ----------------------------------------------------------------- utils -- */

internal fun fmtTime(s: String): String {
    val m = Regex("""(\d{2}:\d{2})""").find(s)
    return m?.groupValues?.get(1) ?: s.take(5)
}

/* ------------------------------------------------ category picker -------- */

@Composable
internal fun CategoryPicker(
    sections: Map<String, CategoryPickerSection>,
    initialType: String,
    onCancel: () -> Unit,
    onSelectionChange: (String, List<String>?) -> Unit,
) {
    val tabOrder = listOf("series", "vod", "live")
    var activeType by remember(initialType) {
        mutableStateOf(initialType.takeIf(sections::containsKey) ?: sections.keys.first())
    }
    val section = sections.getValue(activeType)
    val selectableIds = section.categories.map { it.first }.filter { (section.counts[it] ?: 0) > 0 }
    val selectedIds = section.selectedIds ?: selectableIds.toSet()
    val selectedCount = selectableIds.count(selectedIds::contains)
    val visibleItems = selectableIds.sumOf { id -> if (id in selectedIds) section.counts[id] ?: 0 else 0 }
    val firstTab = rememberInitialFocus()

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .72f)),
        contentAlignment = if (isTvDevice()) Alignment.Center else Alignment.BottomCenter,
    ) {
        Box(
            Modifier.fillMaxSize()
                .focusProperties { canFocus = false }
                .clickable(onClick = onCancel)
        )
        Column(
            Modifier
                .fillMaxWidth(.96f)
                .widthIn(max = if (isTvDevice()) 520.dp else 360.dp)
                .heightIn(max = if (isTvDevice()) 620.dp else 590.dp)
                .navigationBarsPadding()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF101115))
                .border(1.dp, Color(0xFF232529), RoundedCornerShape(16.dp))
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
                .padding(top = 20.dp),
        ) {
            Text(
                stringResource(R.string.catalog_visibility_title),
                color = Color(0xFFE9EAEC),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            Text(
                stringResource(
                    when (activeType) {
                        "series" -> R.string.catalog_visibility_summary_series
                        "vod" -> R.string.catalog_visibility_summary_movies
                        else -> R.string.catalog_visibility_summary_live
                    },
                    selectedCount,
                    section.categories.size,
                    visibleItems,
                ),
                color = Color(0xFF7D818A),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabOrder.forEach { type ->
                    val available = sections.containsKey(type)
                    val active = type == activeType
                    Box(
                        Modifier
                            .then(if (type == initialType) Modifier.focusRequester(firstTab) else Modifier)
                            .focusProperties { canFocus = available }
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (active) Color(0xFFE9EAEC) else Color.Transparent)
                            .border(1.dp, if (active) Color(0xFFE9EAEC) else Color(0xFF34363C), RoundedCornerShape(99.dp))
                            .tvFocus(RoundedCornerShape(99.dp), tint = false)
                            .clickable(enabled = available) { activeType = type }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            stringResource(
                                when (type) {
                                    "series" -> R.string.catalog_series
                                    "vod" -> R.string.catalog_movies
                                    else -> R.string.catalog_live
                                }
                            ),
                            color = when {
                                !available -> Color(0xFF555861)
                                active -> Color.Black
                                else -> Color(0xFFC7C9CE)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            LazyColumn(Modifier.weight(1f)) {
                items(section.categories, key = { it.first }) { (id, title) ->
                    val itemCount = section.counts[id] ?: 0
                    val enabled = itemCount > 0
                    val checked = enabled && id in selectedIds
                    Row(
                        Modifier.fillMaxWidth()
                            .focusProperties { canFocus = enabled }
                            .tvFocus(RoundedCornerShape(6.dp))
                            .clickable(enabled = enabled) {
                                val next = selectedIds.toMutableSet().apply {
                                    if (!add(id)) remove(id)
                                }
                                onSelectionChange(activeType, if (next.size == selectableIds.size) null else next.toList())
                            }
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (checked) Color(0xFFE9EAEC) else Color.Transparent)
                                .border(1.dp, if (checked) Color(0xFFE9EAEC) else Color(0xFF555861), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (checked) Text("✓", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            title,
                            color = if (enabled) Color(0xFFE9EAEC) else Color(0xFF5E6169),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        )
                        Text(
                            itemCount.toString(),
                            color = if (enabled) Color(0xFF989CA5) else Color(0xFF555861),
                            fontSize = 12.sp,
                        )
                    }
                    HorizontalDivider(color = Color(0xFF16171B), thickness = 1.dp)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                listOf(
                    stringResource(R.string.catalog_select_all) to selectableIds,
                    stringResource(R.string.catalog_select_none) to emptyList(),
                ).forEach { (label, ids) ->
                    Text(
                        label,
                        color = Color(0xFF5B9DD9),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .tvFocus(RoundedCornerShape(6.dp))
                            .clickable {
                                onSelectionChange(activeType, ids.takeUnless { it.size == selectableIds.size })
                            }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/* --------------------------------------------------------- settings ------ */
