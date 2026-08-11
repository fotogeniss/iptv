package com.prelude.iptv.data

import java.util.Locale

/**
 * Normalizes provider catalog rows into the entities the UI actually presents.
 *
 * Providers do not agree on what a "series" row means. Xtream normally returns
 * one container per series, while many M3U and Stalker lists return one playable
 * row per episode. Keeping those raw rows in the UI produces duplicate series in
 * Home/Search and leaves the details CTA without a reliable first episode.
 *
 * This object is deliberately pure: it does not cache catalogs or touch provider
 * sessions. It only normalizes the freshly downloaded rows for the current source.
 */
data class CatalogNormalization(
    val items: List<Channel>,
    val seriesEpisodes: Map<String, List<Pair<String, List<Channel>>>> = emptyMap()
)

object CatalogNormalizer {
    private val seasonEpisodePatterns = listOf(
        Regex("""(?i)^(.*?)[\s._\-:|]*(?:S(?:EASON)?\s*0*(\d{1,2})\s*[._\- ]*E(?:P(?:ISODE)?)?\s*0*(\d{1,3}))(?:\b|[\s._\-:|])(.*)$"""),
        Regex("""(?i)^(.*?)[\s._\-:|]+0*(\d{1,2})\s*[xX]\s*0*(\d{1,3})(?:\b|[\s._\-:|])(.*)$"""),
        Regex("""(?i)^(.*?)[\s._\-:|]*(?:SEASON|ΣΕΖΟΝ)\s*0*(\d{1,2})[\s._\-:|]+(?:EPISODE|EP|ΕΠΕΙΣΟΔΙΟ)\s*0*(\d{1,3})(?:\b|[\s._\-:|])(.*)$""")
    )
    private val seasonOnly = Regex("""(?i)(?:SEASON|S|ΣΕΖΟΝ)\s*0*(\d{1,2})""")
    private val trailingDecorations = Regex(
        """(?i)\s*(?:\(|\[)?(?:19|20)\d{2}(?:\)|\])?\s*$"""
    )
    private val whitespace = Regex("""\s+""")
    private val keyNoise = Regex("""[^\p{L}\p{N}]+""")

    private data class EpisodeParts(
        val seriesTitle: String,
        val season: Int,
        val episode: Int?,
        val episodeTitle: String
    )

    private data class EpisodeRow(
        val channel: Channel,
        val season: Int,
        val episode: Int?,
        val order: Int
    )

    private data class SeriesBucket(
        var parent: Channel,
        val rows: MutableList<EpisodeRow> = ArrayList(),
        val order: Int
    )

    fun normalize(contentType: String, rawItems: List<Channel>): CatalogNormalization {
        // ΠΡΩΤΑ ΚΑΘΑΡΙΖΟΥΜΕ ΤΑ «ΑΔΕΙΑ» ΠΕΔΙΑ, ΠΡΙΝ ΑΠΟ ΚΑΘΕ ΑΛΛΗ ΕΠΕΞΕΡΓΑΣΙΑ.
        //
        // Εδώ περνούν όλες οι πηγές, οπότε είναι το μοναδικό σημείο που
        // χρειάζεται. Ο [ProviderMetadataPolicy] αγγίζει μόνο πεδία προβολής —
        // τα `year`/`duration` παραμένουν ανέπαφα επειδή συμμετέχουν στα
        // εφεδρικά κλειδιά ταυτότητας παρακάτω και στο αποθηκευμένο
        // `localSeriesId`.
        val items = rawItems.map(ProviderMetadataPolicy::sanitize)
        return when (contentType) {
            "series" -> normalizeSeries(items)
            "vod" -> CatalogNormalization(deduplicate(items.map { if (it.kind == "vod") it else it.copy(kind = "vod") }, ::movieIdentity))
            "live" -> CatalogNormalization(deduplicate(items.map { if (it.kind == "live") it else it.copy(kind = "live") }, ::liveIdentity))
            else -> CatalogNormalization(deduplicate(items, ::technicalIdentity))
        }
    }

    /** Search must expose one result per title, never one result per episode. */
    fun searchEntries(items: Iterable<Channel>): List<Channel> {
        val all = items.toList()
        val live = deduplicate(all.filter { it.kind == "live" }, ::liveIdentity)
        val seriesRows = all.filter { it.kind == "series" || it.kind == "series_ep" || parseEpisode(it.name) != null }
        val movies = deduplicate(
            all.filter { (it.kind == "vod" || it.kind == "movie") && parseEpisode(it.name) == null },
            ::movieIdentity
        )
        val series = normalizeSeries(seriesRows).items
        val other = deduplicate(
            all.filter { it.kind !in setOf("live", "vod", "movie", "series", "series_ep") },
            ::technicalIdentity
        )
        return live + movies + series + other
    }

    fun isPlayable(channel: Channel): Boolean = channel.url.isNotBlank() || channel.cmd.isNotBlank()

