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
import androidx.compose.ui.platform.*
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
    categories: List<Pair<String, String>>,
    initialSelectedIds: Set<String>? = null,
    onCancel: (() -> Unit)? = null,
    onLoad: (List<String>?) -> Unit
) {
    val selected = remember(categories, initialSelectedIds) {
        mutableStateMapOf<String, Boolean>().apply {
            categories.forEach { (id, _) -> put(id, initialSelectedIds == null || id in initialSelectedIds) }
        }
    }
    val first = rememberInitialFocus()
    val isTv = isTvDevice()
    // ΤΟ ΔΕΞΙ ΒΕΛΑΚΙ ΠΑΕΙ ΚΑΤΕΥΘΕΙΑΝ ΣΤΗ «ΦΟΡΤΩΣΗ»: αλλιώς, για να πατήσεις
    // ΟΚ αφού διάλεξες 3 κατηγορίες, έπρεπε να διασχίσεις με το D-pad ΟΛΗ
    // τη λίστα (και 200 γραμμές) μέχρι κάτω. Premium apps δεν το κάνουν αυτό.
    val okFocus = remember { FocusRequester() }
    var filter by remember { mutableStateOf("") }
    val visible = remember(categories, filter) {
        if (filter.isBlank()) categories
        else categories.filter { it.second.contains(filter.trim(), ignoreCase = true) }
    }
    Column(Modifier.fillMaxSize().background(Bg).navigationBarsPadding()) {
        // κεφαλίδα με «πίσω»: αν το μετανιώσεις, γυρνάς εκεί που ήσουν
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCancel != null) {
                TvIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", onClick = onCancel)
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Διάλεξε κατηγορίες", color = TextHi, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isTv) "Δεξί βελάκι → πάει κατευθείαν στη Φόρτωση"
                    else "Φορτώνει μόνο ό,τι επιλέξεις (όχι τα πάντα)",
                    color = TextLo, fontSize = 11.sp
                )
            }
        }

        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // «Όλες/Καμία» δρουν στα ΟΡΑΤΑ: αν έχεις φιλτράρει «sport», το
            // «Όλες» επιλέγει μόνο τις αθλητικές — αυτό περιμένει ο χρήστης.
            listOf("Όλες" to true, "Καμία" to false).forEachIndexed { bi, (label, v) ->
                Box(
                    Modifier.padding(end = 8.dp).clip(RoundedCornerShape(16.dp)).background(BgElev)
                        .border(1.dp, Line, RoundedCornerShape(16.dp))
                        .then(if (bi == 0) Modifier.focusRequester(first).testTag("category-select-all") else Modifier)
                        .focusProperties { right = okFocus }
                        .tvFocus(RoundedCornerShape(16.dp))
                        .clickable { visible.forEach { selected[it.first] = v } }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) { Text(label, color = TextMid) }
            }
            // φίλτρο: σε λίστες με 100+ κατηγορίες, το scroll δεν είναι λύση
            OutlinedTextField(
                value = filter, onValueChange = { filter = it },
                placeholder = { Text("Φίλτρο…", color = TextLo, fontSize = 13.sp) },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = TextHi, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent, unfocusedBorderColor = Line,
                    unfocusedContainerColor = BgElev, focusedContainerColor = BgElev,
                    cursorColor = Accent
                ),
                modifier = Modifier.weight(1f).height(52.dp)
            )
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(visible) { (id, title) ->
                Row(
                    Modifier.fillMaxWidth()
                        .focusProperties { right = okFocus }   // δεξί = Φόρτωση, ΟΧΙ scroll ως κάτω
                        .tvFocus(RoundedCornerShape(8.dp))
                        .clickable { selected[id] = !(selected[id] ?: false) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = selected[id] == true, onCheckedChange = { selected[id] = it },
                        colors = CheckboxDefaults.colors(checkedColor = Accent))
                    Text(title, color = TextHi, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (visible.isEmpty()) item {
                Text("Καμία κατηγορία δεν ταιριάζει στο «$filter».",
                    color = TextLo, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp))
            }
        }

        val count = selected.values.count { it }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onCancel != null) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Line),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHi),
                    modifier = Modifier.weight(1f).height(48.dp).tvFocus(RoundedCornerShape(14.dp))
                ) { Text("Ακύρωση") }
            }
            Button(
                onClick = {
                    val ids = categories.map { it.first }.filter { selected[it] == true }
                    onLoad(if (ids.size == categories.size) null else ids)
                },
                enabled = count > 0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = BgElev2),
                modifier = Modifier.weight(1.4f).height(48.dp)
                    .focusRequester(okFocus).testTag("category-load").tvFocus(RoundedCornerShape(14.dp), tint = false)
            ) { Text("Φόρτωση ($count)", fontWeight = FontWeight.Bold) }
        }
    }
}

/* --------------------------------------------------------- settings ------ */
