package com.prelude.iptv.ui.mobile.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/** Πόση ακινησία χρειάζεται για να ξαναβγεί η μπάρα. */
private const val IDLE_RETURN_MS = 400L

/**
 * Κατώφλι κίνησης πριν θεωρηθεί σκρολάρισμα.
 *
 * Δεν είναι pixel οθόνης: το [LazyListState] δίνει «δείκτης + μετατόπιση», και
 * τα δύο μαζί γίνονται εδώ ένας μονότονος αριθμός θέσης. Μας ενδιαφέρει μόνο η
 * φορά και το ότι κάτι κινήθηκε αισθητά.
 */
private const val MOVE_THRESHOLD = 6

/** Πόσο «πέφτει» η μπάρα για να βγει εντελώς από την οθόνη. */
internal val ChromeHideDistance = 120.dp

/**
 * true όσο η πλωτή μπάρα πρέπει να είναι μαζεμένη.
 *
 * ΓΙΑΤΙ ΑΚΙΝΗΣΙΑ ΚΑΙ ΟΧΙ ΜΟΝΟ ΚΑΤΕΥΘΥΝΣΗ: το κλασικό «κρύψε κατεβαίνοντας, δείξε
 * ανεβαίνοντας» απαιτεί από τον χρήστη μια κίνηση προς τα πάνω για να ξαναδεί τη
 * μπάρα. Όταν όμως σταματά επειδή βρήκε αυτό που έψαχνε, δεν θέλει να σκρολάρει —
 * θέλει να πατήσει. Έτσι η μπάρα φεύγει από τη μέση όσο κινείσαι και επιστρέφει
 * μόνη της μόλις σταθείς.
 *
 * Στην κορυφή είναι πάντα εκεί: δεν υπάρχει τίποτα να σκεπάσει.
 */
@Composable
internal fun rememberChromeHidden(state: LazyListState): Boolean {
    var hidden by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        var previous = 0
        snapshotFlow { state.firstVisibleItemIndex * 10_000 + state.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { position ->
                hidden = when {
                    position < 40 -> false
                    position > previous + MOVE_THRESHOLD -> true
                    position < previous - MOVE_THRESHOLD -> false
                    else -> hidden
                }
                previous = position
            }
    }

    // Ξεχωριστό effect με κλειδί την ΤΡΕΧΟΥΣΑ θέση: κάθε κίνηση το ακυρώνει και
    // το ξαναξεκινά, οπότε ο χρόνος μετρά πραγματική ακινησία — όχι τον χρόνο από
    // το πρώτο άγγιγμα.
    val position = state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
    LaunchedEffect(position) {
        delay(IDLE_RETURN_MS)
        hidden = false
    }

    return hidden
}
