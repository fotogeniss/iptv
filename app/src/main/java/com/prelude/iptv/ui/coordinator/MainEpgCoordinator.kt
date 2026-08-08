package com.prelude.iptv.ui.coordinator

import android.app.Application
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import com.prelude.iptv.data.EpgAutoSourcePolicy
import com.prelude.iptv.data.EpgSelectionPolicy
import com.prelude.iptv.data.EpgSourceDirectory
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.data.Repository
import com.prelude.iptv.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Owns EPG request lifetime, persistence and source guards.
 *
 * MainViewModel remains the public API boundary, but no longer mixes XMLTV
 * networking/generation state with catalog loading, profiles and playback.
 */
internal class MainEpgCoordinator(
    private val app: Application,
    private val store: PlaylistStore,
    private val state: MutableStateFlow<UiState>,
    private val scope: CoroutineScope,
    private val currentPlaylist: () -> Playlist?,
    private val currentSourceId: () -> String,
    private val m3uEpgUrl: () -> String,
    private val liveChannels: () -> List<Channel>,
    private val parentalAllowed: (Channel) -> Boolean,
) {
    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private val loadGeneration = AtomicInteger(0)
    private val loadMutex = Mutex()

    private enum class LoadPath { STALE, DISK, NETWORK, FAILED }
    private data class LoadResult(
        val path: LoadPath,
        val error: String = "",
        val snapshot: EpgManager.Snapshot? = null,
    )

    fun isEnabled(): Boolean = store.epgEnabled

    fun setEnabled(enabled: Boolean) {
        store.epgEnabled = enabled
        if (!enabled) {
            cancelLoad(clearManager = true)
            state.value = state.value.copy(epgLoaded = false, epgStatus = "")
        } else {
            currentPlaylist()?.let(::loadIfAny)
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
    }

    fun cancelLoad(clearManager: Boolean = false) {
        loadGeneration.incrementAndGet()
        loadJob?.cancel()
        loadJob = null
        if (clearManager) EpgManager.clear()
    }

    private fun beginLoad(): Int {
        loadJob?.cancel()
        return loadGeneration.incrementAndGet()
    }

    private fun isCurrentRequest(generation: Int, sourceId: String): Boolean =
        generation == loadGeneration.get() && currentSourceId() == sourceId

    fun loadIfAny(playlist: Playlist) {
        if (!store.epgEnabled) {
            cancelLoad(clearManager = true)
            state.value = state.value.copy(epgLoaded = false, epgStatus = "")
            return
        }
        val url = EpgSelectionPolicy.normalizeRemoteUrl(autoUrl(playlist))
        if (url == null) {
            cancelLoad(clearManager = true)
            state.value = state.value.copy(epgLoaded = false, epgStatus = "")
            return
        }
        if (EpgManager.currentSource() == url && EpgManager.isLoaded()) {
            // Το loadIfAny καλείται σε ΚΑΘΕ live load/restore. Μην ξαναβγάζεις το ✓
            // μήνυμα αν φαίνεται ήδη — αλλιώς το flash επαναλαμβάνεται συνέχεια
            // (το match count μεγαλώνει καθώς φορτώνουν κανάλια). Μία φορά αρκεί.
            state.value = state.value.copy(
                epgLoaded = true,
                epgStatus = if (state.value.epgStatus.startsWith("✓")) state.value.epgStatus
                else "✓ EPG: ταιριάζει σε ${matchCount()} κανάλια",
            )
            return
        }

        val sourceId = PlaylistIdentity.stableId(playlist)
        val generation = beginLoad()
        val keepsExistingGuide = EpgManager.isLoaded()
        state.value = state.value.copy(
            epgLoaded = keepsExistingGuide,
            epgStatus = if (keepsExistingGuide) {
                "Λήψη νέου EPG… · το τρέχον παραμένει ενεργό"
            } else {
                "Λήψη EPG…"
            },
        )
        loadJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                loadMutex.withLock {
                    if (!isCurrentRequest(generation, sourceId)) {
                        return@withLock LoadResult(LoadPath.STALE)
                    }

                    val disk = EpgManager.readCacheSnapshot(app)
                    if (!isCurrentRequest(generation, sourceId)) {
                        return@withLock LoadResult(LoadPath.STALE)
                    }
                    if (disk?.source == url) {
                        return@withLock LoadResult(LoadPath.DISK, snapshot = disk)
                    }

                    try {
                        val candidate = EpgManager.fetchSnapshot(url)
                        if (!isCurrentRequest(generation, sourceId)) {
                            LoadResult(LoadPath.STALE)
                        } else {
                            LoadResult(LoadPath.NETWORK, snapshot = candidate)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        LoadResult(LoadPath.FAILED, error.message.orEmpty())
                    }
                }
            }

            if (!isCurrentRequest(generation, sourceId)) return@launch
            when (result.path) {
                LoadPath.STALE -> Unit
                LoadPath.FAILED -> state.value = state.value.copy(
                    epgLoaded = EpgManager.isLoaded(),
                    epgStatus = "✗ EPG: ${result.error.ifBlank { "απέτυχε η λήψη" }}" +
                        if (EpgManager.isLoaded()) " · διατηρήθηκε το προηγούμενο" else "",
                )
                LoadPath.DISK, LoadPath.NETWORK -> {
                    val snapshot = result.snapshot ?: return@launch
                    if (!isCurrentRequest(generation, sourceId)) return@launch
                    EpgManager.installSnapshot(snapshot)
                    if (result.path == LoadPath.NETWORK) {
                        withContext(Dispatchers.IO) {
                            if (isCurrentRequest(generation, sourceId)) {
                                EpgManager.saveSnapshotCache(app, snapshot)
                            }
                        }
                    }
                    if (!isCurrentRequest(generation, sourceId)) return@launch
                    val matches = matchCount()
                    state.value = state.value.copy(
                        epgLoaded = true,
                        epgStatus = when {
                            matches == 0 -> "⚠ EPG φορτώθηκε αλλά δεν ταιριάζει με τα tvg-id"
                            result.path == LoadPath.DISK -> "✓ EPG: ταιριάζει σε $matches κανάλια · από δίσκο"
                            else -> "✓ EPG: ταιριάζει σε $matches κανάλια"
                        },
                    )
                }
            }
            if (generation == loadGeneration.get()) loadJob = null
        }
    }

    fun search() {
        val playlist = currentPlaylist() ?: return
        val sourceId = PlaylistIdentity.stableId(playlist)
        cancelSearch()
        val found = LinkedHashMap<String, String>()
        if (playlist.epgUrl.isNotBlank()) {
            found[playlist.epgUrl.trim()] = "Από τις ρυθμίσεις της λίστας"
        }
        val embeddedUrl = m3uEpgUrl()
        if (embeddedUrl.isNotBlank()) found[embeddedUrl] = "Δηλωμένο μέσα στο M3U (url-tvg)"
        if (playlist.type == PlaylistType.XTREAM) {
            val xtreamUrl = Repository.xtreamXmltvUrl(playlist)
            if (xtreamUrl.isNotBlank()) {
                found.putIfAbsent(xtreamUrl, "Xtream — xmltv.php του παρόχου")
            }
        }
        EpgManager.currentSource()?.takeIf(String::isNotBlank)?.let {
            found.putIfAbsent(it, "Ήδη φορτωμένο")
        }
        state.value = state.value.copy(
            epgSources = found.map { it.value to it.key },
            epgStatus = "Αναζήτηση γνωστών δημόσιων XMLTV πηγών…",
        )

        val liveSnapshot = liveChannels().ifEmpty {
            state.value.channels.filter { it.kind.isBlank() || it.kind == "live" }
        }
        searchJob = scope.launch {
            val publicSources = withContext(Dispatchers.IO) {
                try {
                    EpgSourceDirectory.findForChannels(liveSnapshot)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    emptyList()
                }
            }
            if (currentSourceId() != sourceId) return@launch
            publicSources.forEach { candidate ->
                found.putIfAbsent(candidate.url, candidate.label)
            }
            state.value = state.value.copy(
                epgSources = found.map { it.value to it.key },
                epgStatus = when {
                    found.isNotEmpty() -> "Βρέθηκαν ${found.size} πηγές EPG"
                    liveSnapshot.none { it.tvgId.isNotBlank() } ->
                        "Δεν υπάρχουν tvg-id στα κανάλια για ασφαλή αντιστοίχιση δημόσιου EPG."
                    else -> "Δεν βρέθηκε συμβατή πηγή EPG για αυτή τη λίστα."
                },
            )
            searchJob = null
        }
    }

    fun closeSearch() {
        cancelSearch()
        state.value = state.value.copy(epgSources = emptyList(), epgStatus = "")
    }

    fun useSource(rawUrl: String) {
        val url = EpgSelectionPolicy.normalizeRemoteUrl(rawUrl)
        if (url == null) {
            state.value = state.value.copy(
                epgLoaded = EpgManager.isLoaded(),
                epgStatus = "✗ Δώσε έγκυρο http/https URL EPG.",
            )
            return
        }
        val playlist = currentPlaylist() ?: return
        val sourceId = PlaylistIdentity.stableId(playlist)
        val generation = beginLoad()
        val keepsExistingGuide = EpgManager.isLoaded()
        state.value = state.value.copy(
            epgLoaded = keepsExistingGuide,
            epgStatus = if (keepsExistingGuide) {
                "Κατέβασμα νέου EPG… · το τρέχον παραμένει ενεργό"
            } else {
                "Κατέβασμα EPG…"
            },
        )
        loadJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                loadMutex.withLock {
                    if (!isCurrentRequest(generation, sourceId)) {
                        return@withLock LoadResult(LoadPath.STALE)
                    }
                    try {
                        val candidate = EpgManager.fetchSnapshot(url)
                        if (!isCurrentRequest(generation, sourceId)) {
                            LoadResult(LoadPath.STALE)
                        } else {
                            LoadResult(LoadPath.NETWORK, snapshot = candidate)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        LoadResult(LoadPath.FAILED, error.message.orEmpty())
                    }
                }
            }

            if (!isCurrentRequest(generation, sourceId)) return@launch
            if (result.path == LoadPath.FAILED) {
                state.value = state.value.copy(
                    epgLoaded = EpgManager.isLoaded(),
                    epgStatus = "✗ Απέτυχε το κατέβασμα" +
                        result.error.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty() +
                        if (EpgManager.isLoaded()) " · διατηρήθηκε το προηγούμενο" else "",
                )
                if (generation == loadGeneration.get()) loadJob = null
                return@launch
            }
            if (result.path == LoadPath.STALE) return@launch

            val snapshot = result.snapshot ?: return@launch
            val canCommit = EpgSelectionPolicy.shouldCommit(
                requestSourceId = sourceId,
                currentSourceId = currentSourceId(),
                requestedUrl = url,
                loadedUrl = snapshot.source,
            )
            if (!canCommit || !persistUrlForSource(sourceId, url)) return@launch

            if (!isCurrentRequest(generation, sourceId)) return@launch
            EpgManager.installSnapshot(snapshot)
            withContext(Dispatchers.IO) {
                if (isCurrentRequest(generation, sourceId)) {
                    EpgManager.saveSnapshotCache(app, snapshot)
                }
            }
            if (!isCurrentRequest(generation, sourceId)) return@launch

            val matches = matchCount()
            state.value = state.value.copy(
                epgLoaded = true,
                epgStatus = if (matches > 0) {
                    "✓ Αποθηκεύτηκε και ταιριάζει σε $matches κανάλια"
                } else {
                    "⚠ Αποθηκεύτηκε, αλλά δεν ταιριάζει με τα tvg-id της λίστας."
                },
            )
            if (generation == loadGeneration.get()) loadJob = null
        }
    }

    fun liveChannelsWithEpg(): List<Channel> {
        if (!state.value.epgLoaded) return emptyList()
        val live = liveChannels().ifEmpty { state.value.channels.filter { it.kind == "live" } }
        return live.filter {
            it.tvgId.isNotBlank() && EpgManager.hasChannel(it.tvgId) && parentalAllowed(it)
        }
    }

    fun nowText(tvgId: String): String? {
        if (tvgId.isEmpty()) return null
        return EpgManager.nowNext(tvgId).first?.title
    }

    private fun autoUrl(playlist: Playlist): String {
        return EpgAutoSourcePolicy.choose(
            customUrl = playlist.epgUrl,
            embeddedM3uUrl = m3uEpgUrl(),
            xtreamUrl = if (playlist.type == PlaylistType.XTREAM) Repository.xtreamXmltvUrl(playlist) else ""
        )
    }

    private fun matchCount(): Int {
        val live = liveChannels().ifEmpty { state.value.channels.filter { it.kind == "live" } }
        return live.count {
            it.tvgId.isNotBlank() && EpgManager.nowNext(it.tvgId).first != null
        }
    }

    private fun persistUrlForSource(sourceId: String, url: String): Boolean {
        val snapshot = state.value
        val index = snapshot.currentIndex
        val playlist = snapshot.playlists.getOrNull(index) ?: return false
        if (PlaylistIdentity.stableId(playlist) != sourceId) return false
        if (playlist.epgUrl.trim() == url) return true

        val updated = snapshot.playlists.toMutableList()
        updated[index] = playlist.copy(epgUrl = url)
        store.savePlaylists(updated)
        state.value = state.value.copy(playlists = updated)
        return currentSourceId() == sourceId
    }
}
