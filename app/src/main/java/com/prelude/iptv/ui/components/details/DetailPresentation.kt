package com.prelude.iptv.ui.components.details

import androidx.compose.runtime.Immutable
import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.CastMember
import com.prelude.iptv.ui.WatchProgress

@Immutable
data class DetailPresentation(
    val title: String,
    val year: String,
    val rating: String,
    val ageRating: String,
    val duration: String,
    val quality: String,
    val genre: String,
    val director: String,
    val plot: String,
    val posterUrl: String,
    val backdropUrl: String,
    /**
     * true όσο περιμένουμε ακόμη απάντηση από το TMDB για το [backdropUrl].
     *
     * ΓΙΑΤΙ ΔΕΝ ΑΡΚΕΙ ΤΟ «backdropUrl είναι κενό»: κενό σημαίνει ΔΥΟ πράγματα —
     * «δεν έχει έρθει ακόμη» και «δεν υπάρχει». Στο πρώτο πρέπει να περιμένουμε
     * (αλλιώς δείχνουμε κάτι που σε ένα δευτερόλεπτο θα αλλάξει), στο δεύτερο
     * πρέπει να δείξουμε την εφεδρεία. Χωρίς αυτή τη διάκριση, ο χρήστης βλέπει
     * πρώτα μια εικόνα και μετά άλλη.
     */
    val backdropPending: Boolean = false,
    val cast: List<CastMember>,
    val notice: String,
    val contentIsSeries: Boolean,
    val seasons: List<Pair<String, List<Channel>>>,
    val relatedItems: List<Channel>,
    val loading: Boolean,
    val isFav: Boolean,
    val movieProgress: WatchProgress?,
    val episodeProgress: Map<String, WatchProgress>,
    val resumeEpisode: Channel?
) {
    val isSeries: Boolean get() = contentIsSeries || seasons.isNotEmpty()
    // Εδώ ήταν το `heroImage = backdropUrl.ifBlank { posterUrl }`.
    //
    // ΑΦΑΙΡΕΘΗΚΕ ΕΠΙΤΗΔΕΣ. Έκρυβε ότι οι δύο εικόνες έχουν ΔΙΑΦΟΡΕΤΙΚΟ ΣΧΗΜΑ:
    // το backdrop είναι πλατύ, η αφίσα κατακόρυφη. Όποιος τις έπαιρνε ως μία
    // τιμή τις μεταχειριζόταν το ίδιο, και μια κατακόρυφη αφίσα κομμένη για να
    // γεμίσει οθόνη 16:9 φαίνεται τεράστια ζουμαρισμένη — ακριβώς το «πρώτα
    // ζουμάρει και μετά κάθεται σωστά» της οθόνης πληροφοριών.
    //
    // Ο καλών παίρνει και τις δύο και αποφασίζει, βλ. DetailCinematicBackdrop.
    val episodes: List<Channel> get() = seasons.flatMap { it.second }
    val primaryEpisode: Channel? get() = resumeEpisode ?: episodes.firstOrNull()
}
