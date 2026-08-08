package com.prelude.iptv.tvhome

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.prelude.iptv.data.PlaylistStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class TvHomeSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!TvHomeDevice.isTv(applicationContext)) return@withContext Result.success()
        val store = PlaylistStore(applicationContext)

        return@withContext try {
            syncPlayNext(store)
            syncMyList(store)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private fun syncPlayNext(store: PlaylistStore) {
        val entryStore = TvHomeEntryStore(applicationContext)
        val publisher = LegacyWatchNextPublisher(applicationContext, entryStore)
        if (!store.tvHomeEnabled) {
            entryStore.clear()
            publisher.publish(emptyList())
            return
        }
        val selected = TvHomeCatalogRepository(applicationContext).build()
        publisher.publish(entryStore.replace(selected))
    }

    private fun syncMyList(store: PlaylistStore) {
        val entryStore = TvMyListEntryStore(applicationContext)
        val publisher = LegacyMyListChannelPublisher(applicationContext, entryStore)
        if (!store.tvHomeMyListEnabled) {
            entryStore.clear()
            publisher.clear()
            return
        }
        val selected = TvMyListCatalogRepository(applicationContext).build()
        publisher.publish(entryStore.replace(store.activeProfile, selected))
    }
}

object TvHomeSyncScheduler {
    private const val UNIQUE_WORK = "tv-home-content-sync"

    fun schedule(context: Context) {
        val app = context.applicationContext
        if (!TvHomeDevice.isTv(app)) return
        val request = OneTimeWorkRequest.Builder(TvHomeSyncWorker::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(app).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request)
    }
}
