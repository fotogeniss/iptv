package com.prelude.iptv.ui.tv.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.components.details.DetailCinematicBackdrop
import com.prelude.iptv.ui.components.details.DetailPresentation
import com.prelude.iptv.ui.components.details.DetailSection
import com.prelude.iptv.ui.components.details.PremiumCastCard
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.tvFocus
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvPremiumDetailScreen(
    presentation: DetailPresentation,
    onBack: () -> Unit,
    onFav: () -> Unit,
    onShare: () -> Unit,
    onPlayMovie: () -> Unit,
    onRestartMovie: () -> Unit,
    onPlayEpisode: (Channel) -> Unit,
    onOpenRelated: (Channel) -> Unit,
    /** Σβήνει την αποθηκευμένη πρόοδο χωρίς να ξεκινήσει αναπαραγωγή. */
    onClearProgress: () -> Unit = {},
    /**
     * true όσο παίζει κάτι από πάνω και αυτή η οθόνη είναι κρυμμένη.
     *
     * Χρειάζεται γιατί ο player ΔΕΝ είναι πια ξεχωριστό Activity: η επιστροφή δεν
     * φέρνει ON_RESUME, οπότε η οθόνη δεν έχει άλλον τρόπο να μάθει ότι ξαναήρθε
     * μπροστά. Δες παρακάτω τι σπάει χωρίς αυτό.
     */
    obscuredByPlayer: Boolean = false
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
    var focusedEpisodeIndex by remember(seasonIndex, episodes) { mutableIntStateOf(0) }
    val playFocus = remember(presentation.title) { FocusRequester() }
    /**
     * Αυξάνεται όταν επιστρέφουμε από τον player. Μπαίνει στο κλειδί του
     * [rememberInitialFocus] ώστε να ξαναζητηθεί το αρχικό focus — δες παρακάτω.
     */
    var focusEpoch by remember(presentation.title) { mutableIntStateOf(0) }
    // Το αρχικό focus πάει στο «Πίσω» (πάνω-αριστερά) και ΟΧΙ στην Αναπαραγωγή:
    // το κουμπί αναπαραγωγής βρίσκεται χαμηλά στο hero, οπότε το focus εκεί
    // προκαλούσε bring-into-view scroll και το hero άνοιγε κομμένο.
    val backFocus = rememberInitialFocus(key = presentation.title to focusEpoch)
    val listState = rememberLazyListState()

    // ---------------------------------------------------------------------
    // ΕΠΙΣΤΡΟΦΗ ΑΠΟ ΤΟΝ PLAYER
    // ---------------------------------------------------------------------
    // Γυρίζοντας από την αναπαραγωγή, αυτή η οθόνη ΔΕΝ ξαναφτιάχνεται: το
    // rememberInitialFocus έχει ήδη τρέξει και δεν ξανατρέχει, ενώ το focus έχει
    // χαθεί μαζί με τον player που το κρατούσε. Αποτέλεσμα: νεκρό τηλεχειριστήριο.
    //
    // ΓΙΑΤΙ ΕΠΑΝΕΜΦΑΝΙΣΤΗΚΕ ΤΟ ΠΡΟΒΛΗΜΑ: η επαναφορά υπήρχε εδώ και δούλευε —
    // αλλά ήταν δεμένη στο ON_RESUME του Activity, από την εποχή που ο player
    // ΗΤΑΝ ξεχωριστό Activity. Μόλις τον κάναμε επίπεδο μέσα στην ίδια οθόνη, το
    // ON_RESUME έπαψε να έρχεται. Ο κώδικας έμεινε, φαινόταν σωστός, και δεν
    // εκτελούνταν ποτέ.
    //
    // Δεν το έλυσα από έξω, στο BrowseRoute: εκεί θα ήταν δεύτερο σημείο που
    // διεκδικεί το focus, δίπλα στο backFocus αυτής της οθόνης — ακριβώς το
    // μοτίβο που μας κόστισε μέρες στον player. Η οθόνη ξέρει πού πρέπει να πάει
    // το focus· της λείπει μόνο η πληροφορία ΠΟΤΕ.
    // ΓΙΑΤΙ ΔΕΝ ΑΡΚΕΙ ΕΝΑ requestFocus():
    //
    // Όσο παίζει ο player, αυτή η οθόνη είναι απενεργοποιημένη για focus. Όταν
    // κλείνει, δύο πράγματα πρέπει να γίνουν: να φύγει η απενεργοποίηση (μέσω
    // recomposition) και να ζητηθεί το focus. Δεν υπάρχει καμία εγγύηση για τη
    // σειρά τους.
    //
    // Και το χειρότερο: το requestFocus() ΔΕΝ αποτυγχάνει ορατά όταν ο κόμβος
    // είναι απενεργοποιημένος — δεν πετά σφάλμα, απλώς δεν κάνει τίποτα. Έτσι
    // κάθε «έξυπνη» επανάληψη που ελέγχει για εξαίρεση νομίζει ότι πέτυχε και
    // σταματά. Αυτό ακριβώς έκανε τις προηγούμενες διορθώσεις να φαίνονται
    // σωστές και να μη δουλεύουν.
    //
    // Εδώ δεν υποθέτουμε: ελέγχουμε αν το focus ΗΡΘΕ, και επιμένουμε μέχρι να
    // έρθει ή μέχρι να περάσει ένα δευτερόλεπτο.
    // Η ΛΥΣΗ: ΞΑΝΑΠΑΙΖΟΥΜΕ ΤΟ ΑΡΧΙΚΟ FOCUS, ΔΕΝ ΦΤΙΑΧΝΟΥΜΕ ΔΕΥΤΕΡΟ ΜΗΧΑΝΙΣΜΟ.
    //
    // Στο πρώτο άνοιγμα της οθόνης το focus προσγειώνεται σωστά — άρα ο
    // μηχανισμός δουλεύει. Οι προηγούμενες προσπάθειές μου έφτιαχναν παράλληλο
    // μονοπάτι δίπλα του, με δικά του αιτήματα και δικές του αποτυχίες.
    //
    // Το rememberInitialFocus ζητά focus κάθε φορά που αλλάζει το κλειδί του.
    // Βάζοντας έναν μετρητή στο κλειδί, η επιστροφή από τον player εκτελεί
    // ΑΚΡΙΒΩΣ ό,τι εκτελείται στο πρώτο άνοιγμα. Ένας μηχανισμός, μία διαδρομή.
    var wasObscured by remember(presentation.title) { mutableStateOf(false) }
    LaunchedEffect(obscuredByPlayer) {
        if (obscuredByPlayer) {
            wasObscured = true
            return@LaunchedEffect
        }
        if (!wasObscured) return@LaunchedEffect
        wasObscured = false
        // Πρώτα στην κορυφή. ΚΡΙΣΙΜΟ: το κουμπί «Πίσω» ζει μέσα σε item του
        // LazyColumn, και το Compose αποσυνθέτει όσα items βγουν από την οθόνη.
        // Αν είχες κατέβει στα επεισόδια πριν πατήσεις αναπαραγωγή, ο κόμβος δεν
        // υπάρχει καν όταν επιστρέφεις — και το αίτημα πάει στο πουθενά.
        listState.scrollToItem(0, 0)
        focusEpoch++
    }
    // Έναρξη/Συνέχεια σειράς με ΕΝΑ πάτημα.
    //
    // Πριν: το κουμπί δεν έκανε τίποτα μέχρι να κατέβει η λίστα επεισοδίων, γι'
    // αυτό έπρεπε να μπεις στα Επεισόδια και να διαλέξεις χειροκίνητα το πρώτο.
    // Τώρα: αν τα επεισόδια δεν έχουν φτάσει ακόμη, κρατάμε την πρόθεση και
    // ξεκινάμε ΜΟΛΙΣ φτάσουν.
    //
    // Ποιο επεισόδιο: το primaryEpisode είναι «αυτό που είχες αφήσει» (resume) και,
    // αν δεν υπάρχει ιστορικό, το πρώτο της σειράς. Ο player συνεχίζει μόνος του
    // από το λεπτό που είχες μείνει (αποθηκευμένη θέση ανά επεισόδιο).
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
        val foundSeason = presentation.seasons.indexOfFirst { (_, list) -> target in list }
        if (foundSeason >= 0) {
            seasonIndex = foundSeason
            focusedEpisodeIndex = presentation.seasons[foundSeason].second.indexOf(target).coerceAtLeast(0)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(IptvColors.Background)
    ) {
        DetailCinematicBackdrop(
            backdropUrl = presentation.backdropUrl,
            posterUrl = presentation.posterUrl,
            contentDescription = presentation.title,
            mobile = false,
            backdropPending = presentation.backdropPending
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "tv-detail-hero") {
                TvDetailHero(
                    presentation = presentation,
                    playFocus = playFocus,
                    backFocus = backFocus,
                    onBack = onBack,
                    onFav = onFav,
                    onShare = onShare,
                    onPlay = primaryPlay,
                    onRestart = onRestartMovie,
                    onClearProgress = onClearProgress
                )
            }
            // ΟΧΙ stickyHeader: στο Android TV σπάει το DPAD focus traversal
            // (το focus κολλάει/πηδάει). Κανονικό item κρατά καθαρή πλοήγηση.
            item(key = "tv-detail-tabs") {
                TvDetailTabs(
                    tabs = tabs,
                    active = activeSection,
                    seasons = presentation.seasons,
                    seasonIndex = seasonIndex,
                    onTab = { activeSection = it },
                    onSeason = { seasonIndex = it; focusedEpisodeIndex = 0 }
                )
            }
            when (activeSection) {
                DetailSection.Episodes -> item(key = "tv-detail-episodes") {
                    TvEpisodeSection(
                        episodes = episodes,
                        progress = presentation.episodeProgress,
                        focusedIndex = focusedEpisodeIndex,
                        onFocused = { focusedEpisodeIndex = it },
                        onPlay = onPlayEpisode,
                        loading = presentation.loading,
                        seriesTitle = presentation.title,
                        seriesYear = presentation.year,
                        // Ο αριθμός σεζόν από την ετικέτα («Season 2» -> 2).
                        season = presentation.seasons.getOrNull(seasonIndex)?.first
                            ?.filter { it.isDigit() }?.toIntOrNull() ?: (seasonIndex + 1)
                    )
                }
                DetailSection.About -> item(key = "tv-detail-about") { TvAboutSection(presentation) }
                DetailSection.Cast -> item(key = "tv-detail-cast") { TvCastSection(presentation) }
                DetailSection.Similar -> item(key = "tv-detail-similar") { TvSimilarSection(presentation, onOpenRelated) }
            }
        }
    }
}

