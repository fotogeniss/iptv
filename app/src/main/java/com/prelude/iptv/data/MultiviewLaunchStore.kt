package com.prelude.iptv.data

import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-private hand-off for Multiview. Intents carry only an opaque token;
 * provider URLs and credentials never cross the Activity boundary.
 */
object MultiviewLaunchStore {
    data class Stream(
        val url: String,
        val title: String,
        val logo: String = "",
        val sourceId: String = ""
    )

    data class Launch(val primary: Stream, val secondary: Stream)

    private data class Entry(val launch: Launch, val createdAtMs: Long)
    private val entries = ConcurrentHashMap<String, Entry>()
    private val random = SecureRandom()
    private const val TTL_MS = 60_000L

    fun put(launch: Launch, nowMs: Long = System.currentTimeMillis()): String {
        prune(nowMs)
        val bytes = ByteArray(24).also(random::nextBytes)
        val token = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        entries[token] = Entry(launch, nowMs)
        return token
    }

    /** One-shot consume prevents stale replay and duplicate launch. */
    fun consume(token: String?, nowMs: Long = System.currentTimeMillis()): Launch? {
        if (token.isNullOrBlank()) return null
        val entry = entries.remove(token) ?: return null
        return entry.launch.takeIf { nowMs - entry.createdAtMs <= TTL_MS }
    }

    internal fun clearForTests() = entries.clear()

    private fun prune(nowMs: Long) {
        entries.entries.removeIf { nowMs - it.value.createdAtMs > TTL_MS }
    }
}
