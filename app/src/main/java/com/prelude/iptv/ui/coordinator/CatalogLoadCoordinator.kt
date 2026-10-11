package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.CatalogNormalizer
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.data.Repository
import com.prelude.iptv.data.SourceCategoriesCallback
import com.prelude.iptv.data.SourceCategoryTotalsCallback
import com.prelude.iptv.data.SourcePartialCallback
import com.prelude.iptv.data.SourceProgressCallback
import com.prelude.iptv.source.StalkerClient
import com.prelude.iptv.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Single provider-loading boundary for MainViewModel.
 *
 * The coordinator owns provider serialization, category dispatch, section
 * loading and normalization of both progressive and final snapshots. It does
 * not own Android lifecycle or mutate StateFlow directly; MainViewModel keeps
 * the public API and persists successful results through narrow callbacks.
 */
internal class CatalogLoadCoordinator(
    private val stalkerFor: (Playlist) -> StalkerClient,
    private val m3uPayload: suspend (Playlist, SourceProgressCallback?) -> CatalogSessionStore.M3uPayload,
) {
    data class PartialCatalog(
        val items: List<Channel>,
        val seriesEpisodes: Map<String, List<Pair<String, List<Channel>>>> = emptyMap(),
    )

    data class LoadedCatalog(
        val rawItems: List<Channel>,
        val items: List<Channel>,
        val categories: List<Pair<String, String>> = emptyList(),
        val seriesEpisodes: Map<String, List<Pair<String, List<Channel>>>> = emptyMap(),
        /**
         * Πραγματικά πλήθη ανά κατηγορία, όπως τα δηλώνει ο πάροχος
         * (`total_items`), όχι όπως προκύπτουν από τα φορτωμένα.
         *
         * Κενός χάρτης όταν ο πάροχος δεν τα δηλώνει (Xtream, M3U): τότε ισχύει
         * η μέτρηση των φορτωμένων, ακριβώς όπως πριν.
         */
        val categoryTotals: Map<String, Int> = emptyMap(),
        /**
         * Πόσα στοιχεία δίνει ο πάροχος ανά σελίδα (`max_page_items`). Ορίζει
         * ποια είναι η επόμενη σελίδα όταν ζητηθούν κι άλλα. Μηδέν = ο πάροχος
         * δεν σελιδοποιεί έτσι, άρα δεν υπάρχει «επόμενη σελίδα».
         */
        val categoryPageSize: Int = 0,
    )

    /** Snapshot used to roll back progressive refresh publication on failure. */
    data class VisibleCatalogSnapshot(
        val contentType: String,
        val channels: List<Channel>,
        val groups: List<String>,
        val favorites: Set<String>,
        val selectedGroup: String,
    )

    private val providerMutex = Mutex()

    suspend fun categories(
        playlist: Playlist,
        type: String,
        progress: SourceProgressCallback? = null,
    ): List<Pair<String, String>> = withProviderLock {
        withContext(Dispatchers.IO) {
            when (playlist.type) {
                PlaylistType.STALKER -> {
                    progress?.invoke(4, "Σύνδεση Stalker…")
                    val client = stalkerFor(playlist)
                    progress?.invoke(55, "Λήψη κατηγοριών…")
                    Repository.stalkerCategories(client, type).also {
                        progress?.invoke(100, "Οι κατηγορίες είναι έτοιμες")
                    }
                }
                PlaylistType.XTREAM -> when (type) {
                    "vod" -> Repository.xtreamVodCategories(playlist, progress)
                    "series" -> Repository.xtreamSeriesCategories(playlist, progress)
                    else -> Repository.xtreamLiveCategories(playlist, progress)
                }
                PlaylistType.M3U -> m3uPayload(playlist, progress)
                    .channels
                    .asSequence()
                    .filter { it.kind == type }
                    .map { it.group.ifEmpty { "Χωρίς ομάδα" } }
                    .distinct()
                    .sortedBy { it.lowercase() }
                    .map { it to it }
                    .toList()
            }
        }
    }

    suspend fun section(
        playlist: Playlist,
        type: String,
        categoryIds: List<String>?,
        progress: SourceProgressCallback? = null,
        onPartial: ((PartialCatalog) -> Unit)? = null,
    ): LoadedCatalog = withProviderLock {
        withContext(Dispatchers.IO) {
            var loadedCategories = emptyList<Pair<String, String>>()
            val categoriesCallback: SourceCategoriesCallback = { loadedCategories = it }
            // ΤΑ ΠΛΗΘΗ ΕΡΧΟΝΤΑΙ ΝΩΡΙΣ, ΤΑ ΣΤΟΙΧΕΙΑ ΑΡΓΑ. Ο πάροχος δηλώνει
            // `total_items` μέσα στην 1η σελίδα κάθε κατηγορίας, δηλαδή αφού
            // κατέβουν 14 στοιχεία και όχι 4.282.
            var loadedTotals = emptyMap<String, Int>()
            var loadedPageSize = 0
            val totalsCallback: SourceCategoryTotalsCallback = { totals, pageSize ->
                loadedTotals = totals
                loadedPageSize = pageSize
            }
            var lastPartialSize = 0
            var lastPartialAtMs = 0L
            val partialCallback: SourcePartialCallback? = onPartial?.let { publish ->
                { partialRaw ->
                    // Providers can report every few hundred rows. Normalizing the
                    // complete growing list on every report is O(n²) and creates a
                    // large allocation storm on real-world catalogs. Publish an
                    // early preview, then coalesce subsequent snapshots.
                    val now = System.currentTimeMillis()
                    val itemDelta = partialRaw.size - lastPartialSize
                    val shouldPublish = lastPartialSize == 0 ||
                        itemDelta >= 2_000 ||
                        (itemDelta >= 500 && now - lastPartialAtMs >= 1_000L)
                    if (shouldPublish) {
                        lastPartialSize = partialRaw.size
                        lastPartialAtMs = now
                        val normalized = CatalogNormalizer.normalize(type, partialRaw)
                        publish(PartialCatalog(normalized.items, normalized.seriesEpisodes))
                    }
                }
            }
            val rawItems = when (playlist.type) {
                PlaylistType.STALKER -> Repository.stalkerLoad(
                    stalkerFor(playlist), type, categoryIds, progress, partialCallback,
                    categoriesCallback, totalsCallback,
                )
                PlaylistType.XTREAM -> when (type) {
                    "vod" -> Repository.xtreamVodSelected(playlist, categoryIds, progress, partialCallback, categoriesCallback)
                    "series" -> Repository.xtreamSeriesSelected(playlist, categoryIds, progress, partialCallback, categoriesCallback)
                    else -> Repository.xtreamLiveSelected(playlist, categoryIds, progress, partialCallback, categoriesCallback)
                }
                PlaylistType.M3U -> {
                    val payload = m3uPayload(playlist, progress)
                    val ofType = payload.channels.filter { it.kind == type }
                    loadedCategories = ofType.asSequence()
                        .map { it.group.ifEmpty { "Χωρίς ομάδα" } }
                        .distinct()
                        .sortedBy(String::lowercase)
                        .map { it to it }
                        .toList()
                    val selected = if (categoryIds == null) {
                        ofType
                    } else {
                        val selectedIds = categoryIds.toHashSet()
                        ofType.filter { it.group.ifEmpty { "Χωρίς ομάδα" } in selectedIds }
                    }
                    if (selected.isNotEmpty()) partialCallback?.invoke(selected)
                    selected
                }
            }
            val normalized = CatalogNormalizer.normalize(type, rawItems)
            val knownCategories = loadedCategories
                .filter { (id, title) -> id.isNotBlank() && title.isNotBlank() }
                .distinctBy { it.second }
            val knownTitles = knownCategories.mapTo(HashSet()) { it.second }
            val missingGroups = normalized.items.asSequence()
                .map { it.group.ifBlank { "Χωρίς ομάδα" } }
                .filter { it !in knownTitles }
                .distinct()
                .map { it to it }
                .toList()
            LoadedCatalog(
                rawItems,
                normalized.items,
                knownCategories + missingGroups,
                normalized.seriesEpisodes,
                loadedTotals,
                loadedPageSize,
            )
        }
    }

    /**
     * ΜΙΑ σελίδα μιας κατηγορίας, όταν ο χρήστης φτάσει στο τέλος της λίστας.
     *
     * ΠΕΡΝΑΕΙ ΑΠΟ ΤΟΝ ΙΔΙΟ ΣΕΙΡΙΑΚΟ ΦΡΑΓΜΟ με τη φόρτωση: αν κατεβαίνει ακόμη ο
     * κατάλογος, η σελίδα περιμένει αντί να πιέσει το portal παράλληλα. Μετά το
     * κόψιμο του 2ου περάσματος η αναμονή αυτή είναι δευτερόλεπτα, όχι λεπτά.
     *
     * Κενή λίστα = δεν υπάρχει άλλο, ή ο πάροχος δεν σελιδοποιεί (Xtream, M3U).
     */
    suspend fun categoryPage(
        playlist: Playlist,
        type: String,
        categoryId: String,
        categoryTitle: String,
        page: Int,
    ): List<Channel> = withProviderLock {
        withContext(Dispatchers.IO) {
            when (playlist.type) {
                PlaylistType.STALKER -> Repository.stalkerCategoryPage(
                    stalkerFor(playlist), type, categoryId, categoryTitle, page
                )
                else -> emptyList()
            }
        }
    }

    suspend fun <T> withProviderLock(block: suspend () -> T): T = providerMutex.withLock { block() }

    fun captureVisible(state: UiState): VisibleCatalogSnapshot = VisibleCatalogSnapshot(
        contentType = state.contentType,
        channels = state.channels,
        groups = state.groups,
        favorites = state.favorites,
        selectedGroup = state.selectedGroup,
    )

    fun restoreAfterRefreshFailure(
        current: UiState,
        snapshot: VisibleCatalogSnapshot,
        status: String,
    ): UiState = current.copy(
        contentType = snapshot.contentType,
        channels = snapshot.channels,
        groups = snapshot.groups,
        favorites = snapshot.favorites,
        selectedGroup = snapshot.selectedGroup,
        loading = false,
        status = status,
    )
}
