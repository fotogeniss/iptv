@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.prelude.iptv.ui.tv.browse

import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.prelude.iptv.R
import com.prelude.iptv.player.PlaybackEngine
import com.prelude.iptv.ui.player.PlayerExtraAction
import com.prelude.iptv.ui.player.PlayerHost
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.localization.localizedLiveGroupLabel
import com.prelude.iptv.ui.localization.localizedUppercase
import com.prelude.iptv.ui.requestFocusWithRetry
import com.prelude.iptv.ui.tvConfirm
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt

/** Μια εγγραφή προγράμματος EPG, με ό,τι χρειάζεται η οθόνη. */
data class LiveProgramme(
    val time: String,
    val title: String,
    val description: String,
    val isNow: Boolean,
)

/**
 * Ζωντανά κανάλια για τηλεόραση: μία στήλη αριστερά (κατηγορίες → κανάλια) και
 * προεπισκόπηση δεξιά που ΜΕΓΑΛΩΝΕΙ σε πλήρη οθόνη.
 *
 * Κρίσιμη σχεδιαστική επιλογή: υπάρχει ΕΝΑΣ player. Το «πλήρης οθόνη» δεν
 * ξεκινά δεύτερη αναπαραγωγή — απλώς μεγαλώνει το ίδιο surface, οπότε η εικόνα
 * δεν κόβεται ούτε μαυρίζει ποτέ. (Δύο ξεχωριστοί players, όσο καλά κι αν
 * συγχρονιστούν, δίνουν πάντα το «σταμάτησε ο ένας, ξεκίνησε ο άλλος».)
 *
 * Ροή: OK σε κατηγορία → κανάλια. OK σε κανάλι → προεπισκόπηση. Ξανά OK →
 * μεγαλώνει σε πλήρη οθόνη. BACK → μικραίνει πίσω στη θέση του.
 */
