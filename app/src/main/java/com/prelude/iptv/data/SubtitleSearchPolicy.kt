package com.prelude.iptv.data

/** Exact media identity sent to OpenSubtitles. */
data class SubtitleSearchRequest(
    val title: String,
    val year: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val type: String = "all"
) {
    fun displayQuery(): String = buildString {
        append(title)
        if (season != null) append(" S").append(season.toString().padStart(2, '0'))
        if (episode != null) append("E").append(episode.toString().padStart(2, '0'))
        if (year != null && season == null && episode == null) append(" (").append(year).append(')')
    }

    fun apiParameters(language: String): Map<String, String> = buildMap {
        put("query", title)
        put("languages", language)
        if (type == "movie" || type == "episode") put("type", type)
        year?.let { put("year", it.toString()) }
        season?.let { put("season_number", it.toString()) }
        episode?.let { put("episode_number", it.toString()) }
    }
}

/** Pure title/episode normalization used before every subtitle lookup. */
object SubtitleSearchPolicy {
    private val episodePatterns = listOf(
        Regex("""(?i)^(.*?)[\s._\-:|]*S(?:EASON)?\s*0*(\d{1,2})\s*[._\- ]*E(?:P(?:ISODE)?)?\s*0*(\d{1,3})(?:\b|[\s._\-:|])(.*)$"""),
        Regex("""(?i)^(.*?)[\s._\-:|]+0*(\d{1,2})\s*[xX]\s*0*(\d{1,3})(?:\b|[\s._\-:|])(.*)$"""),
        Regex("""(?i)^(.*?)[\s._\-:|]*(?:SEASON|ΣΕΖΟΝ)\s*0*(\d{1,2})[\s._\-:|]+(?:EPISODE|EP|ΕΠΕΙΣΟΔΙΟ)\s*0*(\d{1,3})(?:\b|[\s._\-:|])(.*)$""")
    )
    private val standaloneEpisode = Regex("""(?i)(?:EPISODE|EP|ΕΠΕΙΣΟΔΙΟ)\s*0*(\d{1,3})\b""")
    private val compactEpisode = Regex("""(?i)\bS0*(\d{1,2})\s*[._\- ]*E0*(\d{1,3})\b""")
    private val seasonLabel = Regex("""(?i)(?:SEASON|S|ΣΕΖΟΝ)\s*0*(\d{1,2})\b""")
    private val yearPattern = Regex("""\b((?:19|20)\d{2})\b""")
    private val qualityNoise = Regex(
        """(?i)\b(4K|UHD|FHD|HD|SD|HEVC|H\.?265|H\.?264|1080P?|720P?|2160P?|WEB[ ._-]?DL|WEBRIP|BLU[ ._-]?RAY|BDRIP|DVDRIP|HDR10?|DOLBY[ ._-]?VISION|MULTI|DUAL|DUB(?:BED)?|SUB(?:BED)?|VOSTFR|IMAX|EXTENDED|REMASTERED|UNCUT|PROPER|REPACK|AAC|AC3|DTS)\b"""
    )
    private val bracketNoise = Regex("""[\[\(][^\]\)]*[\]\)]""")
    private val leadingProvider = Regex("""^\s*(?:[A-Z]{2,4}|[A-Z]{2,4}\d?)\s*[|:\-]\s*""")
    private val releaseTail = Regex("""(?i)\s*[|\-:]\s*(?:HBO|AMAZON|DISNEY\+?|APPLE TV\+?|PRELUDE\+?)\s*$""")
    private val trailingLanguage = Regex(
        """(?:\s*[|:\-]\s*|\s+)(?:DE|EN|ENG|GER|FR|FRE|IT|ITA|ES|SPA|EL|GR|GRE|PT|PL|RU|AR|TR|NL|CZ|RO|HU)$"""
    )
    private val separators = Regex("""[._]+""")
    private val whitespace = Regex("""\s+""")

    data class EpisodeIdentity(val seriesTitle: String, val season: Int, val episode: Int)

    fun movie(rawTitle: String, yearHint: String = ""): SubtitleSearchRequest = SubtitleSearchRequest(
        title = cleanTitle(rawTitle),
        year = extractYear(rawTitle, yearHint),
        type = "movie"
    )

    fun generic(rawTitle: String): SubtitleSearchRequest = SubtitleSearchRequest(
        title = cleanTitle(rawTitle),
        year = extractYear(rawTitle),
        type = "all"
    )

    fun episode(
        seriesTitle: String,
        yearHint: String = "",
        season: Int?,
        episode: Int?
    ): SubtitleSearchRequest = SubtitleSearchRequest(
        title = cleanTitle(seriesTitle),
        year = extractYear(seriesTitle, yearHint),
        season = season?.takeIf { it > 0 },
        episode = episode?.takeIf { it > 0 },
        type = "episode"
    )

