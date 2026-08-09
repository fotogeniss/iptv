package com.prelude.iptv.data

import com.prelude.iptv.net.Http
import org.json.JSONArray
import java.net.URI
import java.util.Locale

/**
 * Finds public XMLTV guides that explicitly advertise support for the current
 * playlist channel ids. The directory is the public iptv-org guide index; it
 * contains guide metadata only and never receives provider URLs or credentials.
 */
object EpgSourceDirectory {
    const val DIRECTORY_URL = "https://iptv-org.github.io/api/guides.json"
    private const val CACHE_TTL_MS = 6L * 60L * 60L * 1000L

    data class Candidate(
        val url: String,
        val host: String,
        val matchedChannels: Int
    )

    @Volatile private var cachedJson: String? = null
    @Volatile private var cachedAtMs: Long = 0L

    fun findForChannels(channels: List<Channel>): List<Candidate> {
        val wanted = channels.asSequence()
            .filter { it.kind.isBlank() || it.kind == "live" }
            .map { it.tvgId.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { id -> sequenceOf(id, id.substringBefore('@')) }
            .map { it.lowercase(Locale.ROOT) }
            .toSet()
        if (wanted.isEmpty()) return emptyList()

        val matches = LinkedHashMap<String, MutableSet<String>>()
        val rows = JSONArray(directoryJson())
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val channel = row.optString("channel").trim()
            if (channel.isEmpty()) continue
            val feed = row.optString("feed").trim()
            val ids = buildSet {
                add(channel.lowercase(Locale.ROOT))
                if (feed.isNotEmpty()) add("$channel@$feed".lowercase(Locale.ROOT))
            }
            if (ids.none(wanted::contains)) continue

            val sources = row.optJSONArray("sources") ?: continue
            for (sourceIndex in 0 until sources.length()) {
                val source = sources.optJSONObject(sourceIndex) ?: continue
                val url = source.optString("url").trim()
                val format = source.optString("format").trim().uppercase(Locale.ROOT)
                if (url.isEmpty() || (format.isNotEmpty() && format != "XML" && format != "GZIP")) continue
                matches.getOrPut(url) { linkedSetOf() }.add(channel)
            }
        }

        return matches.map { (url, channelIds) ->
            val host = runCatching { URI(url).host }.getOrNull().orEmpty()
            Candidate(
                url = url,
                host = host,
                matchedChannels = channelIds.size
            )
        }.sortedWith(compareByDescending<Candidate> { it.matchedChannels }.thenBy { it.host })
    }

    @Synchronized
    private fun directoryJson(): String {
        val now = System.currentTimeMillis()
        cachedJson?.takeIf { now - cachedAtMs < CACHE_TTL_MS }?.let { return it }
        return Http.get(DIRECTORY_URL).also {
            cachedJson = it
            cachedAtMs = now
        }
    }
}
