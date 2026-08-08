package com.prelude.iptv.tvhome

import android.content.Context
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.data.SubtitleSearchPolicy

internal class TvHomeCatalogRepository(context: Context) {
    private val store = PlaylistStore(context.applicationContext)

    fun build(): List<TvHomeCandidate> {
        if (!store.tvHomeEnabled) return emptyList()
        val now = System.currentTimeMillis()
        val activeProfile = store.activeProfile
        val candidates = buildList {
            store.loadPlaylists().forEachIndexed sourceLoop@ { sourceIndex, playlist ->
                val sourceId = PlaylistIdentity.stableId(playlist)
                store.loadRecents(sourceId).forEachIndexed recentLoop@ { recentIndex, channel ->
                    val itemKey = PlaybackQueue.favKey(channel)
                    val saved = store.loadSavedPosition(sourceId, itemKey) ?: return@recentLoop
                    val fallbackEngagement = now - sourceIndex * 10_000L - recentIndex
                    val parsedSeries = SubtitleSearchPolicy.parseEpisode(channel.name)?.seriesTitle.orEmpty()
                    add(
                        TvHomeCandidate(
                            profileId = activeProfile,
                            sourceId = sourceId,
                            itemKey = itemKey,
                            channel = channel,
                            positionMs = saved.positionMs,
                            durationMs = saved.durationMs,
                            lastEngagementMs = saved.lastEngagementMs.takeIf { it > 0L }
                                ?: store.ensurePositionEngagement(sourceId, itemKey, fallbackEngagement),
                            seriesKey = channel.seriesId.ifBlank { parsedSeries }
                        )
                    )
                }
            }
        }
        return TvHomeEligibilityPolicy.select(candidates, store.lockedGroups())
    }
}
