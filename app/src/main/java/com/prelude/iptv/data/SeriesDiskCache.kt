package com.prelude.iptv.data

import android.content.Context
import java.io.File

/** Removes obsolete series/episode cache files created by older releases. */
object SeriesDiskCache {
    fun clearAll(ctx: Context) {
        runCatching { File(ctx.filesDir, "seriescache").deleteRecursively() }
    }
}
