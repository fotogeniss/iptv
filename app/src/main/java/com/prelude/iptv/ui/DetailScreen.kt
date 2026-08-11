package com.prelude.iptv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.ProviderMetadataPolicy
import com.prelude.iptv.ui.components.details.DetailPresentation
import com.prelude.iptv.ui.mobile.details.MobilePremiumDetailScreen
import com.prelude.iptv.ui.tv.details.TvPremiumDetailScreen

/** Public model retained for the TMDB mapping in MainActivity. */
@Immutable
data class CastMember(
    val name: String,
    val role: String = "",
    val photoUrl: String? = null
)

/**
 * Adaptive premium details entry point.
 *
 * Mobile and TV intentionally have independent compositions while sharing the
 * same immutable content and callbacks. Data, playback and repository contracts
 * remain outside the UI layer.
 */
@Composable
fun DetailScreen(
    title: String,
    /** TMDB id από τον πάροχο· παρακάμπτει την αναζήτηση τίτλου στα επεισόδια. */
    tmdbId: String = "",
    year: String = "",
    rating: String = "",
    ageRating: String = "",
    duration: String = "",
    quality: String = "",
    genre: String = "",
    director: String = "",
    plot: String = "",
    posterUrl: String = "",
    backdropUrl: String = "",
    /** true όσο περιμένουμε το TMDB. Δες [DetailPresentation.backdropPending]. */
    backdropPending: Boolean = false,
    cast: List<CastMember> = emptyList(),
    showTmdbNotice: Boolean = false,
    contentIsSeries: Boolean = false,
    seasons: List<Pair<String, List<Channel>>> = emptyList(),
    relatedItems: List<Channel> = emptyList(),
    loading: Boolean = false,
    isFav: Boolean = false,
    movieProgress: WatchProgress? = null,
    episodeProgress: Map<String, WatchProgress> = emptyMap(),
    resumeEpisode: Channel? = null,
    mobileBottomPadding: Dp = 42.dp,
    onBack: () -> Unit,
    onFav: () -> Unit = {},
    onShare: () -> Unit = {},
    onPlayMovie: () -> Unit = {},
    onRestartMovie: () -> Unit = {},
    onPlayEpisode: (Channel) -> Unit = {},
    onOpenRelated: (Channel) -> Unit = {},
    /** true όσο παίζει κάτι από πάνω — δες [TvPremiumDetailScreen]. */
    obscuredByPlayer: Boolean = false,
    /** Σβήνει την αποθηκευμένη πρόοδο χωρίς να ξεκινήσει αναπαραγωγή. */
    onClearProgress: () -> Unit = {}
) {
    // ΤΟ ΜΟΝΑΔΙΚΟ ΣΗΜΕΙΟ ΠΟΥ ΧΤΙΖΕΤΑΙ Η ΠΑΡΟΥΣΙΑΣΗ, για κινητό ΚΑΙ τηλεόραση.
    //
    // Το `year` και το `duration` φτάνουν εδώ ακριβώς όπως τα έγραψε ο πάροχος,
    // επειδή συμμετέχουν σε κλειδιά ταυτότητας και δεν επιτρέπεται να αλλάξουν
    // στο μοντέλο (δες [ProviderMetadataPolicy]). Καθαρίζονται εδώ, στο πέρασμα
    // προς την οθόνη: πραγματικό portal στέλνει `"time":"N/a"` και
    // `"year":"1993-08-28"`, και τα δύο εμφανίζονταν αυτούσια στη γραμμή
    // μεταδεδομένων ως «1993-08-28 · N/a».
    val presentation = DetailPresentation(
        title = title,
        tmdbId = tmdbId,
        year = ProviderMetadataPolicy.displayYear(year),
        rating = rating,
        ageRating = ProviderMetadataPolicy.text(ageRating),
        duration = ProviderMetadataPolicy.text(duration),
        quality = quality,
        genre = genre,
        director = director,
        plot = plot,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        backdropPending = backdropPending,
        cast = cast,
        showTmdbNotice = showTmdbNotice,
        contentIsSeries = contentIsSeries,
        seasons = seasons,
        relatedItems = relatedItems,
        loading = loading,
        isFav = isFav,
        movieProgress = movieProgress,
        episodeProgress = episodeProgress,
        resumeEpisode = resumeEpisode
    )
    if (isTvDevice()) {
        TvPremiumDetailScreen(
            presentation = presentation,
            onBack = onBack,
            onFav = onFav,
            onShare = onShare,
            onPlayMovie = onPlayMovie,
            onRestartMovie = onRestartMovie,
            onPlayEpisode = onPlayEpisode,
            onOpenRelated = onOpenRelated,
            obscuredByPlayer = obscuredByPlayer,
            onClearProgress = onClearProgress
        )
    } else {
        MobilePremiumDetailScreen(
            presentation = presentation,
            onBack = onBack,
            onFav = onFav,
            onShare = onShare,
            onPlayMovie = onPlayMovie,
            onRestartMovie = onRestartMovie,
            onPlayEpisode = onPlayEpisode,
            onOpenRelated = onOpenRelated,
            bottomContentPadding = mobileBottomPadding
        )
    }

}
