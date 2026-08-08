package com.prelude.iptv.source

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** Serializes session-sensitive URL resolution per source, while unrelated sources remain independent. */
object ProviderResolutionGate {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> withSource(sourceId: String, block: suspend () -> T): T {
        val key = sourceId.ifBlank { "__unknown_source__" }
        return locks.getOrPut(key) { Mutex() }.withLock { block() }
    }
}