@Composable
private fun TvDetailTabs(
    tabs: List<DetailSection>,
    active: DetailSection,
    seasons: List<Pair<String, List<Channel>>>,
    seasonIndex: Int,
    onTab: (DetailSection) -> Unit,
    onSeason: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            // ΑΔΙΑΦΑΝΟ φόντο (ήταν 0.98): η ελάχιστη διαφάνεια άφηνε το backdrop
            // να διαπερνά ελαφρώς, δημιουργώντας μια αχνή λωρίδα ανάμεσα στις
            // καρτέλες και το περιεχάμενο από κάτω — το «ανεπαίσθητο κενό».
            .background(IptvColors.Background)
            .padding(horizontal = 44.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tabs.forEach { tab ->
            TvDetailTabButton(
                label = tab.tvLabel(),
                selected = tab == active,
                onClick = { onTab(tab) }
            )
        }
        Spacer(Modifier.weight(1f))
        if (active == DetailSection.Episodes && seasons.isNotEmpty()) {
            Box {
                Row(
                    Modifier
                        .height(42.dp)
                        .tvFocus(RoundedCornerShape(9.dp))
                        .clip(RoundedCornerShape(9.dp))
                        .background(IptvColors.SurfaceRaised)
                        .clickable(enabled = seasons.size > 1) { expanded = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        seasons.getOrNull(seasonIndex)?.first.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Default.ExpandMore, null, tint = IptvColors.TextSecondary)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = IptvColors.SurfaceRaised
                ) {
                    seasons.forEachIndexed { index, pair ->
                        DropdownMenuItem(
                            text = { Text("${pair.first} · ${pair.second.size} επεισόδια") },
                            modifier = Modifier.tvFocus(RoundedCornerShape(6.dp)),
                            onClick = { onSeason(index); expanded = false }
                        )
                    }
                }
            }
        }
    }
    // Χωρίς διαχωριστική γραμμή: μαζί με τη διαφάνεια δημιουργούσε ορατή ραφή.
}

