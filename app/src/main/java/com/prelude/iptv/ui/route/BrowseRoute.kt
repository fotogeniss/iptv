package com.prelude.iptv.ui.route

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prelude.iptv.EpgGridScreen
import com.prelude.iptv.ExportScreen
import com.prelude.iptv.R
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.AdaptiveCatalogHome
import com.prelude.iptv.ui.HeroShowcase
import com.prelude.iptv.ui.LibraryDestination
import com.prelude.iptv.ui.MainViewModel
import com.prelude.iptv.ui.PremiumContentRail
import com.prelude.iptv.ui.PremiumTvHero
import com.prelude.iptv.ui.PremiumTvNavigationRail
import com.prelude.iptv.ui.TvDialogTextButton
import com.prelude.iptv.ui.UiState
import com.prelude.iptv.ui.buildCatalogRailSections
import com.prelude.iptv.ui.localization.catalogRailLabels
import com.prelude.iptv.ui.localization.localizedCatalogProgress
import com.prelude.iptv.ui.localization.localizedProfileName
import com.prelude.iptv.ui.status.CatalogStatusKind
import com.prelude.iptv.ui.status.CatalogStatusPolicy
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.isTvDevice
import com.prelude.iptv.ui.mobile.live.MobileLiveChannelsScreen
import com.prelude.iptv.ui.mobile.navigation.PremiumMobileBottomNavigation
import com.prelude.iptv.ui.mobile.navigation.premiumMobileNavigationContentPadding
import com.prelude.iptv.ui.rememberInitialFocus
import com.prelude.iptv.ui.requestFocusWithRetry
import kotlinx.coroutines.launch

/**
 * Πόσο πρέπει να μείνει το focus στο αριστερό μενού πριν αυτό ανοίξει.
 *
 * 120ms: αρκετά ώστε ένα στιγμιαίο πέρασμα του focus να μην το ανοίξει, αρκετά
 * λίγα ώστε να μη γίνεται αντιληπτό όταν πας εκεί επίτηδες — η ίδια η κίνηση
 * ανοίγματος διαρκεί 220ms.
 */
private const val NAV_RAIL_OPEN_DELAY_MS = 120L

/**
 * Πόσο μένει «οπλισμένο» το μενού μετά από πάτημα αριστερού βελακιού.
 *
 * Αν το focus δεν κατέληξε εκεί μέσα σε αυτό το διάστημα, σημαίνει ότι υπήρχε
 * περιεχόμενο αριστερότερα και το αίτημα δεν αφορούσε το μενού. Χωρίς λήξη, ένα
 * μόνο πάτημα θα το άφηνε εστιάσιμο για πάντα και θα επέστρεφε το πρόβλημα.
 */
private const val NAV_RAIL_ARM_WINDOW_MS = 900L

