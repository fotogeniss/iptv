package com.prelude.iptv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prelude.iptv.data.Channel
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
    notice: String = "",
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
    val presentation = DetailPresentation(
        title = title,
        year = year,
        rating = rating,
        ageRating = ageRating,
        duration = duration,
        quality = quality,
        genre = genre,
        director = director,
        plot = plot,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        backdropPending = backdropPending,
        cast = cast,
        notice = notice,
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
