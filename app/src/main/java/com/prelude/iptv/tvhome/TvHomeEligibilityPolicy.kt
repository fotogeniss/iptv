package com.prelude.iptv.tvhome

import com.prelude.iptv.data.Channel

/** Pure policy for the launcher Continue Watching row. */
data class TvHomeCandidate(
    val profileId: Int,
    val sourceId: String,
    val itemKey: String,
    val channel: Channel,
    val positionMs: Long,
    val durationMs: Long,
    val lastEngagementMs: Long,
    val seriesKey: String = ""
) {
    val identity: String get() = "$profileId|$sourceId|$itemKey"
}

object TvHomeEligibilityPolicy {
    const val MAX_ITEMS = 5
    private const val TWO_MINUTES_MS = 2L * 60L * 1000L
    private const val THREE_MINUTES_MS = 3L * 60L * 1000L

    fun select(
        candidates: List<TvHomeCandidate>,
        lockedGroups: Set<String>,
        maxItems: Int = MAX_ITEMS
    ): List<TvHomeCandidate> {
        if (maxItems <= 0) return emptyList()
        val locked = lockedGroups.asSequence().map(::normalize).filter(String::isNotBlank).toHashSet()
        val seenItems = HashSet<String>()
        val seenSeries = HashSet<String>()

        return candidates.asSequence()
            .filter(::isEligible)
            .filter { normalize(it.channel.group) !in locked }
            .sortedByDescending { it.lastEngagementMs }
            .filter { seenItems.add(it.identity) }
            .filter { candidate ->
                if (candidate.channel.kind != "series_ep") return@filter true
                val key = candidate.seriesKey.trim().lowercase()
                key.isBlank() || seenSeries.add("${candidate.sourceId}|$key")
            }
            .take(maxItems)
            .toList()
    }

    fun isEligible(candidate: TvHomeCandidate): Boolean {
        val kind = candidate.channel.kind
        if (kind != "vod" && kind != "movie" && kind != "series_ep") return false
        if (candidate.itemKey.isBlank() || candidate.sourceId.isBlank()) return false
        if (candidate.durationMs <= 0L || candidate.positionMs <= 0L) return false
        if (candidate.positionMs >= candidate.durationMs) return false

        val startThreshold = if (kind == "series_ep") {
            TWO_MINUTES_MS
        } else {
            minOf(TWO_MINUTES_MS, candidate.durationMs * 3L / 100L)
        }
        if (candidate.positionMs < startThreshold) return false

        val remaining = candidate.durationMs - candidate.positionMs
        if (candidate.positionMs >= candidate.durationMs * 95L / 100L) return false
        if (remaining <= THREE_MINUTES_MS) return false
        return true
    }

    private fun normalize(value: String): String = value.trim().lowercase()
}

/** A user-hidden row stays hidden until the same item receives a newer engagement. */
object TvHomeSuppressionPolicy {
    fun shouldPublish(lastEngagementMs: Long, suppressedAtMs: Long?): Boolean =
        suppressedAtMs == null || lastEngagementMs > suppressedAtMs
}
