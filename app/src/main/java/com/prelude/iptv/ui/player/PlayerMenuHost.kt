package com.prelude.iptv.ui.player

import androidx.compose.runtime.Composable
import com.prelude.iptv.player.PlaybackEngine

/**
 * Όλοι οι διάλογοι του player, σε ένα σημείο.
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΟ: το [PlayerHost] κρατά τη λογική πλήκτρων και focus — το
 * δυσκολότερο κομμάτι της εφαρμογής, αυτό που έσπασε δεκάδες φορές. Τα μενού δεν
 * έχουν καμία σχέση με αυτήν: είναι λίστες που δείχνουν τιμές και επιστρέφουν
 * επιλογές. Κάθε φορά που προσθέταμε ένα, το αρχείο με τη δύσκολη λογική
 * μεγάλωνε χωρίς λόγο.
 *
 * Εδώ δεν υπάρχει κατάσταση: κάθε τιμή έρχεται από έξω και κάθε επιλογή φεύγει
 * προς τα έξω. Ό,τι αποφασίζει, το αποφασίζει ο καλών.
 */
@Composable
internal fun PlayerMenuHost(
    open: PlayerMenu?,
    audioTracks: List<PlaybackEngine.TrackOption>,
    subtitleTracks: List<PlaybackEngine.TrackOption>,
    /**
     * Συνάρτηση και όχι λίστα: τα κομμάτια βίντεο διαβάζονται από τη μηχανή τη
     * στιγμή που ανοίγει το μενού, και δεν αξίζει να υπολογίζονται σε κάθε
     * recomposition για ένα μενού που σπάνια ανοίγει.
     */
    videoTracks: () -> List<PlaybackEngine.TrackOption>,
    aspectMode: AspectMode,
    speed: Float,
    sleepMinutes: Int,
    subtitleSize: Int,
    subtitleBackground: String,
    subtitleBold: Boolean,
    /** Ο τίτλος που παίζει — προσυμπληρώνει την αναζήτηση. */
    subtitleQuery: String,
    searchSubtitles: (suspend (String) -> List<ExternalSubtitle>)?,
    /** Αυτόματη λήψη· null όπου δεν έχει νόημα (ζωντανά). */
    onAutoFetchSubtitles: (() -> Unit)?,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    onSelectVideo: (String?) -> Unit,
    onSelectAspect: (AspectMode) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSleep: (Int) -> Unit,
    onSubtitleSize: (Int) -> Unit,
    onSubtitleBackground: (String) -> Unit,
    onSubtitleBold: (Boolean) -> Unit,
    onSubtitleChosen: (ExternalSubtitle) -> Unit,
    onDismiss: () -> Unit,
) {
    when (open) {
        // CC και «Γλώσσα ήχου» ανοίγουν το ΙΔΙΟ panel, στο αντίστοιχο tab.
        // Δεν αλλάζει κανένα control του player· αλλάζει μόνο το layer από πάνω.
        PlayerMenu.AUDIO,
        PlayerMenu.SUBTITLES -> PlayerTracksPanel(
            initialTab = if (open == PlayerMenu.AUDIO) PlayerTracksTab.AUDIO else PlayerTracksTab.SUBTITLES,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            subtitleSize = subtitleSize,
            subtitleBackground = subtitleBackground,
            subtitleBold = subtitleBold,
            subtitleQuery = subtitleQuery,
            searchSubtitles = searchSubtitles,
            onAutoFetchSubtitles = onAutoFetchSubtitles,
            onSelectAudio = onSelectAudio,
            onSelectSubtitle = onSelectSubtitle,
            onSubtitleSize = onSubtitleSize,
            onSubtitleBackground = onSubtitleBackground,
            onSubtitleBold = onSubtitleBold,
            onSubtitleChosen = onSubtitleChosen,
            onDismiss = onDismiss,
        )
        PlayerMenu.QUALITY -> PlayerTrackMenu(
            title = "Ανάλυση",
            options = videoTracks(),
            // «Χωρίς» εδώ σημαίνει αυτόματη επιλογή, όχι απενεργοποίηση —
            // απενεργοποιημένο βίντεο θα ήταν μαύρη οθόνη.
            allowDisable = true,
            disableLabel = "Αυτόματη",
            onSelect = onSelectVideo,
            onDismiss = onDismiss
        )
        PlayerMenu.ASPECT -> PlayerChoiceMenu(
            title = "Αναλογία εικόνας",
            options = AspectMode.entries.map { mode -> mode.label to (mode == aspectMode) },
            onSelect = { index -> onSelectAspect(AspectMode.entries[index]) },
            onDismiss = onDismiss,
        )
        PlayerMenu.SPEED -> PlayerChoiceMenu(
            title = "Ταχύτητα αναπαραγωγής",
            options = SPEED_OPTIONS.map { formatSpeed(it) to (it == speed) },
            onSelect = { index -> onSelectSpeed(SPEED_OPTIONS[index]) },
            onDismiss = onDismiss
        )
        PlayerMenu.SLEEP -> PlayerChoiceMenu(
            title = "Χρονοδιακόπτης ύπνου",
            options = SLEEP_OPTIONS.map { minutes ->
                val label = if (minutes == 0) "Καμία" else "$minutes λεπτά"
                label to (minutes == sleepMinutes)
            },
            onSelect = { index -> onSelectSleep(SLEEP_OPTIONS[index]) },
            onDismiss = onDismiss
        )
        null -> Unit
    }
}
