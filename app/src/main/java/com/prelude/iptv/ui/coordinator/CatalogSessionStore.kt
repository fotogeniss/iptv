package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.LoadResult
import com.prelude.iptv.data.SessionCatalogCache
import com.prelude.iptv.data.SessionCatalogKey
import com.prelude.iptv.data.SessionCatalogSnapshot

/**
 * Bounded, process-only catalog state.
 *
 * This class deliberately contains no provider calls and no UI mutations. It
 * gives MainViewModel one place for source/section invalidation and M3U LRU
 * behavior, preventing cache state from spreading across unrelated features.
 */
internal class CatalogSessionStore(
    maxCatalogEntries: Int = 3,
    private val maxM3uEntries: Int = 1,
) {
    data class M3uPayload(
        val channels: List<Channel>,
        val epgUrl: String,
    )

    private val catalogs = SessionCatalogCache(maxEntries = maxCatalogEntries)
    private val m3uPayloads = LinkedHashMap<String, M3uPayload>()

    var liveChannels: List<Channel> = emptyList()
    var seriesEpisodes: Map<String, List<Pair<String, List<Channel>>>> = emptyMap()

    fun catalogKey(sourceId: String, type: String, categoryIds: List<String>?): SessionCatalogKey =
        SessionCatalogCache.key(sourceId, type, categoryIds)

    fun getCatalog(key: SessionCatalogKey): SessionCatalogSnapshot? = catalogs.get(key)

    fun putCatalog(key: SessionCatalogKey, snapshot: SessionCatalogSnapshot) {
        catalogs.put(key, snapshot)
    }

    fun invalidateSource(sourceId: String) {
        catalogs.invalidateSource(sourceId)
        m3uPayloads.remove(sourceId)
    }

    fun invalidateSection(sourceId: String, type: String) {
        catalogs.invalidateSection(sourceId, type)
    }

    fun getM3u(sourceId: String): M3uPayload? {
        val payload = m3uPayloads.remove(sourceId) ?: return null
        m3uPayloads[sourceId] = payload
        return payload
    }

    fun rememberM3u(sourceId: String, result: LoadResult): M3uPayload {
        // LoadResult is complete and no longer mutated. Keep the same immutable
        // view instead of duplicating a potentially huge M3U catalog.
        val payload = M3uPayload(result.channels, result.epgUrl)
        m3uPayloads.remove(sourceId)
        m3uPayloads[sourceId] = payload
        trimM3u()
        return payload
    }

    fun clearWorkingSet() {
        liveChannels = emptyList()
        seriesEpisodes = emptyMap()
    }

    private fun trimM3u() {
        while (m3uPayloads.size > maxM3uEntries.coerceAtLeast(1)) {
            val iterator = m3uPayloads.entries.iterator()
            if (!iterator.hasNext()) return
            iterator.next()
            iterator.remove()
        }
    }
}
