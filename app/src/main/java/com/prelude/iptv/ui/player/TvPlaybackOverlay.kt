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
import kotlinx.coroutines.CancellationException
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
    val state by engine.state.collectAsState()
    var failed by remember(channel) { mutableStateOf(false) }

    // Ζωντανή ροή: δεν έχει αρχή, τέλος, ούτε «πού είχα μείνει».
    val isLive = channel.kind == "live"

    // Το rememberUpdatedState κρατά την τελευταία έκδοση των callbacks χωρίς να
    // ξαναξεκινά η αναπαραγωγή σε κάθε recomposition.
    val save by rememberUpdatedState(saveResumeMs)

    LaunchedEffect(channel) {
        failed = false
        val url = try {
            resolveUrl(channel)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.w(
                "PreludePlayback",
                "Η επίλυση διεύθυνσης απέτυχε για «${channel.name}» (${channel.kind})",
                error
            )
            ""
        }
        if (url.isBlank()) {
            // ΤΟ ΠΡΟΒΛΗΜΑ ΕΙΝΑΙ ΠΡΙΝ ΤΗ ΜΗΧΑΝΗ.
            //
            // Εδώ δεν έχει τρέξει ούτε ExoPlayer ούτε LibVLC: δεν υπάρχει
            // διεύθυνση να ανοίξει. Το παλιό μήνυμα («Δεν ήταν δυνατή η
            // αναπαραγωγή») ήταν ίδιο με του σφάλματος αναπαραγωγής, και έκρυβε
            // ακριβώς αυτή τη διάκριση — ψάχναμε τη μηχανή ενώ έφταιγε ο κατάλογος.
            android.util.Log.w(
                "PreludePlayback",
                "Κενή διεύθυνση για «${channel.name}» · kind=${channel.kind} · " +
                    "url='${channel.url}' cmd='${channel.cmd}'"
            )
            failed = true
        }
        // Το `live` δεν αφορά τη διεπαφή: λέει στη μηχανή πόσο να αποθηκεύει.
        else engine.open(
            url,
            resumeMs = if (isLive) 0L else loadResumeMs(channel),
            live = isLive,
        )
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
            engine.release()
        }
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
                onChannelStep = onChannelStep,
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