@Composable
internal fun BrowseScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    initialMobileDestination: String? = null,
    onMobileDestinationConsumed: () -> Unit = {}
) {
    val state by vm.catalogState.collectAsStateWithLifecycle()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val activeProfileName = localizedProfileName(vm.activeProfileDisplayName())
    val scope = rememberCoroutineScope()
    var epgChannel by remember { mutableStateOf<Channel?>(null) }
    var showExport by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }
    var multiviewFailure by remember { mutableStateOf<MultiviewLaunchFailure?>(null) }
    var detailChannel by remember { mutableStateOf<Channel?>(null) }
    var recentsTick by remember { mutableStateOf(0) }
    var searchOpen by remember { mutableStateOf(false) }
    var libraryDestination by remember { mutableStateOf<LibraryDestination?>(null) }
    var libraryQuery by rememberSaveable { mutableStateOf("") }
    // Debounce αναζήτησης: το πεδίο ενημερώνεται άμεσα, αλλά το βαρύ φιλτράρισμα
    // (O(universe)) τρέχει μόνο όταν ο χρήστης σταματήσει να πληκτρολογεί — αλλιώς
    // «σερνόταν» σε κάθε πλήκτρο πάνω σε χιλιάδες τίτλους.
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(libraryQuery) {
        if (libraryQuery.isEmpty()) { debouncedQuery = ""; return@LaunchedEffect }
        kotlinx.coroutines.delay(250)
        debouncedQuery = libraryQuery
    }
    var mobilePrimaryDestination by rememberSaveable { mutableStateOf("home") }
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.takeIf(String::isNotBlank)?.let { libraryQuery = it }
        }
    }
    val launchVoiceSearch: () -> Unit = {
        val activeLocale = ctx.resources.configuration.locales[0]
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, ctx.getString(R.string.catalog_voice_prompt))
        }
        runCatching { voiceSearchLauncher.launch(intent) }
            .onFailure { toast(ctx, ctx.getString(R.string.catalog_voice_unavailable)) }
    }
    val isTv = isTvDevice()
    // ΤΗΛΕΟΡΑΣΗ: το BACK μέσα από κάρτα (ταινία/σειρά/live) πηγαίνει στο ΑΡΙΣΤΕΡΟ
    // ΜΕΝΟΥ αντί να σε βγάζει από τη λίστα. Δεύτερο BACK (με το μενού ήδη
    // εστιασμένο) φεύγει κανονικά.
    val navRailFocus = remember { FocusRequester() }
    var navRailFocused by remember { mutableStateOf(false) }

    /**
     * ΤΟ ΜΕΝΟΥ ΑΝΟΙΓΕΙ ΜΟΝΟ ΟΤΑΝ ΜΕΝΕΙΣ ΜΕΣΑ ΤΟΥ.
     *
     * ΤΙ ΣΥΝΕΒΑΙΝΕ: στο άνοιγμα της εφαρμογής και σε κάθε επιστροφή από τον
     * player, το Compose δίνει στιγμιαία το focus στο πρώτο εστιάσιμο στοιχείο
     * της οθόνης — που είναι το μενού — πριν το πάρει ο πραγματικός προορισμός.
     * Το πέρασμα κρατά λίγα καρέ, αλλά το μενού προλάβαινε να ανοίξει και να
     * κλείσει. Μενού που αναβοσβήνει μόνο του δεν διαβάζεται ως λεπτομέρεια·
     * διαβάζεται ως χαλασμένη εφαρμογή.
     *
     * Η διόρθωση δεν είναι να εμποδίσουμε το πέρασμα — είναι φυσιολογική
     * συμπεριφορά του Compose και θα ξαναεμφανιζόταν αλλού. Είναι να ΜΗΝ
     * αντιδρά το μενού σε πέρασμα.
     *
     * ΑΣΥΜΜΕΤΡΟ ΣΚΟΠΙΜΑ: ανοίγει με καθυστέρηση, κλείνει ακαριαία. Το άνοιγμα
     * είναι δήλωση πρόθεσης και αντέχει 120ms· το κλείσιμο πρέπει να είναι
     * άμεσο, γιατί ένα μενού που αργεί να κλείσει σκεπάζει το περιεχόμενο.
     */
    var navRailExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(navRailFocused) {
        if (!navRailFocused) {
            navRailExpanded = false
            return@LaunchedEffect
        }
        // Αν το focus φύγει πριν περάσει η καθυστέρηση, το effect ακυρώνεται και
        // δεν φτάνει ποτέ εδώ — ακριβώς η συμπεριφορά που θέλουμε στο πέρασμα.
        kotlinx.coroutines.delay(NAV_RAIL_OPEN_DELAY_MS)
        navRailExpanded = true
    }

    /**
     * ΠΟΤΕ ΕΠΙΤΡΕΠΕΤΑΙ ΣΤΟ ΜΕΝΟΥ ΝΑ ΔΕΧΤΕΙ FOCUS.
     *
     * ΤΙ ΔΟΚΙΜΑΣΑ ΠΡΙΝ ΚΑΙ ΓΙΑΤΙ ΑΠΕΤΥΧΕ: πρώτα μετρούσα πόση ώρα κρατά το focus,
     * μετά απαριθμούσα τις στιγμές που η οθόνη αδειάζει, και τέλος απαιτούσα ρητό
     * πάτημα. Το πρώτο εξαρτιόταν από την ταχύτητα της συσκευής, το δεύτερο είχε
     * πάντα μια στιγμή που δεν είχα προβλέψει, και το τρίτο χρειαζόταν ΔΥΟ
     * πατήματα — γιατί το πάτημα που «οπλίζει» είναι το ίδιο που ψάχνει πού να
     * πάει το focus, και τότε το μενού δεν έχει γίνει ακόμη εστιάσιμο.
     *
     * Το σωστό ερώτημα δεν είναι «πόση ώρα πέρασε» ούτε «πάτησε ο χρήστης», αλλά:
     * ΥΠΑΡΧΕΙ ΚΑΤΙ ΑΛΛΟ ΕΣΤΙΑΣΜΕΝΟ;
     *
     * Αν ναι, ο χρήστης βρίσκεται κάπου συγκεκριμένα και το αριστερό βελάκι είναι
     * πλοήγηση — το μενού πρέπει να είναι διαθέσιμο αμέσως. Αν όχι, βρισκόμαστε σε
     * ένα από τα κενά που αφήνουν οι τεμπέλικες λίστες, και το μενού δεν πρέπει να
     * μπορεί να αρπάξει τίποτα.
     *
     * Ένας κανόνας, χωρίς χρονόμετρα και χωρίς εξαιρέσεις.
     */
    var contentHasFocus by remember { mutableStateOf(false) }
    /** Ρητό αίτημα με BACK — δουλεύει ακόμη κι όταν τίποτα δεν είναι εστιασμένο. */
    var navRailArmed by remember { mutableStateOf(false) }

    /**
     * Το κλειδί του στοιχείου που άνοιξε τελευταίο από τις λίστες.
     *
     * Επιστρέφοντας από αναπαραγωγή ή λεπτομέρειες, το focus πρέπει να γυρίζει
     * ΕΚΕΙ. Χωρίς αυτό, σε πλέγμα χιλιάδων ταινιών έχανες τη θέση σου κάθε φορά
     * και έπρεπε να την ξαναβρείς με το χέρι.
     *
     * Κρατιέται εδώ και όχι μέσα στις οθόνες: οι οθόνες ξαναχτίζονται όταν
     * αλλάζει η κατηγορία, ενώ η «τελευταία επιλογή» πρέπει να επιβιώνει.
     */
    var lastOpenedKey by remember { mutableStateOf<String?>(null) }

    // Ενεργή ενότητα στην τηλεόραση: "home" | "movies" | "series" | "live".
    var tvSection by rememberSaveable { mutableStateOf("home") }

    /**
     * ΤΟ ΜΕΝΟΥ ΛΕΕΙ ΤΗΝ ΑΛΗΘΕΙΑ ΓΙΑ ΤΟ ΤΙ ΠΡΑΓΜΑΤΙΚΑ ΦΟΡΤΩΘΗΚΕ.
     *
     * Το setContentType ΔΕΝ αλλάζει πάντα ενότητα: επιστρέφει νωρίς αν δεν
     * υπάρχει ακόμη πηγή, ή αν η ενότητα κατεβαίνει ακόμη στο παρασκήνιο. Το
     * tvSection όμως το είχαμε ήδη αλλάξει — και έμενε να λέει «Σειρές» ενώ η
     * οθόνη έδειχνε Ζωντανά.
     *
     * Γι' αυτό συνέβαινε ΜΟΝΟ στο άνοιγμα της εφαρμογής: μετά τη φόρτωση το
     * setContentType πετυχαίνει κανονικά και τα δύο συμφωνούν.
     *
     * Εδώ ευθυγραμμίζουμε την ένδειξη με την πραγματικότητα. Το «vod» δεν το
     * αγγίζουμε: το μοιράζονται Αρχική και Ταινίες, οπότε δεν μπορούμε να
     * συμπεράνουμε ποιο από τα δύο εννοούσε ο χρήστης — εκτός αν η ένδειξη έχει
     * μείνει σε ενότητα που σίγουρα δεν ισχύει.
     */
    LaunchedEffect(state.contentType) {
        tvSection = when (state.contentType) {
            "live" -> "live"
            "series" -> "series"
            else -> if (tvSection == "live" || tvSection == "series") "movies" else tvSection
        }
    }
    // ΤΗΛΕΟΡΑΣΗ: ταινία/επεισόδιο που παίζει ΜΕΣΑ στην εφαρμογή (κοινό επίπεδο
    // αναπαραγωγής), αντί για ξεχωριστό PlayerActivity.
    var inlinePlayback by remember { mutableStateOf<Channel?>(null) }
    // true όσο ο player των ζωντανών πιάνει όλη την οθόνη — τότε το αριστερό
    // μενού δεν πρέπει να συντίθεται καθόλου, ούτε να μπορεί να πάρει focus.
    var liveFullscreen by remember { mutableStateOf(false) }

    // Το «όπλισμα» λήγει: αν το αριστερό πατήθηκε αλλά το focus δεν κατέληξε στο
    // μενού (γιατί υπήρχε περιεχόμενο αριστερότερα), δεν μένει οπλισμένο για
    // πάντα. Όσο το μενού ΕΧΕΙ focus δεν αφοπλίζεται — αλλιώς θα εξαφανιζόταν
    // κάτω από τα δάχτυλά σου ενώ το χρησιμοποιείς.
    LaunchedEffect(navRailArmed, navRailFocused) {
        if (!navRailArmed || navRailFocused) return@LaunchedEffect
        kotlinx.coroutines.delay(NAV_RAIL_ARM_WINDOW_MS)
        navRailArmed = false
    }

    /**
     * ΜΙΑ ΠΟΡΤΑ ΓΙΑ ΤΗΝ ΑΝΑΠΑΡΑΓΩΓΗ.
     *
     * Η οθόνη ξεκινούσε αναπαραγωγή από δεκατρία διαφορετικά σημεία, καθένα με
     * δική του απόφαση. Όποιο ξεχνιόταν, έμενε στον παλιό PlayerActivity — γι' αυτό
     * το «Συνέχισε να βλέπεις» άνοιγε ακόμη τον παλιό player ενώ οι υπόλοιπες
     * λίστες τον νέο. Δεν είναι σφάλμα που διορθώνεται μία φορά· είναι σφάλμα που
     * επανεμφανίζεται σε κάθε καινούργια λίστα, όσο η απόφαση ζει σε κάθε κάρτα.
     *
     * Τώρα η απόφαση ζει σε ένα σημείο: ταινίες και επεισόδια στο κοινό επίπεδο,
     * ζωντανά στη διαδρομή τους. Μια νέα λίστα καλεί αυτό και είναι σωστή εξ ορισμού.
     */
    val playChannel: (Channel) -> Unit = { channel ->
        // ΟΛΑ στο κοινό επίπεδο — και τα ζωντανά.
        //
        // Πριν, τα ζωντανά έφευγαν στον παλιό PlayerActivity ενώ οι ταινίες
        // έμεναν εδώ. Το ίδιο κανάλι συμπεριφερόταν αλλιώς ανάλογα με το αν το
        // πάτησες από την αρχική ή από την οθόνη Live, και ό,τι προσθέταμε στον
        // έναν player έλειπε από τον άλλον: ένδειξη ποιότητας, ρύθμιση
        // αποθέματος, συγχρονισμός χειλιών, υπότιτλοι.
        vm.addRecent(channel)
        lastOpenedKey = vm.favKey(channel)
        inlinePlayback = channel
        recentsTick++
    }

    /**
     * ΜΙΑ ΠΟΡΤΑ ΓΙΑ ΤΗΝ ΑΛΛΑΓΗ ΕΝΟΤΗΤΑΣ: "home" | "movies" | "series" | "live".
     *
     * Υπήρχαν ΟΚΤΩ σχεδόν ίδια μπλοκ — τέσσερα στο αριστερό μενού της τηλεόρασης,
     * τέσσερα στις οθόνες βιβλιοθήκης. Και δεν ήταν ακριβώς ίδια: το μενού ΔΕΝ
     * μηδένιζε την ομάδα στις Ταινίες/Σειρές ενώ η βιβλιοθήκη τη μηδένιζε.
     *
     * Η διαφορά φαινόταν μόνο σε μία περίπτωση —όταν επιλέγεις την ενότητα στην
     * οποία ήδη βρίσκεσαι— και ακριβώς γι' αυτό δεν την είχε προσέξει κανείς.
     * Τώρα η συμπεριφορά είναι μία και ρητή.
     */
    /**
     * Ιστορικό ενοτήτων.
     *
     * ΣΚΟΠΙΜΑ `remember` ΚΑΙ ΟΧΙ `rememberSaveable`: μετά από ανασύσταση της
     * διεργασίας το `mobilePrimaryDestination`/`tvSection` επιβιώνουν και ο
     * συγχρονισμός παρακάτω ξαναχτίζει μια στοίβα ενός επιπέδου. Χάνεται το
     * βάθος του ιστορικού, όχι το πού βρίσκεται ο χρήστης — προτιμότερο από
     * ένα ιστορικό που επιβιώνει μερικώς και στέλνει το «πίσω» σε ενότητα που
     * δεν αντιστοιχεί πια σε τίποτα ορατό.
     */
    var sectionStack by remember { mutableStateOf(listOf("home")) }

    /**
     * Εφαρμόζει την ενότητα ΧΩΡΙΣ να γράψει ιστορικό.
     *
     * Ξεχωριστό από το [openSection] ώστε το «πίσω» να μπορεί να επαναφέρει μια
     * ενότητα χωρίς να την ξαναπροσθέσει στη στοίβα — αλλιώς το πρώτο «πίσω» θα
     * κλείδωνε τον χρήστη σε βρόχο δύο ενοτήτων.
     */
    val applySection: (String) -> Unit = { section ->
        libraryDestination = null
        libraryQuery = ""
        searchOpen = false
        vm.setSearch("")
        // Τα ζωντανά ΔΕΝ μηδενίζουν την ομάδα: εκεί η ομάδα είναι η κατηγορία
        // καναλιών που έβλεπε ο χρήστης και έχει νόημα να διατηρηθεί.
        //
        // Στις υπόλοιπες, η κλήση είναι συνήθως περιττή — το setContentType
        // μηδενίζει μόνο του. Χρειάζεται ΜΟΝΟ όταν η ενότητα δεν αλλάζει
        // πραγματικά (π.χ. Αρχική -> Ταινίες, και οι δύο "vod"), όπου το
        // setContentType επιστρέφει νωρίς χωρίς να πειράξει τίποτα.
        if (section != "live") vm.setGroup(UiState.ALL_GROUP)
        tvSection = section
        mobilePrimaryDestination = section
        // Διαλέγοντας ενότητα από το μενού, το αίτημα έχει εξυπηρετηθεί: το focus
        // πάει στο περιεχόμενο και το μενού ξαναγίνεται αόρατο για το σύστημα.
        navRailArmed = false
        vm.setContentType(
            when (section) {
                "series" -> "series"
                "live" -> "live"
                else -> "vod"
            }
        )
    }

    /** Μετάβαση με καταγραφή ιστορικού: αυτό καλούν τα μενού και τα πλακίδια. */
    val openSection: (String) -> Unit = { section ->
        sectionStack = SectionNavigationPolicy.open(sectionStack, section)
        applySection(section)
    }

    /**
     * Ένα «πίσω» μέσα στις ενότητες. `false` σημαίνει «είμαστε στη ρίζα, δεν
     * είναι δική μας απόφαση» — ο καλών παραδίδει προς τα πάνω.
     */
    val goBackSection: () -> Boolean = {
        val previous = SectionNavigationPolicy.back(sectionStack)
        if (previous == null) {
            false
        } else {
            sectionStack = previous
            applySection(SectionNavigationPolicy.current(previous))
            true
        }
    }

    val fullScreenCatalogOverlay = state.chooseContent || state.pickCategories ||
        state.askLoadMode || state.askRefreshMode
    LaunchedEffect(state.contentType, isTv) {
        if (!isTv) {
            val synced = when (state.contentType) {
                "live" -> "live"
                "series" -> "series"
                "vod" -> if (mobilePrimaryDestination in setOf("home", "movies")) {
                    mobilePrimaryDestination
                } else {
                    "movies"
                }
                else -> mobilePrimaryDestination
            }
            mobilePrimaryDestination = synced
            // ΑΝΤΙΚΑΤΑΣΤΑΣΗ ΚΟΡΥΦΗΣ, ΟΧΙ PUSH: ο τύπος περιεχομένου αλλάζει και
            // χωρίς πλοήγηση του χρήστη (φόρτωση πηγής, επαναφορά κατάστασης).
            // Αν αυτό έγραφε ιστορικό, το «πίσω» θα οδηγούσε σε ενότητα που
            // κανείς δεν επισκέφθηκε.
            sectionStack = SectionNavigationPolicy.replaceTop(sectionStack, synced)
        }
    }
    // Η τηλεόραση ευθυγραμμίζει το `tvSection` σε δικό της effect πιο πάνω, που
    // δηλώνεται πριν καν υπάρξει η στοίβα. Η στοίβα είναι κοινή για τις δύο
    // συσκευές, οπότε πρέπει να ακολουθήσει και εκεί — αλλιώς το «πίσω» στην
    // τηλεόραση θα ξετύλιγε ιστορικό που δεν αντιστοιχεί στην ορατή ενότητα.
    //
    // ΞΕΧΩΡΙΣΤΟ EFFECT ΜΕ ΚΛΕΙΔΙ ΤΟ `tvSection`, ΟΧΙ ΚΛΑΔΟΣ ΤΟΥ ΠΑΡΑΠΑΝΩ: δύο
    // effect με το ίδιο κλειδί δεν έχουν εγγυημένη σειρά εκτέλεσης, οπότε ένας
    // κλάδος που διάβαζε το `tvSection` θα μπορούσε να δει την παλιά τιμή.
    LaunchedEffect(tvSection, isTv) {
        if (isTv) sectionStack = SectionNavigationPolicy.replaceTop(sectionStack, tvSection)
    }
    LaunchedEffect(initialMobileDestination, isTv) {
        val destination = initialMobileDestination ?: return@LaunchedEffect
        if (!isTv) {
            when (destination) {
                "home", "movies", "series", "live" -> openSection(destination)
                // Η αναζήτηση και η βιβλιοθήκη ΔΕΝ είναι ενότητες καταλόγου:
                // ανοίγουν πάνω από ό,τι βλέπεις, χωρίς να το αλλάξουν.
                "search" -> libraryDestination = LibraryDestination.SEARCH
                "library" -> libraryDestination = LibraryDestination.MY_LIST
            }
        }
        onMobileDestinationConsumed()
    }
    // ΜΙΑ φορά το φιλτράρισμα, memoized. Πριν, το vm.visibleChannels() καλούνταν
    // 4 φορές ανά recomposition (header, empty-check, λίστα, focus) — σε 20.000
    // VOD = 80.000 συγκρίσεις σε ΚΑΘΕ ανανέωση οθόνης. Τώρα: μόνο όταν αλλάξει
    // πραγματικά κάποιο από τα inputs του φίλτρου.
    val channels = remember(
        state.channels, state.selectedGroup, state.search, state.favorites,
        state.lockedGroups, state.parentalUnlocked, state.sortMode
    ) { vm.visibleChannels() }
    // Η ΑΡΧΙΚΗ ΤΡΩΕΙ ΑΠΟ ΟΛΕΣ ΤΙΣ ΕΝΟΤΗΤΕΣ, ΟΧΙ ΜΟΝΟ ΑΠΟ ΤΗΝ ΕΝΕΡΓΗ.
    //
    // Ο επεξεργαστής αρχικής απαριθμεί ζωντανά, ταινίες και σειρές, αλλά το
    // `state.channels` κρατά μία ενότητα τη φορά — γι' αυτό, με φορτωμένες τις
    // Ταινίες, οι ράγες σειρών και καναλιών εξαφανίζονταν χωρίς εξήγηση.
    // Το `visibleHomeChannels()` περνά την ένωση από το ΙΔΙΟ φίλτρο γονικού
    // ελέγχου με κάθε άλλη λίστα.
    val homeCatalog by vm.homeCatalogState.collectAsStateWithLifecycle()
    val homeChannels = remember(
        homeCatalog, state.favorites, state.lockedGroups, state.parentalUnlocked, state.sortMode
    ) { vm.visibleHomeChannels() }
    val categoryLayoutRevision by vm.categoryLayoutRevision.collectAsStateWithLifecycle()
    val categoryTitlesInOrder = remember(
        categoryLayoutRevision,
        state.contentType,
        // CatalogUiState deliberately does not expose the shell's playlist
        // index. The group snapshot changes whenever a different source is
        // loaded, so it is the correct catalog-level key here.
        state.groups,
    ) { vm.categoryTitlesInOrder(state.contentType) }
    // ΤΗΛΕΟΡΑΣΗ: η Αρχική είναι ΡΗΤΗ επιλογή, όχι συμπέρασμα από τα φίλτρα.
    //
    // Πριν, το isCatalogHome ήταν αληθές όποτε contentType = vod/series ΚΑΙ group =
    // «Όλα». Πατώντας «Ταινίες» ίσχυαν και τα δύο, οπότε ξαναέβγαινε η Αρχική
    // αντί για την ενότητα — γι' αυτό δεν σε πήγαινε ποτέ στις Ταινίες/Σειρές.
    val isCatalogHome = if (isTv) {
        tvSection == "home" && state.search.isBlank()
    } else {
        state.search.isBlank() &&
            state.selectedGroup == UiState.ALL_GROUP &&
            (state.contentType == "vod" || state.contentType == "series")
    }
    val isPremiumLive = state.contentType == "live" && channels.isNotEmpty()
    // PIN dialog: ("unlock", group) = ξεκλείδωμα συνεδρίας, ("toggle", group) = κλείδωμα/ξεκλείδωμα
    var pinAction by remember { mutableStateOf<Pair<String, String>?>(null) }

    // ---- Chrome που μαζεύεται στο scroll (μόνο κινητό) ----
    // Πριν: header + pills + status + EPG + chips = 5 σειρές ΜΟΝΙΜΑ, δηλαδή
    // μισή οθόνη πριν δεις έστω μία ταινία. Τώρα: 3 σειρές, και σκρολάροντας
    // κάτω μαζεύονται κι αυτές (επιστρέφουν μόλις τραβήξεις πάνω).
    var chromeVisible by remember { mutableStateOf(true) }
    var mobileNavCollapsed by remember { mutableStateOf(false) }

    // ---- Στιγμιαία μηνύματα («Φορτώθηκαν…», «✓ EPG…») ----
    // Είναι πληροφορία ΤΗΣ ΣΤΙΓΜΗΣ, όχι μόνιμο στοιχείο διεπαφής.
    var flash by remember { mutableStateOf<String?>(null) }
    // Το ΙΔΙΟ μήνυμα δεν ξαναδείχνεται. Πριν: το effect ξαναέτρεχε σε ΚΑΘΕ αλλαγή
    // του status/loading (partial publishes), και επειδή το epgStatus έχει
    // προτεραιότητα, ξαναπρόβαλλε το ίδιο «✓ EPG…» κάθε λίγο — μια συνεχής
    // ροή recompositions πάνω στο home ενώ ο χρήστης περιηγείται.
    var lastFlashed by remember { mutableStateOf<String?>(null) }
    // Χωρίς το epgStatus στα κλειδιά: δεν το δείχνουμε πια, οπότε δεν υπάρχει
    // λόγος να ξανατρέχει το effect κάθε φορά που αλλάζει.
    val localizedStatusSurface = isCatalogHome ||
        state.contentType == "live" || state.contentType == "vod" || state.contentType == "series"
    val catalogStatusKind = CatalogStatusPolicy.kindOf(state.status)
    LaunchedEffect(state.status, state.loading, localizedStatusSurface) {
        val msg = when {
            state.loading || localizedStatusSurface -> null
            // ΤΟ EPG ΔΕΝ ΑΝΑΚΟΙΝΩΝΕΤΑΙ.
            //
            // Το «✓ EPG: ταιριάζει σε 412 κανάλια» είναι πληροφορία για όποιον
            // έφτιαξε την εφαρμογή, όχι για όποιον τη χρησιμοποιεί. Ο χρήστης
            // βλέπει αν υπάρχει πρόγραμμα κοιτάζοντας τα κανάλια· δεν χρειάζεται
            // αναφορά κάθε φορά που φορτώνει.
            //
            // Το epgStatus παραμένει στην κατάσταση — το δείχνουν οι Ρυθμίσεις,
            // όπου κάποιος το ψάχνει επίτηδες.
            state.status.isNotBlank() -> state.status
            else -> null
        }
        if (msg == null) { flash = null; return@LaunchedEffect }
        if (msg == lastFlashed) return@LaunchedEffect
        lastFlashed = msg
        flash = msg
        kotlinx.coroutines.delay(4500)
        flash = null
    }
    pinAction?.let { (mode, g) ->
        PinDialog(
            title = when {
                mode == "unlock" -> stringResource(R.string.browse_unlock_group, g)
                g in state.lockedGroups -> stringResource(R.string.browse_unlock_group_management, g)
                else -> stringResource(R.string.browse_lock_group, g)
            },
            onOk = { pin ->
                if (!vm.checkPin(pin)) toast(ctx, ctx.getString(R.string.browse_wrong_pin))
                else when (mode) {
                    "unlock" -> { vm.unlockParental(pin); vm.setGroup(g) }
                    else -> {
                        val wasLocked = g in state.lockedGroups
                        vm.toggleLockGroup(g)
                        toast(
                            ctx,
                            ctx.getString(
                                if (wasLocked) R.string.browse_group_unlocked else R.string.browse_group_locked,
                                g,
                            ),
                        )
                    }
                }
                pinAction = null
            },
            onCancel = { pinAction = null }
        )
    }

    // EPG Grid πιάνει όλη την οθόνη όταν είναι ανοιχτό
    if (showGrid) {
        val gridChannels = remember(showGrid, state.lockedGroups, state.parentalUnlocked) {
            vm.liveChannelsWithEpg()
        }
        BackHandler(enabled = true) { showGrid = false }
        Box(Modifier.fillMaxSize()) {
            EpgGridScreen(
                channels = gridChannels,
                onBack = { showGrid = false },
                onChannelClick = { ch -> showGrid = false; playChannel(ch) },
                onProgramClick = { ch, prog ->
                    val nowMs = System.currentTimeMillis()
                    when {
                        nowMs in prog.startMs until prog.stopMs -> {
                            showGrid = false; playChannel(ch)
                        }
                        prog.stopMs <= nowMs -> {
                            val url = vm.catchupUrl(ch, prog.startMs, prog.stopMs)
                            if (url != null) { showGrid = false; openCatchup(ctx, ch, prog.title, url) }
                            else toast(ctx, "Το κανάλι δεν υποστηρίζει catch-up")
                        }
                        else -> {
                            vm.setReminder(ctx, ch, prog.title, prog.startMs)
                            toast(ctx, "⏰ Υπενθύμιση: ${prog.title}")
                        }
                    }
                },
                mobileBottomPadding = if (isTv) 0.dp else premiumMobileNavigationContentPadding()
            )
            if (!isTv) {
                fun leaveEpg() { showGrid = false }
                // ΣΗΜΕΙΩΣΗ ΑΛΛΑΓΗΣ: το παλιό onLive εδώ έγραφε ΜΟΝΟ
                // `mobilePrimaryDestination = "live"` και ΔΕΝ καλούσε
                // setContentType — μοναδική εξαίρεση ανάμεσα σε έξι αντίγραφα του
                // ίδιου μπλοκ. Ήταν παράλειψη, όχι πρόθεση: από τον οδηγό
                // προγράμματος το «Ζωντανά» άλλαζε την επιλεγμένη καρτέλα χωρίς
                // να φορτώσει τα ζωντανά.
                PremiumMobileBottomNavigation(
                    selected = "live",
                    onHome = { leaveEpg(); openSection("home") },
                    onMovies = { leaveEpg(); openSection("movies") },
                    onSeries = { leaveEpg(); openSection("series") },
                    onLive = { leaveEpg(); openSection("live") },
                    onSearch = { leaveEpg(); libraryDestination = LibraryDestination.SEARCH },
                    onMyList = { leaveEpg(); libraryDestination = LibraryDestination.MY_LIST },
                    onSettings = { leaveEpg(); onOpenSettings() },
                    showSettingsAction = true,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        return
    }

    // ---- ΠΙΣΩ ΜΕΣΑ ΣΤΙΣ ΕΝΟΤΗΤΕΣ ----
    //
    // ΔΗΛΩΝΕΤΑΙ ΠΡΩΤΟ ΕΠΙΤΗΔΕΣ. Στο Compose το BACK το παίρνει ο ΤΕΛΕΥΤΑΙΟΣ
    // ενεργός χειριστής, οπότε η σειρά δήλωσης είναι αντίστροφη προτεραιότητα:
    // ό,τι είναι «από πάνω» (λεπτομέρειες, βιβλιοθήκη, αναζήτηση, επιλογείς)
    // πρέπει να κλείσει πρώτο και μόνο μετά να ξετυλιχθεί το ιστορικό ενοτήτων.
    // Η συνθήκη παρακάτω το κάνει έτσι κι αλλιώς αμοιβαία αποκλειστικό — δύο
    // ανεξάρτητοι λόγοι για την ίδια εγγύηση, γιατί μια σιωπηλή αλλαγή σειράς
    // σύνθεσης δεν θα φαινόταν σε καμία δοκιμή.
    //
    // Στη ΡΙΖΑ δεν είναι ενεργός: εκεί το BACK ανήκει στην από πάνω οθόνη, που
    // ζητά επιβεβαίωση αλλαγής πηγής.
    BackHandler(
        enabled = SectionNavigationPolicy.canGoBack(sectionStack) &&
            !showExport && detailChannel == null && !state.askRefreshMode &&
            !state.pickCategories && !state.chooseContent && !searchOpen &&
            libraryDestination == null && inlinePlayback == null && !showGrid &&
            !liveFullscreen
    ) {
        goBackSection()
    }

    BackHandler(enabled = showExport || detailChannel != null || state.askRefreshMode || state.pickCategories ||
        state.chooseContent || searchOpen || libraryDestination != null) {
        when {
            showExport -> showExport = false
            detailChannel != null -> { detailChannel = null; vm.closeSeries() }
            libraryDestination != null -> { libraryDestination = null; libraryQuery = "" }
            searchOpen -> { searchOpen = false; vm.setSearch("") }
            state.askRefreshMode -> vm.cancelRefreshChoice()
            // refresh picker: ακύρωση στο catalog · initial-load picker: πίσω στην επιλογή τρόπου
            state.pickCategories -> vm.cancelCategoryPicker()
            // αν έχουμε ήδη περιεχόμενο, το πίσω απλά κλείνει την επιλογή ενότητας
            state.chooseContent -> if (state.channels.isNotEmpty()) vm.closeContentChooser() else onBack()
        }
    }

    // ΤΗΛΕΟΡΑΣΗ: όταν το focus είναι σε κάρτα (ταινία/σειρά/live) και πατήσεις
    // BACK, πήγαινε στο αριστερό μενού — μην πετάγεσαι έξω από τη λίστα. Ενεργό
    // μόνο όταν το μενού ΔΕΝ έχει ήδη το focus, ώστε το δεύτερο BACK να βγαίνει
    // κανονικά (ίδιο pattern με Premium/YouTube TV).
    BackHandler(
        enabled = isTv && !navRailFocused && !liveFullscreen && inlinePlayback == null &&
            detailChannel == null && !fullScreenCatalogOverlay &&
            // Μόνο στο κυρίως περιεχόμενο: αναζήτηση/βιβλιοθήκη/εξαγωγή έχουν
            // δικό τους BACK (κλείσιμο), που δεν πρέπει να «κλαπεί».
            libraryDestination == null && !searchOpen && !showExport
    ) {
        // Το BACK είναι ρητό αίτημα, ακριβώς όπως το αριστερό βελάκι.
        navRailArmed = true
        // Με επανάληψη: το ενεργό στοιχείο μπορεί να μην έχει προλάβει να
        // συντεθεί (π.χ. αμέσως μετά από αλλαγή ενότητας). Ένα σκέτο
        // requestFocus() θα αποτύγχανε σιωπηλά και το BACK θα φαινόταν νεκρό.
        scope.launch { navRailFocus.requestFocusWithRetry() }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Όσο παίζει ταινία/επεισόδιο πάνω απ' όλα, το από κάτω περιεχόμενο
            // απενεργοποιείται για focus. Αλλιώς το τηλεχειριστήριο «δουλεύει στο
            // παρασκήνιο»: μετακινεί το focus σε κρυμμένες λίστες αντί να
            // χειρίζεται τον player.
            .then(
                if (inlinePlayback != null) {
                    Modifier.focusProperties { canFocus = false }
                } else Modifier
            )
    ) {
        // ΤΟ ΠΕΡΙΕΧΟΜΕΝΟ ΑΝΑΦΕΡΕΙ ΑΝ ΚΡΑΤΑ ΤΟ FOCUS.
        //
        // Αυτό είναι το κριτήριο για το αν επιτρέπεται στο μενού να δεχτεί focus.
        // Όσο κάποιο στοιχείο περιεχομένου είναι εστιασμένο, ο χρήστης βρίσκεται
        // κάπου συγκεκριμένα και το αριστερό βελάκι είναι πλοήγηση: το μενού
        // πρέπει να είναι διαθέσιμο ΜΕ ΤΟ ΠΡΩΤΟ πάτημα.
        //
        // Όταν όμως το περιεχόμενο ΔΕΝ έχει focus —εκκίνηση, αλλαγή ενότητας,
        // κλείσιμο player, οποιαδήποτε στιγμή οι τεμπέλικες λίστες δεν έχουν
        // συντεθεί— το μενού μένει αόρατο για το σύστημα focus και δεν μπορεί να
        // το αρπάξει. Ακριβώς οι στιγμές που άνοιγε μόνο του.
        //
        // Είναι η ίδια προστασία με πριν, αλλά χωρίς χρονόμετρα και χωρίς να
        // χρειάζεται δεύτερο πάτημα: το ερώτημα δεν είναι «πόση ώρα πέρασε» αλλά
        // «υπάρχει κάτι άλλο εστιασμένο;».
        Box(
            Modifier
                .fillMaxSize()
                .onFocusChanged { contentHasFocus = it.hasFocus }
        ) {
        val activeLibraryDestination = libraryDestination
        if (activeLibraryDestination != null) {
            BrowseLibraryLayer(
                destination = activeLibraryDestination,
                vm = vm,
                visibleChannels = channels,
                sessionChannels = state.channels,
                favoriteKeys = state.favorites,
                recentsVersion = state.recentsVersion,
                query = libraryQuery,
                debouncedQuery = debouncedQuery,
                isTv = isTv,
                onQueryChange = { libraryQuery = it },
                onClose = { libraryDestination = null; libraryQuery = "" },
                onOpenDetails = { ch ->
                    detailChannel = ch
                    if (ch.kind == "series") vm.openSeries(ch)
                },
                onPlay = playChannel,
                onDestinationChange = { next -> libraryDestination = next },
                onOpenSection = openSection,
                onVoiceSearch = launchVoiceSearch,
                onOpenSettings = onOpenSettings
            )
        } else {
        // ΧΩΡΙΣ καθολικό περιθώριο για τη μπάρα: κάθε οθόνη ορίζει το δικό της.
        //
        // Πριν, το περιθώριο έμπαινε ΚΑΙ εδώ ΚΑΙ μέσα στις νέες οθόνες TV — άρα
        // διπλό κενό αριστερά, και σε πλήρη οθόνη ο player ξεκινούσε 74dp μέσα,
        // αφήνοντας μαύρη λωρίδα. Οι παλιές οθόνες παίρνουν το περιθώριο τοπικά.
        val legacyRailInset = if (isTv) 74.dp else 0.dp
        Column(Modifier.fillMaxSize()) {
        // Η premium Home έχει δικό της cinematic chrome. Το legacy top bar
        // παραμένει σε λίστες, live, search και φίλτρα, αλλά όχι πάνω από το Home.
        //
        // ΤΗΛΕΟΡΑΣΗ: δεν εμφανίζεται καθόλου. Οι νέες οθόνες (αρχική, ταινίες/
        // σειρές, ζωντανά) έχουν δικό τους header με το PRELUDE+ — αλλιώς φαινόταν
        // από πάνω το όνομα της πηγής («Xtream …») και το πλήθος στοιχείων.
        if (!isCatalogHome && !isPremiumLive && !isTv) {
            BrowseLegacyTopBar(
                playlistName = vm.currentPlaylist()?.name ?: "",
                visibleCount = channels.size,
                totalCount = state.channels.size,
                sortMode = state.sortMode,
                searchOpen = searchOpen,
                searchText = state.search,
                showEpgGrid = state.contentType == "live" && state.epgLoaded,
                startInset = legacyRailInset,
                // Ίδιος κανόνας με το βελάκι των Ζωντανών: πρώτα ξετυλίγεται το
                // ιστορικό ενοτήτων και μόνο στη ρίζα παραδίδεται προς τα πάνω.
                // Πριν καλούσε κατευθείαν το `onBack`, δηλαδή το βελάκι «πίσω»
                // άνοιγε τον διάλογο εξόδου από την πηγή αντί να πάει πίσω.
                onBack = { if (!goBackSection()) onBack() },
                onSearchOpen = { searchOpen = true },
                onSearchClose = { searchOpen = false; vm.setSearch("") },
                onSearchChange = { vm.setSearch(it) },
                onSortMode = { vm.setSortMode(it) },
                onOpenEpgGrid = { showGrid = true },
                onChooseContent = { vm.showContentChooser() },
                onChangeCategories = { vm.changeCategories() },
                onRefresh = { vm.requestRefresh() },
                onOpenLibrary = { libraryDestination = it },
                onExport = { showExport = true },
            )
        }

        // Και τα τρία υποστηρίζουν ενότητες — στο M3U τις ξεχωρίζουμε στο parse
        // ΚΙΝΗΤΟ: τα pills κρύβονται όταν σκρολάρεις κάτω (βλ. chromeVisible).
        // Σε TV μένουν πάντα — εκεί η πλοήγηση είναι με focus, όχι με scroll.
        androidx.compose.animation.AnimatedVisibility(
            visible = !isTv && chromeVisible && !isCatalogHome && !isPremiumLive
        ) {
            if (!state.chooseContent)
                ContentTypeRow(state.contentType) { vm.setContentType(it) }
        }

        // ΚΙΝΗΤΟ μόνο: inline μπάρα προόδου στη ροή του Column.
        // ΤΗΛΕΟΡΑΣΗ: renders ως FLOATING overlay μέσα στο content Box παρακάτω —
        // εδώ, στη ροή του Column, ΕΣΠΡΩΧΝΕ ολόκληρο το home κάτω/πάνω κάθε φορά
        // που το loading άναβε/έσβηνε (partial section loads) = ο περιβόητος
        // κάθετος «σεισμός» σε κυκλάκια ΚΑΙ κάρτες, ανεξάρτητα από το home UI.
        if (state.loading && !isTv) CatalogLoadingProgress(vm)
        // ΤΑ STATUS ΕΦΥΓΑΝ ΑΠΟ ΕΔΩ: δύο μόνιμες γραμμές («Φορτώθηκαν 46 στοιχεία»
        // + «EPG: ταιριάζει σε 29 κανάλια») έτρωγαν ύψος ΓΙΑ ΠΑΝΤΑ, ενώ είναι
        // πληροφορία της στιγμής — και η πρώτη επαναλάμβανε το «46 στοιχεία»
        // που λέει ήδη ο τίτλος. Τώρα εμφανίζονται ως στιγμιαίο pill πάνω από
        // τη λίστα (κάτω-κέντρο) και σβήνουν μόνα τους. Βλ. `flash` παρακάτω.

        if (isTv && state.contentType == "live" && !state.loading && !isPremiumLive) {
            Box(Modifier.padding(start = legacyRailInset)) {
            GroupChips(
                state.groups,
                state.selectedGroup,
                contentType = state.contentType,
                lockedGroups = state.lockedGroups,
                onLongPress = { group ->
                    when {
                        group == UiState.ALL_GROUP || group == UiState.FAV_GROUP ->
                            toast(ctx, ctx.getString(R.string.browse_group_not_lockable))
                        !vm.hasParentalPin() ->
                            toast(ctx, ctx.getString(R.string.browse_set_parental_pin_first))
                        else -> pinAction = "toggle" to group
                    }
                },
                onSelect = { group ->
                    if (group in state.lockedGroups && !state.parentalUnlocked)
                        pinAction = "unlock" to group
                    else vm.setGroup(group)
                }
            )
            }
        }

        val catalogContinue = remember(
            recentsTick, state.recentsVersion, state.contentType,
            state.lockedGroups, state.parentalUnlocked
        ) { vm.continueWatching() }

        // Μετάβαση Ταινίες/Σειρές/Live: fade-in + ελαφρύ slide-up του περιεχομένου
        // κάθε φορά που αλλάζει η ενότητα. Δεν αλλάζει τη δομή (άρα δεν πειράζει το
        // TV focus) — απλώς animate-άρει το alpha/translationY του root.
        val contentFade = remember { Animatable(1f) }
        LaunchedEffect(state.contentType) {
            contentFade.snapTo(0f)
            contentFade.animateTo(1f, animationSpec = tween(300))
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                alpha = contentFade.value
                translationY = (1f - contentFade.value) * 26f
            }
        ) {
            // Every source now loads all three sections once. Starting the old
            // supplementary backfill here would request the same portal data a
            // second time while the complete source load is already running.
            // ΜΟΝΟ Η ΑΡΧΙΚΗ ΠΑΙΡΝΕΙ ΤΗΝ ΕΝΩΣΗ.
            //
            // Το `isCatalogHome` δεν σημαίνει «Αρχική»: είναι αληθές και στις
            // Ταινίες και στις Σειρές, γιατί περιγράφει «κατάλογος χωρίς
            // αναζήτηση και χωρίς επιλεγμένη ομάδα». Δίνοντας την ένωση σε όλες,
            // οι Ταινίες γέμισαν σειρές και κανάλια. Ο προορισμός είναι το
            // `mobilePrimaryDestination`, και μόνο το "home" θέλει τα πάντα —
            // οι άλλες δύο οθόνες οφείλουν να δείχνουν ό,τι λέει το όνομά τους.
            val homeItems = if (mobilePrimaryDestination == "home") {
                homeChannels.ifEmpty { channels }
            } else {
                channels
            }
            if (isCatalogHome && homeItems.isNotEmpty()) {
                AdaptiveCatalogHome(
                    channels = homeItems,
                    continueWatching = catalogContinue,
                    favoriteKeys = state.favorites,
                    profileName = activeProfileName,
                    tmdbFor = vm::tmdb,
                    onPlay = { channel ->
                        if (channel.kind == "series") {
                            detailChannel = channel
                            vm.openSeries(channel)
                        } else {
                            playChannel(channel)
                        }
                    },
                    onDetails = { channel ->
                        detailChannel = channel
                        if (channel.kind == "series") vm.openSeries(channel)
                    },
                    onToggleFavorite = vm::toggleFavorite,
                    selectedDestination = mobilePrimaryDestination,
                    onOpenHome = { openSection("home") },
                    onOpenMovies = { openSection("movies") },
                    onOpenSeries = { openSection("series") },
                    onOpenLive = { openSection("live") },
                    onOpenSearch = { libraryDestination = LibraryDestination.SEARCH },
                    onOpenMyList = { libraryDestination = LibraryDestination.MY_LIST },
                    onOpenSettings = onOpenSettings,
                    onOpenCategories = { vm.changeCategories() },
                    // ΟΛΟΚΛΗΡΟΣ ο κατάλογος: τα πλακίδια μετρούν και τα ζωντανά,
                    // που το `channels` έχει ήδη φιλτράρει για την ενότητα.
                    allChannels = homeItems,
                    recentLive = remember(recentsTick, state.recentsVersion) { vm.recentLive() },
                    onClearHistory = { vm.clearHomeHistory(it) },
                    onUpdateContents = { vm.requestRefresh() },
                    onExport = { showExport = true },
                    categoryTitlesInOrder = categoryTitlesInOrder,
                    modifier = Modifier.fillMaxSize().padding(start = legacyRailInset)
                )
            } else if (state.contentType == "live" && channels.isNotEmpty() && !isTv) {
                // Λιτό mobile Live: header, αναζήτηση, rails ανά κατηγορία
                // και floating categories action, όπως στο approved reference.
                MobileLiveChannelsScreen(
                    channels = channels,
                    favoriteKeys = state.favorites,
                    keyOf = vm::favKey,
                    onPlay = { channel -> playChannel(channel) },
                    // ΤΟ ΒΕΛΑΚΙ ΚΑΝΕΙ Ο,ΤΙ ΚΑΙ ΤΟ BACK ΤΗΣ ΣΥΣΚΕΥΗΣ.
                    //
                    // Πριν έγραφε `openSection("home")`: σταθερός προορισμός, όχι
                    // πίσω. Ερχόμενος από τις Σειρές, ο χρήστης κατέληγε στην
                    // Αρχική. Χειρότερα, το BACK της συσκευής στο ίδιο σημείο
                    // ζητούσε αλλαγή πηγής — ίδια χειρονομία, δύο αποτελέσματα.
                    onBack = { if (!goBackSection()) onBack() },
                    nowTextFor = { channel ->
                        if (!state.epgLoaded) {
                            null
                        } else {
                            // XMLTV channel ids differ between providers. M3U usually
                            // exposes tvg-id, while MAC/Stalker commonly exposes chId.
                            // Name is the final fallback for guides keyed by display name.
                            sequenceOf(channel.tvgId, channel.chId, channel.streamId, channel.name)
                                .filter(String::isNotBlank)
                                .distinct()
                                .mapNotNull(vm::nowText)
                                .firstOrNull()
                        }
                    },
                    categoryTitlesInOrder = categoryTitlesInOrder,
                    onOpenEpg = if (state.epgLoaded) ({ showGrid = true }) else null,
                    onOpenSettings = onOpenSettings,
                    onNavigationCollapsedChange = { mobileNavCollapsed = it },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isTv) {
                BrowseTvSections(
                    live = state.contentType == "live",
                    groups = state.groups,
                    selectedGroup = state.selectedGroup,
                    channels = channels,
                    favoriteKeys = state.favorites,
                    contentType = state.contentType,
                    lockedGroups = state.lockedGroups,
                    parentalUnlocked = state.parentalUnlocked,
                    vm = vm,
                    onRequestUnlock = { group -> pinAction = "unlock" to group },
                    onFullscreenChange = { liveFullscreen = it },
                    onMultiview = { primary, secondary ->
                        openMultiview(ctx, scope, vm, primary, secondary) { multiviewFailure = it }
                    },
                    onOpenDetails = { channel ->
                        lastOpenedKey = vm.favKey(channel)
                        detailChannel = channel
                        if (channel.kind == "series") vm.openSeries(channel)
                    },
                    onPlay = playChannel,
                    // Επιστρέφοντας από ταινία, σειρά ή λεπτομέρειες, το focus
                    // πηγαίνει στο στοιχείο που άνοιξες — όχι στην κορυφή του
                    // πλέγματος και όχι στο αριστερό μενού.
                    lastOpenedKey = lastOpenedKey,
                    obscuredByPlayer = inlinePlayback != null || detailChannel != null
                )
            } else {
            Column(Modifier.fillMaxSize().padding(start = legacyRailInset)) {
                androidx.compose.animation.AnimatedVisibility(visible = (isTv && !isCatalogHome) || (!isTv && chromeVisible)) {
                GroupChips(
                    state.groups, state.selectedGroup,
                    contentType = state.contentType,
                    lockedGroups = state.lockedGroups,
                    onLongPress = { g ->
                        when {
                            g == UiState.ALL_GROUP || g == UiState.FAV_GROUP ->
                                toast(ctx, ctx.getString(R.string.browse_group_not_lockable))
                            !vm.hasParentalPin() ->
                                toast(ctx, ctx.getString(R.string.browse_set_parental_pin_first))
                            else -> pinAction = "toggle" to g
                        }
                    }
                ) { g ->
                    // κλειδωμένο group χωρίς ξεκλείδωμα -> ζήτα PIN πριν το δείξεις
                    if (g in state.lockedGroups && !state.parentalUnlocked)
                        pinAction = "unlock" to g
                    else vm.setGroup(g)
                }
                }   // τέλος AnimatedVisibility (chips)
                // (το channels υπολογίζεται πλέον μία φορά στην αρχή του BrowseScreen)
                // ΤΗΛΕΟΡΑΣΗ: μόλις φορτώσει η λίστα, δώσε focus στο 1ο κανάλι.
                // Αλλιώς το focus έχει χαθεί μαζί με τον διάλογο και το
                // τηλεχειριστήριο δεν κάνει τίποτα.
                // Το searchOpen μπήκε στο key: κλείνοντας την αναζήτηση, το
                // πεδίο (που είχε το focus) φεύγει από το composition και το
                // focus χανόταν — τώρα ξαναπροσγειώνεται στη λίστα.
                val firstItem = rememberInitialFocus(
                    enabled = isTv && !isCatalogHome && channels.isNotEmpty(),
                    key = Triple(state.contentType, channels.size, searchOpen)
                )
                // PREMIUM ΛΕΠΤΟΜΕΡΕΙΑ: αλλάζοντας group/ενότητα/αναζήτηση, η λίστα
                // γυρνάει στην ΚΟΡΥΦΗ. Πριν έμενες στη μέση του scroll του
                // ΠΡΟΗΓΟΥΜΕΝΟΥ group — έβλεπες «τυχαία» μέση της νέας λίστας.
                val listState = rememberLazyListState()
                LaunchedEffect(state.selectedGroup, state.contentType, state.search) {
                    listState.scrollToItem(0)
                }
                // Κατεύθυνση scroll -> μάζεμα/εμφάνιση chrome (pattern YouTube/Play
                // Store). Κατώφλι 8px ώστε το «τρέμουλο» του δαχτύλου να μην
                // αναβοσβήνει τη μπάρα. Στην κορυφή εμφανίζεται πάντα.
                if (!isTv) LaunchedEffect(listState) {
                    var pi = 0; var po = 0
                    snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                        .collect { (i, o) ->
                            mobileNavCollapsed = i > 0 || o > 40
                            chromeVisible = when {
                                i == 0 && o < 40 -> true
                                i > pi || (i == pi && o > po + 8) -> false
                                i < pi || (i == pi && o < po - 8) -> true
                                else -> chromeVisible
                            }
                            pi = i; po = o
                        }
                }
                // ΤΗΛΕΟΡΑΣΗ (pattern YouTube TV): βαθιά μέσα στη λίστα, το BACK
                // σε πετάει πρώτα στην ΚΟΡΥΦΗ (με focus στο 1ο κανάλι) — όχι
                // «πάνω-πάνω-πάνω…» 200 φορές για να φτάσεις στα chips. Το
                // δεύτερο BACK βγαίνει κανονικά. Όριο >3 ώστε ένα μικρό scroll
                // να μην «κλέβει» το πίσω.
                if (isTv) {
                    BackHandler(enabled = listState.firstVisibleItemIndex > 3) {
                        scope.launch {
                            listState.scrollToItem(0)
                            firstItem.requestFocusWithRetry()
                        }
                    }
                }
                // «Συνέχισε να βλέπεις»: recents με σωσμένη θέση. Υπολογίζεται
                // ΕΞΩ από το LazyColumn (LazyListScope δεν είναι composable) και
                // ξαναδιαβάζεται όταν γυρνάς από τον player (recentsVersion/tick).
                val cw = remember(
                    recentsTick, state.recentsVersion, state.contentType,
                    state.lockedGroups, state.parentalUnlocked
                ) { vm.continueWatching() }
                // πόσα items υπάρχουν στο LazyColumn ΠΡΙΝ τα κανάλια (continue + hero)
                // ώστε το scroll-follows-focus να στοχεύει το σωστό index.
                // Continue Watching is rendered inside the dedicated mobile hero and
                // as a catalog rail on TV. Do not render a second legacy row.
                val hasContinue = false
                val railLabels = catalogRailLabels()
                val tvRails = remember(channels, state.favorites, cw, railLabels) {
                    // This branch is the non-home browser. Building every home
                    // rail here was unused, yet duplicated the largest catalog.
                    if (isCatalogHome) buildCatalogRailSections(channels, state.favorites, cw, railLabels)
                    else emptyList()
                }
                val hasHero = (state.contentType == "vod" || state.contentType == "series") &&
                    state.search.isBlank() && channels.isNotEmpty()
                val headerCount = (if (hasContinue) 1 else 0) + (if (hasHero) 1 else 0)
                if (channels.isEmpty() && !state.loading) EmptyState(
                    hasLoaded = state.channels.isNotEmpty(),
                    isError = catalogStatusKind == CatalogStatusKind.ERROR,
                    // Migrated surfaces consume the typed status classification and
                    // localized recovery copy, never the legacy Greek transport text.
                    message = if (localizedStatusSurface) "" else state.status,
                    onLoad = { vm.loadCurrent() },
                    onClearFilters = { vm.setSearch(""); vm.setGroup(UiState.ALL_GROUP) },
                    onRefresh = { vm.requestRefresh() },
                    onOpenSettings = onOpenSettings,
                )
                else LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = if (isTv) {
                        PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    } else {
                        PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 4.dp,
                            bottom = premiumMobileNavigationContentPadding()
                        )
                    }
                ) {
                    // «Συνέχισε να βλέπεις» — ΚΑΙ σε TV (το hero είναι μόνο κινητό,
                    // αλλά το πού έμεινες στην ταινία το θες παντού). Πάνω-πάνω,
                    // γιατί είναι η πιο πιθανή επόμενη ενέργεια του χρήστη.
                    if (hasContinue) {
                        item(key = "continue") {
                            ContinueWatchingRow(cw) { c ->
                                if (c.kind == "series") { detailChannel = c; vm.openSeries(c) }
                                else playChannel(c)
                            }
                        }
                    }
                    // Hero carousel μόνο σε ταινίες/σειρές, όταν δεν ψάχνεις κάτι
                    // Το carousel είναι σχεδιασμένο για αφή. Στην τηλεόραση το D-pad
                    // το κυλάει ατέρμονα και οι αφίσες δείχνουν χαμένες σε landscape.
                    if (hasHero) {
                        item(key = "hero") {
                            val hero = channels.first()
                            if (isTv) {
                                PremiumTvHero(
                                    channel = hero,
                                    favorite = vm.favKey(hero) in state.favorites,
                                    progress = if (hero.kind == "vod") vm.watchProgress(hero) else null,
                                    tmdbFor = vm::tmdb,
                                    onPlay = {
                                        if (hero.kind == "series") { detailChannel = hero; vm.openSeries(hero) }
                                        else playChannel(hero)
                                    },
                                    onDetails = {
                                        detailChannel = hero
                                        if (hero.kind == "series") vm.openSeries(hero)
                                    },
                                    onFavorite = { vm.toggleFavorite(hero) },
                                    modifier = Modifier.padding(bottom = 18.dp)
                                )
                            } else {
                                HeroShowcase(
                                    items = channels,
                                    recents = cw,
                                    isFav = { vm.favKey(it) in state.favorites },
                                    tmdbFor = vm::tmdb,
                                    onPlay = { c ->
                                        if (c.kind == "series") { detailChannel = c; vm.openSeries(c) }
                                        else playChannel(c)
                                    },
                                    onDetails = { c ->
                                        detailChannel = c
                                        if (c.kind == "series") vm.openSeries(c)
                                    },
                                    onFav = { c -> vm.toggleFavorite(c) }
                                )
                            }
                        }
                    }
                    if (isCatalogHome) {
                        items(tvRails, key = { "rail:${it.id}" }) { section ->
                            PremiumContentRail(
                                section = section,
                                favoriteKeys = state.favorites,
                                onOpen = { ch ->
                                    when (ch.kind) {
                                        "series" -> { detailChannel = ch; vm.openSeries(ch) }
                                        "vod" -> detailChannel = ch
                                        else -> playChannel(ch)
                                    }
                                }
                            )
                        }
                    } else {
                    itemsIndexed(channels, key = { idx, it -> "$idx:${it.name}${it.url}${it.cmd}${it.seriesId}" }) { idx, ch ->
                        ChannelCard(
                            ch = ch,
                            isFav = vm.favKey(ch) in state.favorites,
                            nowText = if (state.epgLoaded) vm.nowText(ch.tvgId) else null,
                            modifier = (if (idx == 0) Modifier.focusRequester(firstItem) else Modifier)
                                // SCROLL-FOLLOWS-FOCUS: όταν ένα card παίρνει focus,
                                // η λίστα σκρολάρει ώστε να μένει ΟΡΑΤΟ. Αυτό λύνει
                                // το «δεν πάω πάνω / δεν ξέρω πού είμαι»: το D-pad
                                // δεν χάνει ποτέ το focused item εκτός οθόνης, οπότε
                                // πάνω/κάτω δουλεύουν πάντα και βλέπεις πού βρίσκεσαι.
                                .then(if (isTv) Modifier.onFocusChanged {
                                    if (it.isFocused) scope.launch {
                                        val li = listState.layoutInfo
                                        val vis = li.visibleItemsInfo.firstOrNull { v -> v.index == idx + headerCount }
                                        val vpEnd = li.viewportEndOffset
                                        val vpStart = li.viewportStartOffset
                                        // αν το item ακουμπάει τα άκρα του viewport, φέρ' το μέσα
                                        if (vis == null || vis.offset < vpStart + 40 ||
                                            vis.offset + vis.size > vpEnd - 40
                                        ) listState.animateScrollToItem(
                                            (idx + headerCount - 2).coerceAtLeast(0)
                                        )
                                    }
                                } else Modifier),
                            onClick = {
                                when (ch.kind) {
                                    "series" -> { detailChannel = ch; vm.openSeries(ch) }
                                    "vod" -> { detailChannel = ch }
                                    else -> playChannel(ch)
                                }
                            },
                            onEpg = { epgChannel = ch },
                            onFav = { vm.toggleFavorite(ch) }
                        )
                    }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
            }
            // Catalog home normally owns its own navigation. When it has no items,
            // preserve the shared menu so the user can reach source settings.
            if (!isTv && !fullScreenCatalogOverlay && (!isCatalogHome || channels.isEmpty()) && detailChannel == null) {
                PremiumMobileBottomNavigation(
                    selected = mobilePrimaryDestination,
                    onHome = { openSection("home") },
                    onMovies = { openSection("movies") },
                    onSeries = { openSection("series") },
                    onLive = { openSection("live") },
                    onSearch = { libraryDestination = LibraryDestination.SEARCH },
                    onMyList = { libraryDestination = LibraryDestination.MY_LIST },
                    onSettings = onOpenSettings,
                    collapsed = mobileNavCollapsed,
                    showSettingsAction = isCatalogHome && channels.isEmpty(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // ΤΗΛΕΟΡΑΣΗ: η μπάρα προόδου φόρτωσης ΕΠΙΠΛΕΕΙ κάτω-αριστερά ως
            // discreet pill — ΔΕΝ μπαίνει στη ροή του layout, άρα το άναμμα/σβήσιμό
            // της δεν μετακινεί ΠΟΤΕ το περιεχόμενο (η ρίζα του κάθετου σεισμού).
            if (isTv && state.loading) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC101014))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .width(260.dp)
                ) {
                    CatalogLoadingProgress(vm)
                }
            }

            // Στιγμιαίο μήνυμα: ΕΠΙΠΛΕΕΙ πάνω από τη λίστα (δεν σπρώχνει
            // περιεχόμενο) και σβήνει μόνο του. Κάτω-κέντρο, μακριά από το
            // chrome, όπως τα snackbars.
            androidx.compose.animation.AnimatedVisibility(
                visible = flash != null,
                enter = androidx.compose.animation.fadeIn(
                    tween(motionDuration(Motion.Overlay), easing = Motion.EmphasizedEasing)
                ),
                exit = androidx.compose.animation.fadeOut(
                    tween(motionDuration(Motion.Fast), easing = Motion.StandardEasing)
                ),
                modifier = Modifier.align(Alignment.BottomCenter).padding(
                    bottom = if (isTv) 16.dp else premiumMobileNavigationContentPadding(extra = 8.dp)
                )
            ) {
                Text(
                    flash ?: "",
                    color = TextHi, fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE61A1A22))
                        .border(1.dp, Line, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }

        }
        } // τέλος του Box που παρακολουθεί το focus του περιεχομένου

        // Σε πλήρη οθόνη το μενού ΔΕΝ συντίθεται: αλλιώς ζωγραφιζόταν πάνω από την
        // εικόνα και — χειρότερα — τραβούσε το focus σε κάθε αλλαγή καναλιού,
        // ανοίγοντας και τα ονόματα.
        if (isTv && !liveFullscreen) {
            Box(
                // Ο FocusRequester ΔΕΝ μπαίνει πια εδώ: ένα focusGroup δίνει το
                // focus στο πρώτο του παιδί, οπότε το BACK κατέληγε πάντα στην
                // «Αναζήτηση». Περνά μέσα, στο ενεργό στοιχείο.
                Modifier.align(Alignment.CenterStart)
                    .focusGroup()
                    .onFocusChanged { navRailFocused = it.hasFocus }
            ) {
            PremiumTvNavigationRail(
                profileName = activeProfileName,
                currentContentType = state.contentType,
                homeSelected = isCatalogHome,
                libraryDestination = libraryDestination,
                epgAvailable = state.epgLoaded,
                onHome = { openSection("home") },
                onSearch = { libraryDestination = LibraryDestination.SEARCH },
                onMyList = { libraryDestination = LibraryDestination.MY_LIST },
                onContinueWatching = { libraryDestination = LibraryDestination.CONTINUE_WATCHING },
                onHistory = { libraryDestination = LibraryDestination.HISTORY },
                onLive = { openSection("live") },
                onMovies = { openSection("movies") },
                onSeries = { openSection("series") },
                onEpg = {
                    if (state.epgLoaded) showGrid = true
                    else toast(ctx, "Δεν έχει φορτωθεί EPG")
                },
                onSources = onBack,
                onSettings = onOpenSettings,
                // Ανοίγει με τα ονόματα μόνο όταν ΜΕΝΕΙΣ μέσα στο μενού — όχι
                // όταν το focus απλώς περνά από πάνω του.
                expanded = navRailExpanded,
                selectedFocus = navRailFocus,
                // Εστιάσιμο όσο κάτι άλλο κρατά το focus (άρα ο χρήστης πλοηγείται
                // και το αριστερό πρέπει να δουλέψει με το πρώτο πάτημα), όσο το
                // ζήτησε ρητά με BACK, ή όσο το χρησιμοποιεί ήδη.
                interactive = contentHasFocus || navRailArmed || navRailFocused
            )
            }
        }
    }

    // ΤΗΛΕΟΡΑΣΗ: αναπαραγωγή ταινίας/επεισοδίου στο ΚΟΙΝΟ επίπεδο, πάνω απ' όλα.
    // Ίδια χειριστήρια με τα ζωντανά (ήχος, υπότιτλοι, αναλογία) συν μπάρα
    // προόδου και ±10 δευτ., χωρίς να ανοίγει ξεχωριστό Activity.
    inlinePlayback?.let { target ->
        BrowsePlaybackLayer(
            target = target,
            vm = vm,
            isTv = isTv,
            favoriteKeys = state.favorites,
            seasons = state.seriesSeasons,
            catalogChannels = state.channels,
            parentContent = detailChannel,
            onClose = { inlinePlayback = null; recentsTick++ },
            onPlayOther = { next ->
                vm.addRecent(next)
                inlinePlayback = next
            },
            onOpenDetails = { related ->
                inlinePlayback = null
                vm.closeSeries()
                detailChannel = related
                if (related.kind == "series") vm.openSeries(related)
            },
        )
    }

    // Οι overlays μπαίνουν ΠΑΝΩ από ολόκληρη την οθόνη. Όταν ήταν μέσα στο Box,
    // φαινόταν και το top bar από κάτω -> δύο «πίσω» ταυτόχρονα.
    detailChannel?.let { dch ->
        // Το focus της επιστροφής ΔΕΝ ζητιέται από εδώ.
        //
        // Η οθόνη λεπτομερειών ξέρει ήδη πού πρέπει να πάει (στο «Πίσω», ώστε να
        // μην ανοίγει κομμένο το hero). Της λείπει μόνο η πληροφορία ΠΟΤΕ — και
        // αυτή της τη δίνουμε με το obscuredByPlayer παρακάτω.
        //
        // Μια δεύτερη διεκδίκηση από εδώ θα ανταγωνιζόταν τη δική της: το ίδιο
        // μοτίβο που έκανε τον player να συμπεριφέρεται διαφορετικά σε κάθε
        // άνοιγμα, ανάλογα με το ποιο αίτημα έτρεχε τελευταίο.
        Box(
            Modifier
                .fillMaxSize()
                // ΤΟ ΣΗΜΕΙΟ ΠΟΥ ΞΕΦΥΓΕ.
                //
                // Είχαμε ήδη απενεργοποιήσει το focus στο κύριο περιεχόμενο όσο
                // παίζει κάτι από πάνω. Η οθόνη λεπτομερειών όμως ΔΕΝ είναι μέσα
                // σε αυτό — είναι αδελφός του, σκόπιμα, ώστε να σκεπάζει και την
                // πάνω μπάρα. Έμενε λοιπόν ζωντανή και εστιάσιμη κάτω από τον
                // player: πατούσες δεξιά περιμένοντας να πας στο επόμενο κουμπί
                // και το focus έφευγε στα επεισόδια από κάτω.
                //
                // Πρέπει να μένει συντεθειμένη (το BACK επιστρέφει εκεί, με τη
                // θέση της λίστας ανέπαφη), αλλά όχι εστιάσιμη.
                //
                // Η ΣΕΙΡΑ ΕΧΕΙ ΣΗΜΑΣΙΑ: το focusProperties ισχύει για τον κόμβο
                // focus που έρχεται ΜΕΤΑ από αυτό στην αλυσίδα. Χωρίς ρητό
                // focusGroup μετά, η απενεργοποίηση δεν είχε ορισμένο στόχο και
                // η συμπεριφορά της ήταν θέμα λεπτομερειών υλοποίησης.
                .then(
                    if (inlinePlayback != null) {
                        Modifier.focusProperties { canFocus = false }
                    } else Modifier
                )
                .focusGroup()
        ) {
            DetailHost(
                ch = dch,
                state = state,
                vm = vm,
                onBack = { detailChannel = null; vm.closeSeries() },
                onPlay = { target, _, _, _ ->
                    // Αναπαραγωγή στο ΚΟΙΝΟ επίπεδο, σε τηλεόραση ΚΑΙ κινητό.
                    // Η οθόνη λεπτομερειών μένει από κάτω, οπότε το BACK επιστρέφει
                    // εκεί — χωρίς μαύρο άνοιγμα ξεχωριστού Activity.
                    inlinePlayback = target
                },
                onOpenRelated = { related ->
                    vm.closeSeries()
                    detailChannel = related
                    if (related.kind == "series") vm.openSeries(related)
                },
                obscuredByPlayer = inlinePlayback != null,
                mobileBottomPadding = if (isTv) 42.dp else premiumMobileNavigationContentPadding()
            )
            if (!isTv) {
                fun leaveDetails() {
                    detailChannel = null
                    vm.closeSeries()
                }
                PremiumMobileBottomNavigation(
                    selected = mobilePrimaryDestination,
                    onHome = { leaveDetails(); openSection("home") },
                    onMovies = { leaveDetails(); openSection("movies") },
                    onSeries = { leaveDetails(); openSection("series") },
                    onLive = { leaveDetails(); openSection("live") },
                    onSearch = {
                        leaveDetails(); libraryDestination = LibraryDestination.SEARCH
                    },
                    onMyList = {
                        leaveDetails(); libraryDestination = LibraryDestination.MY_LIST
                    },
                    onSettings = {
                        leaveDetails(); onOpenSettings()
                    },
                    showSettingsAction = true,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
    if (state.askRefreshMode) RefreshModeDialog(
        contentType = state.contentType,
        onExisting = { vm.refreshExistingSelection() },
        onChooseGroups = { vm.refreshAndChooseGroups() },
        onCancel = { vm.cancelRefreshChoice() }
    )
    // Το ενδιάμεσο «Φόρτωση Χ;» dialog αφαιρέθηκε — το pill tap είναι η πρόθεση.
    if (state.askLoadMode) LoadModeDialog(
        count = state.categories.size,
        onAll = { vm.loadEverything() },
        onChoose = { vm.chooseCategories() },
        onCancel = { vm.cancelLoadMode() }
    )
    if (state.chooseContent) ContentChooser(
        // Χωρίς φορτωμένο περιεχόμενο δεν υπάρχει «τρέχουσα» ενότητα:
        // το τικ στο Live μπέρδευε, έδειχνε σαν να είναι ήδη επιλεγμένο.
        current = if (state.channels.isEmpty()) "" else state.contentType,
        onClose = if (state.channels.isNotEmpty()) ({ vm.closeContentChooser() }) else null,
        onPick = { type ->
            if (type == "all") vm.loadAllSections() else vm.setContentType(type)
        })
    if (state.pickCategories) CategoryPicker(
        categories = state.categories,
        initialSelectedIds = state.categorySelectionIds,
        onCancel = { vm.cancelCategoryPicker() },
        onLoad = { vm.loadSelectedCategories(it) })
    if (showExport) Box(Modifier.fillMaxSize()) { ExportScreen(vm) { showExport = false } }

    epgChannel?.let { ch -> EpgSheet(ch, vm) { epgChannel = null } }

    multiviewFailure?.let { failure ->
        AlertDialog(
            onDismissRequest = { multiviewFailure = null },
            confirmButton = {
                TvDialogTextButton(
                    label = stringResource(R.string.live_close),
                    color = AccentSoft,
                    onClick = { multiviewFailure = null },
                )
            },
            title = { Text(stringResource(R.string.live_playback_error), color = TextHi) },
            text = {
                Text(
                    stringResource(
                        when (failure) {
                            MultiviewLaunchFailure.PRIMARY_UNAVAILABLE -> R.string.live_multiview_primary_unavailable
                            MultiviewLaunchFailure.SECONDARY_UNAVAILABLE -> R.string.live_multiview_secondary_unavailable
                            MultiviewLaunchFailure.START_FAILED -> R.string.live_multiview_start_failed
                        }
                    ),
                    fontSize = 12.sp,
                    color = TextMid,
                )
            },
            containerColor = BgElev2
        )
    }
}

@Composable
private fun CatalogLoadingProgress(vm: MainViewModel) {
    val progressState by vm.catalogProgressState.collectAsStateWithLifecycle()
    val active = progressState.sourceProgress[vm.currentSourceId()]?.takeIf { it.active }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        // Η ενότητα έρχεται από το ΕΝΕΡΓΟ progress, όχι από το state.contentType:
        // κατά τη «λήψη όλων» ο χρήστης μπορεί να στέκεται στις Ταινίες ενώ
        // κατεβαίνουν οι Σειρές, και η μπάρα πρέπει να λέει τι όντως τρέχει.
        val label = localizedCatalogProgress(active?.percent, active?.contentType)
        Text(label, color = TextMid, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        val percent = active?.percent
        if (percent != null) {
            LinearProgressIndicator(
                progress = { (percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = Accent,
                trackColor = BgElev2
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Accent,
                trackColor = BgElev2
            )
        }
    }
}
