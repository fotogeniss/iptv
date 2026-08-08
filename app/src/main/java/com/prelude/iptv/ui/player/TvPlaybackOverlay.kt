package com.prelude.iptv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.prelude.iptv.data.Channel
import com.prelude.iptv.player.PlaybackEngine
import kotlinx.coroutines.delay

/**
 * Πλήρους οθόνης αναπαραγωγή μέσα στην ΙΔΙΑ οθόνη — ταινία, επεισόδιο Ή κανάλι.
 *
 * Χρησιμοποιεί το κοινό [PlayerHost], άρα έχει παντού τα ίδια χειριστήρια: ήχο,
 * υποτίτλους, αναλογία, ανάλυση, ταχύτητα, χρονοδιακόπτη.
 *
 * ΓΙΑΤΙ ΔΕΧΕΤΑΙ ΚΑΙ ΖΩΝΤΑΝΑ: μέχρι τώρα τα ζωντανά από την αρχική, τη βιβλιοθήκη
 * και την αναζήτηση άνοιγαν τον παλιό `PlayerActivity`, ενώ τα ίδια κανάλια από
 * την οθόνη Live άνοιγαν αυτόν εδώ. Ίδιο κανάλι, διαφορετικός player, ανάλογα με
 * το πού το πάτησες — και ό,τι φτιαχνόταν στον έναν έλειπε από τον άλλον.
 *
 * Η διάκριση γίνεται από το ίδιο το [Channel] και όχι από παράμετρο: μια σημαία
 * που περνιέται από έξω μπορεί να δοθεί λάθος σε ένα από τα σημεία κλήσης, και
 * τότε μια ταινία χάνει τη μπάρα προόδου ή ένα κανάλι αποκτά «συνέχεια».
 *
 * Για ταινίες, η θέση αποθηκεύεται περιοδικά ΚΑΙ στην έξοδο, ώστε «Συνέχεια» να
 * δουλεύει ακόμη κι αν η εφαρμογή τερματιστεί απότομα.
 */
