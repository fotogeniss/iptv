package com.prelude.iptv.ui.mobile.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.CatalogRailSection
import com.prelude.iptv.ui.home.HomeRail

/**
 * Ζωγραφίζει ένα [HomeRail] με τις υπάρχουσες κάρτες.
 *
 * ΓΙΑΤΙ ΓΕΦΥΡΑ ΚΑΙ ΟΧΙ ΝΕΕΣ ΚΑΡΤΕΣ: οι κάρτες του [MobilePremiumHomeRail] ξέρουν
 * ήδη αφίσες, TMDB, πρόοδο, αγαπημένα και το πάτημα. Ξαναγράφοντάς τες για χάρη
 * ενός νέου μοντέλου δεδομένων θα φτιάχναμε δεύτερο σημείο που πρέπει να
 * διορθώνεται κάθε φορά — και το πρώτο θα ξεχνιόταν.
 *
 * Η μόνη ουσιαστική διαφορά είναι το σχήμα: τα ζωντανά είναι πλατιά, τα υπόλοιπα
 * κάθετες αφίσες. Αυτό δηλώνεται ρητά αντί να μαντεύεται από το id.
 */
@Composable
internal fun MobileHomeRail(
    rail: HomeRail,
    favoriteKeys: Set<String>,
    onOpen: (Channel) -> Unit,
    onViewAll: (HomeRail) -> Unit,
    modifier: Modifier = Modifier,
) {
    MobilePremiumHomeRail(
        section = CatalogRailSection(
            id = rail.id,
            title = rail.title,
            items = rail.items,
            progress = rail.progress,
            allItems = rail.allItems
        ),
        favoriteKeys = favoriteKeys,
        onOpen = onOpen,
        onViewAll = { onViewAll(rail) },
        modifier = modifier,
        portraitOverride = !rail.live
    )
}
