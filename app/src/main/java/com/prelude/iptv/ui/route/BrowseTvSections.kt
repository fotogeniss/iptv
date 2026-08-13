package com.prelude.iptv.ui.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.MainViewModel
import com.prelude.iptv.ui.player.upcomingProgrammes
import com.prelude.iptv.ui.tv.browse.LiveProgramme
import com.prelude.iptv.ui.tv.browse.TvCategoryBrowseScreen
import com.prelude.iptv.ui.tv.browse.TvLiveBrowseScreen

/**
 * Οι οθόνες περιήγησης της τηλεόρασης: ζωντανά και ενότητες (ταινίες/σειρές).
 *
 * ΓΙΑΤΙ ΜΑΖΙ: μοιράζονται τα ίδια δεδομένα και την ίδια απόφαση για το κλείδωμα
 * ομάδων. Μέσα στο [BrowseRoute] η απόφαση αυτή ήταν γραμμένη δύο φορές, μία για
 * κάθε οθόνη — και δύο αντίγραφα του ίδιου ελέγχου γονικού κλειδώματος είναι
 * ακριβώς εκεί που δεν θέλεις απόκλιση.
 *
 * @param onRequestUnlock η ομάδα είναι κλειδωμένη και χρειάζεται PIN. Η οθόνη δεν
 *   αποφασίζει τι σημαίνει αυτό· το ανεβάζει.
 */
@Composable
internal fun BrowseTvSections(
    live: Boolean,
    groups: List<String>,
    selectedGroup: String,
    channels: List<Channel>,
    favoriteKeys: Set<String>,
    contentType: String,
    lockedGroups: Set<String>,
    parentalUnlocked: Boolean,
    vm: MainViewModel,
    onRequestUnlock: (String) -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    onMultiview: (Channel, Channel) -> Unit,
    onOpenDetails: (Channel) -> Unit,
    onPlay: (Channel) -> Unit,
    /** Κλειδί του στοιχείου που άνοιξε τελευταίο — εκεί επιστρέφει το focus. */
    lastOpenedKey: String? = null,
    /** true όσο παίζει ή φαίνεται κάτι από πάνω. */
    obscuredByPlayer: Boolean = false,
) {
    // ΜΙΑ απόφαση κλειδώματος για ΚΑΙ ΤΙΣ ΔΥΟ οθόνες.
    val selectGroup: (String) -> Unit = { group ->
        if (group in lockedGroups && !parentalUnlocked) onRequestUnlock(group)
        else vm.setGroup(group)
    }

    if (live) {
        // Ζωντανά: κατηγορίες -> κανάλια στην ίδια στήλη, με ζωντανή
        // προεπισκόπηση δεξιά. Δεύτερο OK = πλήρης οθόνη.
        TvLiveBrowseScreen(
            groups = groups,
            selectedGroup = selectedGroup,
            channels = channels,
            favoriteKeys = favoriteKeys,
            keyOf = vm::favKey,
            // Κοινή πηγή με το πρόγραμμα μέσα στον player: ο υπολογισμός του
            // «τώρα» γίνεται σε ΕΝΑ σημείο.
            programmesOf = { ch ->
                upcomingProgrammes(ch.tvgId).map { prog ->
                    LiveProgramme(
                        time = prog.time,
                        title = prog.title,
                        description = prog.description,
                        isNow = prog.isNow
                    )
                }
            },
            // Μέσω ViewModel: για πηγές Stalker (MAC portal) η εντολή χρειάζεται
            // ζωντανή συνεδρία, και το resolvePlayableUrl συνδέεται/ξανασυνδέεται
            // μόνο του. Δες το σχόλιο στο BrowsePlaybackLayer.
            resolveUrl = { ch -> vm.resolvePlayableUrl(ch) },
            onSelectGroup = selectGroup,
            onFullscreenChange = onFullscreenChange,
            onMultiview = onMultiview,
            // ΧΩΡΙΣ start padding: η οθόνη πρέπει να φτάνει ως την άκρη ώστε η
            // μεγέθυνση του player να καλύπτει ΟΛΟ το κάδρο. Το περιθώριο για το
            // αριστερό μενού μπαίνει εσωτερικά, μόνο στο περιεχόμενο.
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Ενότητες (Ταινίες / Σειρές): κατηγορίες αριστερά + πλέγμα αφισών δεξιά,
        // σε μαύρο φόντο. Η Αρχική κρατά το cinematic hero — έτσι μπαίνοντας σε
        // ενότητα βλέπεις καθαρή βιβλιοθήκη αντί για επανάληψη της αρχικής.
        TvCategoryBrowseScreen(
            groups = groups,
            selectedGroup = selectedGroup,
            channels = channels,
            favoriteKeys = favoriteKeys,
            contentType = contentType,
            onSelectGroup = selectGroup,
            onOpen = { channel ->
                // Σειρά ή ταινία ανοίγει λεπτομέρειες· ό,τι άλλο παίζει.
                if (channel.kind == "series" || channel.kind == "vod") onOpenDetails(channel)
                else onPlay(channel)
            },
            // Παρατεταμένο OK = άμεση αναπαραγωγή. Εξαίρεση η σειρά: δεν «παίζει»
            // από μόνη της, πρέπει να διαλέξεις επεισόδιο.
            onLongOpen = { channel ->
                if (channel.kind == "series") onOpenDetails(channel) else onPlay(channel)
            },
            lastOpenedKey = lastOpenedKey,
            obscuredByPlayer = obscuredByPlayer,
            // ΟΛΗ η οθόνη: το μαύρο πρέπει να φτάνει ως τις άκρες. Το περιθώριο
            // για τη μπάρα μπαίνει ΜΕΣΑ, μόνο στο περιεχόμενο — αλλιώς γύρω από
            // το μαύρο έμενε γκρι πλαίσιο του γονέα.
            modifier = Modifier.fillMaxSize()
        )
    }
}
