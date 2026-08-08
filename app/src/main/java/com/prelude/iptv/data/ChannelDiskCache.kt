package com.prelude.iptv.data

import android.content.Context
import java.io.File

/**
 * One-time cleanup for catalog files created by older releases.
 *
 * Live, VOD and Series catalogs are intentionally never persisted. Provider
 * URLs may expire or rotate, so every section load must go back to the source.
 */
object ChannelDiskCache {
    fun clearAll(ctx: Context) {
        runCatching { File(ctx.filesDir, "chcache").deleteRecursively() }
    }
}
