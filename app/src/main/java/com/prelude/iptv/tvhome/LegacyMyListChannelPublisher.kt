package com.prelude.iptv.tvhome

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.tv.TvContract
import android.net.Uri
import android.provider.BaseColumns
import com.prelude.iptv.MainActivity
import com.prelude.iptv.R
import com.prelude.iptv.data.PlaylistIdentity

/** Publishes the app-owned "My List" preview channel on Android TV API 26+. */
internal class LegacyMyListChannelPublisher(
    private val context: Context,
    private val entryStore: TvMyListEntryStore
) {
    fun ensureChannel(): Long {
        if (!TvHomeDevice.isTv(context)) return -1L
        findChannelId()?.let { return it }
        val values = ContentValues().apply {
            put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_PREVIEW)
            put(TvContract.Channels.COLUMN_DISPLAY_NAME, CHANNEL_NAME)
            put(TvContract.Channels.COLUMN_DESCRIPTION, "Αγαπημένα από τις συνδεδεμένες πηγές")
            put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID, CHANNEL_PROVIDER_ID)
            put(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI, appIntent().toUri(Intent.URI_INTENT_SCHEME))
        }
        val uri = context.contentResolver.insert(TvContract.Channels.CONTENT_URI, values) ?: return -1L
        val channelId = uri.lastPathSegment?.toLongOrNull() ?: return -1L
        writeChannelLogo(channelId)
        return channelId
    }

    fun publish(entries: List<TvMyListEntry>): Int {
        if (!TvHomeDevice.isTv(context)) return 0
        val channelId = ensureChannel()
        if (channelId < 0L) return 0
        return runCatching {
            val existing = loadExistingRows(channelId)
            val wanted = HashSet<String>()
            var published = 0
            entries.forEachIndexed { index, entry ->
                val providerId = "$PROGRAM_PREFIX${entry.token}"
                wanted += providerId
                val current = existing[providerId]
                if (current?.browsable == 0) {
                    entryStore.suppress(entry)
                    context.contentResolver.delete(TvContract.buildPreviewProgramUri(current.id), null, null)
                    return@forEachIndexed
                }
                val values = values(channelId, entry, providerId, entries.size - index)
                if (current != null) {
                    val uri = TvContract.buildPreviewProgramUri(current.id)
                    if (context.contentResolver.update(uri, values, null, null) > 0) published++
                    else if (context.contentResolver.insert(TvContract.PreviewPrograms.CONTENT_URI, values) != null) published++
                } else if (context.contentResolver.insert(TvContract.PreviewPrograms.CONTENT_URI, values) != null) {
                    published++
                }
            }
            existing.forEach { (providerId, row) ->
                if (providerId !in wanted) {
                    context.contentResolver.delete(TvContract.buildPreviewProgramUri(row.id), null, null)
                }
            }
            published
        }.getOrDefault(0)
    }

    fun clear() {
        val channelId = findChannelId() ?: return
        runCatching {
            context.contentResolver.delete(TvContract.buildPreviewProgramsUriForChannel(channelId), null, null)
            context.contentResolver.delete(TvContract.buildChannelUri(channelId), null, null)
        }
    }

    private fun findChannelId(): Long? {
        context.contentResolver.query(
            TvContract.Channels.CONTENT_URI,
            arrayOf(
                BaseColumns._ID,
                TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID,
                TvContract.Channels.COLUMN_PACKAGE_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val providerColumn = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID)
            val packageColumn = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_PACKAGE_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(providerColumn) == CHANNEL_PROVIDER_ID &&
                    cursor.getString(packageColumn) == context.packageName
                ) return cursor.getLong(idColumn)
            }
        }
        return null
    }

    private fun loadExistingRows(channelId: Long): Map<String, ExistingRow> {
        val out = LinkedHashMap<String, ExistingRow>()
        context.contentResolver.query(
            TvContract.buildPreviewProgramsUriForChannel(channelId),
            arrayOf(BaseColumns._ID, TvContract.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID, TvContract.PreviewPrograms.COLUMN_BROWSABLE),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val providerColumn = cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID)
            val browsableColumn = cursor.getColumnIndex(TvContract.PreviewPrograms.COLUMN_BROWSABLE)
            while (cursor.moveToNext()) {
                val providerId = cursor.getString(providerColumn).orEmpty()
                if (!providerId.startsWith(PROGRAM_PREFIX)) continue
                out[providerId] = ExistingRow(
                    id = cursor.getLong(idColumn),
                    browsable = if (browsableColumn >= 0) cursor.getInt(browsableColumn) else 1
                )
            }
        }
        return out
    }

    private fun values(
        channelId: Long,
        entry: TvMyListEntry,
        providerId: String,
        weight: Int
    ): ContentValues = ContentValues().apply {
        put(TvContract.PreviewPrograms.COLUMN_CHANNEL_ID, channelId)
        put(TvContract.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID, providerId)
        put(TvContract.PreviewPrograms.COLUMN_CONTENT_ID, "upl:list:${PlaylistIdentity.digest(entry.identity)}")
        put(TvContract.PreviewPrograms.COLUMN_TYPE, programType(entry.channel.kind))
        put(TvContract.PreviewPrograms.COLUMN_TITLE, entry.channel.name)
        entry.channel.plot.takeIf(String::isNotBlank)?.let {
            put(TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION, it.take(240))
        }
        val remoteArtwork = entry.channel.logo.takeIf(::safeArtworkUri)
        put(
            TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
            remoteArtwork ?: "android.resource://${context.packageName}/drawable/ic_app"
        )
        put(
            TvContract.PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
            if (remoteArtwork != null) TvContract.PreviewPrograms.ASPECT_RATIO_2_3
            else TvContract.PreviewPrograms.ASPECT_RATIO_1_1
        )
        put(TvContract.PreviewPrograms.COLUMN_INTENT_URI, playbackIntent(entry.token).toUri(Intent.URI_INTENT_SCHEME))
        put(TvContract.PreviewPrograms.COLUMN_WEIGHT, weight)
    }

    private fun programType(kind: String): Int = when (kind) {
        "series_ep" -> TvContract.PreviewPrograms.TYPE_TV_EPISODE
        "live" -> TvContract.PreviewPrograms.TYPE_CHANNEL
        else -> TvContract.PreviewPrograms.TYPE_MOVIE
    }

    private fun playbackIntent(token: String): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("upl://my-list/$token"),
        context,
        TvHomePlaybackActivity::class.java
    ).setPackage(context.packageName)

    private fun appIntent(): Intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private fun writeChannelLogo(channelId: Long) {
        runCatching {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_app) ?: return
            context.contentResolver.openOutputStream(TvContract.buildChannelLogoUri(channelId))?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }

    private fun safeArtworkUri(value: String): Boolean = runCatching {
        Uri.parse(value).scheme?.lowercase() in setOf("http", "https", "content", "android.resource", "file")
    }.getOrDefault(false)

    private data class ExistingRow(val id: Long, val browsable: Int)

    private companion object {
        const val CHANNEL_NAME = "Η λίστα μου"
        const val CHANNEL_PROVIDER_ID = "upl:channel:my-list"
        const val PROGRAM_PREFIX = "upl:list:"
    }
}

/** Foreground entry point used by Settings to request channel visibility. */
object TvHomeChannelManager {
    fun enableMyList(context: Context) {
        val app = context.applicationContext
        if (!TvHomeDevice.isTv(app)) return
        val channelId = LegacyMyListChannelPublisher(app, TvMyListEntryStore(app)).ensureChannel()
        if (channelId >= 0L) runCatching { TvContract.requestChannelBrowsable(context, channelId) }
        TvHomeSyncScheduler.schedule(app)
    }
}