@Composable
fun TvLiveBrowseScreen(
    groups: List<String>,
    selectedGroup: String,
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    keyOf: (Channel) -> String,
    /**
     * Ολόκληρο το πρόγραμμα του καναλιού. ΜΟΝΑΔΙΚΗ πηγή αλήθειας: και η λίστα
     * καναλιών και το πρόγραμμα κάτω από τον player διαβάζουν από εδώ.
     *
     * Πριν, η λίστα ρωτούσε ξεχωριστά «τι παίζει τώρα» και το πρόγραμμα έβγαινε
     * από άλλη διαδρομή — δύο πηγές που μπορούσαν να δείξουν διαφορετικό EPG για
     * το ίδιο κανάλι.
     */
    programmesOf: (Channel) -> List<LiveProgramme>,
    resolveUrl: suspend (Channel) -> String,
    onSelectGroup: (String) -> Unit,
    /** Διπλή προβολή: δύο κανάλια ταυτόχρονα. */
    onMultiview: (Channel, Channel) -> Unit = { _, _ -> },
    /**
     * Ενημερώνει τη ροή όταν ο player πιάνει όλη την οθόνη, ώστε να κρυφτεί το
     * αριστερό μενού. Χωρίς αυτό η μπάρα ζωγραφίζεται ΠΑΝΩ από την εικόνα (είναι
     * σύμπλεκτο στοιχείο της ροής, δεν γνωρίζει τίποτα για τον player).
     */
    onFullscreenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Πρόγραμμα που ζητήθηκε αναλυτικά (πλήρης περιγραφή).
    var programmeDetails by remember { mutableStateOf<LiveProgramme?>(null) }
    // Τι παίζει τώρα, από την ΙΔΙΑ πηγή με το αναλυτικό πρόγραμμα.
    val nowTitleOf: (Channel) -> String = { ch -> TvLiveBrowsePolicy.nowTitle(programmesOf(ch)) }

    var showingChannels by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Channel?>(null) }
    // Πλήρης οθόνη: ΙΔΙΑ μηχανή και ίδια επιφάνεια, απλώς μεγαλώνει το δοχείο.
    var fullscreen by remember { mutableStateOf(false) }
    // Λίστα καναλιών ΜΕΣΑ στον player (δεξιά), αντί για τυφλή αλλαγή με τα βελάκια.
    var channelPanelOpen by remember { mutableStateOf(false) }

    /**
     * Αυξάνεται όταν κλείνει η λίστα καναλιών, ώστε ο player να ξαναπάρει το focus.
     *
     * Η λίστα είναι επίστρωση ΠΑΝΩ από τον player και του παίρνει το focus. Όταν
     * κλείνει, το focus χάνεται μαζί της: ο player δεν τη βλέπει και δεν έχει
     * λόγο να ξαναζητήσει. Το αποτέλεσμα ήταν νεκρό τηλεχειριστήριο μετά το BACK.
     */
    var playerFocusEpoch by remember { mutableIntStateOf(0) }
    // ΕΝΑ σημείο κλεισίματος, ώστε να μην ξεχαστεί ο μετρητής σε κάποιο από τα
    // δύο (BACK και επιλογή καναλιού).
    val closeChannelPanel: () -> Unit = {
        channelPanelOpen = false
        playerFocusEpoch++
    }
    // Πρώτο κανάλι της διπλής προβολής, αφού «οπλιστεί» με παρατεταμένο OK.
    var multiviewPrimary by remember { mutableStateOf<Channel?>(null) }

    val listState = rememberLazyListState()
    val firstItemFocus = remember(showingChannels, selectedGroup) { FocusRequester() }
    LaunchedEffect(showingChannels, selectedGroup) {
        listState.scrollToItem(0)
        firstItemFocus.requestFocusWithRetry()
    }

    // BACK: η σειρά προτεραιότητας ορίζεται στο TvLiveBrowsePolicy (και δοκιμάζεται
    // εκεί), ώστε να μην κλείνουν δύο πράγματα με ένα πάτημα.
    val level = if (showingChannels) TvLiveBrowsePolicy.Level.CHANNELS
    else TvLiveBrowsePolicy.Level.CATEGORIES
    val backAction = TvLiveBrowsePolicy.onBack(
        detailsOpen = programmeDetails != null,
        level = level
    )
    LaunchedEffect(fullscreen) { onFullscreenChange(fullscreen) }
    // Μικραίνοντας, το focus προσγειώνεται στο κανάλι που ΠΑΙΖΕΙ — όχι στην αρχή
    // της λίστας. Αλλιώς χάνεις το σημείο σου κάθε φορά που βγαίνεις.
    val playingRowFocus = remember { FocusRequester() }
    LaunchedEffect(fullscreen) {
        if (fullscreen || preview == null || !showingChannels) return@LaunchedEffect
        // ΠΡΩΤΑ SCROLL, ΜΕΤΑ FOCUS.
        //
        // Σε πλήρη οθόνη το περιεχόμενο ΔΕΝ συντίθεται καθόλου (το κάναμε ώστε το
        // τηλεχειριστήριο να μη «δουλεύει στο παρασκήνιο»). Επιστρέφοντας, η λίστα
        // ξαναχτίζεται από το μηδέν και η γραμμή του καναλιού που παίζει μπορεί να
        // μην υπάρχει ακόμη — ένα σκέτο αίτημα focus έβρισκε κενό, εξαντλούσε τις
        // προσπάθειές του, και το focus κατέληγε στο αριστερό μενού.
        val index = channels.indexOfFirst { keyOf(it) == preview?.let(keyOf) }
        if (index >= 0) runCatching { listState.scrollToItem(index) }
        playingRowFocus.requestFocusWithRetry()
    }
    // Φεύγοντας από την οθόνη ΕΝΩ είμαστε σε πλήρη οθόνη, κανείς δεν θα
    // μηδένιζε τη σημαία και το μενού θα έμενε κρυμμένο για πάντα.
    DisposableEffect(Unit) { onDispose { onFullscreenChange(false) } }

    // ΤΟ BACK ΣΕ ΠΛΗΡΗ ΟΘΟΝΗ ΤΟ ΧΕΙΡΙΖΕΤΑΙ Ο PLAYER, όχι εδώ.
    //
    // Εκεί υπάρχει διάκριση σύντομου/παρατεταμένου πατήματος (σύντομο =
    // εμφάνιση/απόκρυψη χειριστηρίων, παρατεταμένο = έξοδος). Ένας BackHandler
    // εδώ θα τα «έκλεβε» και θα σε πετούσε έξω με το πρώτο πάτημα.
    // ΠΡΟΣΟΧΗ: το οπλισμένο multiview ΔΕΝ ακυρώνεται με BACK.
    //
    // Το δεύτερο κανάλι βρίσκεται συνήθως σε ΑΛΛΗ κατηγορία, οπότε ο χρήστης
    // πρέπει να πάει πίσω και να αλλάξει ομάδα. Αν το BACK ακύρωνε την επιλογή,
    // η διπλή προβολή θα ήταν πρακτικά αδύνατη μεταξύ κατηγοριών.
    // Ακύρωση: παρατεταμένο OK ξανά στο ίδιο κανάλι.
    BackHandler(enabled = !fullscreen && backAction != TvLiveBrowsePolicy.BackAction.DELEGATE) {
        when (backAction) {
            TvLiveBrowsePolicy.BackAction.CLOSE_DETAILS -> programmeDetails = null
            TvLiveBrowsePolicy.BackAction.BACK_TO_CATEGORIES -> {
                // Η προεπισκόπηση ΔΕΝ μηδενίζεται: το κανάλι συνεχίζει να παίζει
                // (με εικόνα) όσο αλλάζεις ομάδα. Πριν, το preview = null έκρυβε
                // το βίντεο ενώ η μηχανή έπαιζε — άρα άκουγες μόνο τον ήχο.
                showingChannels = false
            }
            TvLiveBrowsePolicy.BackAction.DELEGATE -> Unit
        }
    }

    // ---- Η ΚΟΙΝΗ ΜΗΧΑΝΗ ΑΝΑΠΑΡΑΓΩΓΗΣ ----
    // Δεν φτιάχνουμε δικό μας ExoPlayer: χρησιμοποιούμε το PlaybackEngine, την
    // ίδια μηχανή που θα οδηγεί και τον πλήρη player. Έτσι υπάρχει μία υλοποίηση
    // αναπαραγωγής (ρυθμίσεις δικτύου, ffmpeg renderers, audio focus, επανάληψη
    // σε παροδικά σφάλματα) αντί για δύο που αποκλίνουν.
    val context = androidx.compose.ui.platform.LocalContext.current
    val engine = remember { PlaybackEngine(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { engine.release() } }
    val engineState by engine.state.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> engine.pause()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> engine.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    var failed by remember(preview) { mutableStateOf(false) }
    LaunchedEffect(preview) {
        val channel = preview ?: return@LaunchedEffect
        failed = false
        val url = try {
            resolveUrl(channel)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ""
        }
        if (url.isBlank()) failed = true else engine.open(url)
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val density = LocalDensity.current
        val fullWidthPx = with(density) { maxWidth.toPx() }
        val fullHeightPx = with(density) { maxHeight.toPx() }
        // Θέση/μέγεθος της «φωλιάς» της προεπισκόπησης, σε συντεταγμένες ρίζας.
        var slot by remember { mutableStateOf(Rect.Zero) }

        // ΣΕ ΠΛΗΡΗ ΟΘΟΝΗ ΤΟ ΠΕΡΙΕΧΟΜΕΝΟ ΔΕΝ ΣΥΝΤΙΘΕΤΑΙ ΚΑΘΟΛΟΥ.
        //
        // Πριν, η λίστα καναλιών και το πρόγραμμα έμεναν από κάτω — αόρατα αλλά
        // ΕΣΤΙΑΣΙΜΑ. Έτσι το τηλεχειριστήριο «δούλευε στο παρασκήνιο»: μετακινούσε
        // το focus σε κρυμμένα στοιχεία (π.χ. στο EPG) αντί να χειρίζεται τον
        // player. Χωρίς σύνθεση δεν υπάρχει τίποτα να πάρει focus κατά λάθος.
        //
        // Το περιθώριο μπαίνει ΜΟΝΟ στο περιεχόμενο, όχι στη ρίζα: έτσι ο player
        // σε πλήρη οθόνη φτάνει ως την άκρη, χωρίς μαύρη λωρίδα στη θέση της
        // μπάρας. 64dp = ακριβώς όσο η κλειστή μπάρα εικονιδίων.
        if (!fullscreen) Column(Modifier.fillMaxSize().padding(start = 64.dp)) {
            TvBrowseHeaderPublic()
            // Οδηγία όσο το multiview είναι οπλισμένο — αλλιώς δεν θα ήταν σαφές
            // ότι η εφαρμογή περιμένει δεύτερη επιλογή.
            multiviewPrimary?.let { first ->
                Row(
                    Modifier
                        .padding(start = 30.dp, end = 34.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IptvColors.Primary.copy(alpha = 0.22f))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.live_multiview_select_second, first.name),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.width(320.dp).fillMaxHeight(),
                    state = listState,
                    contentPadding = PaddingValues(start = 10.dp, end = 14.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!showingChannels) {
                        itemsIndexed(groups, key = { i, g -> "live-group:$i:$g" }) { index, group ->
                            TvLiveRow(
                                label = localizedUppercase(localizedLiveGroupLabel(group)),
                                selected = group == selectedGroup,
                                modifier = if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier,
                                onClick = { onSelectGroup(group); showingChannels = true }
                            )
                        }
                    } else {
                        itemsIndexed(channels, key = { i, c -> "live-ch:$i:${keyOf(c)}" }) { index, channel ->
                            val isPreview = preview?.let { currentPreview ->
                                keyOf(channel) == keyOf(currentPreview)
                            } ?: false
                            TvLiveRow(
                                label = channel.name,
                                // Τι παίζει τώρα, ΧΩΡΙΣ να χρειάζεται να το ανοίξεις.
                                subtitle = nowTitleOf(channel),
                                selected = isPreview,
                                favorite = keyOf(channel) in favoriteKeys,
                                // Το κανάλι που παίζει είναι το σημείο επιστροφής
                                // από την πλήρη οθόνη.
                                modifier = when {
                                    isPreview -> Modifier.focusRequester(playingRowFocus)
                                    index == 0 -> Modifier.focusRequester(firstItemFocus)
                                    else -> Modifier
                                },
                                onClick = {
                                    when (
                                        TvLiveBrowsePolicy.onChannelConfirm(
                                            targetKey = keyOf(channel),
                                            multiviewPrimaryKey = multiviewPrimary?.let(keyOf)
                                        )
                                    ) {
                                        TvLiveBrowsePolicy.ChannelAction.START_MULTIVIEW -> {
                                            multiviewPrimary?.let { first ->
                                                onMultiview(first, channel)
                                            }
                                            multiviewPrimary = null
                                        }
                                        // Ένα OK: επιλέγει και ανοίγει την ίδια
                                        // μηχανή/επιφάνεια σε πλήρη οθόνη.
                                        TvLiveBrowsePolicy.ChannelAction.OPEN_PLAYER -> {
                                            preview = channel
                                            fullscreen = true
                                        }
                                    }
                                },
                                // Παρατεταμένο OK: οπλίζει το multiview (διπλή
                                // προβολή). Το EPG παραμένει διαθέσιμο από τη
                                // λίστα προγράμματος δεξιά.
                                onLongClick = {
                                    multiviewPrimary =
                                        if (multiviewPrimary?.let(keyOf) == keyOf(channel)) null
                                        else channel
                                }
                            )
                        }
                    }
                }

                // Η «φωλιά»: κρατά τον χώρο και μας δίνει τη θέση-στόχο. Το ίδιο
                // το βίντεο ζωγραφίζεται στο επίπεδο από πάνω.
                BoxWithConstraints(
                    Modifier.fillMaxSize().padding(start = 8.dp, end = 24.dp, bottom = 16.dp)
                ) {
                    // Ο player περιορίζεται ΚΑΙ από το πλάτος ΚΑΙ από το ύψος.
                    //
                    // Πριν είχε μόνο fillMaxWidth + aspectRatio(16:9): σε φαρδύ
                    // δεξί χώρο το ύψος που προέκυπτε ξεπερνούσε τη στήλη, οπότε
                    // ο player κοβόταν και έσπρωχνε το πρόγραμμα εκτός οθόνης.
                    // Κρατάμε ~50% του ύψους για τον player και τα υπόλοιπα για
                    // λεζάντα + EPG.
                    val heightCap = maxHeight * 0.50f
                    val playerHeight = minOf(maxWidth * 9f / 16f, heightCap)
                    val playerWidth = playerHeight * 16f / 9f
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .width(playerWidth)
                                .height(playerHeight)
                                .onGloballyPositioned { slot = it.boundsInRoot() }
                        )
                        preview?.let { currentPreview ->
                            Spacer(Modifier.height(10.dp))
                            TvPreviewCaption(currentPreview, nowTitleOf)
                            Spacer(Modifier.height(8.dp))
                            // ΟΛΟ το πρόγραμμα του καναλιού, όχι μόνο τώρα/επόμενο.
                            TvChannelSchedule(programmesOf(currentPreview)) { programmeDetails = it }
                        }
                    }
                }
            }
        }

        // ---- ΤΟ ΚΟΙΝΟ ΕΠΙΠΕΔΟ ΒΙΝΤΕΟ ----
        //
        // Ίδια μηχανή, ίδια επιφάνεια: το OK μεγαλώνει το δοχείο σε πλήρη οθόνη
        // χωρίς να διακοπεί η ροή. Δεν ανοίγει δεύτερος player.
        preview?.let { channel ->
            PlayerHost(
                engine = engine,
                title = channel.name,
                subtitle = nowTitleOf(channel),
                inlineBounds = slot,
                fullscreen = fullscreen,
                fullWidthPx = fullWidthPx,
                fullHeightPx = fullHeightPx,
                onExitFullscreen = { fullscreen = false },
                onOpenList = { channelPanelOpen = true },
                focusEpoch = playerFocusEpoch,
                overlayOpen = channelPanelOpen,
                onChannelStep = { delta ->
                    val index = channels.indexOfFirst { keyOf(it) == keyOf(channel) }
                    channels.getOrNull(index + delta)?.let { preview = it }
                },
                errorText = if (failed) stringResource(R.string.live_channel_unavailable) else null,
                extraActions = {
                    PlayerExtraAction(stringResource(R.string.live_channels)) { channelPanelOpen = true }
                }
            )
        }

        // ---- ΛΙΣΤΑ ΚΑΝΑΛΙΩΝ ΜΕΣΑ ΣΤΟΝ PLAYER ----
        // Δεξιά επίστρωση, μόνο σε πλήρη οθόνη. Βλέπεις τι διαλέγεις (όνομα + τι
        // παίζει) πριν αλλάξεις — αντί να πέφτεις τυφλά σε άλλο κανάλι.
        if (fullscreen && channelPanelOpen) {
            val panelFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { panelFocus.requestFocusWithRetry() }
            BackHandler(enabled = true) { closeChannelPanel() }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(360.dp)
                    .background(Color(0xF00A0A0C))
            ) {
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    Text(
                        localizedUppercase(stringResource(R.string.live_channels)),
                        color = IptvColors.Primary,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(10.dp))
                    val panelState = rememberLazyListState()
                    LaunchedEffect(preview) {
                        val index = channels.indexOfFirst { keyOf(it) == preview?.let(keyOf) }
                        if (index >= 0) panelState.scrollToItem(index)
                    }
                    LazyColumn(
                        state = panelState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(channels, key = { i, c -> "panel:$i:${keyOf(c)}" }) { index, item ->
                            val isCurrent = keyOf(item) == preview?.let(keyOf)
                            TvLiveRow(
                                label = item.name,
                                subtitle = nowTitleOf(item),
                                selected = isCurrent,
                                favorite = keyOf(item) in favoriteKeys,
                                modifier = if (isCurrent || (index == 0 && preview == null)) {
                                    Modifier.focusRequester(panelFocus)
                                } else Modifier,
                                onClick = {
                                    preview = item
                                    closeChannelPanel()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Placeholder όταν δεν έχει επιλεγεί κανάλι.
        if (preview == null && slot != Rect.Zero) {
            Box(
                Modifier
                    .offset { IntOffset(slot.left.roundToInt(), slot.top.roundToInt()) }
                    .size(with(density) { slot.width.toDp() }, with(density) { slot.height.toDp() })
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF14141A))
                    .border(1.dp, IptvColors.Divider, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LiveTv, null, tint = IptvColors.TextTertiary, modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.live_select_preview),
                        color = IptvColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Αναλυτική πληροφορία προγράμματος (OK σε εγγραφή EPG ή παρατεταμένο OK
        // σε κανάλι): ΟΛΗ η περιγραφή, με δυνατότητα κύλισης.
        programmeDetails?.let { programme ->
            val closeFocus = remember(programme) { FocusRequester() }
            LaunchedEffect(programme) { closeFocus.requestFocusWithRetry() }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { programmeDetails = null },
                containerColor = IptvColors.SurfaceRaised,
                title = {
                    Column {
                        if (programme.time.isNotBlank() || programme.isNow) {
                            Text(
                                if (programme.isNow) {
                                    stringResource(R.string.live_schedule_now_time, programme.time).trim()
                                } else programme.time,
                                color = IptvColors.Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            programme.title,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                text = {
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        item {
                            Text(
                                programme.description.ifBlank { stringResource(R.string.live_no_description) },
                                color = IptvColors.TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    com.prelude.iptv.ui.TvDialogTextButton(
                        label = stringResource(R.string.live_close),
                        color = Color.White,
                        onClick = { programmeDetails = null },
                        modifier = Modifier.focusRequester(closeFocus)
                    )
                }
            )
        }
    }
}

/** Πρόγραμμα καναλιού: ώρα + τίτλος, με το τρέχον τονισμένο. */
@Composable
private fun TvChannelSchedule(
    programmes: List<LiveProgramme>,
    onOpen: (LiveProgramme) -> Unit
) {
    if (programmes.isEmpty()) {
        Text(
            stringResource(R.string.live_no_schedule),
            color = IptvColors.TextTertiary,
            fontSize = 11.sp
        )
        return
    }
    Text(
        stringResource(R.string.live_schedule),
        color = IptvColors.Primary,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp,
        fontWeight = FontWeight.Black
    )
    Spacer(Modifier.height(6.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        itemsIndexed(programmes) { _, programme ->
            // Κάθε εγγραφή είναι επιλέξιμη: πηγαίνεις δεξιά με το D-pad και με OK
            // βλέπεις ολόκληρη την περιγραφή.
            var focused by remember { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            focused -> Color.White
                            programme.isNow -> IptvColors.Primary.copy(alpha = 0.16f)
                            else -> Color.Transparent
                        }
                    )
                    .onFocusChanged { focused = it.isFocused }
                    .combinedClickable(onClick = { onOpen(programme) })
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (programme.isNow) stringResource(R.string.live_schedule_now) else programme.time,
                    color = when {
                        focused -> Color.Black
                        programme.isNow -> IptvColors.Primary
                        else -> IptvColors.TextTertiary
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(62.dp)
                )
                Text(
                    programme.title,
                    color = when {
                        focused -> Color.Black
                        programme.isNow -> Color.White
                        else -> IptvColors.TextSecondary
                    },
                    fontSize = 12.sp,
                    fontWeight = if (programme.isNow || focused) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Όνομα καναλιού + τι παίζει τώρα. Και κάτω από τη φωλιά και σε πλήρη οθόνη. */
@Composable
private fun TvPreviewCaption(
    channel: Channel,
    nowTitleOf: (Channel) -> String,
    compact: Boolean = false
) {
    val now = nowTitleOf(channel)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (channel.logo.isNotBlank()) {
            AsyncImage(
                channel.logo, channel.name,
                Modifier.size(width = 56.dp, height = 34.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                channel.name,
                color = Color.White,
                fontSize = if (compact) 20.sp else 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (now.isNotBlank()) {
                Text(
                    now,
                    color = IptvColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!compact) {
            Text(
                stringResource(R.string.live_ok_again_to_play),
                color = IptvColors.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Γραμμή λίστας (κατηγορία ή κανάλι) με ορατό TV focus. */
@Composable
private fun TvLiveRow(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    favorite: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val background = when {
        focused -> Color.White
        selected -> IptvColors.Primary
        else -> Color.Transparent
    }
    val foreground = when {
        focused -> Color.Black
        selected -> Color.White
        else -> IptvColors.TextSecondary
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background, shape)
            .onFocusChanged { focused = it.isFocused }
            // tvConfirm: το παρατεταμένο OK του τηλεχειριστηρίου. Το
            // combinedClickable μένει για αφή/ποντίκι και για να είναι focusable.
            .tvConfirm(onClick = onClick, onLongClick = onLongClick)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color = foreground,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                fontWeight = if (selected || focused) FontWeight.Black else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = if (focused) Color(0xFF55555D) else IptvColors.TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (favorite) {
            Icon(
                Icons.Default.Favorite, null,
                tint = IptvColors.Primary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