    private fun normalizeSeries(rawItems: List<Channel>): CatalogNormalization {
        if (rawItems.isEmpty()) return CatalogNormalization(emptyList())

        val explicitParentsById = rawItems.asSequence()
            .filter { it.kind == "series" && it.seriesId.isNotBlank() && !isPlayable(it) && parseEpisode(it.name) == null }
            .associateBy { it.seriesId }

        val buckets = LinkedHashMap<String, SeriesBucket>()

        // Register real provider containers first, preserving provider order.
        rawItems.forEachIndexed { index, item ->
            if (item.kind == "series" && !isPlayable(item) && parseEpisode(item.name) == null) {
                val id = item.seriesId.ifBlank { localSeriesId(cleanSeriesTitle(item.name), item.year) }
                val parent = item.copy(
                    name = cleanSeriesTitle(item.name),
                    kind = "series",
                    seriesId = id,
                    url = "",
                    cmd = "",
                    streamId = "",
                    chId = "",
                    tvgId = ""
                )
                buckets.putIfAbsent(id, SeriesBucket(parent, order = index))
            }
        }

        rawItems.forEachIndexed { index, item ->
            val parsed = parseEpisode(item.name)
            val explicitParent = item.seriesId.takeIf(String::isNotBlank)?.let(explicitParentsById::get)
            val isContainer = item.kind == "series" && !isPlayable(item) && parsed == null
            if (isContainer) return@forEachIndexed

            // Every playable row in the Series section represents an episode,
            // even when the provider omitted SxxExx from its title.
            val shouldBecomeEpisode = item.kind == "series_ep" || parsed != null || isPlayable(item)
            if (!shouldBecomeEpisode) {
                val id = item.seriesId.ifBlank { localSeriesId(cleanSeriesTitle(item.name), item.year) }
                buckets.putIfAbsent(
                    id,
                    SeriesBucket(item.copy(kind = "series", seriesId = id, url = "", cmd = ""), order = index)
                )
                return@forEachIndexed
            }

            val parentTitle = cleanSeriesTitle(explicitParent?.name ?: parsed?.seriesTitle ?: item.name)
            val parentId = explicitParent?.seriesId?.takeIf(String::isNotBlank)
                ?: item.seriesId.takeIf { it.isNotBlank() && explicitParentsById.containsKey(it) }
                ?: localSeriesId(parentTitle, explicitParent?.year?.ifBlank { item.year } ?: item.year)
            val season = parsed?.season ?: seasonFromGroup(item.group) ?: 1
            val episode = parsed?.episode

            val bucket = buckets.getOrPut(parentId) {
                val representative = explicitParent ?: item
                SeriesBucket(
                    parent = representative.copy(
                        name = parentTitle,
                        kind = "series",
                        seriesId = parentId,
                        url = "",
                        cmd = "",
                        streamId = "",
                        chId = "",
                        tvgId = ""
                    ),
                    order = index
                )
            }
            bucket.parent = mergeParent(bucket.parent, explicitParent ?: item, parentTitle, parentId)
            bucket.rows += EpisodeRow(
                channel = item.copy(
                    kind = "series_ep",
                    seriesId = parentId,
                    group = "Season $season"
                ),
                season = season,
                episode = episode,
                order = index
            )
        }

        val orderedBuckets = buckets.values.sortedBy { it.order }
        val episodesBySeries = LinkedHashMap<String, List<Pair<String, List<Channel>>>>()
        orderedBuckets.forEach { bucket ->
            val seenEpisodes = HashSet<String>()
            val seasons = bucket.rows
                .sortedWith(compareBy<EpisodeRow>({ it.season }, { it.episode ?: Int.MAX_VALUE }, { it.order }))
                .filter { seenEpisodes.add(episodeIdentity(it.channel, it.season, it.episode)) }
                .groupBy { it.season }
                .toSortedMap()
                .map { (season, rows) -> "Season $season" to rows.map(EpisodeRow::channel) }
            if (seasons.isNotEmpty()) episodesBySeries[bucket.parent.seriesId] = seasons
        }

        val parents = deduplicate(orderedBuckets.map { it.parent }, ::seriesIdentity)
        return CatalogNormalization(parents, episodesBySeries)
    }

    private fun mergeParent(current: Channel, candidate: Channel, title: String, id: String): Channel = current.copy(
        name = title,
        group = current.group.ifBlank { candidate.group },
        logo = current.logo.ifBlank { candidate.logo },
        kind = "series",
        seriesId = id,
        plot = current.plot.ifBlank { candidate.plot },
        cast = current.cast.ifBlank { candidate.cast },
        director = current.director.ifBlank { candidate.director },
        genre = current.genre.ifBlank { candidate.genre },
        year = current.year.ifBlank { candidate.year },
        duration = current.duration.ifBlank { candidate.duration },
        // Ίδιος κανόνας με τα υπόλοιπα πεδία προβολής: η πρώτη μη κενή τιμή
        // κερδίζει. Χωρίς αυτή τη γραμμή, μια σειρά που χτίζεται πρώτα από
        // γραμμή χωρίς `tmdb_id` θα κρατούσε το κενό για πάντα, ακόμη κι αν το
        // επόμενο επεισόδιο το κουβαλούσε. ΔΕΝ αγγίζει ταυτότητα — δες
        // [Channel.tmdbId].
        tmdbId = current.tmdbId.ifBlank { candidate.tmdbId },
        // Για μια σειρά, «πότε μπήκε» είναι το ΠΙΟ ΠΡΟΣΦΑΤΟ επεισόδιο, όχι το
        // πρώτο που έτυχε να χτίσει τον κάδο: μια σειρά που παίρνει καινούριο
        // επεισόδιο κάθε βδομάδα είναι νέα, όσο παλιά κι αν είναι η πρεμιέρα της.
        addedAt = maxOf(current.addedAt, candidate.addedAt),
        rating = current.rating.ifBlank { candidate.rating },
        url = "",
        cmd = "",
        streamId = "",
        chId = "",
        tvgId = ""
    )