@Composable
private fun TvDetailTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(9.dp)
    Column(
        modifier = Modifier
            .height(46.dp)
            .widthIn(min = 104.dp)
            .clip(shape)
            .background(
                when {
                    focused -> IptvColors.SurfaceSelected
                    selected -> IptvColors.SurfaceRaised.copy(alpha = 0.72f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = when {
                    focused -> Color.White
                    selected -> IptvColors.DividerStrong
                    else -> Color.Transparent
                },
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 15.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = if (selected || focused) Color.White else IptvColors.TextTertiary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .width(28.dp)
                .height(3.dp)
                .background(
                    if (selected) IptvColors.Primary else Color.Transparent,
                    RoundedCornerShape(99.dp)
                )
        )
    }
}

@Composable
private fun TvEpisodeSection(
    episodes: List<Channel>,
    progress: Map<String, com.prelude.iptv.ui.WatchProgress>,
    focusedIndex: Int,
    onFocused: (Int) -> Unit,
    onPlay: (Channel) -> Unit,
    loading: Boolean,
    seriesTitle: String = "",
    seriesYear: String = "",
    season: Int = 1
) {
    if (loading && episodes.isEmpty()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(IptvColors.Background)
                .padding(horizontal = 44.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(4) {
                Box(
                    Modifier
                        .width(245.dp)
                        .height(138.dp)
                        .background(IptvColors.Surface, RoundedCornerShape(11.dp))
                )
            }
        }
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(184.dp)
            .background(IptvColors.Background)
            // Μικρότερο αριστερό περιθώριο (34 αντί 44): μαζί με το contentPadding
            // της σειράς, η εστιασμένη κάρτα έχει χώρο να μεγαλώσει χωρίς να κόβεται.
            .padding(start = 34.dp, end = 44.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            // Οριζόντιο περιθώριο: η εστιασμένη κάρτα μεγαλώνει (~7dp κάθε πλευρά)
            // και χωρίς αυτό η ΠΡΩΤΗ κάρτα κοβόταν στο αριστερό όριο της σειράς.
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                episodes,
                key = { index, ep -> "tv-detail-episode:$index:${PlaybackQueue.favKey(ep)}" }
            ) { index, episode ->
                TvEpisodeCard(
                    episode = episode,
                    number = index + 1,
                    progress = progress[PlaybackQueue.favKey(episode)],
                    onFocused = { onFocused(index) },
                    onClick = { onPlay(episode) },
                    seriesTitle = seriesTitle,
                    seriesYear = seriesYear,
                    season = season
                )
            }
        }
        val focused = episodes.getOrNull(focusedIndex)
        TvEpisodeInfoPanel(
            episode = focused,
            number = focusedIndex + 1,
            nextTitle = episodes.getOrNull(focusedIndex + 1)?.name.orEmpty(),
            seriesTitle = seriesTitle,
            seriesYear = seriesYear,
            season = season
        )
    }
}

