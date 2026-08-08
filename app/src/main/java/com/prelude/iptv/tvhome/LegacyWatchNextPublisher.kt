package com.prelude.iptv.tvhome

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.net.Uri
import android.provider.BaseColumns
import com.prelude.iptv.data.PlaylistIdentity

internal interface TvContinueWatchingPublisher {
    fun publish(entries: List<TvHomeEntry>): Int
}

/** Android TV legacy Watch Next bridge, isolated behind an interface for Engage migration. */
internal class LegacyWatchNextPublisher(
    private val context: Context,
    private val entryStore: TvHomeEntryStore
) : TvContinueWatchingPublisher {
    override fun publish(entries: List<TvHomeEntry>): Int {
        if (!TvHomeDevice.isTv(context)) return 0
        val resolver = context.contentResolver
        return runCatching {
            val existing = loadExistingRows()
            val wantedIds = HashSet<String>()
            var published = 0

            entries.forEach { entry ->
                val providerId = "$PREFIX${entry.token}"
                wantedIds += providerId
                val current = existing[providerId]
                if (current?.browsable == 0) {
                    entryStore.suppress(entry)
                    resolver.delete(TvContract.buildWatchNextProgramUri(current.id), null, null)
                    return@forEach
                }
                val values = values(entry, providerId)
                if (current != null) {
                    val uri = TvContract.buildWatchNextProgramUri(current.id)
                    if (resolver.update(uri, values, null, null) > 0) published++
                    else if (resolver.insert(TvContract.WatchNextPrograms.CONTENT_URI, values) != null) published++
                } else {
                    if (resolver.insert(TvContract.WatchNextPrograms.CONTENT_URI, values) != null) published++
                }
            }

            existing.forEach { (providerId, row) ->
                if (providerId !in wantedIds) {
                    resolver.delete(TvContract.buildWatchNextProgramUri(row.id), null, null)
                }
            }
            published
        }.getOrDefault(0)
    }

    private fun loadExistingRows(): Map<String, ExistingRow> {
        val out = LinkedHashMap<String, ExistingRow>()
        context.contentResolver.query(
            TvContract.WatchNextPrograms.CONTENT_URI,
            arrayOf(BaseColumns._ID, TvContract.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID, TvContract.WatchNextPrograms.COLUMN_BROWSABLE),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val providerColumn = cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID)
            val browsableColumn = cursor.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_BROWSABLE)
            while (cursor.moveToNext()) {
                val providerId = cursor.getString(providerColumn).orEmpty()
                if (!providerId.startsWith(PREFIX)) continue
                out[providerId] = ExistingRow(
                    id = cursor.getLong(idColumn),
                    browsable = if (browsableColumn >= 0) cursor.getInt(browsableColumn) else 1
                )
            }
        }
        return out
    }

    private fun values(entry: TvHomeEntry, providerId: String): ContentValues = ContentValues().apply {
        put(TvContract.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID, providerId)
        put(TvContract.WatchNextPrograms.COLUMN_CONTENT_ID, "upl:${PlaylistIdentity.digest(entry.identity)}")
        put(TvContract.WatchNextPrograms.COLUMN_TYPE, if (entry.channel.kind == "series_ep") TvContract.WatchNextPrograms.TYPE_TV_EPISODE else TvContract.WatchNextPrograms.TYPE_MOVIE)
        put(TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE, TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
        put(TvContract.WatchNextPrograms.COLUMN_TITLE, entry.channel.name)
        entry.channel.plot.takeIf(String::isNotBlank)?.let { put(TvContract.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION, it.take(240)) }
        val remoteArtwork = entry.channel.logo.takeIf(::safeArtworkUri)
        put(
            TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
            remoteArtwork ?: "android.resource://${context.packageName}/drawable/ic_app"
        )
        put(
            TvContract.WatchNextPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
            if (remoteArtwork != null) POSTER_ASPECT_RATIO_2_3 else POSTER_ASPECT_RATIO_1_1
        )
        put(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS, entry.durationMs)
        put(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, entry.positionMs)
        put(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, entry.lastEngagementMs)
        put(TvContract.WatchNextPrograms.COLUMN_INTENT_URI, playbackIntent(entry.token).toUri(Intent.URI_INTENT_SCHEME))
    }

    private fun playbackIntent(token: String): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("upl://play-next/$token"),
        context,
        TvHomePlaybackActivity::class.java
    ).setPackage(context.packageName)

    private fun safeArtworkUri(value: String): Boolean = runCatching {
        Uri.parse(value).scheme?.lowercase() in setOf("http", "https", "content", "android.resource", "file")
    }.getOrDefault(false)

    private data class ExistingRow(val id: Long, val browsable: Int)

    private companion object {
        const val PREFIX = "upl:"

        // TvContract preview/watch-next poster aspect-ratio contract values (API 26+).
        // Kept local because some Android SDK stubs do not expose the inherited constants
        // through WatchNextPrograms to Kotlin, even though the provider column accepts them.
        const val POSTER_ASPECT_RATIO_1_1 = 3
        const val POSTER_ASPECT_RATIO_2_3 = 4
    }
}