    fun fromChannel(channel: Channel, yearHint: String = channel.year): SubtitleSearchRequest {
        val parsed = parseEpisode(channel.name)
        return if (channel.kind == "series_ep" || parsed != null) {
            episode(
                seriesTitle = parsed?.seriesTitle ?: channel.name,
                yearHint = yearHint,
                season = parsed?.season ?: seasonNumber(channel.group),
                episode = parsed?.episode ?: episodeNumber(channel.name)
            )
        } else if (channel.kind == "vod" || channel.kind == "movie") {
            movie(channel.name, yearHint)
        } else {
            generic(channel.name)
        }
    }

    /**
     * Playback-aware identity. Episode playlists often name an item only "S01E03";
     * [seriesTitle] supplies the parent series name shown on its details page.
     */
    fun fromPlayback(
        channel: Channel,
        seriesTitle: String,
        yearHint: String = channel.year,
    ): SubtitleSearchRequest {
        if (channel.kind != "series_ep" && parseEpisode(channel.name) == null) {
            return fromChannel(channel, yearHint)
        }
        val parsed = parseEpisode(channel.name)
        val parent = cleanTitle(seriesTitle).takeIf { it.isNotBlank() }
        val resolvedYear = extractYear(seriesTitle, yearHint)?.toString().orEmpty()
        return episode(
            seriesTitle = parsed?.seriesTitle?.takeIf { it.isNotBlank() }
                ?: parent
                ?: cleanTitle(channel.name),
            yearHint = resolvedYear,
            season = parsed?.season ?: seasonNumber(channel.name) ?: seasonNumber(channel.group),
            episode = parsed?.episode ?: episodeNumber(channel.name),
        )
    }

    /** Editable manual query while retaining structured episode identity. */
    fun manual(rawQuery: String, fallback: SubtitleSearchRequest): SubtitleSearchRequest {
        parseEpisode(rawQuery)?.let { parsed ->
            return episode(parsed.seriesTitle, fallback.year?.toString().orEmpty(), parsed.season, parsed.episode)
        }
        return when (fallback.type) {
            "episode" -> episode(
                seriesTitle = cleanTitle(rawQuery).takeUnless(::isEpisodeCodeOnly) ?: fallback.title,
                yearHint = fallback.year?.toString().orEmpty(),
                season = fallback.season,
                episode = fallback.episode,
            )
            "movie" -> movie(rawQuery, fallback.year?.toString().orEmpty())
            else -> generic(rawQuery)
        }
    }

    fun parseEpisode(rawTitle: String): EpisodeIdentity? {
        val value = rawTitle.trim()
        for (pattern in episodePatterns) {
            val match = pattern.matchEntire(value) ?: continue
            val title = cleanTitle(match.groupValues[1])
            val season = match.groupValues[2].toIntOrNull() ?: continue
            val episode = match.groupValues[3].toIntOrNull() ?: continue
            if (title.isNotBlank()) return EpisodeIdentity(title, season, episode)
        }
        return null
    }

    fun seasonNumber(label: String, fallback: Int? = null): Int? =
        compactEpisode.find(label)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: seasonLabel.find(label)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: fallback

    fun episodeNumber(title: String, fallback: Int? = null): Int? =
        parseEpisode(title)?.episode
            ?: compactEpisode.find(title)?.groupValues?.getOrNull(2)?.toIntOrNull()
            ?: standaloneEpisode.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: fallback

    private fun isEpisodeCodeOnly(value: String): Boolean =
        compactEpisode.matchEntire(value.trim()) != null

    fun cleanTitle(raw: String): String {
        parseEpisodeWithoutCleaning(raw)?.let { return cleanTitleBase(it.seriesTitle) }
        return cleanTitleBase(raw)
    }

    fun extractYear(raw: String, fallback: String = ""): Int? =
        yearPattern.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: yearPattern.find(fallback)?.groupValues?.getOrNull(1)?.toIntOrNull()

    private fun parseEpisodeWithoutCleaning(rawTitle: String): EpisodeIdentity? {
        val value = rawTitle.trim()
        for (pattern in episodePatterns) {
            val match = pattern.matchEntire(value) ?: continue
            val title = match.groupValues[1].trim()
            val season = match.groupValues[2].toIntOrNull() ?: continue
            val episode = match.groupValues[3].toIntOrNull() ?: continue
            return EpisodeIdentity(title, season, episode)
        }
        return null
    }

    private fun cleanTitleBase(raw: String): String {
        var value = raw.trim()
        value = value.replace(leadingProvider, "")
        value = value.replace(Regex("""\((?:19|20)\d{2}\)"""), " ")
        value = value.replace(bracketNoise, " ")
        value = value.replace(qualityNoise, " ")
        value = value.replace(releaseTail, " ")
        value = value.replace(trailingLanguage, " ")
        value = value.replace(separators, " ")
        value = value.replace(whitespace, " ")
        return value.trim().trim('-', '|', ':').trim().ifBlank { raw.trim() }
    }
}
