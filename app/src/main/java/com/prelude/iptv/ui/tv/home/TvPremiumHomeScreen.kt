package com.prelude.iptv.ui.tv.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.CatalogRailSection
import com.prelude.iptv.ui.TvDialogTextButton
import com.prelude.iptv.ui.tvFocus
import com.prelude.iptv.ui.components.home.HomeCinematicBackdrop
import com.prelude.iptv.ui.components.home.homeHeroCandidates
import com.prelude.iptv.ui.requestFocusWithRetry

private data class TvHomeHeroState(
    val channel: Channel,
    val meta: TmdbClient.Meta?
)

/**
 * TV Home — αναδομημένο πιστά στο Figma «Premium Home Page desktop/TV»:
 * fullscreen backdrop, hero info αριστερά με Play/Πληροφορίες pills, και από
 * κάτω rails με καθαρές portrait αφίσες. Το αριστερό icon rail έρχεται από το
 * PremiumTvNavigationRail (icon-only, κόκκινο underline στο ενεργό).
 *
 * Θεμέλια σταθερότητας (ΜΗΝ αφαιρεθούν — καθένα έλυσε πραγματικό «σεισμό»):
 * 1. homeKey: τα states ΔΕΝ κλειδώνουν στο list identity — τα partial publishes
 *    του καταλόγου (κάθε ~900ms στη φόρτωση) δεν κάνουν κανένα reset.
 * 2. Ενιαίο commit ανά ταινία (κείμενα+εικόνα ΜΑΖΙ, με timeout) — ποτέ δεύτερη
 *    «διόρθωση» που ξαναγράφει το info.
 * 3. Το hero είναι ΕΚΤΟΣ της λίστας που σκρολάρει (δεν ανήκει σε scroll
 *    container), και τα rails έχουν δικό τους viewport διαστασιολογημένο ώστε
 *    το πρώτο rail να χωράει ολόκληρο. Καμία ταλάντωση scroll, ποτέ.
 * 4. Όλα τα ύψη του hero είναι κλειδωμένα και το info αλλάζει με Crossfade.
 */
