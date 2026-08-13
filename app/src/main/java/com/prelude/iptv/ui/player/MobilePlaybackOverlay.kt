package com.prelude.iptv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.prelude.iptv.player.NextEpisodePolicy
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.data.SubtitleSearchPolicy
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.player.PlaybackEngine
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.localizedPlaybackSpeed
import com.prelude.iptv.ui.mobile.navigation.MobilePlayerDockState
import com.prelude.iptv.ui.mobile.navigation.PremiumMobileBottomDockFallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Αναπαραγωγή σε κινητό, πάνω από την ίδια [PlaybackEngine] με την τηλεόραση.
 *
 * Ίδια μηχανή, διαφορετικά χειριστήρια: εδώ όλα είναι με αφή. Η λογική
 * αναπαραγωγής (δίκτυο, κωδικοποιητές, επανάληψη, θέση) δεν επαναλαμβάνεται —
 * ζει στη μηχανή. Η εμφάνιση ζει στο [MobilePlayerControls].
 *
 * ΔΥΟ ΚΑΤΑΣΤΑΣΕΙΣ, ΟΠΩΣ ΣΤΟ YOUTUBE:
 *
 * - **μαζεμένη**: η εικόνα σε 16:9 στην κορυφή, ο τίτλος από κάτω. Ο χρήστης
 *   βλέπει ΤΙ παίζει χωρίς να χρειάζεται τα χειριστήρια ανοιχτά.
 * - **πλήρης**: η εικόνα γεμίζει την οθόνη, ο τίτλος φεύγει.
 *
 * Το «μαζεμένη» είναι η ΠΡΟΕΠΙΛΟΓΗ. Πριν, η εικόνα γέμιζε πάντα την οθόνη και ο
 * τίτλος υπήρχε μόνο μέσα στη μπάρα των χειριστηρίων — μόλις εκείνα έσβηναν μετά
 * από 3,5 δευτερόλεπτα, δεν φαινόταν πουθενά τι παίζει.
 */
