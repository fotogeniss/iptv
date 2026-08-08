package com.prelude.iptv.tvhome

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.prelude.iptv.player.PlayerLaunchRequest
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.data.Repository
import com.prelude.iptv.data.SubtitleSearchPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Resolves an opaque launcher token and starts the internal player without exposing credentials. */
class TvHomePlaybackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        val request = TvHomePlaybackRoutePolicy.parse(
            scheme = data?.scheme,
            host = data?.host,
            pathSegments = data?.pathSegments.orEmpty(),
        ) ?: return finishWithError()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                resolvePlayback(request.route, request.token)
            }
            result.onSuccess { startActivity(it) }
                .onFailure {
                    Toast.makeText(
                        this@TvHomePlaybackActivity,
                        "Το στοιχείο δεν είναι πλέον διαθέσιμο",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            finish()
        }
    }

    private fun resolvePlayback(route: String, token: String): Result<Intent> = try {
        val store = PlaylistStore(this)
        val entry = when (route) {
            ROUTE_PLAY_NEXT -> {
                check(store.tvHomeEnabled)
                val value = TvHomeEntryStore(this).resolve(token) ?: error("Missing token")
                LauncherEntry(
                    profileId = value.profileId,
                    sourceId = value.sourceId,
                    itemKey = value.itemKey,
                    channel = value.channel,
                )
            }
            ROUTE_MY_LIST -> {
                check(store.tvHomeMyListEnabled)
                val value = TvMyListEntryStore(this).resolve(token) ?: error("Missing token")
                check(store.isFavorite(value.sourceId, value.itemKey))
                LauncherEntry(
                    profileId = value.profileId,
                    sourceId = value.sourceId,
                    itemKey = value.itemKey,
                    channel = value.channel,
                )
            }
            else -> error("Unsupported route")
        }

        check(entry.profileId == store.activeProfile)
        val lockedGroups = store.lockedGroups().mapTo(HashSet()) { it.trim().lowercase() }
        check(entry.channel.group.trim().lowercase() !in lockedGroups)
        val playlist = store.loadPlaylists()
            .firstOrNull { PlaylistIdentity.stableId(it) == entry.sourceId }
            ?: error("Missing source")
        val stalker = if (playlist.type == PlaylistType.STALKER && entry.channel.cmd.isNotBlank()) {
            Repository.stalkerConnect(playlist)
        } else {
            null
        }
        val url = Repository.playableUrl(entry.channel, stalker).takeIf(String::isNotBlank)
            ?: error("Empty URL")
        val subtitle = SubtitleSearchPolicy.fromChannel(entry.channel)

        PlaybackQueue.items = listOf(entry.channel)
        PlaybackQueue.index = 0
        PlaybackQueue.stalker = stalker
        PlaybackQueue.sourceId = entry.sourceId
        PlaybackQueue.subtitleRequests = mapOf(entry.itemKey to subtitle)

        Result.success(
            PlayerLaunchRequest.forChannel(
                url = url,
                channel = entry.channel,
                sourceId = entry.sourceId,
                positionKey = entry.itemKey,
                subtitle = subtitle,
            ).toIntent(this)
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun finishWithError() {
        Toast.makeText(this, "Μη έγκυρος σύνδεσμος αναπαραγωγής", Toast.LENGTH_SHORT).show()
        finish()
    }

    private data class LauncherEntry(
        val profileId: Int,
        val sourceId: String,
        val itemKey: String,
        val channel: Channel
    )

    private companion object {
        const val ROUTE_PLAY_NEXT = TvHomePlaybackRoutePolicy.ROUTE_PLAY_NEXT
        const val ROUTE_MY_LIST = TvHomePlaybackRoutePolicy.ROUTE_MY_LIST
    }
}
