package com.prelude.iptv.ui

import androidx.compose.runtime.Immutable
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient

/** Groups kept for compatibility with older search consumers and tests. */
data class SearchGroups(
    val live: List<Channel>,
    val movies: List<Channel>,
    val series: List<Channel>,
    val other: List<Channel>
) {
    val total: Int get() = live.size + movies.size + series.size + other.size
}

@Immutable
enum class PremiumSearchFilter(val label: String) {
    ALL("Όλα"),
    MOVIES("Ταινίες"),
    SERIES("Σειρές"),
    LIVE("Live"),
    SPORTS("Αθλητικά"),
    DOCUMENTARIES("Ντοκιμαντέρ")
}

/** Pure search presentation policies. The repository/search engine stays unchanged. */
object SearchUiPolicy {
    private val sportsTerms = listOf(
        "sport", "sports", "football", "soccer", "basket", "formula", "tennis",
        "αθλη", "ποδόσφ", "μπάσκετ", "αγών", "champions", "nba", "f1"
    )
    private val documentaryTerms = listOf(
        "documentary", "documentaries", "docu", "nature", "history",
        "ντοκιμαντέρ", "φύση", "ιστορία"
    )

    fun group(items: List<Channel>): SearchGroups = SearchGroups(
        live = items.filter { it.kind == "live" },
        movies = items.filter { it.kind == "vod" || it.kind == "movie" },
        series = items.filter { it.kind == "series" },
        other = items.filter { it.kind !in setOf("live", "vod", "movie", "series") }
    )

    fun suggestions(items: List<Channel>, limit: Int = 8): List<String> =
        items.asSequence()
            .map { it.genre.ifBlank { it.group } }
            .filter { it.isNotBlank() }
            .distinct()
            .take(limit.coerceAtLeast(0))
            .toList()

    fun discovery(items: List<Channel>, limit: Int = 60): List<Channel> {
        val preferred = items.asSequence()
            .filter { it.logo.isNotBlank() }
            .sortedBy { channel ->
                when (channel.kind) {
                    "vod", "movie" -> 0
                    "series" -> 1
                    "live" -> 2
                    else -> 3
                }
            }
            .take(limit)
            .toList()
        return preferred.ifEmpty { items.take(limit) }
    }

    fun filter(items: List<Channel>, filter: PremiumSearchFilter): List<Channel> = when (filter) {
        PremiumSearchFilter.ALL -> items
        // Οι ταινίες εξαιρούν τα ντοκιμαντέρ — αυτά έχουν δική τους κατηγορία
        // (DOCUMENTARIES), ώστε οι καρτέλες να μη διπλοεμφανίζουν το ίδιο vod.
        PremiumSearchFilter.MOVIES -> items.filter {
            (it.kind == "vod" || it.kind == "movie") && !matchesTerms(it, documentaryTerms)
        }
        PremiumSearchFilter.SERIES -> items.filter { it.kind == "series" }
        PremiumSearchFilter.LIVE -> items.filter { it.kind == "live" }
        PremiumSearchFilter.SPORTS -> items.filter { matchesTerms(it, sportsTerms) }
        PremiumSearchFilter.DOCUMENTARIES -> items.filter { matchesTerms(it, documentaryTerms) }
    }

    fun title(query: String, filter: PremiumSearchFilter): String = when {
        query.isNotBlank() -> "Αποτελέσματα για «${query.trim()}»"
        filter != PremiumSearchFilter.ALL -> filter.label
        else -> "Δημοφιλή τώρα"
    }

    fun category(channel: Channel): String = when (channel.kind) {
        "live" -> "Ζωντανό"
        "series" -> "Σειρά"
        "vod", "movie" -> "Ταινία"
        else -> channel.group.ifBlank { "Περιεχόμενο" }
    }

    fun metaLine(channel: Channel, meta: TmdbClient.Meta? = null): String =
        listOf(
            meta?.year?.ifBlank { channel.year } ?: channel.year,
            meta?.genres?.ifBlank { channel.genre.ifBlank { channel.group } }
                ?: channel.genre.ifBlank { channel.group },
            channel.duration
        ).filter(String::isNotBlank).distinct().joinToString(" · ")

    fun description(channel: Channel, meta: TmdbClient.Meta?): String =
        meta?.overview?.takeIf(String::isNotBlank)
            ?: channel.plot.takeIf(String::isNotBlank)
            ?: "Ανακάλυψε περισσότερα για ${channel.name}."

    private fun matchesTerms(channel: Channel, terms: List<String>): Boolean {
        val haystack = listOf(channel.name, channel.group, channel.genre, channel.plot)
            .joinToString(" ").lowercase()
        return terms.any(haystack::contains)
    }
}
