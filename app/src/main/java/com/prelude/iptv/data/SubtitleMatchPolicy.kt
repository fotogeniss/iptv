package com.prelude.iptv.data

import kotlin.math.roundToInt

/**
 * Υπολογίζει πόσο καλά ταιριάζει ένα αποτέλεσμα OpenSubtitles στο media.
 *
 * Το API δεν επιστρέφει έτοιμο «match %». Το ποσοστό εδώ είναι ντετερμινιστικό
 * και βασίζεται στα στοιχεία που πράγματι έχουμε: τίτλος, έτος, σεζόν,
 * επεισόδιο και δημοτικότητα του αρχείου. Χρησιμοποιείται και για ταξινόμηση και
 * για εμφάνιση, ώστε το νούμερο που βλέπει ο χρήστης να συμφωνεί με τη σειρά.
 */
object SubtitleMatchPolicy {

    /** Reject unrelated titles and explicitly wrong movie/episode identities. */
    fun accepts(
        request: SubtitleSearchRequest,
        fileName: String,
        release: String,
        featureTitle: String,
        year: Int?,
        season: Int?,
        episode: Int?,
    ): Boolean {
        if (request.year != null && year != null && request.year != year) return false

        val parsed = listOf(fileName, release)
            .mapNotNull(SubtitleSearchPolicy::parseEpisode)
            .firstOrNull()
        val actualSeason = season ?: parsed?.season
        val actualEpisode = episode ?: parsed?.episode
        if (request.season != null && actualSeason != null && request.season != actualSeason) return false
        if (request.episode != null && actualEpisode != null && request.episode != actualEpisode) return false

        val wanted = normalize(request.title)
        if (wanted.isBlank()) return false
        return listOf(featureTitle, release, fileName)
            .map(::normalize)
            .filter(String::isNotBlank)
            .any { candidate -> strongTitleMatch(wanted, candidate) }
    }

    fun percent(
        request: SubtitleSearchRequest,
        fileName: String,
        release: String,
        featureTitle: String,
        year: Int?,
        season: Int?,
        episode: Int?,
        downloads: Int,
    ): Int {
        val wanted = normalize(request.title)
        val candidates = listOf(featureTitle, release, fileName)
            .map(::normalize)
            .filter { it.isNotBlank() }

        var score = 35
        when {
            wanted.isBlank() -> Unit
            candidates.any { it == wanted } -> score += 30
            candidates.any { it.contains(wanted) || wanted.contains(it) } -> score += 24
            else -> score += (tokenOverlap(wanted, candidates) * 20f).roundToInt()
        }

        score += fieldScore(request.year, year, match = 5, missing = 0, mismatch = -10)
        score += fieldScore(request.season, season, match = 10, missing = -3, mismatch = -20)
        score += fieldScore(request.episode, episode, match = 15, missing = -5, mismatch = -25)
        score += when {
            downloads >= 10_000 -> 5
            downloads >= 1_000 -> 4
            downloads >= 100 -> 3
            downloads >= 10 -> 2
            downloads > 0 -> 1
            else -> 0
        }
        return score.coerceIn(10, 99)
    }

    private fun fieldScore(
        requested: Int?,
        actual: Int?,
        match: Int,
        missing: Int,
        mismatch: Int,
    ): Int = when {
        requested == null -> 0
        actual == null -> missing
        requested == actual -> match
        else -> mismatch
    }

    private fun tokenOverlap(wanted: String, candidates: List<String>): Float {
        val wantedTokens = wanted.split(' ').filter { it.length > 1 }.toSet()
        if (wantedTokens.isEmpty()) return 0f
        return candidates.maxOfOrNull { candidate ->
            val candidateTokens = candidate.split(' ').filter { it.length > 1 }.toSet()
            wantedTokens.intersect(candidateTokens).size.toFloat() / wantedTokens.size
        } ?: 0f
    }

    private fun strongTitleMatch(wanted: String, candidate: String): Boolean {
        if (candidate == wanted) return true
        val wantedTokens = wanted.split(' ').filter { it.length > 1 }.toSet()
        val candidateTokens = candidate.split(' ').filter { it.length > 1 }.toSet()
        if (wantedTokens.isEmpty()) return candidateTokens.contains(wanted)
        val overlap = wantedTokens.intersect(candidateTokens).size.toFloat() / wantedTokens.size
        return overlap >= .75f
    }

    private fun normalize(value: String): String =
        SubtitleSearchPolicy.cleanTitle(value).lowercase().replace(Regex("""\s+"""), " ").trim()
}
