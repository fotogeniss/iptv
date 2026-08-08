package com.prelude.iptv.data

import android.content.SharedPreferences

/**
 * Persists source-scoped recents and resume positions for the active profile.
 *
 * PlaylistStore remains the public facade. Keeping the implementation here
 * prevents profile/source key migration and playback-history reconciliation
 * from being mixed with playlist credentials, UI preferences and categories.
 */
internal class PlaybackHistoryStore(
    private val prefs: SharedPreferences,
    private val profileKey: (String) -> String,
    private val loadChannels: (String) -> MutableList<Channel>,
    private val saveChannels: (String, List<Channel>) -> Unit,
    private val removeSecure: (String) -> Unit,
) {
    data class SavedPosition(
        val positionMs: Long,
        val durationMs: Long,
        val lastEngagementMs: Long,
    )

    private fun historyScope(sourceId: String): String = PlaylistIdentity.digest(sourceId)
    private fun recentsKey(sourceId: String): String =
        profileKey("recents_${historyScope(sourceId)}")
    private fun recentLiveKey(sourceId: String): String =
        profileKey("recent_live_${historyScope(sourceId)}")
    private fun positionKey(sourceId: String, itemKey: String): String =
        profileKey("pos_${PlaylistIdentity.digest("$sourceId|$itemKey")}")
    private fun positionEngagementKey(sourceId: String, itemKey: String): String =
        profileKey("pos_engagement_${PlaylistIdentity.digest("$sourceId|$itemKey")}")
    private fun engagementKeyForPositionKey(scopedPositionKey: String): String =
        scopedPositionKey.replaceFirst("pos_", "pos_engagement_")
    private fun positionIndexKey(sourceId: String): String =
        profileKey("pos_index_${historyScope(sourceId)}")
    private fun legacyMigrationKey(sourceId: String): String =
        profileKey("history_migrated_${historyScope(sourceId)}")

    fun loadRecents(sourceId: String): MutableList<Channel> =
        if (sourceId.isBlank()) mutableListOf() else loadChannels(recentsKey(sourceId))

    fun addRecent(sourceId: String, channel: Channel) {
        if (sourceId.isBlank() || channel.kind == "live") return
        val itemKey = PlaybackQueue.favKey(channel)
        if (itemKey.isBlank()) return
        val list = loadRecents(sourceId)
        list.removeAll { PlaybackQueue.favKey(it) == itemKey }
        list.add(0, channel)
        saveChannels(recentsKey(sourceId), list.take(MAX_RECENTS))
    }

    fun loadRecentLive(sourceId: String): MutableList<Channel> =
        if (sourceId.isBlank()) mutableListOf() else loadChannels(recentLiveKey(sourceId))

    fun addRecentLive(sourceId: String, channel: Channel) {
        if (sourceId.isBlank() || channel.kind != "live") return
        val itemKey = PlaybackQueue.favKey(channel)
        if (itemKey.isBlank()) return
        val list = loadRecentLive(sourceId)
        list.removeAll { PlaybackQueue.favKey(it) == itemKey }
        list.add(0, channel)
        saveChannels(recentLiveKey(sourceId), list.take(MAX_RECENT_LIVE))
    }

    fun clearRecentLive(sourceId: String) {
        if (sourceId.isNotBlank()) saveChannels(recentLiveKey(sourceId), emptyList())
    }

    fun removeRecent(sourceId: String, itemKey: String) {
        if (sourceId.isBlank() || itemKey.isBlank()) return
        val list = loadRecents(sourceId)
        if (list.removeAll { PlaybackQueue.favKey(it) == itemKey }) {
            saveChannels(recentsKey(sourceId), list)
        }
    }

    /** Migrates legacy global history only when the item exists in this source. */
    fun migrateLegacyHistory(sourceId: String, sourceItems: List<Channel>) {
        if (sourceId.isBlank() || sourceItems.isEmpty()) return
        val alreadyMigrated = (
            prefs.getStringSet(legacyMigrationKey(sourceId), emptySet()) ?: emptySet()
        ).toMutableSet()
        val pendingLegacy = loadChannels(profileKey("recents")).filter { item ->
            val itemKey = PlaybackQueue.favKey(item)
            item.kind != "live" && itemKey.isNotBlank() &&
                PlaylistIdentity.digest(itemKey) !in alreadyMigrated
        }
        if (pendingLegacy.isEmpty()) return

        val wantedKeys = pendingLegacy.mapTo(HashSet(pendingLegacy.size)) {
            PlaybackQueue.favKey(it)
        }
        val validKeys = HashSet<String>(wantedKeys.size)
        for (item in sourceItems) {
            val itemKey = PlaybackQueue.favKey(item)
            if (itemKey in wantedKeys) {
                validKeys += itemKey
                if (validKeys.size == wantedKeys.size) break
            }
        }
        val legacy = pendingLegacy.filter { PlaybackQueue.favKey(it) in validKeys }
        if (legacy.isEmpty()) return

        val merged = loadRecents(sourceId)
        val existing = merged.map { PlaybackQueue.favKey(it) }.toHashSet()
        legacy.asReversed().forEach { item ->
            val itemKey = PlaybackQueue.favKey(item)
            alreadyMigrated.add(PlaylistIdentity.digest(itemKey))
            if (existing.add(itemKey)) merged.add(0, item)
            migrateLegacyPosition(sourceId, itemKey)
        }
        saveChannels(recentsKey(sourceId), merged.take(MAX_RECENTS))
        prefs.edit().putStringSet(legacyMigrationKey(sourceId), alreadyMigrated).apply()
    }

    private fun migrateLegacyPosition(sourceId: String, itemKey: String) {
        val legacyKey = profileKey("pos_$itemKey")
        val oldPosition = prefs.getString(legacyKey, null) ?: return
        val scopedKey = positionKey(sourceId, itemKey)
        if (prefs.getString(scopedKey, null) == null) {
            val indexKey = positionIndexKey(sourceId)
            val index = (prefs.getStringSet(indexKey, emptySet()) ?: emptySet()).toMutableSet()
            index.add(scopedKey)
            prefs.edit()
                .putString(scopedKey, oldPosition)
                .putStringSet(indexKey, index)
                .remove(legacyKey)
                .apply()
        } else {
            prefs.edit().remove(legacyKey).apply()
        }
    }

    /** Refreshes bounded history snapshots from the current provider catalog. */
    fun reconcileHistory(sourceId: String, sourceItems: List<Channel>) {
        if (sourceId.isBlank() || sourceItems.isEmpty()) return
        val recents = loadRecents(sourceId)
        if (recents.isEmpty()) return
        val wanted = buildSet {
            recents.forEach { item ->
                add(historyMatchKey(item))
                add(historyMetadataKey(item))
                historyTransportKey(item)?.let { add(it) }
            }
        }
        val fresh = HashMap<String, Channel>(wanted.size)
        sourceItems.forEach { item ->
            val match = historyMatchKey(item)
            val metadata = historyMetadataKey(item)
            val transport = historyTransportKey(item)
            if (match in wanted) fresh[match] = item
            if (metadata in wanted) fresh[metadata] = item
            if (transport != null && transport in wanted) fresh[transport] = item
        }
        var changed = false
        val updated = recents.map { old ->
            val replacement = fresh[historyMatchKey(old)]
                ?: fresh[historyMetadataKey(old)]
                ?: historyTransportKey(old)?.let(fresh::get)
                ?: return@map old
            val merged = replacement.copy(
                plot = replacement.plot.ifBlank { old.plot },
                cast = replacement.cast.ifBlank { old.cast },
                director = replacement.director.ifBlank { old.director },
                genre = replacement.genre.ifBlank { old.genre },
                year = replacement.year.ifBlank { old.year },
                duration = replacement.duration.ifBlank { old.duration },
            )
            val oldKey = PlaybackQueue.favKey(old)
            val newKey = PlaybackQueue.favKey(merged)
            if (oldKey != newKey && oldKey.isNotBlank() && newKey.isNotBlank()) {
                movePosition(sourceId, oldKey, newKey)
            }
            if (merged != old) changed = true
            merged
        }
        if (changed) saveChannels(recentsKey(sourceId), updated.take(MAX_RECENTS))
    }

    private fun historyMatchKey(channel: Channel): String = when {
        channel.streamId.isNotBlank() -> "${channel.kind}|stream|${channel.streamId}"
        channel.seriesId.isNotBlank() && channel.kind == "series" -> "series|${channel.seriesId}"
        channel.chId.isNotBlank() -> "${channel.kind}|channel|${channel.chId}"
        else -> historyMetadataKey(channel)
    }

    private fun historyMetadataKey(channel: Channel): String =
        "${channel.kind}|meta|${channel.name.trim().lowercase()}|${channel.group.trim().lowercase()}"

    private fun historyTransportKey(channel: Channel): String? = when {
        channel.streamId.isNotBlank() -> "transport|stream|${channel.streamId}"
        channel.chId.isNotBlank() -> "transport|channel|${channel.chId}"
        channel.url.isNotBlank() -> "transport|url|${channel.url}"
        channel.cmd.isNotBlank() -> "transport|cmd|${channel.cmd}"
        else -> null
    }

    private fun movePosition(sourceId: String, oldItemKey: String, newItemKey: String) {
        val oldKey = positionKey(sourceId, oldItemKey)
        val value = prefs.getString(oldKey, null) ?: return
        val oldEngagementKey = positionEngagementKey(sourceId, oldItemKey)
        val engagement = prefs.getLong(oldEngagementKey, 0L)
        val newKey = positionKey(sourceId, newItemKey)
        val newEngagementKey = positionEngagementKey(sourceId, newItemKey)
        val indexKey = positionIndexKey(sourceId)
        val index = (prefs.getStringSet(indexKey, emptySet()) ?: emptySet()).toMutableSet()
        index.remove(oldKey)
        index.add(newKey)
        prefs.edit()
            .remove(oldKey)
            .remove(oldEngagementKey)
            .putString(newKey, value)
            .putLong(newEngagementKey, engagement)
            .putStringSet(indexKey, index)
            .apply()
    }

    fun savePosition(
        sourceId: String,
        itemKey: String,
        positionMs: Long,
        durationMs: Long,
        lastEngagementMs: Long,
    ) {
        if (sourceId.isBlank() || itemKey.isBlank() || durationMs <= 0L) return
        if (positionMs < MIN_RESUME_MS || positionMs > durationMs * COMPLETE_PERCENT / 100) {
            clearPosition(sourceId, itemKey)
            return
        }
        val scopedKey = positionKey(sourceId, itemKey)
        val indexKey = positionIndexKey(sourceId)
        val index = (prefs.getStringSet(indexKey, emptySet()) ?: emptySet()).toMutableSet()
        index.add(scopedKey)
        prefs.edit()
            .putString(scopedKey, "$positionMs/$durationMs")
            .putLong(positionEngagementKey(sourceId, itemKey), lastEngagementMs.coerceAtLeast(0L))
            .putStringSet(indexKey, index)
            .apply()
    }

    fun loadSavedPosition(sourceId: String, itemKey: String): SavedPosition? {
        if (sourceId.isBlank() || itemKey.isBlank()) return null
        val raw = prefs.getString(positionKey(sourceId, itemKey), null) ?: return null
        val position = raw.substringBefore('/').toLongOrNull() ?: return null
        val duration = raw.substringAfter('/').toLongOrNull() ?: return null
        return SavedPosition(
            positionMs = position,
            durationMs = duration,
            lastEngagementMs = prefs.getLong(positionEngagementKey(sourceId, itemKey), 0L),
        )
    }

    fun ensurePositionEngagement(sourceId: String, itemKey: String, fallbackMs: Long): Long {
        if (sourceId.isBlank() || itemKey.isBlank()) return 0L
        val engagementKey = positionEngagementKey(sourceId, itemKey)
        val existing = prefs.getLong(engagementKey, 0L)
        if (existing > 0L) return existing
        val migrated = fallbackMs.coerceAtLeast(1L)
        prefs.edit().putLong(engagementKey, migrated).apply()
        return migrated
    }

    fun clearPosition(sourceId: String, itemKey: String) {
        if (sourceId.isBlank() || itemKey.isBlank()) return
        val scopedKey = positionKey(sourceId, itemKey)
        val indexKey = positionIndexKey(sourceId)
        val index = (prefs.getStringSet(indexKey, emptySet()) ?: emptySet()).toMutableSet()
        index.remove(scopedKey)
        prefs.edit()
            .remove(scopedKey)
            .remove(positionEngagementKey(sourceId, itemKey))
            .putStringSet(indexKey, index)
            .apply()
    }

    fun clearHistory(sourceId: String) {
        if (sourceId.isBlank()) return
        val recents = recentsKey(sourceId)
        val indexKey = positionIndexKey(sourceId)
        val positions = prefs.getStringSet(indexKey, emptySet()) ?: emptySet()
        removeSecure(recents)
        val editor = prefs.edit().remove(recents).remove(indexKey)
        positions.forEach { position ->
            editor.remove(position)
            editor.remove(engagementKeyForPositionKey(position))
        }
        editor.apply()
    }

    private companion object {
        const val MAX_RECENTS = 60
        const val MAX_RECENT_LIVE = 20
        const val MIN_RESUME_MS = 60_000L
        const val COMPLETE_PERCENT = 95
    }
}