@Composable
private fun TvAboutSection(presentation: DetailPresentation) {
    var focused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(IptvColors.Background)
            .padding(horizontal = 44.dp, vertical = 20.dp)
    ) {
        Text(
            if (presentation.isSeries) "Σχετικά με τη σειρά" else "Σχετικά με την ταινία",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(IptvColors.SurfaceRaised.copy(alpha = 0.72f))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) Color.White.copy(alpha = 0.88f) else IptvColors.Divider,
                    shape = cardShape
                )
                .onFocusChanged { focused = it.isFocused || it.hasFocus }
                .focusable()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    presentation.plot.ifBlank { "Δεν υπάρχει διαθέσιμη περιγραφή." },
                    color = Color.White.copy(alpha = 0.90f),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (presentation.notice.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        presentation.notice,
                        color = IptvColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Column(Modifier.width(320.dp)) {
                TvDetailMetadata("ΔΗΜΙΟΥΡΓΟΣ", presentation.director)
                TvDetailMetadata(
                    "ΠΡΩΤΑΓΩΝΙΣΤΟΥΝ",
                    presentation.cast.take(6).joinToString { it.name }
                )
                TvDetailMetadata("ΕΙΔΗ", presentation.genre)
            }
        }
    }
}

@Composable
private fun TvDetailMetadata(label: String, value: String) {
    if (value.isBlank()) return
    Text(
        label,
        color = IptvColors.Primary,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Black
    )
    Spacer(Modifier.height(4.dp))
    Text(
        value,
        color = IptvColors.TextSecondary,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun TvCastSection(presentation: DetailPresentation) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(IptvColors.Background)
            .padding(top = 20.dp, bottom = 18.dp)
    ) {
        Text(
            "Ηθοποιοί",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 44.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 44.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(presentation.cast, key = { it.name + it.role }) { member ->
                PremiumCastCard(
                    member,
                    112.dp,
                    Modifier.tvFocus(RoundedCornerShape(12.dp), tint = false).focusable()
                )
            }
        }
    }
}

@Composable
private fun TvSimilarSection(
    presentation: DetailPresentation,
    onOpenRelated: (Channel) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(IptvColors.Background)
            .padding(top = 20.dp, bottom = 18.dp)
    ) {
        Text(
            "Παρόμοια για εσένα",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 44.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 44.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(presentation.relatedItems, key = { PlaybackQueue.favKey(it) }) { item ->
                TvRelatedCard(item) { onOpenRelated(item) }
            }
        }
    }
}

private fun DetailSection.tvLabel(): String = when (this) {
    DetailSection.Episodes -> "ΕΠΕΙΣΟΔΙΑ"
    DetailSection.About -> "ΣΧΕΤΙΚΑ"
    DetailSection.Cast -> "ΗΘΟΠΟΙΟΙ"
    DetailSection.Similar -> "ΠΑΡΟΜΟΙΑ"
}
