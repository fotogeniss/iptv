package com.prelude.iptv.data

/**
 * Process-memory cache for provider catalogs.
 *
 * Nothing in this class is written to disk. The cache lives only for the
 * lifetime of MainViewModel and is deliberately bounded so switching through
 * many sources cannot retain every catalog forever.
 */
data class SessionCatalogKey(
    val sourceId: String,
    val contentType: String,
    val categorySignature: String
)

data class SessionCatalogSnapshot(
    val channels: List<Channel>,
    val groups: List<String>,
    val seriesEpisodes: Map<String, List<Pair<String, List<Channel>>>> = emptyMap(),
    val epgUrl: String = "",
    val loadedAtMs: Long = System.currentTimeMillis()
)

class SessionCatalogCache(
    private val maxEntries: Int = 6,
    private val maxRetainedItems: Int = 120_000,
) {
    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
        require(maxRetainedItems > 0) { "maxRetainedItems must be positive" }
    }

    private val entries = LinkedHashMap<SessionCatalogKey, SessionCatalogSnapshot>(
        maxEntries,
        0.75f,
        true
    )

    @Synchronized
    fun get(key: SessionCatalogKey): SessionCatalogSnapshot? = entries[key]

    @Synchronized
    fun put(key: SessionCatalogKey, snapshot: SessionCatalogSnapshot) {
        // Catalog snapshots are built once and treated as immutable throughout
        // the UI layer. Defensive deep copies doubled peak memory for large
        // playlists (and copied every episode list), without adding safety.
        entries.remove(key)
        val itemCount = retainedItemCount(snapshot)
        // An exceptionally large active catalog already lives in UiState. Do not
        // retain a second section beside it when that alone exceeds the LRU budget.
        if (itemCount > maxRetainedItems) return
        entries[key] = snapshot
        while (entries.size > maxEntries || retainedItemCount() > maxRetainedItems) {
            val eldest = entries.entries.iterator()
            if (!eldest.hasNext()) break
            eldest.next()
            eldest.remove()
        }
    }

    @Synchronized
    fun invalidateSource(sourceId: String) {
        entries.keys.removeAll { it.sourceId == sourceId }
    }

    @Synchronized
    fun invalidateSection(sourceId: String, contentType: String) {
        entries.keys.removeAll { it.sourceId == sourceId && it.contentType == contentType }
    }

    @Synchronized
    fun clear() = entries.clear()

    @Synchronized
    fun size(): Int = entries.size

    private fun retainedItemCount(snapshot: SessionCatalogSnapshot): Int {
        var count = snapshot.channels.size.toLong()
        snapshot.seriesEpisodes.values.forEach { seasons ->
            seasons.forEach { (_, episodes) -> count += episodes.size.toLong() }
        }
        return count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun retainedItemCount(): Int {
        var count = 0L
        entries.values.forEach { count += retainedItemCount(it).toLong() }
        return count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    companion object {
        const val ALL_CATEGORIES = "*"
        const val NO_CATEGORIES = "!"

        fun categorySignature(ids: List<String>?): String {
            if (ids == null) return ALL_CATEGORIES
            return ids.asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .sorted()
                .joinToString(separator = "\u001f")
                .ifEmpty { NO_CATEGORIES }
        }

        fun key(sourceId: String, contentType: String, ids: List<String>?): SessionCatalogKey =
            SessionCatalogKey(
                sourceId = sourceId,
                contentType = contentType,
                categorySignature = categorySignature(ids)
            )
    }
}