    private fun parseEpisode(name: String): EpisodeParts? {
        val cleaned = name.trim()
        for (pattern in seasonEpisodePatterns) {
            val match = pattern.matchEntire(cleaned) ?: continue
            val title = cleanSeriesTitle(match.groupValues[1])
            if (title.isBlank()) continue
            return EpisodeParts(
                seriesTitle = title,
                season = match.groupValues[2].toIntOrNull()?.coerceAtLeast(1) ?: 1,
                episode = match.groupValues[3].toIntOrNull(),
                episodeTitle = match.groupValues.getOrElse(4) { "" }.trim(' ', '-', '.', '_', ':', '|')
            )
        }
        return null
    }

    private fun seasonFromGroup(group: String): Int? =
        seasonOnly.find(group)?.groupValues?.getOrNull(1)?.toIntOrNull()

    private fun cleanSeriesTitle(value: String): String = value
        .replace('.', ' ')
        .replace('_', ' ')
        .trim(' ', '-', ':', '|')
        .replace(trailingDecorations, "")
        .replace(whitespace, " ")
        .trim()
        .ifBlank { value.trim() }

    private fun localSeriesId(title: String, year: String): String {
        val raw = "${titleKey(title)}|${year.trim()}"
        return "local:${Integer.toUnsignedString(raw.hashCode(), 36)}"
    }

    private fun titleKey(value: String): String = keyNoise
        .replace(value.lowercase(Locale.ROOT), " ")
        .replace(whitespace, " ")
        .trim()

    private fun liveIdentity(channel: Channel): String = when {
        channel.streamId.isNotBlank() -> "live|stream|${channel.streamId}"
        channel.chId.isNotBlank() -> "live|channel|${channel.chId}"
        channel.url.isNotBlank() -> "live|url|${channel.url.trim()}"
        channel.cmd.isNotBlank() -> "live|cmd|${channel.cmd.trim()}"
        channel.tvgId.isNotBlank() -> "live|tvg|${channel.tvgId.lowercase(Locale.ROOT)}|${titleKey(channel.name)}"
        else -> "live|meta|${titleKey(channel.name)}|${titleKey(channel.group)}"
    }

    private fun movieIdentity(channel: Channel): String = when {
        channel.streamId.isNotBlank() -> "vod|stream|${channel.streamId}"
        channel.url.isNotBlank() -> "vod|url|${channel.url.trim()}"
        channel.cmd.isNotBlank() -> "vod|cmd|${channel.cmd.trim()}"
        else -> "vod|meta|${titleKey(channel.name)}|${channel.year.trim()}|${channel.duration.trim()}"
    }

    private fun seriesIdentity(channel: Channel): String = when {
        channel.seriesId.isNotBlank() -> "series|id|${channel.seriesId}"
        else -> "series|meta|${titleKey(channel.name)}|${channel.year.trim()}"
    }

    private fun episodeIdentity(channel: Channel, season: Int, episode: Int?): String = when {
        channel.streamId.isNotBlank() -> "episode|stream|${channel.streamId}"
        channel.url.isNotBlank() -> "episode|url|${channel.url.trim()}"
        channel.cmd.isNotBlank() -> "episode|cmd|${channel.cmd.trim()}"
        else -> "episode|meta|${channel.seriesId}|$season|${episode ?: -1}|${titleKey(channel.name)}"
    }

    private fun technicalIdentity(channel: Channel): String = when {
        channel.streamId.isNotBlank() -> "${channel.kind}|stream|${channel.streamId}"
        channel.chId.isNotBlank() -> "${channel.kind}|channel|${channel.chId}"
        channel.url.isNotBlank() -> "${channel.kind}|url|${channel.url.trim()}"
        channel.cmd.isNotBlank() -> "${channel.kind}|cmd|${channel.cmd.trim()}"
        channel.seriesId.isNotBlank() -> "${channel.kind}|series|${channel.seriesId}"
        else -> "${channel.kind}|meta|${titleKey(channel.name)}|${titleKey(channel.group)}"
    }

    private inline fun deduplicate(
        items: Iterable<Channel>,
        identity: (Channel) -> String
    ): List<Channel> {
        val seen = HashSet<String>()
        val out = ArrayList<Channel>()
        items.forEach { channel -> if (seen.add(identity(channel))) out += channel }
        return out
    }
}