@Composable
fun MobilePlaybackOverlay(
    channel: Channel,
    title: String,
    isLive: Boolean,
    resolveUrl: suspend (Channel) -> String,
    loadResumeMs: (Channel) -> Long,
    saveResumeMs: (Channel, Long, Long) -> Unit,
    onClose: () -> Unit,
    fetchSubtitles: (suspend (PlaybackEngine) -> String)? = null,
    searchSubtitles: (suspend (String) -> List<ExternalSubtitle>)? = null,
    applySubtitle: (suspend (PlaybackEngine, ExternalSubtitle) -> String)? = null,
    /** Δευτερεύουσα γραμμή κάτω από τον τίτλο (έτος · είδος, ή τι παίζει τώρα). */
    subtitle: String = "",
    nextTitle: String? = null,
    nextImageUrl: String? = null,
    onPlayNext: (() -> Unit)? = null,
    onPlayPrevious: (() -> Unit)? = null,
    /** CH+/CH− με σύρσιμο αριστερά/δεξιά. Μόνο για ζωντανά — null αλλού. */
    onChannelStep: ((Int) -> Unit)? = null,
    infoChannel: Channel = channel,
    metadata: TmdbClient.Meta? = null,
    relatedItems: List<Channel> = emptyList(),
    seasons: List<Pair<String, List<Channel>>> = emptyList(),
    onPlayContextItem: (Channel) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val subtitleDownloadFailed = stringResource(R.string.player_subtitle_download_failed_general)
    val subtitleApplyUnavailable = stringResource(R.string.player_subtitle_apply_unavailable)
    val engine = remember { PlaybackEngine(context.applicationContext) }
    val videoFrameCapture = remember { PlayerVideoFrameCapture() }
    // ΜΙΑ ΕΠΙΦΑΝΕΙΑ, ΠΟΥ ΜΕΤΑΚΟΜΙΖΕΙ. ΔΕΝ ΦΤΙΑΧΝΕΤΑΙ ΔΕΥΤΕΡΗ.
    //
    // Πριν, η πλήρης οθόνη και η λωρίδα έφτιαχναν η καθεμιά το δικό της
    // TextureView. Το μάζεμα σήμαινε «σκότωσε τη μία, φτιάξε την άλλη» πάνω σε
    // player που έπαιζε: δύο επιφάνειες διεκδικούσαν την ίδια έξοδο βίντεο και
    // το αποτέλεσμα κρεμόταν από τη σειρά με την οποία το Compose εφαρμόζει
    // εισαγωγές και ακυρώσεις, από τον κύκλο ζωής του SurfaceTexture και από το
    // αν ο codec δέχεται εναλλαγή εξόδου εν κινήσει. Όταν έχανε, ο ήχος
    // συνέχιζε και η εικόνα χανόταν — ακριβώς το σύμπτωμα.
    //
    // Το [movableContentOf] μεταφέρει τους ΙΔΙΟΥΣ κόμβους από τον έναν γονέα
    // στον άλλο. Το TextureView δεν καταστρέφεται ποτέ, η έξοδος του ExoPlayer
    // δεν αλλάζει ποτέ, και δεν υπάρχει πια σειρά που να μπορεί να χαθεί.
    val videoSurface = remember(engine, videoFrameCapture) {
        movableContentOf { keepOn: Boolean, surfaceModifier: Modifier ->
            PlayerVideoSurface(
                engine = engine,
                keepScreenOn = keepOn,
                // Ίδιο TextureView σε πλήρη οθόνη και σε λωρίδα: συντίθεται στο
                // ίδιο layer με το Compose UI και ακολουθεί σωστά resize,
                // clipping και z-order. Το SurfaceView ζει σε ξεχωριστό system
                // layer και σε ορισμένες συσκευές μένει πίσω από το μαύρο
                // container.
                preferSmoothResize = true,
                frameCapture = videoFrameCapture,
                modifier = surfaceModifier,
            )
        }
    }
    val liveTransitionCoordinator = remember(engine, videoFrameCapture) {
        LiveChannelTransitionCoordinator(engine, videoFrameCapture)
    }
    val state by engine.state.collectAsState()
    var failed by remember(channel) { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    val contentScrollState = rememberScrollState()
    // Μαζεμένος σε λωρίδα στο κάτω μέρος, με τον ήχο να συνεχίζει. Δες
    // [MobileMiniPlayer] για το γιατί αυτό είναι κατάσταση ΕΔΩ και όχι στον γονέα.
    var collapsed by remember { mutableStateOf(false) }
    val dockBottomPadding = with(LocalDensity.current) {
        MobilePlayerDockState.navigationHeightPx
            .takeIf { it > 0 }
            ?.toDp()
            ?: PremiumMobileBottomDockFallback
    }
    DisposableEffect(collapsed) {
        MobilePlayerDockState.isDocked = collapsed
        onDispose {
            if (collapsed) MobilePlayerDockState.isDocked = false
        }
    }
    // Πόσο έχει τραβήξει το δάχτυλο προς τα κάτω, σε pixel. Ζωντανή ανάδραση:
    // χωρίς αυτή, ο χρήστης τραβά στο κενό και δεν ξέρει αν κάτι θα γίνει.
    var dragY by remember { mutableFloatStateOf(0f) }
    // While the finger is down, follow it exactly. If the gesture does not cross
    // the dock threshold, spring the complete page back instead of snapping only
    // the video to the top.
    val renderedPageDragY by animateFloatAsState(
        targetValue = dragY,
        animationSpec = if (dragY == 0f) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        } else {
            snap()
        },
        label = "mobile-player-page-drag",
    )
    // Πόσο έχει τραβήξει το δάχτυλο οριζόντια, σε pixel — για το CH+/CH− των
    // ζωντανών με σύρσιμο. Ίδιο μοτίβο με το dragY, απλά στον άλλο άξονα.
    var dragX by remember { mutableFloatStateOf(0f) }
    var verticalControl by remember { mutableStateOf<VerticalPlayerControl?>(null) }
    var verticalControlPercent by remember { mutableIntStateOf(0) }
    var verticalControlStart by remember { mutableFloatStateOf(0f) }
    var verticalControlDelta by remember { mutableFloatStateOf(0f) }
    var verticalFeedbackVersion by remember { mutableIntStateOf(0) }

    // Συσσωρευτής διπλού αγγίγματος: γρήγορα διαδοχικά χτυπήματα προσθέτουν
    // δευτερόλεπτα αντί να μετράει μόνο το τελευταίο (συμπεριφορά YouTube).
    var seekFeedbackMs by remember { mutableLongStateOf(0L) }
    var seekFeedbackForward by remember { mutableStateOf(true) }

    var channelTransitionSequence by remember { mutableIntStateOf(0) }
    var channelTransition by remember { mutableStateOf<LiveChannelTransitionRequest?>(null) }
    var pendingChannelTransitionDirection by remember { mutableStateOf<Int?>(null) }
    var pendingChannelTransitionVersion by remember { mutableIntStateOf(0) }
    // Όνομα του νέου καναλιού σε συννεφάκι πάνω-πάνω. null = κρυμμένο.
    var channelToast by remember { mutableStateOf<String?>(null) }
    // Το πρώτο κανάλι δεν πρέπει να δείξει συννεφάκι «άλλαξε» — μόνο αυτά που
    // έρχονται από σύρσιμο.
    var isFirstChannel by remember { mutableStateOf(true) }

    // ΤΟ ΣΥΡΣΙΜΟ ΔΕΝ ΓΡΑΦΕΙ ΚΑΤΕΥΘΕΙΑΝ ΣΤΗ ΜΗΧΑΝΗ.
    //
    // Κάθε κίνηση του δαχτύλου θα ήταν ένα seek — δεκάδες ανά δευτερόλεπτο, με τη
    // ροή να ξαναφορτώνει σε κάθε ένα. Η μπάρα ακολουθεί το δάχτυλο τοπικά και η
    // μηχανή ενημερώνεται ΜΙΑ φορά, όταν ο χρήστης το σηκώσει.
    var scrubbing by remember { mutableStateOf(false) }
    var scrubMs by remember { mutableLongStateOf(0L) }
    val shownPositionMs = if (scrubbing) scrubMs else state.positionMs
    var nextOfferDismissed by remember(channel) { mutableStateOf(false) }
    val hasNextEpisode = onPlayNext != null && !nextTitle.isNullOrBlank()
    val offerNextEpisode = !nextOfferDismissed && NextEpisodePolicy.shouldOffer(
        positionMs = state.positionMs,
        durationMs = state.durationMs,
        hasNext = hasNextEpisode,
    )
    val autoPlayNextEpisode = !nextOfferDismissed && NextEpisodePolicy.shouldAutoPlay(
        positionMs = state.positionMs,
        durationMs = state.durationMs,
        hasNext = hasNextEpisode,
    )

    // Auto-play only if the user left the offer untouched. Closing it is an
    // explicit opt-out for the current episode.
    LaunchedEffect(state.playing, autoPlayNextEpisode) {
        if (state.playing && autoPlayNextEpisode) onPlayNext?.invoke()
    }

    // Ρυθμίσεις υποτίτλων: ίδιο μοτίβο με την τηλεόραση — διαβάζονται από τον
    // δίσκο και γράφονται πίσω, ώστε να ισχύουν και στην επόμενη ταινία.
    val appContext = context.applicationContext
    val store = remember(appContext) { PlaylistStore(appContext) }
    var subtitleSize by remember { mutableIntStateOf(store.subtitleSizePercent) }
    var subtitleBackground by remember { mutableStateOf(store.subtitleBackground) }
    var subtitleBold by remember { mutableStateOf(store.subtitleBold) }
    var subtitleMessage by remember { mutableStateOf<String?>(null) }
    var fetchingSubtitles by remember { mutableStateOf(false) }
    // Η αναλογία ζει στο mobile overlay, όχι στα controls: το γρανάζι την αλλάζει,
    // αλλά η θέση και η εμφάνιση των σημερινών controls παραμένουν ίδιες.
    var aspectMode by remember { mutableStateOf(AspectMode.FIT) }
    val scope = rememberCoroutineScope()
    var openMenu by remember { mutableStateOf<PlayerMenu?>(null) }

    LaunchedEffect(subtitleMessage) {
        if (subtitleMessage != null) {
            delay(3_500)
            subtitleMessage = null
        }
    }
    // Το γρανάζι δεν ανοίγει κατευθείαν την ανάλυση: στο YouTube δίνει κατάλογο
    // ρυθμίσεων, και το κρύψιμο της ταχύτητας ή της γλώσσας ήχου πίσω από τρία
    // πατήματα αλλού θα σήμαινε ότι δεν τις βρίσκει κανείς.
    var settingsOpen by remember { mutableStateOf(false) }
    var aspectMenuOpen by remember { mutableStateOf(false) }
    // Η ταχύτητα δεν ζει στη μηχανή: το ExoPlayer τη δέχεται αλλά δεν τη
    // δημοσιεύει, και το μενού πρέπει να ξέρει τι είναι επιλεγμένο.
    var speed by remember { mutableFloatStateOf(1f) }
    var sleepMinutes by remember { mutableIntStateOf(0) }

    val save by rememberUpdatedState(saveResumeMs)

    LaunchedEffect(channel) {
        failed = false
        contentScrollState.scrollTo(0)
        // Νέο περιεχόμενο ξαναμεγαλώνει τον player. Ο χρήστης μπορεί να είχε
        // μαζεμένη τη λωρίδα και να πάτησε άλλη ταινία από τη λίστα: το να άρχιζε
        // να παίζει κρυμμένη μέσα σε 58 pixel δεν είναι αυτό που ζήτησε.
        collapsed = false
        channelTransition = null
        channelToast = null
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
            onOutgoingFrameCaptured = { frame ->
                // Καλύπτει αμέσως την πραγματική επιφάνεια με το παγωμένο
                // τελευταίο καρέ, ΠΡΙΝ καν ξεκινήσει η επίλυση/άνοιγμα του
                // νέου καναλιού. Διαφορετικά η πραγματική εναλλαγή ροής
                // (μαύρο/artifact) φαινόταν σαν ένα ξένο "φλας" πριν προλάβει
                // να ξεκινήσει το εφέ — startReveal=false το κρατά ακίνητο.
                channelTransition = LiveChannelTransitionRequest(
                    sequence = ++channelTransitionSequence,
                    direction = transitionDirection ?: 1,
                    outgoingFrame = frame,
                )
            },
        )) {
            is LiveChannelOpenResult.Failed -> {
                result.cause?.let { error ->
                    android.util.Log.w(
                        "PreludePlayback",
                        "Η επίλυση διεύθυνσης απέτυχε για «${channel.name}» (${channel.kind})",
                        error
                    )
                }
                // Δες [TvPlaybackOverlay]: εδώ δεν έχει τρέξει καμία μηχανή. Το
                // πρόβλημα είναι ο κατάλογος, όχι η αναπαραγωγή.
                android.util.Log.w(
                    "PreludePlayback",
                    "Κενή διεύθυνση για «${channel.name}» · kind=${channel.kind} · " +
                        "url='${channel.url}' cmd='${channel.cmd}'"
                )
                // Αν είχε προλάβει να παγώσει καρέ πριν αποτύχει η επίλυση,
                // δεν έχει νόημα να μείνει κολλημένο πάνω από την οθόνη λάθους.
                channelTransition = null
                failed = true
            }
            is LiveChannelOpenResult.Opened -> {
                if (result.transitionCommitted) {
                    channelTransition = channelTransition?.copy(startReveal = true)
                    channelToast = title
                    delay(900)
                    channelToast = null
                } else {
                    // Ποτέ δεν ήρθε καρέ (timeout) — δεν έχει νόημα να μείνει
                    // παγωμένο το παλιό κανάλι πάνω από ό,τι δείχνει τώρα η μηχανή.
                    channelTransition = null
                }
            }
        }
    }

    // A boundary swipe may not have a next/previous item. Do not let that stale
    // direction leak into a later channel selected from the context list.
    LaunchedEffect(pendingChannelTransitionVersion) {
        if (pendingChannelTransitionVersion == 0) return@LaunchedEffect
        val version = pendingChannelTransitionVersion
        delay(1_200)
        if (pendingChannelTransitionVersion == version) {
            pendingChannelTransitionDirection = null
        }
    }

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

    // The engine belongs to the complete overlay, not to one channel. Releasing
    // it from DisposableEffect(channel) tears down the same engine during every
    // zap and races the next open/first-frame signal.
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

    // Χρονόμετρο αδράνειας. Ανοιχτό μενού ή σύρσιμο το κρατούν: στο πρώτο ο
    // χρήστης διαβάζει, στο δεύτερο δεν έχει καν αφήσει την οθόνη.
    var lastInteraction by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controlsVisible, state.playing, openMenu, scrubbing, lastInteraction) {
        if (controlsVisible && state.playing && openMenu == null && !scrubbing) {
            delay(3_500)
            controlsVisible = false
        }
    }
    val interact: () -> Unit = {
        controlsVisible = true
        lastInteraction = android.os.SystemClock.uptimeMillis()
    }

    // Χρονοδιακόπτης ύπνου. Κάθε αλλαγή ακυρώνει τον προηγούμενο — αλλιώς δύο
    // ρυθμίσεις θα έτρεχαν μαζί και η ταινία θα σταματούσε στην πρώτη από τις δύο.
    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes <= 0) return@LaunchedEffect
        delay(sleepMinutes * 60_000L)
        engine.pause()
        controlsVisible = true
    }

    LaunchedEffect(seekFeedbackMs) {
        if (seekFeedbackMs != 0L) {
            delay(700)
            seekFeedbackMs = 0L
        }
    }

    // ---- ΜΑΖΕΜΕΝΟΣ PLAYER ----
    //
    // Όσο είναι μαζεμένος, το BACK ανήκει στην οθόνη από κάτω: ο χρήστης πλοηγείται
    // κανονικά και η λωρίδα μένει. Αυτό είναι όλο το νόημα — αν το BACK έκλεινε τη
    // λωρίδα, δεν θα μπορούσες να φύγεις από τη σελίδα χωρίς να χάσεις τον ήχο.
    // Το BACK ΜΑΖΕΥΕΙ, ΔΕΝ ΚΛΕΙΝΕΙ. Ίδιο αποτέλεσμα με το τράβηγμα προς τα κάτω,
    // ώστε η πιο συνηθισμένη κίνηση εξόδου να μη σκοτώνει την αναπαραγωγή. Το
    // κλείσιμο μένει ρητή πράξη: το «×» πάνω στη λωρίδα. Σε πλήρη οθόνη το BACK
    // βγάζει πρώτα από την πλήρη οθόνη, όπως πριν.
    BackHandler(enabled = !collapsed) {
        if (expanded) expanded = false else collapsed = true
    }

    if (collapsed) {
        // ΔΙΑΦΑΝΟ ΚΑΙ ΧΩΡΙΣ ΧΕΙΡΟΝΟΜΙΕΣ: ένα Box που δεν έχει gesture modifier δεν
        // καταναλώνει αγγίγματα, οπότε περνούν στη λίστα από κάτω. Αν έβαζε
        // background ή pointerInput, η οθόνη θα έμοιαζε ζωντανή αλλά θα ήταν νεκρή.
        Box(modifier.fillMaxSize()) {
            MobileMiniPlayer(
                engine = engine,
                video = { surfaceModifier -> videoSurface(state.playing, surfaceModifier) },
                playing = state.playing,
                title = title,
                subtitle = subtitle,
                onExpand = { collapsed = false; interact() },
                onClose = onClose,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp)
                    // Attach to the menu's actual measured top edge. The host
                    // may already have consumed the system navigation inset.
                    .padding(bottom = dockBottomPadding)
            )
        }
        return
    }

    // Πλήρης οθόνη = ΓΥΡΙΣΜΑ της συσκευής, όχι μεγαλύτερο κουτί. Δες
    // [FullscreenEffect] για το γιατί το δεύτερο δεν κάνει τίποτα σε 16:9 βίντεο.
    FullscreenEffect(active = expanded)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            // The player is a page, not a loose video rectangle. During the
            // downward gesture the video, title, metadata, seasons and episodes
            // travel together, revealing the browse screen underneath. Once the
            // threshold is crossed the layout is replaced by MobileMiniPlayer.
            .graphicsLayer {
                translationY = renderedPageDragY
                val progress = (
                    renderedPageDragY / (size.height.coerceAtLeast(1f) * .55f)
                ).coerceIn(0f, 1f)
                val pageScale = 1f - progress * .035f
                scaleX = pageScale
                scaleY = pageScale
                transformOrigin = TransformOrigin(.5f, 0f)
                shape = RoundedCornerShape(22.dp)
                clip = progress > .01f
                shadowElevation = if (progress > .01f) 18.dp.toPx() else 0f
            }
            .background(Color.Black)
            // ΤΟ ΜΑΥΡΟ ΔΕΝ ΠΙΑΝΕΙ ΑΓΓΙΓΜΑΤΑ ΜΟΝΟ ΤΟΥ. Χωρίς αυτό, τα πατήματα
            // κάτω από τον τίτλο περνούσαν στην προηγούμενη οθόνη και άνοιγαν
            // αόρατες κάρτες.
            .consumeAllTouches()
    ) {
        // YouTube συμπεριφορά: ο player είναι ξεχωριστό ανώτερο layer. Το
        // περιεχόμενο κυλά κάτω από αυτόν αντί να τον παρασύρει μαζί του.
        val stickyPlayerHeight = if (expanded) maxHeight else maxWidth / (16f / 9f)
        val playerModifier = (if (expanded) Modifier.fillMaxSize()
            // 16:9 και όχι η αναλογία της ροής: το κουτί πρέπει να έχει σταθερό
            // ύψος. Με την αναλογία του βίντεο, το κουτί θα άλλαζε μέγεθος τη
            // στιγμή που φτάνουν οι διαστάσεις της ροής — και ο τίτλος από κάτω θα
            // πηδούσε.
            else Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .zIndex(1f))
            // The whole sticky slot must be opaque. The actual video may be
            // narrower/shorter in FIT mode; without this background, scrolling
            // cards were visible through its letterbox area.
            .background(Color.Black)
            .clipToBounds()
        BoxWithConstraints(playerModifier) {
            val density = LocalDensity.current
            val boxWidth = maxWidth
            val boxHeight = maxHeight
            val widthPx = with(density) { boxWidth.toPx() }

            // ---- Εικόνα, με σωστή αναλογία (το SurfaceView δεν κάνει letterbox) ----
            val containerAspect = if (boxHeight.value > 0f) boxWidth / boxHeight else 16f / 9f
            val videoAspect = when (aspectMode) {
                AspectMode.FORCE_4_3 -> 4f / 3f
                AspectMode.FORCE_16_9 -> 16f / 9f
                else -> state.videoAspect.takeIf { it > 0f } ?: containerAspect
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                val fitByWidth = videoAspect >= containerAspect
                videoSurface(
                    state.playing,
                    when (aspectMode) {
                        // Γεμίζει χωρίς να παραμορφώνει: η μεγαλύτερη διάσταση
                        // περισσεύει και κόβεται από το clipToBounds του container.
                        AspectMode.FILL -> if (fitByWidth) {
                            Modifier.fillMaxHeight().width(boxHeight * videoAspect)
                        } else {
                            Modifier.fillMaxWidth().height(boxWidth / videoAspect)
                        }
                        else -> if (fitByWidth) {
                            Modifier.fillMaxWidth().height(boxWidth / videoAspect)
                        } else {
                            Modifier.fillMaxHeight().width(boxHeight * videoAspect)
                        }
                    },
                )
            }

            if (isLive) {
                MobileLiveChannelTransition(
                    request = channelTransition,
                    onFinished = { sequence ->
                        if (channelTransition?.sequence == sequence) channelTransition = null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            PlayerSubtitles(
                engine = engine,
                sizePercent = subtitleSize,
                background = subtitleBackground,
                bold = subtitleBold,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (controlsVisible) 62.dp else 16.dp)
            )

            // ---- Χειρονομίες ----
            //
            // ΤΡΕΙΣ ΑΝΙΧΝΕΥΤΕΣ ΣΤΟΝ ΙΔΙΟ ΚΟΜΒΟ, ΜΕ ΤΑ ΣΥΡΣΙΜΑΤΑ ΠΡΩΤΑ. Κάθε
            // detectXDragGestures περιμένει το δικό του κατώφλι κίνησης (touch
            // slop) πριν καταναλώσει το γεγονός, οπότε ένα σκέτο άγγιγμα περνά
            // στον επόμενο ανιχνευτή. Με το detectTapGestures πρώτο, θα
            // κατανάλωνε το πάτημα και κανένα σύρσιμο δεν θα ξεκινούσε ποτέ.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(channel, expanded) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                if (expanded) {
                                    verticalControl = if (offset.x < widthPx / 2f) {
                                        VerticalPlayerControl.BRIGHTNESS
                                    } else {
                                        VerticalPlayerControl.VOLUME
                                    }
                                    verticalControlDelta = 0f
                                    verticalControlStart = when (verticalControl) {
                                        VerticalPlayerControl.BRIGHTNESS -> currentBrightness(context)
                                        VerticalPlayerControl.VOLUME -> currentVolume(context)
                                        null -> 0f
                                    }
                                    verticalControlPercent = (verticalControlStart * 100f).toInt()
                                }
                            },
                            onDragEnd = {
                                if (expanded && verticalControl != null) {
                                    val version = ++verticalFeedbackVersion
                                    scope.launch {
                                        delay(700)
                                        if (verticalFeedbackVersion == version) verticalControl = null
                                    }
                                } else {
                                    // Κατώφλι στο ΕΝΑ ΤΕΤΑΡΤΟ του ύψους της εικόνας.
                                    val threshold = with(density) { boxHeight.toPx() } / 4f
                                    if (dragY > threshold) {
                                        collapsed = true
                                        expanded = false
                                    }
                                    dragY = 0f
                                }
                            },
                            onDragCancel = {
                                dragY = 0f
                                verticalControl = null
                            },
                        ) { _, amount ->
                            if (expanded && verticalControl != null) {
                                // Up = increase, down = decrease. A full-height swipe
                                // changes roughly the full available range.
                                verticalControlDelta -= amount / boxHeight.toPx().coerceAtLeast(1f)
                                val value = (verticalControlStart + verticalControlDelta).coerceIn(0f, 1f)
                                verticalControlPercent = (value * 100f).toInt()
                                when (verticalControl) {
                                    VerticalPlayerControl.BRIGHTNESS -> setBrightness(context, value)
                                    VerticalPlayerControl.VOLUME -> setVolume(context, value)
                                    null -> Unit
                                }
                            } else {
                                // In compact mode the same gesture keeps its existing
                                // purpose: drag down to dock the player.
                                dragY = (dragY + amount).coerceAtLeast(0f)
                            }
                        }
                    }
                    .pointerInput(channel, isLive, onChannelStep) {
                        if (isLive && onChannelStep != null) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    // Κατώφλι στο ΕΝΑ ΠΕΜΠΤΟ του πλάτους — ίδια
                                    // λογική με το dragY: αναλογικό στην οθόνη,
                                    // όχι σταθερά pixel.
                                    val threshold = widthPx / 5f
                                    if (dragX <= -threshold) {
                                        pendingChannelTransitionDirection =
                                            LiveChannelTransitionMotion.direction(1)
                                        pendingChannelTransitionVersion++
                                        onChannelStep(1)
                                    } else if (dragX >= threshold) {
                                        pendingChannelTransitionDirection =
                                            LiveChannelTransitionMotion.direction(-1)
                                        pendingChannelTransitionVersion++
                                        onChannelStep(-1)
                                    }
                                    dragX = 0f
                                },
                                onDragCancel = { dragX = 0f },
                            ) { _, amount -> dragX += amount }
                        }
                    }
                    .pointerInput(channel, isLive) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                if (isLive) return@detectTapGestures
                                val forward = offset.x > widthPx / 2f
                                if (forward != seekFeedbackForward) seekFeedbackMs = 0L
                                seekFeedbackForward = forward
                                seekFeedbackMs += 10_000L
                                engine.seekBy(if (forward) 10_000L else -10_000L)
                            }
                        )
                    }
            )

            if (seekFeedbackMs != 0L) {
                Box(
                    Modifier
                        .align(if (seekFeedbackForward) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(horizontal = 36.dp)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = .55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (if (seekFeedbackForward) "+" else "−") + stringResource(
                            R.string.player_seek_seconds,
                            seekFeedbackMs / 1_000L,
                        ),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            verticalControl?.let { control ->
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = .76f))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(
                            if (control == VerticalPlayerControl.BRIGHTNESS) {
                                R.string.player_brightness
                            } else {
                                R.string.player_volume
                            }
                        ),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "$verticalControlPercent%",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            // Συννεφάκι με το όνομα του καναλιού μετά από σύρσιμο.
            if (channelToast != null) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = .70f))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        channelToast.orEmpty(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (failed || state.error != null) {
                Text(
                    stringResource(
                        if (failed) R.string.player_source_unavailable
                        else R.string.player_playback_failed
                    ),
                    color = IptvColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            subtitleMessage?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = .76f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }

            MobilePlayerControls(
                engine = engine,
                state = state,
                isLive = isLive,
                visible = controlsVisible,
                expanded = expanded,
                scrubPositionMs = shownPositionMs,
                onScrubStart = { scrubbing = true; scrubMs = state.positionMs },
                onScrub = { scrubMs = it.coerceIn(0L, state.durationMs) },
                onScrubEnd = {
                    engine.seekTo(scrubMs)
                    scrubbing = false
                    interact()
                },
                // Το «⌄» ΜΑΖΕΥΕΙ, δεν κλείνει. Το βέλος δείχνει προς τα κάτω
                // επειδή εκεί πάει ο player — και εκεί συνεχίζει να παίζει. Το
                // κλείσιμο είναι το «×» της λωρίδας.
                onClose = { if (expanded) expanded = false else collapsed = true },
                onToggleExpanded = { expanded = !expanded; interact() },
                onOpenSubtitles = { openMenu = PlayerMenu.SUBTITLES },
                onOpenSettings = { settingsOpen = true },
                aspectMode = aspectMode,
                onOpenAspectRatio = { aspectMenuOpen = true },
                onOpenSleep = { openMenu = PlayerMenu.SLEEP },
                onInteract = interact,
                onPlayNext = onPlayNext,
                onPlayPrevious = onPlayPrevious,
                modifier = Modifier.fillMaxSize()
            )

            if (offerNextEpisode) {
                MobileNextEpisodeOffer(
                    title = nextTitle.orEmpty(),
                    imageUrl = nextImageUrl,
                    autoPlayInSeconds = NextEpisodePolicy.autoPlayInSeconds(
                        state.positionMs,
                        state.durationMs,
                    ),
                    onPlay = { onPlayNext?.invoke() },
                    onDismiss = { nextOfferDismissed = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }

        // ---- Περιεχόμενο που κυλά ΚΑΤΩ από τον sticky player ----
        if (!expanded) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScrollState)
                    .padding(top = stickyPlayerHeight)
            ) {
                if (isLive) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            title,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 25.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                subtitle,
                                color = IptvColors.TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                MobilePlayerContextContent(
                    playing = channel,
                    infoChannel = infoChannel,
                    metadata = metadata,
                    related = relatedItems,
                    seasons = seasons,
                    playingQuality = qualityLabel(state.quality.height),
                    onPlay = onPlayContextItem,
                )
            }
        }
    }

    // Τα μενού ζωγραφίζονται ΕΞΩ από το κουτί της εικόνας: σε μαζεμένη κατάσταση
    // το κουτί είναι μόνο το πάνω τρίτο της οθόνης, και ένας διάλογος μέσα του θα
    // ήταν στριμωγμένος σε 16:9.
    if (settingsOpen) {
        PlayerChoiceMenu(
            title = stringResource(R.string.player_settings),
            options = listOf(
                stringResource(R.string.player_resolution) to false,
                stringResource(R.string.player_speed_value, localizedPlaybackSpeed(speed)) to false,
                stringResource(R.string.player_audio_language) to false,
            ),
            onSelect = { index ->
                settingsOpen = false
                when (index) {
                    0 -> openMenu = PlayerMenu.QUALITY
                    1 -> openMenu = PlayerMenu.SPEED
                    else -> openMenu = PlayerMenu.AUDIO
                }
            },
            onDismiss = { settingsOpen = false; interact() }
        )
    }

    if (aspectMenuOpen) {
        val aspectModes = AspectMode.entries
        PlayerChoiceMenu(
            title = stringResource(R.string.player_aspect_ratio),
            options = aspectModes.map { stringResource(it.labelRes()) to (it == aspectMode) },
            onSelect = { index ->
                aspectMode = aspectModes[index]
                aspectMenuOpen = false
                interact()
            },
            onDismiss = { aspectMenuOpen = false; interact() },
        )
    }

    PlayerMenuHost(
        open = openMenu,
        audioTracks = state.audioTracks,
        subtitleTracks = state.subtitleTracks,
        videoTracks = { engine.videoTracks() },
        aspectMode = aspectMode,
        speed = speed,
        sleepMinutes = sleepMinutes,
        subtitleSize = subtitleSize,
        subtitleBackground = subtitleBackground,
        subtitleBold = subtitleBold,
        subtitleQuery = SubtitleSearchPolicy.fromPlayback(
            channel = channel,
            seriesTitle = infoChannel.name,
            yearHint = metadata?.year?.takeIf(String::isNotBlank) ?: infoChannel.year,
        ).displayQuery(),
        searchSubtitles = if (isLive) null else searchSubtitles,
        onAutoFetchSubtitles = if (isLive) null else fetchSubtitles?.let { fetch ->
            {
                if (!fetchingSubtitles) {
                    fetchingSubtitles = true
                    openMenu = null
                    scope.launch {
                        subtitleMessage = runCatching { fetch(engine) }
                            .getOrElse { subtitleDownloadFailed }
                        fetchingSubtitles = false
                    }
                }
            }
        },
        onSelectAudio = { engine.selectAudio(it) },
        onSelectSubtitle = { engine.selectSubtitle(it) },
        onSelectVideo = { engine.selectVideo(it) },
        onSelectAspect = { aspectMode = it; openMenu = null },
        onSelectSpeed = { speed = it; engine.setSpeed(it); openMenu = null },
        onSelectSleep = { sleepMinutes = it; openMenu = null },
        onSubtitleSize = { subtitleSize = it; store.subtitleSizePercent = it },
        onSubtitleBackground = { subtitleBackground = it; store.subtitleBackground = it },
        onSubtitleBold = { subtitleBold = it; store.subtitleBold = it },
        onSubtitleChosen = { choice ->
            openMenu = null
            scope.launch {
                subtitleMessage = runCatching {
                    applySubtitle?.invoke(engine, choice)
                        ?: subtitleApplyUnavailable
                }.getOrElse { subtitleDownloadFailed }
            }
        },
        onDismiss = { openMenu = null; interact() },
    )
}