@Composable
fun TvPlaybackOverlay(
    channel: Channel,
    title: String,
    subtitle: String,
    resolveUrl: suspend (Channel) -> String,
    /** Αποθηκευμένη θέση σε ms· 0 = από την αρχή. Αγνοείται στα ζωντανά. */
    loadResumeMs: (Channel) -> Long,
    /** channel, θέση, συνολική διάρκεια. Δεν καλείται στα ζωντανά. */
    saveResumeMs: (Channel, Long, Long) -> Unit,
    onClose: () -> Unit,
    /** Αλλαγή καναλιού με CH+/CH−. null = δεν υπάρχει λίστα να διατρέξουμε. */
    onChannelStep: ((Int) -> Unit)? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    nextTitle: String? = null,
    /** Στιγμιότυπο επόμενου επεισοδίου για την κάρτα. */
    nextImageUrl: String? = null,
    onPlayNext: (() -> Unit)? = null,
    /**
     * Λήψη υποτίτλων. Δέχεται τη μηχανή ώστε ο καλών να δηλώσει το αρχείο που
     * κατέβασε, και επιστρέφει μήνυμα για τον χρήστη.
     */
    fetchSubtitles: (suspend (PlaybackEngine) -> String)? = null,
    /** Χειροκίνητη αναζήτηση: επιστρέφει υποψήφιους για επιλογή. */
    searchSubtitles: (suspend (String) -> List<ExternalSubtitle>)? = null,
    /** Κατεβάζει και εφαρμόζει τον επιλεγμένο στη μηχανή. */
    applySubtitle: (suspend (PlaybackEngine, ExternalSubtitle) -> String)? = null,
    /** Επιπλέον κουμπιά της κάθε διαδρομής (π.χ. «Πρόγραμμα» στα ζωντανά). */
    extraActions: (@Composable () -> Unit)? = null,
    /**
     * true όσο η διαδρομή έχει ανοιχτή δική της επίστρωση πάνω από τον player.
     * Χωρίς αυτό, το χρονόμετρο αδράνειας του player συνεχίζει να τρέχει από
     * κάτω και του κλέβει το focus. Δες [PlayerHost.overlayOpen].
     */
    overlayOpen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val engine = remember { PlaybackEngine(context.applicationContext) }
    val videoFrameCapture = remember { PlayerVideoFrameCapture() }
    val liveTransitionCoordinator = remember(engine, videoFrameCapture) {
        LiveChannelTransitionCoordinator(engine, videoFrameCapture)
    }
    val state by engine.state.collectAsState()
    var failed by remember(channel) { mutableStateOf(false) }
    var channelTransition by remember {
        mutableStateOf<LiveChannelTransitionRequest?>(null)
    }
    var channelTransitionSequence by remember { mutableIntStateOf(0) }
    var pendingChannelTransitionDirection by remember { mutableStateOf<Int?>(null) }
    var pendingChannelTransitionVersion by remember { mutableIntStateOf(0) }
    var isFirstChannel by remember { mutableStateOf(true) }

    // Ζωντανή ροή: δεν έχει αρχή, τέλος, ούτε «πού είχα μείνει».
    val isLive = channel.kind == "live"

    // Το rememberUpdatedState κρατά την τελευταία έκδοση των callbacks χωρίς να
    // ξαναξεκινά η αναπαραγωγή σε κάθε recomposition.
    val save by rememberUpdatedState(saveResumeMs)
    val stepChannel by rememberUpdatedState(onChannelStep)
    val directionalChannelStep: ((Int) -> Unit)? = onChannelStep?.let {
        { step ->
            if (isLive) {
                pendingChannelTransitionDirection =
                    TvLiveChannelTransitionMotion.direction(step)
                pendingChannelTransitionVersion++
            }
            stepChannel?.invoke(step)
        }
    }

    LaunchedEffect(channel) {
        failed = false
        channelTransition = null
        val initialChannel = isFirstChannel
        if (initialChannel) isFirstChannel = false
        val transitionDirection = pendingChannelTransitionDirection
            .takeIf { !initialChannel && isLive }
        pendingChannelTransitionDirection = null

        when (val result = liveTransitionCoordinator.open(
            channel = channel,
            isLive = isLive,
            transitionDirection = transitionDirection,
            resolveUrl = resolveUrl,
            loadResumeMs = loadResumeMs,
        )) {
            is LiveChannelOpenResult.Failed -> {
                result.cause?.let { error ->
                    android.util.Log.w(
                        "PreludePlayback",
                        "Η επίλυση διεύθυνσης απέτυχε για «${channel.name}» (${channel.kind})",
                        error,
                    )
                }
                android.util.Log.w(
                    "PreludePlayback",
                    "Κενή διεύθυνση για «${channel.name}» · kind=${channel.kind} · " +
                        "url='${channel.url}' cmd='${channel.cmd}'",
                )
                failed = true
            }
            is LiveChannelOpenResult.Opened -> {
                result.transition?.let { prepared ->
                    channelTransition = LiveChannelTransitionRequest(
                        sequence = ++channelTransitionSequence,
                        direction = prepared.direction,
                        outgoingFrame = prepared.outgoingFrame,
                    )
                }
            }
        }
    }

    // A boundary CH+/CH− press may not publish another channel. Expire that
    // intent so a later list selection cannot inherit the wrong direction.
    LaunchedEffect(pendingChannelTransitionVersion) {
        if (pendingChannelTransitionVersion == 0) return@LaunchedEffect
        val version = pendingChannelTransitionVersion
        delay(1_200)
        if (pendingChannelTransitionVersion == version) {
            pendingChannelTransitionDirection = null
        }
    }

    // Περιοδική αποθήκευση: αν κοπεί το ρεύμα ή σκοτωθεί η εφαρμογή, δεν χάνεται
    // η πρόοδος. Ο κλασικός player έκανε το ίδιο.
    if (!isLive) {
        LaunchedEffect(channel) {
            while (true) {
                delay(10_000)
                val position = engine.currentPositionMs()
                if (position > 0) save(channel, position, engine.durationMs())
            }
        }
    }

    DisposableEffect(channel) {
        onDispose {
            if (!isLive) {
                val position = engine.currentPositionMs()
                if (position > 0) save(channel, position, engine.durationMs())
            }
        }
    }

    // One overlay owns one engine. Releasing on each channel key would race the
    // next open and its first-rendered-frame transition.
    DisposableEffect(engine) {
        onDispose { engine.release() }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    engine.pause()
                    if (!isLive) {
                        val position = engine.currentPositionMs()
                        if (position > 0) save(channel, position, engine.durationMs())
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> engine.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    BackHandler(enabled = true) { onClose() }

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val density = LocalDensity.current
        val fullWidthPx = with(density) { maxWidth.toPx() }
        val fullHeightPx = with(density) { maxHeight.toPx() }
        Box(Modifier.fillMaxSize()) {
            PlayerHost(
                engine = engine,
                title = title,
                subtitle = subtitle,
                // Ξεκινά ήδη σε πλήρη οθόνη: δεν υπάρχει «φωλιά» να μεταβεί.
                inlineBounds = Rect(0f, 0f, fullWidthPx, fullHeightPx),
                fullscreen = true,
                fullWidthPx = fullWidthPx,
                fullHeightPx = fullHeightPx,
                onExitFullscreen = onClose,
                // Σε ζωντανή ροή δεν υπάρχει πού να πας: ούτε μπάρα προόδου,
                // ούτε ±10 δευτερόλεπτα. Αντ' αυτού, τα βελάκια αλλάζουν κανάλι.
                seekable = !isLive,
                onChannelStep = directionalChannelStep,
                frameCapture = videoFrameCapture,
                videoOverlay = if (isLive) {
                    {
                        TvLiveChannelTransition(
                            request = channelTransition,
                            onFinished = { sequence ->
                                if (channelTransition?.sequence == sequence) {
                                    channelTransition = null
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else null,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                nextTitle = nextTitle,
                nextImageUrl = nextImageUrl,
                onPlayNext = onPlayNext,
                // Στα ζωντανά δεν έχει νόημα: δεν υπάρχει τίτλος ταινίας να
                // αναζητηθεί, μόνο ένα κανάλι που παίζει συνέχεια.
                onFetchSubtitles = if (isLive) null else fetchSubtitles?.let { fetch ->
                    { fetch(engine) }
                },
                onSearchSubtitles = if (isLive) null else searchSubtitles,
                onApplySubtitle = if (isLive) null else applySubtitle?.let { apply ->
                    { choice -> apply(engine, choice) }
                },
                extraActions = extraActions,
                overlayOpen = overlayOpen,
                // Δύο ΔΙΑΦΟΡΕΤΙΚΑ προβλήματα, δύο διαφορετικά μηνύματα. Το ίδιο
                // κείμενο και για τα δύο μας έστειλε να ψάχνουμε τη λάθος μεριά.
                errorText = if (failed) {
                    "Η πηγή δεν έδωσε διεύθυνση για αυτό το περιεχόμενο"
                } else state.error
            )
        }
    }
}
