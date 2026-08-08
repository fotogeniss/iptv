package com.prelude.iptv.tvhome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.net.Uri

/** Honors launcher removals and prevents immediate re-publication until user activity changes the item. */
class TvHomeBrowsableDisabledReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TvContract.ACTION_WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED -> {
                val id = intent.getLongExtra(TvContract.EXTRA_WATCH_NEXT_PROGRAM_ID, -1L)
                if (id >= 0L) suppressWatchNext(context, id)
            }
            TvContract.ACTION_PREVIEW_PROGRAM_BROWSABLE_DISABLED -> {
                val id = intent.getLongExtra(TvContract.EXTRA_PREVIEW_PROGRAM_ID, -1L)
                if (id >= 0L) suppressMyList(context, id)
            }
        }
    }

    private fun suppressWatchNext(context: Context, programId: Long) {
        val uri = TvContract.buildWatchNextProgramUri(programId)
        val store = TvHomeEntryStore(context)
        val token = providerToken(
            context,
            uri,
            TvContract.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID,
            TvHomeProviderOwnershipPolicy.WATCH_NEXT_PREFIX,
        ) ?: return
        val entry = store.resolve(token) ?: return
        store.suppress(entry)
        context.contentResolver.delete(uri, null, null)
    }

    private fun suppressMyList(context: Context, programId: Long) {
        val uri = TvContract.buildPreviewProgramUri(programId)
        val store = TvMyListEntryStore(context)
        val token = providerToken(
            context,
            uri,
            TvContract.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID,
            TvHomeProviderOwnershipPolicy.MY_LIST_PREFIX,
        ) ?: return
        val entry = store.resolve(token) ?: return
        store.suppress(entry)
        context.contentResolver.delete(uri, null, null)
    }

    private fun providerToken(context: Context, uri: Uri, column: String, prefix: String): String? {
        var token: String? = null
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (cursor.moveToNext()) {
                token = TvHomeProviderOwnershipPolicy.ownedToken(cursor.getString(0), prefix)
            }
        }
        return token
    }

}
