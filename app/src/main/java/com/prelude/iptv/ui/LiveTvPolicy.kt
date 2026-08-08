package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager

/** Pure policies used by the Live TV UI and covered by JVM tests. */
object LiveTvPolicy {
    fun filter(
        channels: List<Channel>,
        favoriteKeys: Set<String>,
        recentKeys: Set<String>,
        keyOf: (Channel) -> String,
        mode: String
    ): List<Channel> = when (mode) {
        "favorites" -> channels.filter { keyOf(it) in favoriteKeys }
        "recent" -> channels.filter { keyOf(it) in recentKeys }
        else -> if (mode.startsWith("group:")) {
            val group = mode.removePrefix("group:")
            channels.filter { it.group == group }
        } else channels
    }

    fun progress(programme: EpgManager.Prog?, nowMs: Long): Float {
        if (programme == null || programme.stopMs <= programme.startMs) return 0f
        return ((nowMs - programme.startMs).toFloat() / (programme.stopMs - programme.startMs))
            .coerceIn(0f, 1f)
    }
}
