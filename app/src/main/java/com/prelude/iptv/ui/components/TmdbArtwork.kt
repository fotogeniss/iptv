@file:android.annotation.SuppressLint("ProduceStateDoesNotAssignValue")

package com.prelude.iptv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Κάθετη αφίσα για κάρτες καταλόγου, με προτεραιότητα στο TMDB.
 *
 * Οι λίστες (M3U/Xtream) δίνουν άλλοτε κακής ποιότητας, άλλοτε λάθος και πολύ
 * συχνά καθόλου artwork — γι' αυτό κάποιες κάρτες έμεναν κενές ενώ η μεγάλη
 * εικόνα (που έρχεται από TMDB) υπήρχε πάντα.
 *
 * Μέχρι να απαντήσει το TMDB επιστρέφεται ό,τι έχει η λίστα, ώστε να μη μένει
 * κενό· αν το TMDB δεν βρει τίτλο, μένει επίσης αυτό. Ο [TmdbClient] έχει mem +
 * disk cache, οπότε κάθε τίτλος κατεβαίνει μία μόνο φορά.
 *
 * Καλεί απευθείας τον [TmdbClient] (όπως το `MainViewModel.tmdb`), ώστε να μην
 * χρειάζεται να περνάει `tmdbFor` μέσα από κάθε οθόνη και κάρτα.
 *
 * Τα ζωντανά κανάλια εξαιρούνται: δεν είναι ταινίες/σειρές, το TMDB δεν έχει
 * νόημα γι' αυτά και θα σπαταλούσε κλήσεις.
 */
/**
 * Στιγμιότυπο (still) για κάρτα επεισοδίου, από TMDB.
 *
 * [seriesTitle]/[seriesYear] = η ΣΕΙΡΑ (όχι το επεισόδιο), [season] και
 * [episodeNumber] 1-based. Επιστρέφει το artwork του επεισοδίου αν υπάρχει,
 * αλλιώς ό,τι έχει η λίστα — κι αν ούτε αυτό, κενό (ο caller δείχνει placeholder).
 *
 * Μία κλήση δικτύου ανά σεζόν χάρη στο cache του [TmdbClient].
 */
@Composable
fun rememberEpisodeMeta(
    seriesTitle: String,
    seriesYear: String,
    season: Int,
    episodeNumber: Int,
    /** Το TMDB id του παρόχου, αν υπάρχει. Παρακάμπτει την αναζήτηση τίτλου. */
    seriesTmdbId: String = "",
): TmdbClient.EpisodeMeta? {
    val meta by produceState<TmdbClient.EpisodeMeta?>(
        null, seriesTitle, seriesYear, season, episodeNumber, seriesTmdbId,
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                TmdbClient.episodeMeta(seriesTitle, seriesYear, season, seriesTmdbId)[episodeNumber]
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }
    return meta
}

@Composable
fun rememberPosterArtwork(channel: Channel): String {
    val poster by produceState<String?>(null, channel) {
        value = if (channel.kind == "live") null else withContext(Dispatchers.IO) {
            try {
                TmdbClient.fetch(channel.name, channel.kind == "series", channel.year)?.poster
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }
    return poster?.takeIf { it.isNotBlank() } ?: channel.logo
}