@Composable
fun TvPremiumHomeScreen(
    channels: List<Channel>,
    sections: List<CatalogRailSection>,
    favoriteKeys: Set<String>,
    profileName: String,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?,
    onPlay: (Channel) -> Unit,
    onDetails: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) return
    val firstHero = remember(channels) { homeHeroCandidates(channels).first() }
    // Σταθερό κλειδί ταυτότητας: ίδιο όσο δεν αλλάζει πραγματικά ο κατάλογος.
    val homeKey = remember(channels) { PlaybackQueue.favKey(firstHero) }

    var hero by remember(homeKey) { mutableStateOf(TvHomeHeroState(firstHero, null)) }
    var focusedChannel by remember(homeKey) { mutableStateOf(firstHero) }
    // Το backdrop αλλάζει ΜΙΑ φορά ανά ταινία, μαζί με το info.
    var backdropImage by remember(homeKey) { mutableStateOf(firstHero.logo) }

    LaunchedEffect(focusedChannel) {
        val settledChannel = focusedChannel
        delay(450)
        // Εγγυημένο, ενιαίο commit: το TMDB δεν επιτρέπεται να το μπλοκάρει ούτε
        // να το ξαναγράψει αργότερα. Αν δεν προλάβει το 1.2s, η ταινία δείχνει
        // τα στοιχεία του καναλιού και ΜΕΝΕΙ έτσι.
        val quick = withTimeoutOrNull(1200) {
            try {
                tmdbFor(settledChannel)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
        hero = TvHomeHeroState(settledChannel, quick)
        val bd = quick?.backdrop
        backdropImage = if (!bd.isNullOrBlank()) bd else settledChannel.logo
    }

    // Κάρτα που ζήτησε μενού ενεργειών με παρατεταμένο OK.
    var actionChannel by remember { mutableStateOf<Channel?>(null) }

    // ================== ΜΙΑ ΣΕΙΡΑ ΤΗ ΦΟΡΑ, ΧΩΡΙΣ ΚΑΘΕΤΟ SCROLL ==================
    // Όλες οι προηγούμενες προσπάθειες απέτυχαν για τον ίδιο λόγο: όσο υπάρχει
    // κάθετο scrollable (LazyColumn), το bring-into-view μπορεί ΠΑΝΤΑ να το
    // μετακινήσει λίγα pixel σε κάθε αλλαγή focus — και καμία ρύθμιση δεν το
    // σταματά (ούτε userScrollEnabled = false, ούτε ακριβής γεωμετρία).
    //
    // Εδώ το κάθετο scrollable απλώς ΔΕΝ ΥΠΑΡΧΕΙ: εμφανίζεται ΜΟΝΟ η ενεργή
    // σειρά, και τα πάνω/κάτω βελάκια αλλάζουν ποια είναι αυτή. Χωρίς scroll
    // container, καμία κάθετη μετατόπιση δεν είναι δυνατή — εξ ορισμού.
    val sectionsSignature = remember(sections) {
        TvHomeRailPolicy.signature(sections.map { it.id })
    }
    var focusedRail by remember(homeKey) { mutableIntStateOf(0) }
    // Αλλαγή ομάδας/ενότητας: πάντα από την πρώτη σειρά.
    LaunchedEffect(sectionsSignature) { focusedRail = 0 }
    // Ο κατάλογος μπορεί να ξαναχτιστεί με λιγότερες σειρές ενώ βρίσκεσαι βαθιά.
    val safeRail = TvHomeRailPolicy.coerce(focusedRail, sections.size)
    // Το focus προσγειώνεται στην πρώτη κάρτα της σειράς που μόλις εμφανίστηκε.
    val railFocus = remember { FocusRequester() }
    var railChanged by remember { mutableStateOf(false) }
    LaunchedEffect(focusedRail) {
        if (railChanged) {
            railFocus.requestFocusWithRetry()
            railChanged = false
        }
    }

    // ---- ΚΛΟΠΗ FOCUS (η δεύτερη πηγή του τρεμουλιάσματος) ----
    // Πριν: rememberInitialFocus(key = homeKey). Το homeKey προκύπτει από το
    // πρώτο κανάλι με logo, άρα ΑΛΛΑΖΕΙ όταν φτάνουν partial publishes ή όταν
    // ενημερώνεται το state (π.χ. το περιοδικό μήνυμα EPG). Κάθε τέτοια αλλαγή
    // έφτιαχνε ΝΕΟ FocusRequester και ζητούσε ξανά focus στο κουμπί του hero —
    // δηλαδή σου έκλεβε το focus από την κάρτα που περιηγόσουν και σε πετούσε
    // πίσω στο hero.
    // Τώρα: το αρχικό focus ζητείται ΜΙΑ ΚΑΙ ΜΟΝΗ φορά, ποτέ ξανά.
    var initialFocusDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialFocusDone) {
            railFocus.requestFocusWithRetry()
            initialFocusDone = true
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        // Το hero είναι σταθερό στοιχείο, εκτός οποιουδήποτε scroll container.
        // Η σειρά από κάτω χρειάζεται 237dp (padding 10 + τίτλος 21 + κενό 8 +
        // [8 + κάρτα 176 + 8] + padding 6) — η κάρτα κρατά 176dp επειδή δεσμεύει
        // από πριν τον χώρο του scale 1.12. Κρατάμε 250dp με περιθώριο.
        //
        // Υπολογίζονται ΕΔΩ (scope του BoxWithConstraints): μέσα σε Column το
        // DslMarker του Compose δεν επιτρέπει πρόσβαση στο maxHeight.
        val heroHeight = (maxHeight - 250.dp).coerceIn(150.dp, 372.dp)
        val railHeight = (maxHeight - heroHeight).coerceAtLeast(250.dp)
        HomeCinematicBackdrop(
            channel = hero.channel,
            meta = hero.meta,
            mobile = false,
            modifier = Modifier.fillMaxSize(),
            imageOverride = backdropImage
        )
        Column(Modifier.fillMaxSize()) {
            // ΕΚΤΟΣ scroll — δεν μετακινείται ποτέ.
            TvPremiumHomeHero(
                channel = hero.channel,
                meta = hero.meta,
                heroHeight = heroHeight
            )
            // ΜΟΝΟ η ενεργή σειρά. Κανένα κάθετο scroll container -> καμία κάθετη
            // μετατόπιση είναι δυνατή. Τα πάνω/κάτω βελάκια αλλάζουν σειρά.
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val move = when (event.key) {
                            Key.DirectionDown -> TvHomeRailPolicy.moveDown(safeRail, sections.size)
                            Key.DirectionUp -> TvHomeRailPolicy.moveUp(safeRail)
                            else -> return@onPreviewKeyEvent false
                        }
                        if (move.consumed) {
                            focusedRail = move.index
                            railChanged = true
                        }
                        move.consumed
                    }
            ) {
                sections.getOrNull(safeRail)?.let { section ->
                    TvPremiumHomeRail(
                        section = section,
                        favoriteKeys = favoriteKeys,
                        onFocused = { channel -> focusedChannel = channel },
                        onOpen = if (section.id == "continue") onPlay else onDetails,
                        onLongOpen = { channel -> actionChannel = channel },
                        railHeight = railHeight,
                        firstCardFocus = railFocus
                    )
                }
            }
        }

        // Μενού ενεργειών (παρατεταμένο OK σε κάρτα): Αναπαραγωγή / Πληροφορίες.
        actionChannel?.let { target ->
            val actionFocus = remember(target) { FocusRequester() }
            LaunchedEffect(target) { actionFocus.requestFocusWithRetry() }
            AlertDialog(
                onDismissRequest = { actionChannel = null },
                containerColor = IptvColors.SurfaceRaised,
                title = {
                    Text(
                        target.name,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvActionRow("Αναπαραγωγή", Modifier.focusRequester(actionFocus)) {
                            actionChannel = null
                            onPlay(target)
                        }
                        TvActionRow("Πληροφορίες") {
                            actionChannel = null
                            onDetails(target)
                        }
                        TvActionRow(
                            if (PlaybackQueue.favKey(target) in favoriteKeys) "Αφαίρεση από τη λίστα μου"
                            else "Προσθήκη στη λίστα μου"
                        ) {
                            actionChannel = null
                            onToggleFavorite(target)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TvDialogTextButton(
                        label = "Κλείσιμο",
                        color = Color.White,
                        onClick = { actionChannel = null }
                    )
                }
            )
        }
    }
}

/** Γραμμή ενέργειας με ορατό focus για τηλεόραση. */
@Composable
private fun TvActionRow(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        label,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .tvFocus(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}
