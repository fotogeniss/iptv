package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue

/**
 * Pure policies for the TV library/search destinations.
 * Keeping identity, de-duplication and matching outside Compose makes the
 * screens deterministic and cheap to test on the JVM.
 */
object LibraryPolicy {
    fun unique(items: Iterable<Channel>): List<Channel> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<Channel>()
        items.forEach { channel ->
            val key = PlaybackQueue.favKey(channel).ifBlank {
                listOf(channel.kind, channel.name, channel.seriesId, channel.streamId, channel.url)
                    .joinToString("|")
            }
            if (seen.add(key)) out += channel
        }
        return out
    }

    fun search(items: Iterable<Channel>, query: String): List<Channel> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        return unique(items).filter { channel ->
            val haystack = listOf(
                channel.name,
                channel.group,
                channel.genre,
                channel.year,
                channel.cast,
                channel.director,
                channel.plot
            ).joinToString(" ").lowercase()
            terms.all(haystack::contains)
        }
    }

    fun favorites(items: Iterable<Channel>, favoriteKeys: Set<String>): List<Channel> =
        unique(items).filter { PlaybackQueue.favKey(it) in favoriteKeys }
}
