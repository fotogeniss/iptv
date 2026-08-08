package com.prelude.iptv.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.player.NextEpisodePolicy
import com.prelude.iptv.player.PlaybackEngine
import com.prelude.iptv.player.PlaybackQualityPolicy
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.requestFocusWithRetry
import kotlin.math.roundToInt

/**
 * Το ΕΝΑ επίπεδο βίντεο της εφαρμογής.
 *
 * Κρατά την επιφάνεια προβολής και τα χειριστήρια, και μεταβαίνει ομαλά ανάμεσα
 * σε δύο καταστάσεις: «ένθετο» (μικρό, μέσα σε μια οθόνη περιήγησης) και «πλήρης
 * οθόνη». Η μετάβαση ΔΕΝ σταματά τη ροή: αλλάζει μόνο το μέγεθος του δοχείου,
 * ενώ η [PlaybackEngine] και το TextureView παραμένουν τα ίδια.
 *
 * Γι' αυτό είναι ξεχωριστό, επαναχρησιμοποιήσιμο στοιχείο και όχι κώδικας μέσα
 * σε μια οθόνη: στόχος είναι να το χρησιμοποιούν ΟΛΕΣ οι διαδρομές (ζωντανά,
 * ταινίες, σειρές), ώστε να υπάρχει μία εμπειρία αναπαραγωγής — αυτό ήταν και το
 * πρόβλημα με τον ξεχωριστό player: κάθε νέα λειτουργία έπρεπε να γραφτεί δύο φορές.
 */
/** Πόσο μένουν οι πληροφορίες χωρίς πάτημα πριν σβήσουν μόνες τους. */
private const val CONTROLS_TIMEOUT_MS = 4_000L

/**
 * Ανώτατος χρόνος που δείχνουμε παγωμένο καρέ.
 *
 * Λίγο πάνω από τη διάρκεια της μεγέθυνσης (360ms). Αν η νέα επιφάνεια αργήσει
 * περισσότερο, προτιμότερο να δει κανείς ότι κάτι φορτώνει παρά μια ακίνητη
 * εικόνα που παριστάνει τη ζωντανή.
 */
private const val FREEZE_MAX_MS = 900L

/** Ένας και μόνο προορισμός focus όταν εμφανίζεται ξανά το TV chrome. */
private enum class PlayerFocusTarget { PLAY, PROGRESS }

// ΚΑΤΕΥΘΥΝΣΗ ΑΛΛΑΓΗΣ ΚΑΝΑΛΙΟΥ.
//
// Το σκέτο «-1» και «+1» δεν λέει τίποτα στο σημείο που γράφεται, και ακριβώς
// γι' αυτό ήταν ανάποδα: το CH+ έστελνε -1, δηλαδή στο ΠΡΟΗΓΟΥΜΕΝΟ κανάλι.
// Με όνομα, η ανάποδη σύνδεση φαίνεται με το μάτι.
//
// Ο κανόνας είναι αυτός κάθε τηλεχειριστηρίου: CH+ = επόμενο κανάλι της λίστας.
private const val CHANNEL_STEP_NEXT = 1
private const val CHANNEL_STEP_PREVIOUS = -1

// Τα κουμπιά, τα μενού, οι μορφοποιήσεις και οι απαριθμήσεις ζουν στο
// PlayerControls.kt — δες εκεί το γιατί.

