package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.CatalogNormalization
import com.prelude.iptv.data.CatalogNormalizer
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.data.Repository
import com.prelude.iptv.data.SourceProgressCallback
import com.prelude.iptv.source.StalkerClient
import com.prelude.iptv.source.XtreamClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provider boundary for expanding one series into seasons and episodes.
 *
 * The coordinator owns provider selection, serialization and the temporary
 * Stalker connection used by a refresh. It deliberately does not mutate
 * MainViewModel state or session caches: the caller commits an outcome only
 * after its source/series generation token is still current.
 */
internal class SeriesLoadCoordinator(
    private val catalogLoader: CatalogLoadCoordinator,
) {
    data class Outcome(
        val seasons: List<Pair<String, List<Channel>>>,
        val synthetic: Boolean = false,
        val freshCatalog: CatalogNormalization? = null,
        val stalkerClient: StalkerClient? = null,
    )

    private var pendingStalker: StalkerClient? = null

    fun requiresFreshCatalog(
        playlist: Playlist,
        series: Channel,
        cached: List<Pair<String, List<Channel>>>?,
    ): Boolean = cached.isNullOrEmpty() && !hasDirectXtreamEndpoint(playlist, series)

    suspend fun load(
        playlist: Playlist,
        series: Channel,
        cached: List<Pair<String, List<Channel>>>?,
        rememberedCategoryIds: List<String>?,
        progress: SourceProgressCallback?,
    ): Outcome {
        if (!cached.isNullOrEmpty()) return Outcome(seasons = cached)

        if (hasDirectXtreamEndpoint(playlist, series)) {
            val seasons = catalogLoader.withProviderLock {
                withContext(Dispatchers.IO) {
                    XtreamClient.seriesEpisodes(
                        playlist.server,
                        playlist.username,
                        playlist.password,
                        series.seriesId,
                    )
                }
            }
            return Outcome(seasons = seasons)
        }

        return loadFreshCatalog(playlist, series, rememberedCategoryIds, progress)
    }

    fun cancel() {
        pendingStalker?.cancelPendingRequests()
        pendingStalker = null
    }

    private suspend fun loadFreshCatalog(
        playlist: Playlist,
        series: Channel,
        rememberedCategoryIds: List<String>?,
        progress: SourceProgressCallback?,
    ): Outcome {
        var connectedStalker: StalkerClient? = null
        return try {
            val raw = catalogLoader.withProviderLock {
                withContext(Dispatchers.IO) {
                    when (playlist.type) {
                        PlaylistType.M3U -> Repository.load(playlist, "series", progress)
                            .channels
                            .filter { it.kind == "series" }

                        PlaylistType.STALKER -> {
                            Repository.stalkerConnect(playlist).also { client ->
                                connectedStalker = client
                                pendingStalker = client
                            }.let { client ->
                                Repository.stalkerLoad(
                                    client,
                                    "series",
                                    rememberedCategoryIds,
                                    progress,
                                )
                            }
                        }

                        PlaylistType.XTREAM -> Repository.xtreamSeriesSelected(
                            playlist,
                            rememberedCategoryIds,
                            progress,
                        )
                    }
                }
            }
            val normalized = CatalogNormalizer.normalize("series", raw)
            val resolution = SeriesLoadPolicy.resolve(series, normalized)
            Outcome(
                seasons = resolution.seasons,
                synthetic = resolution.synthetic,
                freshCatalog = normalized,
                stalkerClient = connectedStalker,
            )
        } catch (cancelled: CancellationException) {
            connectedStalker?.cancelPendingRequests()
            throw cancelled
        } catch (error: Exception) {
            connectedStalker?.cancelPendingRequests()
            throw error
        } finally {
            if (pendingStalker === connectedStalker) pendingStalker = null
        }
    }

    private fun hasDirectXtreamEndpoint(playlist: Playlist, series: Channel): Boolean =
        playlist.type == PlaylistType.XTREAM &&
            series.seriesId.isNotBlank() &&
            !series.seriesId.startsWith("local:")
}

internal data class SeriesResolution(
    val seasons: List<Pair<String, List<Channel>>>,
    val synthetic: Boolean,
)

/** Pure matching/fallback policy kept separate from provider I/O. */
internal object SeriesLoadPolicy {
    fun resolve(series: Channel, catalog: CatalogNormalization): SeriesResolution {
        val target = catalog.items.firstOrNull { it.seriesId == series.seriesId }
            ?: catalog.items.firstOrNull { it.name.equals(series.name, ignoreCase = true) }
        val found = target?.let { catalog.seriesEpisodes[it.seriesId] }.orEmpty()
        if (found.isNotEmpty()) return SeriesResolution(found, synthetic = false)

        if (CatalogNormalizer.isPlayable(series)) {
            return SeriesResolution(
                seasons = listOf(
                    "Season 1" to listOf(
                        series.copy(kind = "series_ep", seriesId = series.seriesId)
                    )
                ),
                synthetic = true,
            )
        }
        return SeriesResolution(emptyList(), synthetic = false)
    }
}
