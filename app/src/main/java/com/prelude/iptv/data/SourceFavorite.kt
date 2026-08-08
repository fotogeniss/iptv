package com.prelude.iptv.data

/** A favorite belongs to one profile (via its storage key) and one provider source. */
data class SourceFavorite(
    val sourceId: String,
    val itemKey: String,
    val channel: Channel,
    val addedAtMs: Long
) {
    val identity: String get() = "$sourceId|$itemKey"
}

/** Pure matching rules used by migration and launcher publishing. */
object SourceFavoritePolicy {
    fun playable(channel: Channel): Boolean =
        channel.kind != "series" && PlaybackQueue.favKey(channel).isNotBlank()

    fun selectLegacyMatches(
        sourceId: String,
        legacyKeys: Set<String>,
        sourceItems: List<Channel>,
        nowMs: Long
    ): List<SourceFavorite> {
        if (sourceId.isBlank() || legacyKeys.isEmpty() || sourceItems.isEmpty()) return emptyList()
        val seen = HashSet<String>()
        return sourceItems.mapIndexedNotNull { index, channel ->
            val itemKey = PlaybackQueue.favKey(channel)
            val hasProviderTransport = channel.url.isNotBlank() || channel.cmd.isNotBlank()
            if (!hasProviderTransport || itemKey !in legacyKeys || itemKey.isBlank() || !seen.add(itemKey)) {
                return@mapIndexedNotNull null
            }
            SourceFavorite(
                sourceId = sourceId,
                itemKey = itemKey,
                channel = channel,
                addedAtMs = nowMs - index
            )
        }
    }

    fun reconcileSnapshots(
        entries: List<SourceFavorite>,
        sourceId: String,
        sourceItems: List<Channel>
    ): List<SourceFavorite> {
        if (sourceId.isBlank() || sourceItems.isEmpty()) return entries
        val wanted = entries.asSequence()
            .filter { it.sourceId == sourceId }
            .map { it.itemKey }
            .filter(String::isNotBlank)
            .toHashSet()
        if (wanted.isEmpty()) return entries
        val fresh = HashMap<String, Channel>(wanted.size)
        for (channel in sourceItems) {
            val key = PlaybackQueue.favKey(channel)
            if (key in wanted) {
                fresh[key] = channel
                if (fresh.size == wanted.size) break
            }
        }
        if (fresh.isEmpty()) return entries
        return entries.map { entry ->
            if (entry.sourceId != sourceId) entry
            else fresh[entry.itemKey]?.let { entry.copy(channel = it) } ?: entry
        }
    }
}