@Composable
fun PlayerHost(
    engine: PlaybackEngine,
    /** Τίτλος που δείχνεται στη μπάρα (π.χ. όνομα καναλιού). */
    title: String,
    /** Δευτερεύουσα γραμμή (π.χ. τι παίζει τώρα από το EPG). */
    subtitle: String,
    /** Θέση/μέγεθος όταν είναι ένθετο, σε συντεταγμένες ρίζας. */
    inlineBounds: Rect,
    fullscreen: Boolean,
    /** Πλήρες μέγεθος του διαθέσιμου χώρου, σε pixels. */
    fullWidthPx: Float,
    fullHeightPx: Float,
    onExitFullscreen: () -> Unit,
    /**
     * Άνοιγμα λίστας επιλογής (π.χ. κανάλια) μέσα στον player.
     *
     * Τα βελάκια ΔΕΝ αλλάζουν απευθείας περιεχόμενο: ανοίγουν τη λίστα και ο
     * χρήστης διαλέγει. Έτσι δεν πέφτεις κατά λάθος σε άλλο κανάλι και βλέπεις τι
     * επιλέγεις πριν το επιλέξεις.
     */
    onOpenList: (() -> Unit)? = null,
    /**
     * Αλλαγή καναλιού με τα ΠΛΗΚΤΡΑ ΚΑΝΑΛΙΟΥ του τηλεχειριστηρίου (CH+/CH−),
     * όπως σε κανονική τηλεόραση. Το D-pad ΔΕΝ αλλάζει κανάλι — ανοίγει λίστα.
     */
    onChannelStep: ((Int) -> Unit)? = null,
    /** Optional one-frame capture bridge used only by visual channel handoffs. */
    frameCapture: PlayerVideoFrameCapture? = null,
    /** Non-interactive visual drawn above video and below subtitles/chrome. */
    videoOverlay: (@Composable () -> Unit)? = null,
    /**
     * Αυξήστε το όταν κλείνει μια επίστρωση που είχε πάρει το focus (π.χ. η λίστα
     * καναλιών), ώστε ο player να το ξαναπάρει.
     *
     * Χρειάζεται γιατί ο player δεν βλέπει τις επιστρώσεις που ζωγραφίζει η οθόνη
     * από πάνω του: όταν εκείνες κλείνουν, το focus χάνεται μαζί τους και τίποτα
     * δεν το ζητά πίσω. Ίδιο μοτίβο με την οθόνη λεπτομερειών.
     */
    focusEpoch: Int = 0,
    /**
     * true όσο η οθόνη έχει ανοιχτή δική της επίστρωση ΠΑΝΩ από τον player
     * (λίστα καναλιών, πρόγραμμα) που κρατά το focus.
     *
     * ΓΙΑΤΙ ΧΡΕΙΑΖΕΤΑΙ: ο player έχει ήδη το [focusEpoch] για το ΚΛΕΙΣΙΜΟ της
     * επίστρωσης, αλλά τίποτα για το ΔΙΑΣΤΗΜΑ που είναι ανοιχτή — και σε αυτό το
     * διάστημα εξακολουθούσε να τρέχει το χρονόμετρο αδράνειας. Τέσσερα
     * δευτερόλεπτα αφότου άνοιγε η λίστα, τα χειριστήρια «κρύβονταν» από κάτω, το
     * `controlsVisible` άλλαζε, και ο κανόνας του focus έστελνε το focus πίσω
     * στην εικόνα — μέσα από τη λίστα, χωρίς να τη ρωτήσει κανείς.
     *
     * Το συμπτωματικό ήταν ότι το ίδιο δεν συνέβαινε ανοίγοντας τη λίστα με το
     * ΔΕΞΙ βελάκι: εκεί τα χειριστήρια είναι ήδη κρυφά, το χρονόμετρο δεν τρέχει
     * καν, και δεν υπήρχε τίποτα να κλέψει το focus. Ίδια λίστα, δύο δρόμοι, ένα
     * χρονόμετρο που έκανε τη διαφορά.
     *
     * Δεν φτάνει η [openMenu]: εκείνη ξέρει μόνο τα ΔΙΚΑ ΤΟΥ μενού. Ό,τι
     * ζωγραφίζει η οθόνη από πάνω του, ο player δεν το βλέπει — πρέπει να του
     * ειπωθεί.
     */
    overlayOpen: Boolean = false,
    /**
     * Αγαπημένο: null σημαίνει ότι η διαδρομή δεν υποστηρίζει αγαπημένα και το
     * κουμπί δεν εμφανίζεται καθόλου — καλύτερα από ένα κουμπί που δεν κάνει τίποτα.
     */
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    /** Τίτλος επόμενου επεισοδίου. null = δεν υπάρχει συνέχεια. */
    nextTitle: String? = null,
    /** Στιγμιότυπο επόμενου επεισοδίου· κενό = δείχνουμε μόνο κείμενο. */
    nextImageUrl: String? = null,
    onPlayNext: (() -> Unit)? = null,
    /**
     * Λήψη υποτίτλων από το διαδίκτυο. null = η διαδρομή δεν το υποστηρίζει
     * (ζωντανά) ή λείπουν τα διαπιστευτήρια.
     *
     * Επιστρέφει μήνυμα για τον χρήστη — η αποτυχία εδώ είναι συνηθισμένη
     * (δεν υπάρχει υπότιτλος για τον τίτλο) και δεν είναι σφάλμα.
     */
    onFetchSubtitles: (suspend () -> String)? = null,
    /**
     * Χειροκίνητη αναζήτηση: επιστρέφει υποψήφιους για να διαλέξει ο χρήστης.
     *
     * Η αυτόματη ([onFetchSubtitles]) παίρνει το πρώτο αποτέλεσμα, που συχνά
     * είναι λάθος συγχρονισμένο ή για άλλη έκδοση της ταινίας. Χωρίς τη
     * χειροκίνητη, ο χρήστης δεν έχει καμία διέξοδο όταν το πρώτο δεν κάνει.
     */
    onSearchSubtitles: (suspend (String) -> List<ExternalSubtitle>)? = null,
    /** Κατεβάζει και εφαρμόζει τον επιλεγμένο· επιστρέφει μήνυμα για τον χρήστη. */
    onApplySubtitle: (suspend (ExternalSubtitle) -> String)? = null,
    /**
     * true για ταινίες/επεισόδια: εμφανίζει μπάρα προόδου και ενεργοποιεί
     * αναζήτηση με ◄► (±10 δευτερόλεπτα). Στα ζωντανά δεν έχει νόημα.
     */
    seekable: Boolean = false,
    errorText: String? = null,
    /** Επιπλέον κουμπιά της συγκεκριμένης οθόνης (π.χ. «Κανάλια», «Πρόγραμμα»). */
    extraActions: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val state by engine.state.collectAsState()
    val progress by animateFloatAsState(
        if (fullscreen) 1f else 0f,
        tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "playerExpand"
    )

    // Χειριστήρια: εμφανίζονται σε κάθε αλλαγή και σβήνουν μόνα τους.
    var controlsVisible by remember { mutableStateOf(true) }
    val focus = remember { FocusRequester() }
    var aspectMode by remember { mutableStateOf(AspectMode.FIT) }
    // Ανοιχτό μενού επιλογών (ήχος/υπότιτλοι/αναλογία), null = κλειστό.
    var openMenu by remember { mutableStateOf<PlayerMenu?>(null) }
    val controlsFocus = remember { FocusRequester() }
    val progressFocus = remember { FocusRequester() }
    val tracksFocus = remember { FocusRequester() }
    val aspectFocus = remember { FocusRequester() }
    val qualityFocus = remember { FocusRequester() }
    val speedFocus = remember { FocusRequester() }
    val sleepFocus = remember { FocusRequester() }
    var menuReturnTarget by remember { mutableStateOf<PlayerMenu?>(null) }
    var focusTarget by remember { mutableStateOf(PlayerFocusTarget.PLAY) }
    val scope = rememberCoroutineScope()
    // Χρόνος πατήματος BACK, για διάκριση σύντομου/παρατεταμένου.
    var backPressStartMs by remember { mutableLongStateOf(0L) }

    val qualityLabel = remember(state.quality) { PlaybackQualityPolicy.label(state.quality) }
    // Υπολογίζεται μόνο όταν αλλάζουν τα κομμάτια (η ποιότητα ενημερώνεται τότε),
    // όχι σε κάθε recomposition: το videoTracks() ρωτά τη μηχανή αναπαραγωγής.
    val hasMultipleQualities = remember(state.quality) { engine.videoTracks().size > 1 }

    // Ταχύτητα και χρονοδιακόπτης: υπήρχαν στον παλιό player και χάθηκαν στη
    // μετάβαση. Ζουν εδώ, στο κοινό επίπεδο, ώστε να τα έχουν ΟΛΕΣ οι διαδρομές.
    var speed by remember { mutableStateOf(1f) }
    var sleepMinutes by remember { mutableStateOf(0) }
    var sleepRemainingMs by remember { mutableLongStateOf(0L) }
    var fetchingSubtitles by remember { mutableStateOf(false) }
    // Μήνυμα αποτελέσματος λήψης υποτίτλων· σβήνει μόνο του.
    var subtitleMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(subtitleMessage) {
        if (subtitleMessage != null) {
            kotlinx.coroutines.delay(3_500)
            subtitleMessage = null
        }
    }

    // Χρονοδιακόπτης ύπνου: μετρά αντίστροφα και στο τέλος κάνει παύση αντί για
    // έξοδο. Αν κάποιος είναι ακόμη ξύπνιος, πατά ένα κουμπί και συνεχίζει —
    // αντί να βρει την εφαρμογή κλειστή και να ψάχνει πού είχε μείνει.
    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes <= 0) {
            sleepRemainingMs = 0L
            return@LaunchedEffect
        }
        var remaining = sleepMinutes * 60_000L
        while (remaining > 0) {
            sleepRemainingMs = remaining
            kotlinx.coroutines.delay(1_000)
            remaining -= 1_000
        }
        sleepRemainingMs = 0L
        sleepMinutes = 0
        engine.pause()
    }

    // Συγχρονισμός συχνότητας οθόνης — δες PlayerFrameRateSync.kt για το γιατί.
    PlayerFrameRateSync(active = fullscreen, contentFrameRate = state.quality.frameRate)

    // ---- ΕΜΦΑΝΙΣΗ ΥΠΟΤΙΤΛΩΝ ----
    // Διαβάζονται μία φορά από τις ρυθμίσεις και μετά ζουν εδώ: ο χρήστης τα
    // αλλάζει ΜΕΣΑ στην αναπαραγωγή και πρέπει να βλέπει το αποτέλεσμα αμέσως.
    // Γράφονται πίσω στον δίσκο ώστε να ισχύουν και στην επόμενη ταινία.
    val appContext = LocalContext.current.applicationContext
    val subtitleStore = remember(appContext) { PlaylistStore(appContext) }
    var subtitleSize by remember { mutableIntStateOf(subtitleStore.subtitleSizePercent) }
    var subtitleBackground by remember { mutableStateOf(subtitleStore.subtitleBackground) }
    var subtitleBold by remember { mutableStateOf(subtitleStore.subtitleBold) }

    LaunchedEffect(fullscreen) {
        if (fullscreen) {
            focusTarget = PlayerFocusTarget.PLAY
            controlsVisible = true
        }
    }

    // ---------------------------------------------------------------------
    // ΕΝΑ ΣΗΜΕΙΟ ΑΠΟΦΑΣΙΖΕΙ ΠΟΥ ΠΑΕΙ ΤΟ FOCUS
    // ---------------------------------------------------------------------
    // ΤΙ ΠΗΓΕ ΣΤΡΑΒΑ: υπήρχαν ΔΥΟ αιτήματα focus που ξεκινούσαν μαζί — ένα για
    // την εικόνα και ένα για τα κουμπιά. Και τα δύο «πετύχαιναν», γιατί το
    // requestFocus() δεν παραπονιέται όταν ο κόμβος υπάρχει· κέρδιζε όποιο
    // τύχαινε να τρέξει τελευταίο. Άλλοτε το focus κατέληγε στα κουμπιά και η
    // πλοήγηση δούλευε, άλλοτε στην εικόνα και ο player έμοιαζε νεκρός.
    //
    // Δύο διαφορετικά σημεία που διεκδικούν το ίδιο πράγμα δεν δίνουν σφάλμα —
    // δίνουν συμπεριφορά που αλλάζει από άνοιγμα σε άνοιγμα, και ακριβώς γι' αυτό
    // ήταν τόσο δύσκολο να πιαστεί.
    //
    // Ο κανόνας τώρα είναι ένας και ρητός:
    //   χειριστήρια ορατά  -> focus στα κουμπιά  (βελάκια = πλοήγηση)
    //   χειριστήρια κρυφά  -> focus στην εικόνα  (βελάκια = συντομεύσεις)
    //
    // Το focus δεν μένει ΠΟΤΕ πουθενά: κάθε κατάσταση έχει τον προορισμό της.
    // Έχει ο χρήστης μετακινηθεί ρητά μέσα στα χειριστήρια; Μηδενίζεται κάθε
    // φορά που κρύβονται, ώστε το επόμενο άνοιγμα να ξεκινά πάλι «ανέγγιχτο».
    var controlsEngaged by remember { mutableStateOf(false) }
    var lastFocusEpoch by remember { mutableIntStateOf(focusEpoch) }
    LaunchedEffect(fullscreen, controlsVisible, focusTarget, focusEpoch, overlayOpen) {
        if (!fullscreen) return@LaunchedEffect
        // Όσο μια επίστρωση της οθόνης κρατά το focus, ο player δεν το διεκδικεί.
        // Το ξαναπαίρνει όταν εκείνη κλείσει, μέσω του focusEpoch.
        if (overlayOpen) return@LaunchedEffect
        if (focusEpoch != lastFocusEpoch) {
            // Επιστροφή από επίστρωση που έκλεισε. Ο χρήστης ήταν ήδη «μέσα» στα
            // εργαλεία πριν ανοίξει — δεν τον ξαναρωτάμε από την αρχή.
            lastFocusEpoch = focusEpoch
            focusTarget = PlayerFocusTarget.PLAY
            controlsVisible = true
            controlsEngaged = true
        }
        if (controlsVisible) {
            controlsEngaged = true
            when (focusTarget) {
                PlayerFocusTarget.PLAY -> controlsFocus.requestFocusWithRetry()
                PlayerFocusTarget.PROGRESS -> progressFocus.requestFocusWithRetry()
            }
        } else {
            focus.requestFocusWithRetry()
        }
    }

    // ---- ΠΑΓΩΜΕΝΟ ΚΑΡΕ ΓΙΑ ΤΗ ΜΕΤΑΒΑΣΗ ----
    // Το κρατά η επιφάνεια που φεύγει· σβήνει μόλις η καινούργια βγάλει εικόνα.
    var freezeFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var freezeVisible by remember { mutableStateOf(false) }
    val freezeAlpha by animateFloatAsState(
        if (freezeVisible) 1f else 0f,
        tween(durationMillis = 200),
        label = "freezeFade"
    )
    LaunchedEffect(freezeFrame) {
        if (freezeFrame == null) return@LaunchedEffect
        freezeVisible = true
        val framesAtStart = state.renderedFrames
        // Περιμένουμε την πρώτη εικόνα της νέας επιφάνειας — ΜΕ ΟΡΙΟ.
        //
        // Αν η καινούργια επιφάνεια δεν βγάλει ποτέ καρέ (σφάλμα ροής, κανάλι
        // που πέθανε στη μετάβαση), χωρίς όριο θα έμενε για πάντα μια παγωμένη
        // εικόνα που μοιάζει με ζωντανή. Αυτό είναι χειρότερο από αναβόσβημα:
        // δείχνει ότι όλα πάνε καλά ενώ δεν πάνε.
        kotlinx.coroutines.withTimeoutOrNull(FREEZE_MAX_MS) {
            // Απευθείας από τη μηχανή, όχι από το snapshot της σύνθεσης: μας
            // ενδιαφέρει το γεγονός τη στιγμή που συμβαίνει.
            engine.state.first { it.renderedFrames > framesAtStart }
        }
        freezeVisible = false
        kotlinx.coroutines.delay(220)
        freezeFrame = null
    }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) controlsEngaged = false
    }
    // ΑΠΟΚΡΥΨΗ ΜΕ ΧΡΟΝΟΜΕΤΡΟ ΑΔΡΑΝΕΙΑΣ.
    //
    // Πριν, η συνθήκη ήταν «κρύψε τα αν ΔΕΝ έχουν focus». Όταν όμως φτιάξαμε τα
    // χειριστήρια να παίρνουν πάντα το focus μόλις εμφανιστούν, η συνθήκη έγινε
    // μονίμως ψευδής — και οι πληροφορίες δεν έφευγαν ποτέ. Δύο σωστές αλλαγές
    // που μαζί έδωσαν λάθος αποτέλεσμα.
    //
    // Τώρα μετράμε ΑΔΡΑΝΕΙΑ, όχι focus: κάθε πάτημα ανανεώνει τον μετρητή, και
    // μετά από 4 δευτερόλεπτα ησυχίας τα χειριστήρια φεύγουν. Ανοιχτό μενού
    // (ήχος/υπότιτλοι) τα κρατά — εκεί ο χρήστης διαβάζει, δεν πατά.
    var lastInteractionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controlsVisible, fullscreen, openMenu, overlayOpen, lastInteractionMs) {
        // Ανοιχτή επίστρωση της οθόνης = ο χρήστης διαλέγει εκεί. Ό,τι ισχύει για
        // τα δικά μας μενού ισχύει και για τα δικά της: δεν μετράμε αδράνεια όταν
        // η αδράνεια είναι φαινομενική.
        if (controlsVisible && fullscreen && openMenu == null && !overlayOpen) {
            kotlinx.coroutines.delay(CONTROLS_TIMEOUT_MS)
            // Το focus το αναλαμβάνει το effect πιο πάνω — εδώ αλλάζουμε ΜΟΝΟ
            // την κατάσταση. Αν το ζητούσαμε κι εδώ, θα ξαναφτιάχναμε ακριβώς το
            // πρόβλημα των δύο ανταγωνιστικών αιτημάτων.
            controlsVisible = false
        }
    }

    if (inlineBounds == Rect.Zero) return

    fun lerp(from: Float, to: Float) = from + (to - from) * progress
    val x = lerp(inlineBounds.left, 0f)
    val y = lerp(inlineBounds.top, 0f)
    val w = lerp(inlineBounds.width, fullWidthPx)
    val h = lerp(inlineBounds.height, fullHeightPx)

    Box(
        modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(with(density) { w.toDp() }, with(density) { h.toDp() })
            .clip(RoundedCornerShape((12 * (1f - progress)).dp))
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (!fullscreen) return@onPreviewKeyEvent false

                // ---- BACK: προστασία από κατά λάθος πάτημα ----
                // Σύντομο BACK: εμφανίζει/κρύβει τα χειριστήρια. Παρατεταμένο:
                // έξοδος. Έτσι ένα τυχαίο πάτημα δεν σου κόβει την ταινία.
                // (Μετράμε KeyDown -> KeyUp, όπως και στο OK.)
                if (event.key == Key.Back || event.key == Key.Escape) {
                    return@onPreviewKeyEvent when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (backPressStartMs == 0L) {
                                backPressStartMs = android.os.SystemClock.uptimeMillis()
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            val held = if (backPressStartMs == 0L) 0L
                            else android.os.SystemClock.uptimeMillis() - backPressStartMs
                            backPressStartMs = 0L
                            val longEnough =
                                held >= android.view.ViewConfiguration.getLongPressTimeout().toLong()
                            when {
                                longEnough -> onExitFullscreen()
                                controlsVisible -> controlsVisible = false
                                // Μόνο κατάσταση — το focus το αναλαμβάνει το ένα
                                // effect που αποφασίζει γι' αυτό.
                                else -> controlsVisible = true
                            }
                            true
                        }
                        else -> false
                    }
                }

                // ---- ΤΟ ΟΚ ΠΟΥ ΑΝΟΙΓΕΙ, ΠΡΙΝ ΑΠΟ ΟΤΙΔΗΠΟΤΕ ΑΛΛΟ ----
                //
                // ΓΙΑΤΙ ΕΠΕΜΕΝΕ ΤΟ ΠΡΟΒΛΗΜΑ: κατανάλωνα μόνο το KeyDown. Αλλά το
                // `clickable` του Compose ενεργοποιείται στο KeyUp — η απελευθέρωση
                // περνούσε ανενόχλητη και πατούσε το εστιασμένο κουμπί. Έβλεπες
                // «Παύση» αν και το πάτημα υποτίθεται ότι είχε καταναλωθεί.
                //
                // Μισό πλήκτρο δεν καταναλώνεται: ή όλο ή τίποτα. Εδώ σβήνουμε
                // ΚΑΙ τα δύο μισά, και σημαδεύουμε στην απελευθέρωση.
                val isConfirm = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter || event.key == Key.NumPadEnter
                if (isConfirm && !controlsEngaged) {
                    if (event.type == KeyEventType.KeyUp) {
                        focusTarget = PlayerFocusTarget.PLAY
                        controlsVisible = true
                        controlsEngaged = true
                        lastInteractionMs = android.os.SystemClock.uptimeMillis()
                    }
                    return@onPreviewKeyEvent true
                }

                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                // Κάθε πάτημα ανανεώνει το χρονόμετρο απόκρυψης: όσο ο χρήστης
                // ασχολείται, οι πληροφορίες μένουν.
                lastInteractionMs = android.os.SystemClock.uptimeMillis()

                // ΑΛΛΑΓΗ ΚΑΝΑΛΙΟΥ: ΠΑΝΤΑ, ΑΝΕΞΑΡΤΗΤΑ ΑΠΟ ΤΟ ΠΟΥ ΕΙΝΑΙ ΤΟ FOCUS.
                //
                // Τα CH+/CH− δεν ανταγωνίζονται τίποτα — δεν υπάρχει κουμπί που να
                // τα διεκδικεί. Βρίσκονταν όμως κάτω από τον έλεγχο του focus πιο
                // κάτω, οπότε μόλις εμφανίζονταν οι πληροφορίες (και τα κουμπιά
                // έπαιρναν το focus) η αλλαγή καναλιού σταματούσε να δουλεύει.
                when (event.key) {
                    Key.ChannelUp -> if (onChannelStep != null) {
                        onChannelStep(CHANNEL_STEP_NEXT); controlsVisible = true
                        return@onPreviewKeyEvent true
                    } else Unit
                    Key.ChannelDown -> if (onChannelStep != null) {
                        onChannelStep(CHANNEL_STEP_PREVIOUS); controlsVisible = true
                        return@onPreviewKeyEvent true
                    } else Unit
                    else -> Unit
                }

                // ΟΡΑΤΑ ΧΕΙΡΙΣΤΗΡΙΑ -> ΤΟ BOX ΔΕΝ ΑΓΓΙΖΕΙ ΤΙΠΟΤΑ.
                //
                // Ο έλεγχος ήταν «έχει focus η ΓΡΑΜΜΗ των κουμπιών;». Αλλά τα
                // χειριστήρια δεν είναι μόνο εκείνη η γραμμή: το «← Έξοδος» είναι
                // πάνω-αριστερά, η κάρτα επόμενου επεισοδίου κάτω-δεξιά. Μόλις το
                // focus πήγαινε σε ένα από αυτά, η σημαία γινόταν ψευδής και το
                // Box ξανάπαιρνε τον έλεγχο — καταπίνοντας το OK.
                //
                // Γι' αυτό το «Έξοδος» δεν έκανε τίποτα: το πάτημα δεν έφτανε ποτέ
                // στο κουμπί. Ο έλεγχος περιέγραφε μια ΘΕΣΗ, ενώ ο κανόνας μιλάει
                // για ΚΑΤΑΣΤΑΣΗ. Τώρα ρωτάμε αυτό που εννοούμε.
                //
                // Το onPreviewKeyEvent τρέχει από τη ρίζα προς τα κάτω, οπότε
                // αυτό το Box έβλεπε το πλήκτρο ΠΡΙΝ από το εστιασμένο κουμπί:
                // πατούσες δεξιά για να πας στο επόμενο κουμπί και άνοιγε η
                // λίστα καναλιών.
                if (controlsVisible) {
                    // Όσο το chrome φαίνεται, τα βελάκια ανήκουν αποκλειστικά στο
                    // εστιασμένο control. Το root δεν κάνει δεύτερη, κρυφή ενέργεια.
                    when (event.key) {
                        Key.DirectionUp,
                        Key.DirectionDown,
                        Key.DirectionLeft,
                        Key.DirectionRight -> {
                            controlsEngaged = true
                            return@onPreviewKeyEvent false
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                }
                // ΠΛΟΗΓΗΣΗ ΜΕΣΑ ΣΤΟΝ PLAYER
                //
                // Το κλειδί: το focus μένει στην ΕΙΚΟΝΑ. Τα κουμπιά δεν το
                // αρπάζουν μόλις εμφανιστούν — αλλιώς τα βελάκια μετακινούνταν
                // ανάμεσά τους και δεν γινόταν ποτέ αναζήτηση.
                //
                // ◄►  : ταινίες -> focus στη μπάρα· ζωντανά -> λίστα καναλιών
                // ▲▼  : εμφάνιση χειριστηρίων· αν ήδη φαίνονται, το focus κατεβαίνει
                //        σε αυτά (επιστρέφουμε false ώστε να δουλέψει η αναζήτηση focus)
                // CH+/− : αλλαγή καναλιού
                when (event.key) {
                    Key.DirectionLeft -> when {
                        seekable -> {
                            focusTarget = PlayerFocusTarget.PROGRESS
                            controlsVisible = true
                            controlsEngaged = true
                            true
                        }
                        // Στα ζωντανά ΚΑΙ τα δύο βελάκια ανοίγουν τη λίστα.
                        //
                        // Το δεξί το έκανε ήδη, το αριστερό επέστρεφε false — και
                        // τότε το focus κατέβαινε στα εργαλεία. Αν το αφήναμε έτσι,
                        // το ίδιο παράπονο θα ερχόταν αύριο για το αριστερό.
                        //
                        // Εκεί που δεν υπάρχει «πίσω» και «μπροστά», δεν υπάρχει
                        // λόγος τα δύο βελάκια να κάνουν διαφορετικά πράγματα.
                        onOpenList != null -> { onOpenList(); true }
                        else -> false
                    }
                    Key.DirectionRight -> when {
                        seekable -> {
                            focusTarget = PlayerFocusTarget.PROGRESS
                            controlsVisible = true
                            controlsEngaged = true
                            true
                        }
                        onOpenList != null -> { onOpenList(); true }
                        else -> false
                    }
                    Key.DirectionDown, Key.DirectionUp -> {
                        // ΤΟ ΚΑΤΩ ΣΗΜΑΙΝΕΙ «ΜΠΑΙΝΩ ΣΤΑ ΕΡΓΑΛΕΙΑ».
                        //
                        // Εδώ ήταν το σφάλμα: άνοιγε τα χειριστήρια αλλά ΔΕΝ
                        // δήλωνε ότι ο χρήστης μπήκε μέσα τους. Το επόμενο δεξί
                        // έβρισκε `controlsEngaged = false`, θεωρούσε ότι το focus
                        // είναι ακόμη στην εικόνα, και άνοιγε τη λίστα καναλιών
                        // αντί να πάει στο επόμενο κουμπί.
                        //
                        // Όταν τα χειριστήρια είναι κρυφά, το ΚΑΤΩ δεν έχει άλλη
                        // σημασία: μόνο «θέλω τα εργαλεία».
                        focusTarget = PlayerFocusTarget.PLAY
                        controlsVisible = true
                        controlsEngaged = true
                        true
                    }
                    else -> false
                }
            }
    ) {
        // ---- ΑΝΑΛΟΓΙΑ ΕΙΚΟΝΑΣ ----
        // Το σκέτο TextureView ΤΕΝΤΩΝΕΙ το βίντεο στο μέγεθός του — δεν κάνει
        // letterbox από μόνο του (σε αντίθεση με το PlayerView). Υπολογίζουμε
        // εμείς το ορθογώνιο προβολής, αλλιώς μια 4:3 μετάδοση θα φαινόταν
        // παραμορφωμένη μέσα σε 16:9 πλαίσιο.
        val containerAspect = if (h > 0f) w / h else 16f / 9f
        val sourceAspect = when (aspectMode) {
            AspectMode.FORCE_4_3 -> 4f / 3f
            AspectMode.FORCE_16_9 -> 16f / 9f
            else -> state.videoAspect.takeIf { it > 0f } ?: containerAspect
        }
        val videoModifier = when (aspectMode) {
            // Γέμισμα: πιάνει όλο το πλαίσιο (με κόψιμο άκρων).
            AspectMode.FILL -> Modifier.fillMaxSize()
            else -> {
                // Fit: κρατά την αναλογία και αφήνει μαύρες μπάρες όπου χρειάζεται.
                val fitByWidth = sourceAspect >= containerAspect
                val videoW = if (fitByWidth) w else h * sourceAspect
                val videoH = if (fitByWidth) w / sourceAspect else h
                Modifier.size(
                    with(density) { videoW.toDp() },
                    with(density) { videoH.toDp() }
                )
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // ---- ΠΟΙΑ ΕΠΙΦΑΝΕΙΑ, ΚΑΙ ΓΙΑΤΙ ΔΥΟ ----
            //
            // Το TextureView αλλάζει μέγεθος ομαλά — γι' αυτό το διαλέξαμε, ώστε
            // η μεγέθυνση από προεπισκόπηση σε πλήρη οθόνη να μη σταματά τη ροή.
            // Περνά όμως από τον compositor του GPU με έναν επιπλέον buffer, και
            // η στιγμή εμφάνισης κάθε καρέ δεν κλειδώνει τέλεια στο vsync. Αυτό
            // είναι το judder που έμεινε ΑΦΟΥ διορθώθηκε η συχνότητα οθόνης:
            // πλέον έρχονται τα σωστά καρέ, απλώς όχι σε ακριβώς σωστές στιγμές.
            //
            // Το SurfaceView συντίθεται απευθείας από το σύστημα και δίνει
            // ακριβές pacing, αλλά δεν αλλάζει μέγεθος το ίδιο καλά.
            //
            // Κρατάμε και τα δύο, ανάλογα με τη δουλειά:
            //   - Προεπισκόπηση: μικρή εικόνα, το judder δεν διακρίνεται· εκεί
            //     προέχει η ομαλή μεγέθυνση -> TextureView.
            //   - Πλήρης οθόνη: εκεί φαίνεται το judder· προέχει το pacing
            //     -> SurfaceView.
            //
            // Η εναλλαγή γίνεται με το ΠΟΥ ΠΑΕΙ, όχι όταν φτάσει: το `fullscreen`
            // αλλάζει στην ΑΡΧΗ της μεγέθυνσης, οπότε το στιγμιαίο κενό της
            // αλλαγής επιφάνειας πέφτει μέσα στην κίνηση που το κρύβει — αντί να
            // εμφανιστεί ως αναβόσβημα τη στιγμή που η εικόνα ησυχάζει.
            PlayerVideoSurface(
                engine = engine,
                keepScreenOn = state.playing,
                preferSmoothResize = !fullscreen,
                frameCapture = frameCapture,
                onLastFrame = { bitmap ->
                    if (bitmap != null) freezeFrame = bitmap.asImageBitmap()
                },
                modifier = videoModifier
            )
            // ---- ΚΑΛΥΨΗ ΤΗΣ ΕΝΑΛΛΑΓΗΣ ΕΠΙΦΑΝΕΙΑΣ ----
            //
            // Η μεγέθυνση αλλάζει TextureView σε SurfaceView. Ανάμεσά τους
            // μεσολαβεί ένα κενό καρέ: μαύρο αναβόσβημα ακριβώς τη στιγμή που η
            // εικόνα μεγαλώνει, δηλαδή εκεί που κοιτάζει το μάτι.
            //
            // Δεν προσθέτουμε εφέ για να κρύψουμε το πρόβλημα — δείχνουμε το
            // ΤΕΛΕΥΤΑΙΟ ΠΡΑΓΜΑΤΙΚΟ ΚΑΡΕ, με το ίδιο modifier που μεγαλώνει.
            // Έτσι η ακίνητη εικόνα μεγαλώνει ομαλά μαζί με το κάδρο και μετά
            // ξεθωριάζει πάνω στη ζωντανή. Ο θεατής δεν βλέπει μετάβαση· βλέπει
            // την ίδια εικόνα να μεγαλώνει.
            freezeFrame?.let { frame ->
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = videoModifier.graphicsLayer { alpha = freezeAlpha }
                )
            }
        }

        // Οι υπότιτλοι πάνω από την εικόνα, ΚΑΤΩ από τα χειριστήρια: όταν
        // εμφανίζεται η μπάρα δεν πρέπει να σκεπάζει τις γραμμές — γι' αυτό
        // ανεβαίνουν όσο φαίνεται.
        videoOverlay?.invoke()

        PlayerSubtitles(
            engine = engine,
            sizePercent = subtitleSize,
            background = subtitleBackground,
            bold = subtitleBold,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (controlsVisible && progress > 0.6f) 190.dp else 24.dp)
        )

        // Αποτέλεσμα λήψης υποτίτλων: πάνω-κέντρο, μακριά από τα χειριστήρια.
        subtitleMessage?.let { message ->
            Text(
                message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
                    .background(Color.Black.copy(alpha = 0.86f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        // ---- ΕΠΟΜΕΝΟ ΕΠΕΙΣΟΔΙΟ ----
        val hasNext = onPlayNext != null && nextTitle != null
        var nextOfferDismissed by remember(nextTitle) { mutableStateOf(false) }
        val offerNext = !nextOfferDismissed && NextEpisodePolicy.shouldOffer(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            hasNext = hasNext,
        )
        val autoPlayNext = !nextOfferDismissed && NextEpisodePolicy.shouldAutoPlay(
            state.positionMs,
            state.durationMs,
            hasNext,
        )
        // ΑΥΤΟΜΑΤΗ ΕΝΑΡΞΗ — ΜΟΝΟ ΟΣΟ ΠΑΙΖΕΙ.
        //
        // Αν έχεις κάνει παύση, δεν βλέπεις τους τίτλους: κάθεσαι. Το να ξεκινούσε
        // τότε μόνο του το επόμενο θα ήταν ενέργεια που δεν ζήτησες, τη στιγμή που
        // ρητά σταμάτησες.
        LaunchedEffect(
            state.playing,
            autoPlayNext,
        ) {
            if (!state.playing) return@LaunchedEffect
            if (!autoPlayNext) return@LaunchedEffect
            onPlayNext?.invoke()
        }
        if (offerNext && progress > 0.6f) {
            PlayerNextEpisodeCard(
                title = nextTitle.orEmpty(),
                imageUrl = nextImageUrl,
                autoPlayInSeconds = NextEpisodePolicy.autoPlayInSeconds(
                    state.positionMs, state.durationMs
                ),
                onPlay = { onPlayNext?.invoke() },
                onDismiss = { nextOfferDismissed = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 44.dp, bottom = if (controlsVisible) 200.dp else 44.dp)
            )
        }

        val message = errorText ?: state.error
        if (message != null) {
            Text(
                message,
                color = IptvColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (state.buffering) {
            Text(
                "Φόρτωση…",
                color = IptvColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Έξοδος πάνω-αριστερά: ρητός, ορατός τρόπος να βγεις — ώστε το BACK να
        // μπορεί να κάνει κάτι πιο ήπιο (εμφάνιση/απόκρυψη χειριστηρίων).
        if (progress > 0.6f && controlsVisible) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.72f),
                            1f to Color.Transparent
                        )
                    )
                    .padding(start = 32.dp, top = 22.dp, bottom = 34.dp, end = 32.dp)
            ) {
                PlayerActionButton(label = "← Έξοδος", onClick = onExitFullscreen)
            }
        }

        // Μπάρα χειριστηρίων — μόνο σε πλήρη οθόνη. Καθαρή ζωγραφική, δες
        // PlayerControls.kt· εδώ μένει η λογική πλήκτρων και focus.
        if (progress > 0.6f && controlsVisible) {
            PlayerControlsBar(
                title = title,
                subtitle = subtitle,
                qualityLabel = qualityLabel,
                playing = state.playing,
                seekable = seekable,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                aspectLabel = aspectMode.label,
                speedLabel = formatSpeed(speed),
                sleepRemainingMs = if (sleepMinutes > 0) sleepRemainingMs else 0L,
                hasMultipleQualities = hasMultipleQualities,
                channelStepAvailable = onChannelStep != null,
                fetchingSubtitles = fetchingSubtitles,
                progressFocus = progressFocus,
                playFocus = controlsFocus,
                tracksFocus = tracksFocus,
                aspectFocus = aspectFocus,
                qualityFocus = qualityFocus,
                speedFocus = speedFocus,
                sleepFocus = sleepFocus,
                onSeekBy = { delta -> engine.seekBy(delta) },
                onTogglePlay = { engine.togglePlay() },
                onOpenMenu = {
                    menuReturnTarget = it
                    openMenu = it
                },
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                extraActions = extraActions,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        // Μενού επιλογών — πάνω από την εικόνα, χωρίς να τη σταματούν. Οι ίδιοι
        // οι διάλογοι ζουν στο PlayerMenuHost.kt· εδώ μένει μόνο η σύνδεσή τους
        // με την κατάσταση και τη μηχανή.
        //
        // Κλείσιμο + επαναφορά χειριστηρίων σε ΕΝΑ σημείο: κάθε μενού το έκανε
        // μόνο του, και ήταν θέμα χρόνου να το ξεχάσει κάποιο.
        val closeMenu: () -> Unit = {
            val returnFocus = when (menuReturnTarget) {
                PlayerMenu.AUDIO,
                PlayerMenu.SUBTITLES -> tracksFocus
                PlayerMenu.ASPECT -> aspectFocus
                PlayerMenu.QUALITY -> qualityFocus
                PlayerMenu.SPEED -> speedFocus
                PlayerMenu.SLEEP -> sleepFocus
                null -> controlsFocus
            }
            openMenu = null
            controlsVisible = true
            controlsEngaged = true
            scope.launch { returnFocus.requestFocusWithRetry() }
        }
        PlayerMenuHost(
            open = openMenu,
            // Από την ΚΑΤΑΣΤΑΣΗ: ενημερώνονται μόνα τους όταν τα κομμάτια γίνονται
            // γνωστά, ακόμη κι αν το μενού είναι ήδη ανοιχτό.
            audioTracks = state.audioTracks,
            subtitleTracks = state.subtitleTracks,
            videoTracks = { engine.videoTracks() },
            aspectMode = aspectMode,
            speed = speed,
            sleepMinutes = sleepMinutes,
            subtitleSize = subtitleSize,
            subtitleBackground = subtitleBackground,
            subtitleBold = subtitleBold,
            // Ο τίτλος προσυμπληρώνει την αναζήτηση: συνήθως δεν χρειάζεται να
            // γράψεις τίποτα, αλλά μπορείς να τον διορθώσεις.
            subtitleQuery = title,
            searchSubtitles = onSearchSubtitles,
            onAutoFetchSubtitles = onFetchSubtitles?.let { fetch ->
                {
                    if (!fetchingSubtitles) {
                        fetchingSubtitles = true
                        closeMenu()
                        scope.launch {
                            subtitleMessage = try {
                                fetch()
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                "Η λήψη υποτίτλων απέτυχε."
                            }
                            fetchingSubtitles = false
                        }
                    }
                }
            },
            onSelectAudio = { id -> engine.selectAudio(id); closeMenu() },
            onSelectSubtitle = { id -> engine.selectSubtitle(id); closeMenu() },
            onSelectVideo = { id -> engine.selectVideo(id); closeMenu() },
            onSelectAspect = { value -> aspectMode = value; closeMenu() },
            onSelectSpeed = { value -> speed = value; engine.setSpeed(value); closeMenu() },
            onSelectSleep = { minutes -> sleepMinutes = minutes; closeMenu() },
            // Η εμφάνιση υποτίτλων ΔΕΝ κλείνει το μενού — δες PlayerMenuHost.
            onSubtitleSize = { value ->
                subtitleSize = value
                subtitleStore.subtitleSizePercent = value
                lastInteractionMs = android.os.SystemClock.uptimeMillis()
            },
            onSubtitleBackground = { value ->
                subtitleBackground = value
                subtitleStore.subtitleBackground = value
                lastInteractionMs = android.os.SystemClock.uptimeMillis()
            },
            onSubtitleBold = { value ->
                subtitleBold = value
                subtitleStore.subtitleBold = value
                lastInteractionMs = android.os.SystemClock.uptimeMillis()
            },
            onSubtitleChosen = { choice ->
                closeMenu()
                scope.launch {
                    subtitleMessage = try {
                        onApplySubtitle?.invoke(choice) ?: ""
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        "Η λήψη υποτίτλων απέτυχε."
                    }
                }
            },
            onDismiss = closeMenu,
        )
    }
}
