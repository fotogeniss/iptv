package com.prelude.iptv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.player.PlaybackEngine
import com.prelude.iptv.ui.localization.labelRes
import com.prelude.iptv.ui.localization.localizedPlaybackSpeed

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
            title = stringResource(R.string.player_resolution),
            options = videoTracks(),
            // «Χωρίς» εδώ σημαίνει αυτόματη επιλογή, όχι απενεργοποίηση —
            // απενεργοποιημένο βίντεο θα ήταν μαύρη οθόνη.
            allowDisable = true,
            disableLabel = stringResource(R.string.player_automatic),
            onSelect = onSelectVideo,
            onDismiss = onDismiss
        )
        PlayerMenu.ASPECT -> PlayerChoiceMenu(
            title = stringResource(R.string.player_aspect_ratio),
            options = AspectMode.entries.map { mode -> stringResource(mode.labelRes()) to (mode == aspectMode) },
            onSelect = { index -> onSelectAspect(AspectMode.entries[index]) },
            onDismiss = onDismiss,
        )
        PlayerMenu.SPEED -> PlayerChoiceMenu(
            title = stringResource(R.string.player_speed),
            options = SPEED_OPTIONS.map { localizedPlaybackSpeed(it) to (it == speed) },
            onSelect = { index -> onSelectSpeed(SPEED_OPTIONS[index]) },
            onDismiss = onDismiss
        )
        PlayerMenu.SLEEP -> PlayerChoiceMenu(
            title = stringResource(R.string.player_sleep_timer),
            options = SLEEP_OPTIONS.map { minutes ->
                val label = if (minutes == 0) {
                    stringResource(R.string.player_none)
                } else {
                    stringResource(R.string.player_minutes, minutes)
                }
                label to (minutes == sleepMinutes)
            },
            onSelect = { index -> onSelectSleep(SLEEP_OPTIONS[index]) },
            onDismiss = onDismiss
        )
        null -> Unit
    }
}
