package com.prelude.iptv.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.data.SourceLoadProgress
import com.prelude.iptv.data.SourceProgressCallback
import com.prelude.iptv.data.Repository
import com.prelude.iptv.data.SessionCatalogSnapshot
import com.prelude.iptv.net.Http
import com.prelude.iptv.source.StalkerClient
import com.prelude.iptv.tvhome.TvHomeSyncScheduler
import com.prelude.iptv.ui.coordinator.CatalogLoadCoordinator
import com.prelude.iptv.ui.coordinator.CatalogSessionStore
import com.prelude.iptv.ui.coordinator.CategoryEditorCoordinator
import com.prelude.iptv.ui.coordinator.ExportRelayCoordinator
import com.prelude.iptv.ui.coordinator.MainEpgCoordinator
import com.prelude.iptv.ui.coordinator.ProfileSettingsCoordinator
import com.prelude.iptv.ui.coordinator.SeriesLoadCoordinator
import com.prelude.iptv.ui.coordinator.SourceGenerationGate
import com.prelude.iptv.ui.coordinator.SourceSwitchCoordinator
import com.prelude.iptv.ui.coordinator.SourceSwitchStatePolicy
import com.prelude.iptv.ui.epg.EpgStatus
import com.prelude.iptv.ui.policy.CatalogPresentationPolicy
import com.prelude.iptv.ui.profile.ProfileDisplayName
import com.prelude.iptv.category.CategoryEditorState
import com.prelude.iptv.category.CategoryLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = PlaylistStore(app)
    private val _profiles = MutableStateFlow<List<PlaylistStore.Profile>>(store.profiles())
    val profilesState: StateFlow<List<PlaylistStore.Profile>> = _profiles.asStateFlow()
    private var stalker: StalkerClient? = null
    private var stalkerSourceId: String = ""
    private var m3uCache: List<Channel> = emptyList()
    /** EPG δηλωμένο στην κεφαλίδα του M3U (url-tvg) */
    private var m3uEpgUrl: String = ""

    /** Τι διάλεξε ο χρήστης ανά (λίστα, ενότητα): null = όλα. Αν υπάρχει, δεν ξαναρωτάμε. */
    private val loadChoice = HashMap<String, List<String>?>()

    /** Bounded process-only catalogs and source working sets. */
    private val catalogSession = CatalogSessionStore()
    private val sourceGeneration = SourceGenerationGate()

    /**
     * Σταθερή ταυτότητα λίστας. ΠΡΟΣΟΧΗ: με index ως κλειδί, μια διαγραφή
     * μετατόπιζε τα index και οι λίστες κληρονομούσαν ξένα δεδομένα.
     */
    private fun plId(pl: Playlist?): String =
        pl?.let(com.prelude.iptv.data.PlaylistIdentity::stableId) ?: "?"

    fun currentSourceId(): String = currentPlaylist()?.let(PlaylistIdentity::stableId).orEmpty()

    private fun updateSourceProgress(
        pl: Playlist,
        percent: Int?,
        stage: String,
        active: Boolean = true,
        contentType: String = _state.value.contentType
    ) {
        val sourceId = PlaylistIdentity.stableId(pl)
        val safePercent = percent?.coerceIn(0, 100)
        _state.update { current ->
            val next = SourceLoadProgress(
                percent = safePercent,
                stage = stage,
                contentType = contentType,
                active = active
            )
            if (current.sourceProgress[sourceId] == next) current
            else current.copy(sourceProgress = current.sourceProgress + (sourceId to next))
        }
    }

    private fun progressCallback(pl: Playlist, gen: Int, type: String): SourceProgressCallback = { percent, stage ->
        if (sourceGeneration.isCurrentLoad(gen)) {
            updateSourceProgress(pl, percent, stage, active = true, contentType = type)
        }
    }

    private fun finishSourceProgress(pl: Playlist, stage: String, success: Boolean, type: String) {
        updateSourceProgress(
            pl = pl,
            percent = if (success) 100 else null,
            stage = stage,
            active = false,
            contentType = type
        )
    }

    /**
     * Η επιλογή του χρήστη, από μνήμη ή δίσκο.
     * Χάρη στον δίσκο, μετά από επανεκκίνηση ΔΕΝ ξαναρωτάει.
     */
    private fun rememberedChoice(k: String): Pair<Boolean, List<String>?> {
        if (loadChoice.containsKey(k)) return true to loadChoice[k]
        val (has, ids) = store.loadChoiceFor(k)
        if (has) loadChoice[k] = ids
        return has to ids
    }

    private fun forgetRememberedChoices(sourceId: String) {
        if (sourceId.isBlank()) return
        loadChoice.keys.removeAll { it.startsWith("$sourceId:") }
    }

    private fun clearPersistedSourceState(
        playlist: Playlist,
        sourceId: String,
        deleteLocalFile: Boolean,
    ) {
        store.clearLoadChoices(sourceId)
        store.clearCategoryLayouts(sourceId)
        store.clearSection(sourceId)
        forgetRememberedChoices(sourceId)
        store.clearHistory(sourceId)
        store.clearFavorites(sourceId)
        clearSourceSession(playlist)
        if (deleteLocalFile && playlist.type == com.prelude.iptv.data.PlaylistType.M3U && !playlist.isUrl) {
            runCatching { java.io.File(playlist.source).delete() }
        }
    }

    /**
     * Cancels the actual coroutine and its provider HTTP calls. The generation
     * check remains as a second safety net for blocking provider code that is
     * already unwinding after cancellation.
     */
    private fun cancelActiveLoad() {
        lastPartialPublishMs = 0L
        sourceGeneration.invalidateAll()
        cancelActiveLoadJobs()
    }

    /** Cancels jobs after their generation has already been invalidated. */
    private fun cancelActiveLoadJobs() {
        val hadActiveProviderWork = catalogLoadJob?.isActive == true ||
            allSectionsLoadJob?.isActive == true || seriesLoadJob?.isActive == true
        catalogLoadJob?.cancel()
        allSectionsLoadJob?.cancel()
        epgCoordinator.cancelSearch()
        seriesLoadJob?.cancel()
        seriesLoader.cancel()
        catalogLoadJob = null
        allSectionsLoadJob = null
        seriesLoadJob = null
        if (hadActiveProviderWork) {
            stalker?.cancelPendingRequests()
            stalker = null
            stalkerSourceId = ""
            Http.cancelProviderRequests()
        }
        _state.update { current ->
            val cancelled = current.sourceProgress.mapValues { (_, progress) ->
                if (progress.active) progress.copy(active = false, percent = null, stage = "Η λήψη ακυρώθηκε")
                else progress
            }
            if (cancelled == current.sourceProgress && !current.loadingAllSections) current
            else current.copy(sourceProgress = cancelled, loadingAllSections = false)
        }
    }

    /**
     * Σταματά ΚΑΘΕ εργασία της τρέχουσας πηγής και ξεχνά ό,τι κρατούσε στη μνήμη.
     *
     * ΓΙΑΤΙ ΥΠΑΡΧΕΙ: αυτές οι έξι γραμμές ήταν γραμμένες τρεις φορές — στην
     * προσθήκη, στην επεξεργασία και στη διαγραφή λίστας. Τρία αντίγραφα σημαίνει
     * ότι κάθε νέο πεδίο συνεδρίας πρέπει να θυμηθεί κανείς να το μηδενίσει σε
     * τρία σημεία, και το σημείο που ξεχνιέται δεν δίνει σφάλμα: δίνει δεδομένα
     * της ΠΡΟΗΓΟΥΜΕΝΗΣ πηγής μέσα στη νέα.
     *
     * Ακριβώς αυτό ήταν το «η λίστα κληρονομούσε ξένα δεδομένα».
     */
    private fun resetSourceSession() {
        cancelActiveLoad()
        epgCoordinator.cancelLoad(clearManager = true)
        stalker = null
        stalkerSourceId = ""
        m3uCache = emptyList()
        m3uEpgUrl = ""
    }

    /** Invalidates only one source. History and category preferences remain untouched. */
    private fun clearSourceSession(pl: Playlist) {
        val sourceId = PlaylistIdentity.stableId(pl)
        catalogSession.invalidateSource(sourceId)
        if (stalkerSourceId == sourceId) {
            stalker?.cancelPendingRequests()
            stalker = null
            stalkerSourceId = ""
        }
        if (currentSourceId() == sourceId) {
            m3uCache = emptyList()
            m3uEpgUrl = ""
            catalogSession.clearWorkingSet()
        }
    }

    private fun categoryChoice(pl: Playlist, type: String): Pair<Boolean, List<String>?> =
        rememberedChoice("${plId(pl)}:$type")

    private fun cacheKey(pl: Playlist, type: String, ids: List<String>?) =
        catalogSession.catalogKey(PlaylistIdentity.stableId(pl), type, ids)

    private fun cacheCatalog(
        pl: Playlist,
        type: String,
        ids: List<String>?,
        channels: List<Channel>,
        groups: List<String>,
        seriesEpisodes: Map<String, List<Pair<String, List<Channel>>>>
    ) {
        catalogSession.putCatalog(
            cacheKey(pl, type, ids),
            SessionCatalogSnapshot(
                channels = channels,
                groups = groups,
                seriesEpisodes = seriesEpisodes,
                epgUrl = if (pl.type == com.prelude.iptv.data.PlaylistType.M3U) m3uEpgUrl else ""
            )
        )
        refreshHomeCatalog()
    }

    /* ============ Αρχική: η ένωση των ενοτήτων, όχι η ενεργή ============ */

    /**
     * ΤΟ ΠΡΟΒΛΗΜΑ ΠΟΥ ΛΥΝΕΙ: ο επεξεργαστής αρχικής απαριθμεί δέκα ενότητες —
     * ζωντανά, ταινίες, σειρές, νέα, κορυφαία — αλλά η Αρχική ζωγραφιζόταν από
     * το `state.channels`, δηλαδή **μόνο την ενότητα που είναι φορτωμένη**. Με
     * φορτωμένες τις Ταινίες, οι ράγες σειρών και καναλιών δεν είχαν από πού να
     * πάρουν δεδομένα και εξαφανίζονταν. Ο χρήστης έβλεπε δέκα διακόπτες και
     * τρεις ράγες, χωρίς καμία ένδειξη γιατί.
     *
     * Δεν χρειάζεται νέα λήψη: το [CatalogSessionStore] κρατά ήδη LRU τριών
     * στιγμιοτύπων, ένα ανά ενότητα. Αυτή η ροή είναι η ένωσή τους — αναφορές
     * στις ίδιες λίστες, όχι αντίγραφα.
     */
    private val _homeCatalog = MutableStateFlow<List<Channel>>(emptyList())
    val homeCatalogState: StateFlow<List<Channel>> = _homeCatalog.asStateFlow()

    private val homeSectionTypes = listOf("live", "vod", "series")

    private fun cachedSection(pl: Playlist, type: String): List<Channel>? {
        return catalogSession.getCatalog(cacheKey(pl, type, null))?.channels
    }

    private fun refreshHomeCatalog() {
        val pl = currentPlaylist()
        if (pl == null) {
            _homeCatalog.value = emptyList()
            return
        }
        _homeCatalog.value = homeSectionTypes.flatMap { cachedSection(pl, it).orEmpty() }
    }

    private fun cacheSeriesEpisodes(
        pl: Playlist,
        seriesId: String,
        seasons: List<Pair<String, List<Channel>>>
    ) {
        if (seriesId.isBlank() || seasons.isEmpty()) return
        val (hasChoice, ids) = categoryChoice(pl, "series")
        if (!hasChoice) return
        val key = cacheKey(pl, "series", ids)
        val snapshot = catalogSession.getCatalog(key) ?: return
        catalogSession.putCatalog(
            key,
            snapshot.copy(seriesEpisodes = snapshot.seriesEpisodes + (seriesId to seasons))
        )
    }

    /** Restores a catalog without network, parsing or normalization work. */
    private fun restoreSessionCatalog(pl: Playlist, type: String, ids: List<String>?): Boolean {
        val snapshot = catalogSession.getCatalog(cacheKey(pl, type, ids)) ?: return false
        loadedContentType = type
        store.saveLastSection(plId(pl), type)
        catalogSession.seriesEpisodes = if (type == "series") snapshot.seriesEpisodes else emptyMap()
        catalogSession.liveChannels = if (type == "live") snapshot.channels else catalogSession.liveChannels
        if (pl.type == com.prelude.iptv.data.PlaylistType.M3U && snapshot.epgUrl.isNotBlank()) {
            m3uEpgUrl = snapshot.epgUrl
        }
        finishSourceProgress(
            pl,
            "Έτοιμο από μνήμη · ${snapshot.channels.size} στοιχεία",
            success = true,
            type = type
        )
        val sourceId = PlaylistIdentity.stableId(pl)
        val favoriteCandidates = if (type == "series") {
            snapshot.channels + snapshot.seriesEpisodes.values.flatMap { seasons -> seasons.flatMap { it.second } }
        } else snapshot.channels
        store.reconcileFavorites(sourceId, favoriteCandidates)
        persistCatalogCount(sourceId, type, snapshot.channels.size)
        val favoriteKeys = store.loadFavorites(sourceId)
        val restoredGroups = buildGroups(snapshot.channels, favoriteKeys.isNotEmpty())
        _state.value = _state.value.copy(
            chooseContent = false,
            askLoadType = null,
            askLoadMode = false,
            pickCategories = false,
            contentType = type,
            channels = snapshot.channels,
            favorites = favoriteKeys,
            groups = restoredGroups,
            selectedGroup = UiState.ALL_GROUP,
            loading = false,
            status = "Έτοιμα ${snapshot.channels.size} στοιχεία · μνήμη συνεδρίας"
        )
        if (type == "live") loadEpgIfAny(pl)
        return true
    }

    private fun restoreRememberedSession(pl: Playlist, type: String): Boolean {
        val (hasChoice, ids) = categoryChoice(pl, type)
        return hasChoice && restoreSessionCatalog(pl, type, ids)
    }

    private fun rememberM3uPayload(
        pl: Playlist,
        result: com.prelude.iptv.data.LoadResult
    ): CatalogSessionStore.M3uPayload {
        val payload = catalogSession.rememberM3u(PlaylistIdentity.stableId(pl), result)
        m3uCache = payload.channels
        m3uEpgUrl = payload.epgUrl
        return payload
    }

    private fun m3uPayload(
        pl: Playlist,
        onProgress: SourceProgressCallback?
    ): CatalogSessionStore.M3uPayload {
        val sourceId = PlaylistIdentity.stableId(pl)
        catalogSession.getM3u(sourceId)?.let { payload ->
            m3uCache = payload.channels
            m3uEpgUrl = payload.epgUrl
            return payload
        }
        return rememberM3uPayload(pl, Repository.load(pl, "live", onProgress))
    }

    /**
     * Τα generation tokens ανήκουν στο SourceGenerationGate. Κάθε coroutine
     * κρατάει το token εκκίνησής της και απορρίπτει late callbacks μετά από
     * αλλαγή πηγής, νέο refresh ή νεότερο series request.
     */
    private var catalogLoadJob: Job? = null
    private var allSectionsLoadJob: Job? = null
    private var seriesLoadJob: Job? = null

    private val catalogLoader by lazy {
        CatalogLoadCoordinator(
            stalkerFor = ::stalkerFor,
            m3uPayload = { playlist, progress -> m3uPayload(playlist, progress) },
        )
    }

    private val seriesLoader by lazy { SeriesLoadCoordinator(catalogLoader) }

    private val categoryEditor by lazy {
        CategoryEditorCoordinator(
            scope = viewModelScope,
            currentPlaylist = ::currentPlaylist,
            currentSourceId = ::currentSourceId,
            currentContentType = { _state.value.contentType },
            loadLayout = store::loadCategoryLayout,
            loadCategories = { playlist, type -> catalogLoader.categories(playlist, type) },
            saveLayout = store::saveCategoryLayout,
            saveChoice = { key, ids ->
                loadChoice[key] = ids
                store.saveLoadChoice(key, ids)
            },
            reloadSelection = ::loadSelectedCategories,
        )
    }
    val categoryEditorState: StateFlow<CategoryEditorState> get() = categoryEditor.state
    val categoryLayoutRevision: StateFlow<Int> get() = categoryEditor.revision

    fun categoryTitlesInOrder(type: String): List<String> =
        categoryEditor.titlesInOrder(type)

    private var loadedContentType: String = "live"

    private val _state = MutableStateFlow(UiState())

    private val epgCoordinator by lazy {
        MainEpgCoordinator(
            app = getApplication(),
            store = store,
            state = _state,
            scope = viewModelScope,
            currentPlaylist = ::currentPlaylist,
            currentSourceId = ::currentSourceId,
            m3uEpgUrl = { m3uEpgUrl },
            liveChannels = { catalogSession.liveChannels },
            // Το ΙΔΙΟ φίλτρο γονικού ελέγχου με τη βιβλιοθήκη, όχι αντίγραφο:
            // δύο υλοποιήσεις θα αποκλίνανε, και η απόκλιση εδώ σημαίνει
            // κλειδωμένο περιεχόμενο που εμφανίζεται στο πρόγραμμα.
            parentalAllowed = { library.parentalAllowed(it) },
        )
    }

    private val sourceSwitchCoordinator by lazy {
        SourceSwitchCoordinator(
            generationGate = sourceGeneration,
            callbacks = SourceSwitchCoordinator.Callbacks(
                playlists = { _state.value.playlists },
                persistLastPlaylist = { store.lastPlaylist = it },
                cancelActiveWork = ::cancelActiveLoadJobs,
                cancelEpgLoad = { epgCoordinator.cancelLoad(clearManager = true) },
                resetSourceRuntime = {
                    stalker = null
                    stalkerSourceId = ""
                    m3uCache = emptyList()
                    m3uEpgUrl = ""
                    catalogSession.liveChannels = emptyList()
                    catalogSession.seriesEpisodes = emptyMap()
                },
                invalidateRepository = Repository::invalidate,
                clearSourceSession = ::clearSourceSession,
                lastSection = store::lastSection,
                hasRememberedChoice = { store.loadChoiceFor(it).first },
                favoritesFor = store::loadFavorites,
                publish = { plan ->
                    loadedContentType = plan.contentType
                    _state.value = SourceSwitchStatePolicy.apply(_state.value, plan)
                },
                autoLoad = ::loadAllSections,
            ),
        )
    }

    /** Synchronous profile + parental-control boundary; no source/catalog state. */
    private val profileSettings by lazy {
        ProfileSettingsCoordinator(
            app = getApplication(),
            store = store,
            state = _state,
            profiles = _profiles,
        )
    }

    /**
     * Αγαπημένα, ιστορικό, αναζήτηση, πρόοδος — τα δεδομένα ΤΟΥ ΧΡΗΣΤΗ, ξεχωριστά
     * από τον κατάλογο του παρόχου.
     *
     * Το sourceId περνιέται ως συνάρτηση και όχι ως τιμή: η ενεργή πηγή αλλάζει
     * όσο ζει το ViewModel, και μια παγωμένη τιμή θα έγραφε τα αγαπημένα της μιας
     * πηγής στα δεδομένα της άλλης.
     */
    private val library by lazy {
        com.prelude.iptv.ui.coordinator.LibraryCoordinator(
            app = getApplication(),
            store = store,
            state = _state,
            sourceId = ::currentSourceId,
            favKey = ::favKey,
        )
    }

    /** Relay and M3U export boundary; MainViewModel keeps only the public facade. */
    private val exportRelay by lazy {
        ExportRelayCoordinator(
            currentStalker = ::currentStalker,
            currentChannels = { _state.value.channels },
            resolvePlayableUrl = ::resolvePlayableUrl,
            startRelayService = { com.prelude.iptv.RelayService.start(getApplication()) },
            stopRelayService = { com.prelude.iptv.RelayService.stop(getApplication()) },
            publishRelayState = { running, url ->
                _state.value = _state.value.copy(relayRunning = running, relayUrl = url)
            },
        )
    }

    /**
     * Compatibility stream for non-Compose business helpers. New Compose route
     * boundaries should collect one of the smaller slices below.
     */
    val state: StateFlow<UiState> = _state.asStateFlow()

    val appShellState: StateFlow<AppShellUiState> = _state
        .map(UiState::toAppShellUiState)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState().toAppShellUiState())

    val catalogState: StateFlow<CatalogUiState> = _state
        .map(UiState::toCatalogUiState)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), UiState().toCatalogUiState())

    val catalogProgressState: StateFlow<CatalogProgressUiState> = _state
        .map(UiState::toCatalogProgressUiState)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), UiState().toCatalogProgressUiState())

    val settingsState: StateFlow<SettingsUiState> = _state
        .map(UiState::toSettingsUiState)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), UiState().toSettingsUiState())

    val epgState: StateFlow<EpgUiState> = _state
        .map(UiState::toEpgUiState)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), UiState().toEpgUiState())

    val exportState: StateFlow<ExportUiState> = _state
        .map(UiState::toExportUiState)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), UiState().toExportUiState())

    init {
        // φόρτωσε ρυθμίσεις υποτίτλων στον SubtitleClient
        val (k, u, p) = store.loadSubSettings()
        com.prelude.iptv.data.SubtitleClient.apiKey = k
        com.prelude.iptv.data.SubtitleClient.osUser = u
        com.prelude.iptv.data.SubtitleClient.osPass = p
        com.prelude.iptv.data.TmdbClient.init(app)
        // Από αυτή την έκδοση τα catalogs κατεβαίνουν πάντα φρέσκα. Καθάρισε
        // παλιά persistent cache αρχεία που μπορεί να περιέχουν ληγμένα URLs.
        viewModelScope.launch(Dispatchers.IO) {
            com.prelude.iptv.data.ChannelDiskCache.clearAll(app)
            com.prelude.iptv.data.SeriesDiskCache.clearAll(app)
        }

        val pls = store.loadPlaylists()
        pls.forEach(store::migrateLegacyPlaylistKeys)
        store.purgeUnsafeLegacyKeys()
        val locked = store.lockedGroups()
        val idx = store.lastPlaylist.coerceIn(0, (pls.size - 1).coerceAtLeast(0))
        val initialSourceId = pls.getOrNull(idx)?.let(PlaylistIdentity::stableId).orEmpty()
        val favs = store.loadFavorites(initialSourceId)
        _state.value = _state.value.copy(playlists = pls, currentIndex = idx, favorites = favs, lockedGroups = locked, fontScale = store.fontScale)
        if (pls.isNotEmpty()) loadAllSections()
    }

    fun saveSubSettings(key: String, user: String, pass: String) {
        store.saveSubSettings(key, user, pass)
        com.prelude.iptv.data.SubtitleClient.apiKey = key
        com.prelude.iptv.data.SubtitleClient.osUser = user
        com.prelude.iptv.data.SubtitleClient.osPass = pass
    }

    fun loadSubSettings(): Triple<String, String, String> = store.loadSubSettings()

    /** Πρόσφατα — φιλτραρισμένα από τον γονικό έλεγχο (τα τραβάει το Hero). */
    fun recents(): List<Channel> = library.recents()

    fun addRecent(ch: Channel) = library.addRecent(ch)

    /** Ζωντανά που είδε πρόσφατα — ξεχωριστά από το ιστορικό ταινιών/σειρών. */
    fun recentLive(): List<Channel> = library.recentLive()

    /** «Καθάρισμα» από την «Επεξεργασία αρχικής». */
    fun clearHomeHistory(sectionId: String) = library.clearHomeHistory(sectionId)

    fun epgEnabled(): Boolean = epgCoordinator.isEnabled()

    fun setEpgEnabled(v: Boolean) = epgCoordinator.setEnabled(v)

    fun loadTmdbKey(): String = store.tmdbKey

    fun saveTmdbKey(key: String) {
        store.tmdbKey = key
        com.prelude.iptv.data.TmdbClient.setKey(getApplication(), key)
    }

    /** Εμπλουτισμός από TMDB (τρέχει σε IO). */
    suspend fun tmdb(ch: Channel): com.prelude.iptv.data.TmdbClient.Meta? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.prelude.iptv.data.TmdbClient.fetch(ch.name, ch.kind == "series", ch.year)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }

    /** Ενιαίο κλειδί — ίδιο με τον player (PlaybackQueue), ΟΧΙ δεύτερο αντίγραφο. */
    fun favKey(ch: Channel): String = com.prelude.iptv.data.PlaybackQueue.favKey(ch)

    fun currentPlaylist(): Playlist? =
        _state.value.playlists.getOrNull(_state.value.currentIndex)

    fun currentStalker(): StalkerClient? =
        stalker.takeIf { stalkerSourceId == currentSourceId() }

    private fun stalkerFor(pl: Playlist): StalkerClient {
        val sourceId = PlaylistIdentity.stableId(pl)
        stalker?.takeIf { stalkerSourceId == sourceId }?.let { return it }
        return Repository.stalkerConnect(pl).also {
            stalker = it
            stalkerSourceId = sourceId
        }
    }

    /**
     * Resolves the playable URL at click time. Stalker commands need a live
     * provider session, so a cached catalog never depends on an old client.
     */
    suspend fun resolvePlayableUrl(ch: Channel): String = withContext(Dispatchers.IO) {
        val pl = currentPlaylist() ?: return@withContext ""
        if (pl.type != com.prelude.iptv.data.PlaylistType.STALKER || ch.cmd.isBlank()) {
            return@withContext Repository.playableUrl(ch, currentStalker())
        }

        fun connectFresh(): StalkerClient = Repository.stalkerConnect(pl).also {
            stalker = it
            stalkerSourceId = PlaylistIdentity.stableId(pl)
        }

        var client = currentStalker() ?: connectFresh()
        val first = try {
            Repository.playableUrl(ch, client)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ""
        }
        if (first.isNotBlank()) return@withContext first

        // A portal token may expire while the in-memory catalog is still valid.
        client = connectFresh()
        Repository.playableUrl(ch, client)
    }

    fun selectPlaylist(i: Int) {
        sourceSwitchCoordinator.switchTo(i)
        // Η ένωση της Αρχικής είναι δεμένη με την πηγή. Χωρίς αυτό, μετά την
        // αλλαγή θα έδειχνε για λίγο τον κατάλογο της προηγούμενης.
        refreshHomeCatalog()
    }

    fun saveFontScale(f: Float) {
        val v = f.coerceIn(0.7f, 1.8f)
        store.fontScale = v
        _state.value = _state.value.copy(fontScale = v)
    }

    /* ==================== ΦΟΡΤΩΣΗ ΕΝΟΤΗΤΩΝ ============================
     * Κανόνες:
     *  1. Οι επιλογές κατηγοριών παραμένουν αποθηκευμένες ανά πηγή/ενότητα.
     *  2. Τα catalogs κρατιούνται μόνο στη μνήμη της τρέχουσας εφαρμογής.
     *  3. Αλλαγή tab επαναφέρει το session snapshot χωρίς δίκτυο.
     *  4. Το ρητό refresh ακυρώνει cache/session και ζητά νέα δεδομένα.
     *  5. Το ιστορικό/resume παραμένει χωριστά από catalog και ανά πηγή.
     * ================================================================= */

    /** Ξανανοίγει την επιλογή ενότητας (Live / Ταινίες / Σειρές). */
    fun showContentChooser() {
        _state.value = _state.value.copy(chooseContent = true)
    }

    fun closeContentChooser() {
        _state.value = _state.value.copy(chooseContent = false)
    }

    /**
     * Αλλαγή ενότητας από τα pills. Το πρώτο άνοιγμα κάνει πραγματική λήψη,
     * ενώ η επιστροφή στην ίδια ενότητα χρησιμοποιεί το session-only snapshot.
     */
    fun setContentType(t: String) {
        val pl = currentPlaylist() ?: return
        if (t == _state.value.contentType && _state.value.channels.isNotEmpty()) {
            _state.value = _state.value.copy(chooseContent = false, askLoadType = null)
            return
        }

        // “Όλα” imports sections sequentially in the background. Completed sections
        // remain immediately browsable; selecting a section that is still pending
        // must not cancel the import or blank the currently visible catalog.
        if (_state.value.loadingAllSections) {
            if (restoreSessionCatalog(pl, t, null)) {
                _state.value = _state.value.copy(chooseContent = false)
                return
            }
            _state.value = _state.value.copy(
                chooseContent = false,
                status = "Η ενότητα ${sectionLabel(t)} κατεβαίνει ακόμη στο παρασκήνιο…"
            )
            return
        }

        cancelActiveLoad()
        _state.value = _state.value.copy(
            chooseContent = false,
            askLoadType = null,
            contentType = t,
            channels = emptyList(),
            groups = emptyList(),
            selectedGroup = UiState.ALL_GROUP,
            askRefreshMode = false,
            askLoadMode = false,
            pickCategories = false,
            categoryPickerFromRefresh = false,
            categorySelectionIds = null,
            loading = false,
            status = ""
        )
        if (restoreSessionCatalog(pl, t, null)) return
        loadAllSections()
    }

    // Τα confirmLoadType/cancelLoadType αφαιρέθηκαν μαζί με το ενδιάμεσο
    // dialog «Φόρτωση Χ;» — το setContentType πάει πλέον κατευθείαν στη ροή.

    /** Router: Stalker/Xtream → κατηγορίες· M3U → επιλογή group (μετά το parse). */
    fun loadCurrent(forceNetwork: Boolean = false) {
        val pl = currentPlaylist() ?: return
        val type = _state.value.contentType
        if (!forceNetwork && restoreSessionCatalog(pl, type, null)) return
        loadAllSections()
    }

    /** Refresh is available whenever an active source exists. */
    fun canRefreshCurrentSection(): Boolean = currentPlaylist() != null

    /** Opens the shared TV/mobile refresh choice without starting network work. */
    fun requestRefresh() {
        if (currentPlaylist() == null || _state.value.loading) return
        _state.value = _state.value.copy(askRefreshMode = true)
    }

    fun cancelRefreshChoice() {
        _state.value = _state.value.copy(askRefreshMode = false)
    }

    private fun invalidateForRefresh(pl: Playlist) {
        cancelActiveLoad()
        Repository.invalidate(pl)
        clearSourceSession(pl)
        stalker = null
        stalkerSourceId = ""
    }

    /**
     * Refreshes only the groups already stored for the current source/section.
     * The visible catalog stays in place until fresh data succeeds.
     */
    fun refreshExistingSelection() {
        val pl = currentPlaylist() ?: return
        val type = _state.value.contentType
        val hasRememberedChoice = rememberedChoice("${plId(pl)}:$type").first
        if (!hasRememberedChoice) {
            refreshAndChooseGroups()
            return
        }
        invalidateForRefresh(pl)
        updateSourceProgress(pl, 0, "Ανανέωση υπαρχόντων groups…", active = true, contentType = type)
        _state.value = _state.value.copy(
            askRefreshMode = false,
            loading = true,
            status = "Ανανέωση υπαρχόντων groups…"
        )
        loadRemembered(pl, force = true)
    }

    /**
     * Downloads a fresh provider group list and opens the picker directly.
     * Existing channels remain visible if the user cancels the new selection.
     */
    fun refreshAndChooseGroups() {
        val pl = currentPlaylist() ?: return
        val type = _state.value.contentType
        val (_, rememberedIds) = categoryChoice(pl, type)
        invalidateForRefresh(pl)
        updateSourceProgress(pl, 0, "Ανανέωση διαθέσιμων groups…", active = true, contentType = type)
        _state.value = _state.value.copy(
            askRefreshMode = false,
            askLoadMode = false,
            pickCategories = false,
            categoryPickerFromRefresh = false,
            categorySelectionIds = null,
            loading = true,
            status = "Ανανέωση διαθέσιμων groups…"
        )
        startCategoryPick(
            pl = pl,
            forceAsk = true,
            openPickerDirectly = true,
            initialSelectionIds = rememberedIds
        )
    }

    /** Compatibility entry point for older call sites. */
    fun refresh() = refreshExistingSelection()

    /**
     * Φέρνει τη λίστα κατηγοριών.
     * ΔΕΝ αγγίζει τα κανάλια που βλέπει ο χρήστης: αν ακυρώσει, όλα μένουν ως έχουν.
     */
    private fun startCategoryPick(
        pl: Playlist,
        forceAsk: Boolean = false,
        openPickerDirectly: Boolean = false,
        initialSelectionIds: List<String>? = null
    ) {
        // Ξέρουμε ήδη τι θέλει (από προηγούμενη φορά); Τότε ΔΕΝ κατεβάζουμε καν
        // κατηγορίες και δεν ξαναρωτάμε: φορτώνουμε κατευθείαν ό,τι είχε
        // επιλέξει — ΣΕ ΟΛΕΣ τις ενότητες που είχε ανοίξει, όχι μόνο στην τελευταία.
        if (!forceAsk && loadRemembered(pl)) return
        _state.value = _state.value.copy(loading = true, status = "Σύνδεση…")
        // Παγώνουμε ΤΩΡΑ τι φορτώνουμε: αν στο μεταξύ ο χρήστης αλλάξει
        // λίστα/ενότητα, τα captured pl/type/gen δεν «γλιστράνε» στη νέα.
        val gen = sourceGeneration.currentLoad()
        val type = _state.value.contentType
        val progress = progressCallback(pl, gen, type)
        updateSourceProgress(pl, 1, "Σύνδεση…", active = true, contentType = type)
        catalogLoadJob = viewModelScope.launch {
            try {
                val cats = catalogLoader.categories(pl, type, progress)
                if (!sourceGeneration.isCurrentLoad(gen)) return@launch     // άλλαξε λίστα στο μεταξύ
                val k = "${plId(pl)}:$type"
                val (remembered, rememberedIds) = rememberedChoice(k)
                when {
                    // Καμία κατηγορία -> φόρτωσε τα πάντα. Αν αυτό ξεκίνησε από
                    // «Ανανέωση + επιλογή νέων groups», κράτησε transactional
                    // persistence ακόμη και χωρίς ενδιάμεσο picker.
                    cats.isEmpty() -> loadSelectedCategoriesInternal(
                        ids = null,
                        replaceActive = false,
                        refreshSelectionOverride = openPickerDirectly
                    )
                    // refresh + επιλογή: πήγαινε κατευθείαν στον picker, με την παλιά επιλογή προσημειωμένη
                    openPickerDirectly -> {
                        finishSourceProgress(pl, "Τα διαθέσιμα groups είναι έτοιμα", success = true, type = type)
                        _state.value = _state.value.copy(
                            loading = false,
                            categories = cats,
                            askLoadMode = false,
                            pickCategories = true,
                            categoryPickerFromRefresh = true,
                            categorySelectionIds = CatalogRefreshPolicy.initialSelection(cats, initialSelectionIds),
                            status = "${cats.size} διαθέσιμα groups"
                        )
                    }
                    // ξέρουμε ήδη τι θέλει (ακόμα κι από προηγούμενη εκτέλεση) -> φόρτωσε
                    !forceAsk && remembered -> loadSelectedCategoriesInternal(rememberedIds, replaceActive = false)
                    // αλλιώς ρώτα: όλα ή επιλογή;
                    else -> {
                        finishSourceProgress(pl, "Οι κατηγορίες είναι έτοιμες", success = true, type = type)
                        _state.value = _state.value.copy(
                            loading = false,
                            categories = cats,
                            askLoadMode = true,
                            pickCategories = false,
                            categoryPickerFromRefresh = false,
                            categorySelectionIds = null,
                            status = "${cats.size} κατηγορίες"
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (!sourceGeneration.isCurrentLoad(gen)) return@launch     // μην εμφανίσεις σφάλμα άλλης λίστας
                // απέτυχε: γύρνα το pill στην ενότητα που όντως έχει περιεχόμενο
                finishSourceProgress(pl, "Σφάλμα: ${e.message}", success = false, type = type)
                _state.value = _state.value.copy(
                    loading = false, status = "Σφάλμα: ${e.message}",
                    contentType = loadedContentType
                )
            }
        }
    }

    private fun sectionLabel(type: String): String = when (type) {
        "live" -> "Live TV"
        "vod" -> "Ταινίες"
        "series" -> "Σειρές"
        else -> type
    }

    /**
     * Downloads Live TV, Movies and Series sequentially. A section is published
     * only after its complete provider result has been normalized and cached;
     * provider partials are deliberately never exposed to UiState. The first
     * completed section opens immediately while the remaining complete sections
     * become available through normal navigation. Sequential provider access
     * also protects Stalker/Xtream sessions.
     */
    fun loadAllSections() {
        val pl = currentPlaylist() ?: return
        cancelActiveLoad()
        Repository.invalidate(pl)
        clearSourceSession(pl)

        val gen = sourceGeneration.currentLoad()
        val sourceId = PlaylistIdentity.stableId(pl)
        val sections = listOf("live", "vod", "series")
        _state.value = _state.value.copy(
            chooseContent = false,
            askLoadMode = false,
            pickCategories = false,
            channels = emptyList(),
            groups = emptyList(),
            selectedGroup = UiState.ALL_GROUP,
            loading = true,
            loadingAllSections = true,
            loadedSections = emptySet(),
            status = "Λήψη όλων των ενοτήτων…"
        )
        updateSourceProgress(pl, 0, "Προετοιμασία όλων των ενοτήτων…", active = true, contentType = "all")

        allSectionsLoadJob = viewModelScope.launch {
            val failures = mutableListOf<String>()
            var published = false
            for ((index, type) in sections.withIndex()) {
                if (!sourceGeneration.isCurrentLoad(gen)) return@launch
                val base = index * 100 / sections.size
                val span = 100 / sections.size
                val progress: SourceProgressCallback = { percent, stage ->
                    if (sourceGeneration.isCurrentLoad(gen)) {
                        val mapped = percent?.let { (base + it * span / 100).coerceIn(0, 99) }
                        updateSourceProgress(
                            pl, mapped, "${sectionLabel(type)} · $stage",
                            active = true, contentType = "all"
                        )
                    }
                }
                try {
                    val loaded = catalogLoader.section(pl, type, null, progress)
                    if (!sourceGeneration.isCurrentLoad(gen)) return@launch
                    val rawChannels = loaded.rawItems
                    val channels = loaded.items
                    // rawChannels already contains every provider row/episode.
                    // Re-flattening seriesEpisodes duplicated the largest list at
                    // the exact moment peak loading memory was already highest.
                    val historyCandidates = if (type == "series") rawChannels else channels
                    store.migrateLegacyHistory(sourceId, historyCandidates)
                    store.reconcileHistory(sourceId, historyCandidates)
                    store.reconcileFavorites(sourceId, historyCandidates)
                    val favoriteKeys = store.loadFavorites(sourceId)
                    val groups = buildGroups(channels, favoriteKeys.isNotEmpty())
                    cacheCatalog(pl, type, null, channels, groups, loaded.seriesEpisodes)
                    persistCatalogCount(sourceId, type, channels.size)
                    if (type == "live") catalogSession.liveChannels = channels
                    if (type == "series") catalogSession.seriesEpisodes = loaded.seriesEpisodes

                    val completed = _state.value.loadedSections + type
                    val shouldPublish = !published || _state.value.contentType == type
                    if (shouldPublish) {
                        published = true
                        loadedContentType = type
                        store.saveLastSection(plId(pl), type)
                        _state.value = _state.value.copy(
                            contentType = type,
                            channels = channels,
                            groups = groups,
                            favorites = favoriteKeys,
                            selectedGroup = UiState.ALL_GROUP,
                            loading = true,
                            loadedSections = completed,
                            status = "${sectionLabel(type)} έτοιμο · συνεχίζεται η λήψη στο παρασκήνιο"
                        )
                        if (type == "live") loadEpgIfAny(pl)
                    } else {
                        _state.value = _state.value.copy(
                            loadedSections = completed,
                            status = "${sectionLabel(type)} έτοιμο · ${completed.size}/3 ενότητες"
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failures += "${sectionLabel(type)}: ${e.message ?: "σφάλμα"}"
                }
            }
            if (!sourceGeneration.isCurrentLoad(gen)) return@launch
            // Keep only the working set required by the section that remains
            // visible. Completed background sections are already in the bounded LRU.
            if (_state.value.contentType != "live") catalogSession.liveChannels = emptyList()
            if (_state.value.contentType != "series") catalogSession.seriesEpisodes = emptyMap()
            finishSourceProgress(
                pl,
                if (failures.isEmpty()) "Ολοκληρώθηκαν όλες οι ενότητες" else "Ολοκληρώθηκε με ${failures.size} αποτυχίες",
                success = failures.isEmpty(),
                type = "all"
            )
            _state.value = _state.value.copy(
                loading = false,
                loadingAllSections = false,
                status = if (failures.isEmpty()) {
                    "Έτοιμες Live TV, Ταινίες και Σειρές"
                } else {
                    "Ολοκληρώθηκε με προβλήματα: ${failures.joinToString(" · ")}"
                }
            )
            allSectionsLoadJob = null
        }
    }

    /** «Όλα» σημαίνει όλες τις κατηγορίες της ορατής ενότητας. */
    fun loadEverything() {
        loadSelectedCategories(null)
    }

    /** Restores the session snapshot first; network is only the cache-miss path. */
    private fun loadRemembered(pl: Playlist, force: Boolean = false): Boolean {
        val id = plId(pl)
        val type = _state.value.contentType
        val (hasChoice, ids) = rememberedChoice("$id:$type")
        if (!hasChoice) return false
        if (!force && restoreSessionCatalog(pl, type, ids)) return true
        val gen = sourceGeneration.currentLoad()
        val selectedGroupBeforeRefresh = _state.value.selectedGroup
        val rollbackSnapshot = if (force) catalogLoader.captureVisible(_state.value) else null
        val progress = progressCallback(pl, gen, type)
        updateSourceProgress(pl, 0, if (force) "Ανανέωση από την πηγή…" else "Λήψη από την πηγή…", active = true, contentType = type)
        _state.value = _state.value.copy(
            loading = true,
            status = if (force) "Ανανέωση από την πηγή…" else "Λήψη από την πηγή…",
            askLoadMode = false,
            pickCategories = false,
            chooseContent = false,
            askLoadType = null
        )
        catalogLoadJob = viewModelScope.launch {
            try {
                val loaded = catalogLoader.section(
                    pl, type, ids, progress,
                    partialPublisher(pl, type, gen, "Λήψη σε εξέλιξη")
                )
                if (!sourceGeneration.isCurrentLoad(gen)) return@launch
                val rawChannels = loaded.rawItems
                val channels = loaded.items
                catalogSession.seriesEpisodes = if (type == "series") loaded.seriesEpisodes else emptyMap()
                loadedContentType = type
                store.saveLastSection(id, type)
                val sourceId = PlaylistIdentity.stableId(pl)
                val historyCandidates = if (type == "series") rawChannels else channels
                store.migrateLegacyHistory(sourceId, historyCandidates)
                store.reconcileHistory(sourceId, historyCandidates)
                store.reconcileFavorites(sourceId, historyCandidates)
                val favoriteKeys = store.loadFavorites(sourceId)
                val groups = buildGroups(channels, favoriteKeys.isNotEmpty())
                catalogSession.liveChannels = if (type == "live") channels else emptyList()
                cacheCatalog(pl, type, ids, channels, groups, loaded.seriesEpisodes)
                persistCatalogCount(sourceId, type, channels.size)
                finishSourceProgress(pl, "Ολοκληρώθηκε · ${channels.size} στοιχεία", success = true, type = type)
                _state.value = _state.value.copy(
                    contentType = type,
                    channels = channels,
                    groups = groups,
                    favorites = favoriteKeys,
                    selectedGroup = if (force) {
                        CatalogRefreshPolicy.restoredVisibleGroup(
                            previousGroup = selectedGroupBeforeRefresh,
                            freshGroups = groups,
                            allGroup = UiState.ALL_GROUP
                        )
                    } else UiState.ALL_GROUP,
                    loading = false,
                    status = if (force) "Ενημερώθηκε · ${channels.size} στοιχεία"
                    else "Φορτώθηκαν ${channels.size} στοιχεία"
                )
                if (type == "live") loadEpgIfAny(pl)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (!sourceGeneration.isCurrentLoad(gen)) return@launch
                finishSourceProgress(pl, "Σφάλμα ανανέωσης: ${e.message}", success = false, type = type)
                val status = "Σφάλμα ανανέωσης: ${e.message}"
                _state.value = rollbackSnapshot?.let {
                    catalogLoader.restoreAfterRefreshFailure(_state.value, it, status)
                } ?: _state.value.copy(loading = false, status = status)
            }
        }
        return true
    }

    /** «Θέλω να επιλέξω» → ανοίγει ο διαλογέας κατηγοριών */
    fun chooseCategories() {
        _state.value = _state.value.copy(
            askLoadMode = false,
            pickCategories = true,
            categoryPickerFromRefresh = false,
            categorySelectionIds = null,
            status = "Διάλεξε κατηγορίες (${_state.value.categories.size})"
        )
    }

    /** ⋮ → «Κατηγορίες / Ομάδες…»: το ΜΟΝΟ σημείο που ξαναρωτάει επίτηδες. */
    fun changeCategories() {
        val pl = currentPlaylist() ?: return
        cancelActiveLoad()
        // Keep the old snapshot until the user confirms a new selection.
        // The parsed M3U payload is also reusable because this is a local filter change.
        startCategoryPick(pl, forceAsk = true)
    }

    /** Loads the complete provider category catalogue for the settings editor. */
    fun openCategoryEditor() = categoryEditor.open()

    fun updateCategoryEditorLayout(type: String, layout: CategoryLayout) =
        categoryEditor.updateLayout(type, layout)

    /** Persists all three tabs and immediately reloads the active section when needed. */
    fun saveCategoryEditor() = categoryEditor.save()

    /** Πίσω από τον διαλογέα κατηγοριών στην ερώτηση «όλα ή επιλογή;». */
    fun backToLoadMode() {
        _state.value = _state.value.copy(
            pickCategories = false,
            askLoadMode = true,
            categoryPickerFromRefresh = false,
            categorySelectionIds = null
        )
    }

    /** Back from a refresh picker cancels it; normal initial-load picker returns to mode choice. */
    fun cancelCategoryPicker() {
        if (_state.value.categoryPickerFromRefresh) cancelLoadMode() else backToLoadMode()
    }

    /** Άκυρο σε οποιαδήποτε ερώτηση: μένουμε σε ό,τι είναι ήδη φορτωμένο. */
    fun cancelLoadMode() {
        _state.value = _state.value.copy(
            askLoadMode = false,
            askRefreshMode = false,
            pickCategories = false,
            categoryPickerFromRefresh = false,
            categorySelectionIds = null,
            loading = false,
            categories = emptyList(),
            contentType = loadedContentType
        )
    }

    /** Συμβατότητα για παλιότερα call sites. */
    fun cancelCategoryPick() = cancelCategoryPicker()

    /**
     * Publishes immutable partial catalogs while provider work continues.
     * Provider dispatch and normalization live in CatalogLoadCoordinator;
     * this boundary only applies source/generation guards and updates UI state.
     */
    /**
     * Throttle για progressive partials: το UI ανανεωνόταν σε ΚΑΘΕ batch κατά το
     * loading, με O(όλα τα κανάλια) recompute κάθε φορά — «σερνόταν» σε μεγάλους
     * καταλόγους. Τώρα δημοσιεύουμε ένα intermediate partial το πολύ ανά
     * [partialThrottleMs]. Το ΤΕΛΙΚΟ πλήρες αποτέλεσμα δημοσιεύεται πάντα ξεχωριστά,
     * οπότε δεν χάνεται περιεχόμενο.
     */
    private var lastPartialPublishMs = 0L
    private val partialThrottleMs = 900L

    private fun shouldPublishPartial(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastPartialPublishMs < partialThrottleMs) return false
        lastPartialPublishMs = now
        return true
    }

    private fun partialPublisher(
        pl: Playlist,
        type: String,
        gen: Int,
        stage: String
    ): (CatalogLoadCoordinator.PartialCatalog) -> Unit = { partialCatalog ->
        val partial = partialCatalog.items
        if (partial.isNotEmpty() && sourceGeneration.isCurrentLoad(gen) && currentSourceId() == PlaylistIdentity.stableId(pl) && shouldPublishPartial()) {
            val favorites = store.loadFavorites(PlaylistIdentity.stableId(pl))
            val groups = buildGroups(partial, favorites.isNotEmpty())
            _state.update { current ->
                if (!sourceGeneration.isCurrentLoad(gen) || current.contentType != type) current
                else current.copy(
                    channels = partial,
                    groups = groups,
                    favorites = favorites,
                    selectedGroup = CatalogRefreshPolicy.restoredVisibleGroup(
                        current.selectedGroup, groups, UiState.ALL_GROUP
                    ),
                    loading = true,
                    status = "$stage · ${partial.size} στοιχεία διαθέσιμα"
                )
            }
        }
    }

    fun loadSelectedCategories(ids: List<String>?) =
        loadSelectedCategoriesInternal(ids, replaceActive = true)

    private fun loadSelectedCategoriesInternal(
        ids: List<String>?,
        replaceActive: Boolean,
        refreshSelectionOverride: Boolean = false
    ) {
        val pl = currentPlaylist() ?: return
        if (replaceActive && catalogLoadJob?.isActive == true) {
            sourceGeneration.invalidateLoad()
            catalogLoadJob?.cancel()
            catalogLoadJob = null
            stalker?.cancelPendingRequests()
            stalker = null
            stalkerSourceId = ""
            Http.cancelProviderRequests()
        }
        // Παγώνουμε source/type ΤΩΡΑ, ώστε αλλαγή λίστας στη μέση της λήψης
        // να μην ενημερώσει το state της επόμενης πηγής.
        val type = _state.value.contentType
        val k = "${plId(pl)}:$type"
        val refreshSelection = CatalogRefreshPolicy.usesTransactionalSelectionCommit(
            pickerFromRefresh = _state.value.categoryPickerFromRefresh,
            directRefreshFallback = refreshSelectionOverride
        )
        val selectedGroupBeforeRefresh = _state.value.selectedGroup
        val rollbackSnapshot = if (refreshSelection) catalogLoader.captureVisible(_state.value) else null
        val gen = sourceGeneration.currentLoad()
        catalogSession.invalidateSection(PlaylistIdentity.stableId(pl), type)
        val progress = progressCallback(pl, gen, type)
        updateSourceProgress(
            pl,
            0,
            if (refreshSelection) "Ανανέωση επιλεγμένων groups…" else "Φόρτωση $type…",
            active = true,
            contentType = type
        )
        // Στην κανονική πρώτη φόρτωση η επιλογή αποθηκεύεται αμέσως, όπως πριν.
        // Στο refresh picker όμως η νέα επιλογή γίνεται commit μόνο μετά από
        // επιτυχημένη λήψη, ώστε failure/cancellation να μην αλλάξει σιωπηλά
        // τα αποθηκευμένα groups πίσω από το παλιό ορατό catalog.
        if (!refreshSelection) {
            loadChoice[k] = ids
            store.saveLoadChoice(k, ids)
        }
        _state.value = _state.value.copy(
            askRefreshMode = false,
            askLoadMode = false,
            pickCategories = false,
            categoryPickerFromRefresh = false,
            categorySelectionIds = null,
            loading = true,
            status = if (refreshSelection) "Ανανέωση επιλεγμένων groups…" else "Φόρτωση…"
        )
        catalogLoadJob = viewModelScope.launch {
            try {
                val loaded = catalogLoader.section(
                    pl, type, ids, progress,
                    partialPublisher(pl, type, gen, "Λήψη σε εξέλιξη")
                )
                if (!sourceGeneration.isCurrentLoad(gen)) return@launch    // άλλαξε λίστα όσο κατέβαινε
                val rawChannels = loaded.rawItems
                val channels = loaded.items
                catalogSession.seriesEpisodes = if (type == "series") loaded.seriesEpisodes else emptyMap()
                loadedContentType = type
                store.saveLastSection(plId(pl), type)
                val sourceId = PlaylistIdentity.stableId(pl)
                val historyCandidates = if (type == "series") rawChannels else channels
                store.migrateLegacyHistory(sourceId, historyCandidates)
                store.reconcileHistory(sourceId, historyCandidates)
                store.reconcileFavorites(sourceId, historyCandidates)
                val favoriteKeys = store.loadFavorites(sourceId)
                val groups = buildGroups(channels, favoriteKeys.isNotEmpty())
                catalogSession.liveChannels = if (type == "live") channels else emptyList()
                cacheCatalog(pl, type, ids, channels, groups, loaded.seriesEpisodes)
                if (refreshSelection) {
                    loadChoice[k] = ids
                    store.saveLoadChoice(k, ids)
                }
                val st = if (refreshSelection) {
                    "Ενημερώθηκαν ${channels.size} στοιχεία."
                } else {
                    "Φορτώθηκαν ${channels.size} στοιχεία."
                }
                finishSourceProgress(pl, "Ολοκληρώθηκε · ${channels.size} στοιχεία", success = true, type = type)
                _state.value = _state.value.copy(
                    channels = channels,
                    groups = groups,
                    favorites = favoriteKeys,
                    selectedGroup = if (refreshSelection) {
                        CatalogRefreshPolicy.restoredVisibleGroup(
                            previousGroup = selectedGroupBeforeRefresh,
                            freshGroups = groups,
                            allGroup = UiState.ALL_GROUP
                        )
                    } else UiState.ALL_GROUP,
                    loading = false,
                    status = st,
                    askLoadMode = false,
                    askLoadType = null
                )
                if (type == "live") loadEpgIfAny(pl)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (!sourceGeneration.isCurrentLoad(gen)) return@launch
                val status = "Σφάλμα: ${e.message}"
                finishSourceProgress(pl, status, success = false, type = type)
                _state.value = rollbackSnapshot?.let {
                    catalogLoader.restoreAfterRefreshFailure(_state.value, it, status)
                } ?: _state.value.copy(loading = false, status = status)
            }
        }
    }

    /** Άμεση φόρτωση (M3U, Xtream VOD/Series). */
    fun startRelay(selected: List<Channel>): String = exportRelay.startRelay(selected)

    fun stopRelay() = exportRelay.stopRelay()

    /** Πληροφορίες ταινίας (MAC: από το κανάλι, Xtream: get_vod_info). */
    suspend fun vodInfo(ch: Channel): Map<String, String> = withContext(Dispatchers.IO) {
        val pl = currentPlaylist() ?: return@withContext emptyMap()
        if (pl.type == com.prelude.iptv.data.PlaylistType.XTREAM && ch.streamId.isNotEmpty()) {
            Repository.xtreamVodInfo(pl, ch.streamId)
        } else {
            mapOf(
                "plot" to ch.plot, "cast" to ch.cast, "director" to ch.director,
                "genre" to ch.genre, "year" to ch.year, "duration" to ch.duration
            )
        }
    }

    fun exportableChannels(): List<Channel> = exportRelay.exportableChannels()

    /** Κάνει resolve τα κανάλια και φτιάχνει M3U με πραγματικά URLs (για MAC → αρχείο). */
    suspend fun buildResolvedM3u(channels: List<Channel>): String =
        exportRelay.buildResolvedM3u(channels)

    fun setSearch(s: String) { _state.value = _state.value.copy(search = s) }
    fun setGroup(g: String) { _state.value = _state.value.copy(selectedGroup = g) }

    fun addPlaylist(pl: Playlist) {
        val list = _state.value.playlists.toMutableList().apply { add(pl) }
        store.savePlaylists(list)
        _state.value = _state.value.copy(playlists = list)
        resetSourceSession()
        selectPlaylist(list.size - 1)
    }

    /**
     * Αποθηκεύει το πλήθος στοιχείων μιας ενότητας (live/vod/series) πάνω στο
     * [Playlist], ώστε η κάρτα στις «Πηγές» να το δείχνει χωρίς να ξαναφορτώνει
     * τον κατάλογο. Ενημερώνεται ΜΟΝΟ όταν μια φόρτωση πετύχει ήδη — δεν κάνει
     * δικό της δίκτυο.
     */
    private fun persistCatalogCount(sourceId: String, type: String, count: Int) {
        if (type !in setOf("live", "vod", "series")) return
        val list = _state.value.playlists.toMutableList()
        val index = list.indexOfFirst { PlaylistIdentity.stableId(it) == sourceId }
        if (index < 0) return
        val current = list[index]
        val updated = when (type) {
            "live" -> current.copy(liveCount = count)
            "vod" -> current.copy(vodCount = count)
            else -> current.copy(seriesCount = count)
        }
        if (updated == current) return
        list[index] = updated
        store.savePlaylists(list)
        _state.value = _state.value.copy(playlists = list)
    }

    /** Ενημερώνει υπάρχουσα λίστα (επεξεργασία από τις 3 τελίτσες). */
    fun updatePlaylist(index: Int, pl: Playlist) {
        val before = _state.value
        val list = before.playlists.toMutableList()
        val previous = list.getOrNull(index) ?: return
        val previousSourceId = PlaylistIdentity.stableId(previous)
        val newSourceId = PlaylistIdentity.stableId(pl)
        val editingActiveSource = index == before.currentIndex

        list[index] = pl
        store.savePlaylists(list)

        if (editingActiveSource) {
            // Stop the old source before clearing or reusing any of its session
            // state. Generation guards then reject provider callbacks already unwinding.
            resetSourceSession()
        }

        // Editing a background card must never cancel or blank the source that
        // the user is currently browsing. Only the edited source namespace is
        // invalidated. If its identity changed, clean the old namespace only
        // when no duplicate playlist still references it.
        if (previousSourceId != newSourceId && SourceDeletionPolicy.isLastReference(
                previousSourceId,
                list.map(PlaylistIdentity::stableId),
            )
        ) {
            clearPersistedSourceState(previous, previousSourceId, deleteLocalFile = true)
        }
        _state.value = before.copy(playlists = list)

        if (editingActiveSource) {
            // selectPlaylist invalidates the edited source and performs the fresh reload.
            selectPlaylist(index)
        }
    }

    fun deleteCurrent() = deletePlaylist(_state.value.currentIndex)

    /**
     * Διαγραφή με index, ΧΩΡΙΣ selectPlaylist πρώτα. Το παλιό
     * «selectPlaylist(i); deleteCurrent()» των καρτών ξεκινούσε φόρτωση
     * της λίστας που πήγαινε για διαγραφή (άσκοπο δίκτυο + race με το
     * in-flight coroutine που έγραφε δεδομένα μετά τη διαγραφή).
     */
    fun deletePlaylist(index: Int) {
        val before = _state.value
        val list = before.playlists.toMutableList()
        val gone = list.getOrNull(index) ?: return
        list.removeAt(index)
        store.savePlaylists(list)

        val goneSourceId = PlaylistIdentity.stableId(gone)
        val remainingSourceIds = list.map(PlaylistIdentity::stableId)
        val deleteSourceData = SourceDeletionPolicy.isLastReference(goneSourceId, remainingSourceIds)
        val decision = SourceDeletionPolicy.decide(
            sizeAfter = list.size,
            removedIndex = index,
            activeIndex = before.currentIndex,
        )

        if (decision.removedActiveSource) {
            // Invalidate generations before deleting session data. Otherwise an
            // in-flight callback could republish the deleted source in the small
            // window between cleanup and cancellation.
            resetSourceSession()
        }

        // Duplicate entries may intentionally point to the same provider. Never
        // erase their shared preferences, favorites, history or local file while
        // another reference remains.
        if (deleteSourceData) {
            clearPersistedSourceState(gone, goneSourceId, deleteLocalFile = true)
        }
        TvHomeSyncScheduler.schedule(getApplication())

        val nextProgress = if (deleteSourceData) before.sourceProgress - goneSourceId
        else before.sourceProgress

        // Removing a different card must not cancel, blank or reload the source
        // the user is currently browsing. Only its numeric index may shift.
        if (!decision.removedActiveSource) {
            store.lastPlaylist = decision.newActiveIndex
            _state.value = before.copy(
                playlists = list,
                currentIndex = decision.newActiveIndex,
                sourceProgress = nextProgress,
            )
            return
        }

        _state.value = before.copy(
            playlists = list,
            currentIndex = decision.newActiveIndex,
            channels = emptyList(),
            favorites = emptySet(),
            sourceProgress = nextProgress,
            groups = emptyList(),
            selectedGroup = UiState.ALL_GROUP,
            search = "",
            loading = false,
            loadingAllSections = false,
            loadedSections = emptySet(),
            epgLoaded = false,
            epgSources = emptyList(),
            epgStatus = EpgStatus.Idle,
            chooseContent = false,
            askRefreshMode = false,
            askLoadMode = false,
            pickCategories = false,
            categoryPickerFromRefresh = false,
            categorySelectionIds = null,
            openSeriesTitle = null,
            seriesSeasons = emptyList(),
            seriesLoading = false,
        )

        if (decision.hasReplacementSource) {
            selectPlaylist(decision.newActiveIndex)
        } else {
            store.lastPlaylist = 0
        }
    }

    /**
     * Συγχρονισμός με αλλαγές που έγιναν μέσα στον player: αγαπημένα ΚΑΙ
     * θέσεις/recents (το recentsVersion αναγκάζει το «Συνέχισε να βλέπεις»
     * να ξαναδιαβάσει). Καλείται στο ON_RESUME από το MainActivity.
     */
    fun refreshFavorites() {
        expireParentalIfNeeded()          // έληξε το ξεκλείδωμα όσο έλειπες;
        val favs = store.loadFavorites(currentSourceId())
        _state.value = _state.value.copy(
            favorites = if (favs != _state.value.favorites) favs else _state.value.favorites,
            recentsVersion = _state.value.recentsVersion + 1
        )
    }

    // ---------------------------------------------------------------------
    // ΒΙΒΛΙΟΘΗΚΗ ΤΟΥ ΧΡΗΣΤΗ
    // ---------------------------------------------------------------------
    // Οι υλοποιήσεις ζουν στο LibraryCoordinator. Εδώ μένουν μόνο οι υπογραφές,
    // ώστε οι οθόνες να μη χρειαστεί να αλλάξουν: το ViewModel παραμένει το
    // δημόσιο όριο, όπως και με τους υπόλοιπους coordinators.

    /**
     * «Συνέχισε να βλέπεις»: ταινίες/επεισόδια με σωσμένη θέση αναπαραγωγής.
     * Το PlaylistStore κόβει ήδη <60s και >95% — ό,τι έρθει είναι όντως «στη
     * μέση». Επιστρέφει (κανάλι, πρόοδος 0..1).
     */
    fun continueWatching(): List<Pair<Channel, Float>> = library.continueWatching()

    fun searchLibrary(query: String): List<Channel> = library.searchLibrary(query)

    /** Stable catalog used by search: one result per movie/series/live entity. */
    fun searchUniverse(): List<Channel> = library.searchUniverse()

    /** Γρήγορη αναζήτηση πάνω σε ΗΔΗ υπολογισμένο [universe]. */
    fun searchInUniverse(universe: List<Channel>, query: String): List<Channel> =
        library.searchInUniverse(universe, query)

    fun favoriteLibraryItems(): List<Channel> = library.favoriteLibraryItems()

    fun historyItems(): List<Channel> = library.historyItems()

    fun removeHistoryItem(ch: Channel) = library.removeHistoryItem(ch)

    /** Progress for details/episode rails. Null means start from the beginning. */
    fun watchProgress(ch: Channel): WatchProgress? = library.watchProgress(ch)

    fun watchProgress(items: List<Channel>): Map<String, WatchProgress> =
        library.watchProgress(items)

    /** Removes both the resume marker and the recent entry for this item. */
    fun clearWatchProgress(ch: Channel) = library.clearWatchProgress(ch)

    fun toggleFavorite(ch: Channel) = library.toggleFavorite(ch)

    private fun loadEpgIfAny(pl: Playlist) = epgCoordinator.loadIfAny(pl)

    fun liveChannelsWithEpg(): List<Channel> = epgCoordinator.liveChannelsWithEpg()

    /**
     * Catch-up URL (Xtream timeshift). Returns null when the active source or
     * channel does not support the Xtream catch-up URL contract.
     */
    fun catchupUrl(ch: Channel, startMs: Long, stopMs: Long): String? {
        val pl = currentPlaylist() ?: return null
        if (pl.type != com.prelude.iptv.data.PlaylistType.XTREAM) return null
        return com.prelude.iptv.data.CatchupUrl.build(
            pl.server, pl.username, pl.password, ch.streamId, startMs, stopMs
        )
    }

    fun setReminder(ctx: android.content.Context, ch: Channel, title: String, startMs: Long) {
        com.prelude.iptv.data.ReminderScheduler.schedule(ctx, ch, title, startMs)
    }

    fun searchEpg() = epgCoordinator.search()

    fun closeEpgSearch() = epgCoordinator.closeSearch()

    fun useEpgSource(rawUrl: String) = epgCoordinator.useSource(rawUrl)

    fun nowText(tvgId: String): String? = epgCoordinator.nowText(tvgId)

    private fun buildGroups(channels: List<Channel>, hasFavs: Boolean): List<String> {
        val sourceId = currentSourceId()
        val preferred = store.loadCategoryLayout(sourceId, _state.value.contentType).orderedTitles
        return CatalogPresentationPolicy.groups(
            channels = channels,
            hasFavorites = hasFavs,
            preferredTitles = preferred,
            allGroupLabel = UiState.ALL_GROUP,
            favoritesGroupLabel = UiState.FAV_GROUP,
        )
    }

    /** Τα κανάλια που φαίνονται με βάση ομάδα + αναζήτηση. */
    /**
     * Η Αρχική, περασμένη από ΤΟ ΙΔΙΟ φίλτρο με κάθε άλλη λίστα.
     *
     * ΓΙΑΤΙ ΔΕΝ ΑΡΚΕΙ ΤΟ [homeCatalogState] ΣΚΕΤΟ: εκείνο είναι ωμή ένωση των
     * αποθηκευμένων ενοτήτων. Το [CatalogPresentationPolicy] είναι που αφαιρεί
     * τις **κλειδωμένες ομάδες** όταν ο γονικός έλεγχος δεν έχει ξεκλειδωθεί.
     * Αν η Αρχική διάβαζε την ένωση απευθείας, κλειδωμένο περιεχόμενο θα
     * εμφανιζόταν στις ράγες της — παράκαμψη γονικού ελέγχου μέσω μιας οθόνης
     * που απλώς ήθελε περισσότερα δεδομένα.
     *
     * Αναζήτηση και επιλεγμένη ομάδα ΔΕΝ εφαρμόζονται: η Αρχική εμφανίζεται μόνο
     * όταν και τα δύο είναι ουδέτερα, και οι ράγες της κάνουν τη δική τους
     * ομαδοποίηση.
     */
    fun visibleHomeChannels(): List<Channel> {
        val s = _state.value
        return CatalogPresentationPolicy.visibleChannels(
            channels = _homeCatalog.value,
            search = "",
            selectedGroup = UiState.ALL_GROUP,
            allGroupLabel = UiState.ALL_GROUP,
            favoritesGroupLabel = UiState.FAV_GROUP,
            favorites = s.favorites,
            lockedGroups = s.lockedGroups,
            parentalUnlocked = s.parentalUnlocked,
            sortMode = s.sortMode,
            favoriteKey = ::favKey,
        )
    }

    fun visibleChannels(): List<Channel> {
        val s = _state.value
        return CatalogPresentationPolicy.visibleChannels(
            channels = s.channels,
            search = s.search,
            selectedGroup = s.selectedGroup,
            allGroupLabel = UiState.ALL_GROUP,
            favoritesGroupLabel = UiState.FAV_GROUP,
            favorites = s.favorites,
            lockedGroups = s.lockedGroups,
            parentalUnlocked = s.parentalUnlocked,
            sortMode = s.sortMode,
            favoriteKey = ::favKey,
        )
    }

    /* ---- Γονικός έλεγχος & ταξινόμηση ---- */

    fun hasParentalPin() = profileSettings.hasParentalPin()
    fun checkPin(pin: String) = profileSettings.checkPin(pin)

    /* ==================== Προφίλ & γονικός έλεγχος ====================
     * Η λογική ζει πλέον στο ProfileSettingsCoordinator (σύγχρονη, χωρίς
     * source/catalog state). Οι παρακάτω public μέθοδοι μένουν σταθερές ως
     * λεπτοί delegators, ώστε κανένας caller να μη χρειαστεί migration.
     * ================================================================= */

    fun profiles(): List<PlaylistStore.Profile> = profileSettings.profiles()

    fun activeProfileId(): Int = profileSettings.activeProfileId()

    fun activeProfileName(): String = profileSettings.activeProfileName()

    fun activeProfileDisplayName(): ProfileDisplayName =
        profileSettings.activeProfileDisplayName()

    fun profileNeedsPin(p: com.prelude.iptv.data.PlaylistStore.Profile): Boolean =
        profileSettings.profileNeedsPin(p)

    fun addProfile(name: String, protectedProfile: Boolean) =
        profileSettings.addProfile(name, protectedProfile)

    fun deleteProfile(id: Int) = profileSettings.deleteProfile(id)

    fun setActiveProfile(id: Int) = profileSettings.setActiveProfile(id)

    fun setParentalPin(pin: String) = profileSettings.setParentalPin(pin)

    fun unlockParental(pin: String): Boolean = profileSettings.unlockParental(pin)

    private fun expireParentalIfNeeded() = profileSettings.expireParentalIfNeeded()

    fun toggleLockGroup(g: String) = profileSettings.toggleLockGroup(g)

    fun setSortMode(m: String) { _state.value = _state.value.copy(sortMode = m) }

    /**
     * Featured group rails (τα «μεγάλα εικονίδια» στην αρχική) ανά τρέχουσα πηγή
     * + ενότητα. Επιστρέφει τη ΩΜΗ αποθηκευμένη επιλογή (κενή = καμία ακόμη)· η
     * ανάλυση σε default/έγκυρα γίνεται από το FeaturedGroupsPolicy στην οθόνη.
     */
    fun featuredGroups(): List<String> =
        store.loadFeaturedGroups(currentSourceId(), _state.value.contentType)

    fun saveFeaturedGroups(groups: List<String>) {
        store.saveFeaturedGroups(
            currentSourceId(),
            _state.value.contentType,
            groups.take(FeaturedGroupsPolicy.MAX)
        )
    }

    // ---------------------------------------------------- σειρές ----------

    fun openSeries(ch: Channel) {
        val pl = currentPlaylist() ?: return
        if (seriesLoadJob?.isActive == true) {
            seriesLoadJob?.cancel()
            seriesLoader.cancel()
            stalker?.cancelPendingRequests()
            stalker = null
            stalkerSourceId = ""
            Http.cancelProviderRequests()
        }

        val request = sourceGeneration.beginSeriesRequest()
        val gen = request.loadGeneration
        _state.value = _state.value.copy(
            openSeriesTitle = ch.name,
            seriesLoading = true,
            seriesSeasons = emptyList(),
        )

        seriesLoadJob = viewModelScope.launch {
            val cached = catalogSession.seriesEpisodes[ch.seriesId]
            val (_, rememberedIds) = rememberedChoice("${plId(pl)}:series")
            val requiresFresh = seriesLoader.requiresFreshCatalog(pl, ch, cached)
            if (requiresFresh) {
                updateSourceProgress(
                    pl,
                    0,
                    "Λήψη φρέσκων επεισοδίων…",
                    active = true,
                    contentType = "series",
                )
            }

            try {
                val outcome = seriesLoader.load(
                    playlist = pl,
                    series = ch,
                    cached = cached,
                    rememberedCategoryIds = rememberedIds,
                    progress = progressCallback(pl, gen, "series"),
                )
                Log.d(
                    "SeriesLoad",
                    "loaded name=${ch.name} type=${pl.type} seriesId=${ch.seriesId} " +
                        "cachedBefore=${cached?.size ?: -1} requiresFresh=$requiresFresh " +
                        "seasons=${outcome.seasons.size} episodes=${outcome.seasons.sumOf { it.second.size }} " +
                        "synthetic=${outcome.synthetic} usedFreshCatalog=${outcome.freshCatalog != null}",
                )
                if (!sourceGeneration.isCurrent(request)) {
                    Log.w("SeriesLoad", "discarded result for ${ch.name}: series generation changed while loading")
                    outcome.stalkerClient?.cancelPendingRequests()
                    return@launch
                }

                val sourceId = PlaylistIdentity.stableId(pl)
                outcome.stalkerClient?.let { client ->
                    stalker = client
                    stalkerSourceId = sourceId
                }
                outcome.freshCatalog?.let { fresh ->
                    catalogSession.seriesEpisodes =
                        catalogSession.seriesEpisodes + fresh.seriesEpisodes
                }
                if (requiresFresh) {
                    finishSourceProgress(
                        pl,
                        "Τα επεισόδια είναι έτοιμα",
                        success = true,
                        type = "series",
                    )
                }

                val episodes = outcome.seasons.flatMap { it.second }
                if (episodes.isNotEmpty()) {
                    store.migrateLegacyHistory(sourceId, episodes)
                    store.reconcileHistory(sourceId, episodes)
                    store.reconcileFavorites(sourceId, episodes)
                    if (currentSourceId() == sourceId) {
                        _state.value = _state.value.copy(
                            favorites = store.loadFavorites(sourceId)
                        )
                    }
                }

                // A synthetic single-episode fallback is never persisted. A
                // later open must retry the provider instead of treating the
                // fallback as authoritative catalog data.
                if (!outcome.synthetic && ch.seriesId.isNotBlank() && outcome.seasons.isNotEmpty()) {
                    catalogSession.seriesEpisodes =
                        catalogSession.seriesEpisodes + (ch.seriesId to outcome.seasons)
                    cacheSeriesEpisodes(pl, ch.seriesId, outcome.seasons)
                }
                _state.value = _state.value.copy(
                    seriesSeasons = outcome.seasons,
                    seriesLoading = false,
                    status = if (outcome.seasons.isEmpty()) {
                        "Δεν βρέθηκαν διαθέσιμα επεισόδια για τη σειρά."
                    } else {
                        _state.value.status
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(
                    "SeriesLoad",
                    "failed name=${ch.name} type=${pl.type} seriesId=${ch.seriesId}",
                    error,
                )
                if (!sourceGeneration.isCurrent(request)) return@launch
                if (requiresFresh) {
                    finishSourceProgress(
                        pl,
                        "Σφάλμα επεισοδίων: ${error.message}",
                        success = false,
                        type = "series",
                    )
                }
                _state.value = _state.value.copy(
                    seriesLoading = false,
                    seriesSeasons = emptyList(),
                    status = "Σφάλμα σειράς: ${error.message}",
                )
            }
        }
    }

    fun closeSeries() {
        if (seriesLoadJob?.isActive == true) {
            sourceGeneration.invalidateSeries()
            seriesLoadJob?.cancel()
            seriesLoader.cancel()
            seriesLoadJob = null
            stalker?.cancelPendingRequests()
            Http.cancelProviderRequests()
        }
        _state.value = _state.value.copy(
            openSeriesTitle = null,
            seriesSeasons = emptyList(),
            seriesLoading = false,
        )
    }

    // ---------------------------------------------------- EPG -------------

    suspend fun fetchEpg(ch: Channel): List<com.prelude.iptv.data.EpgEntry> {
        val pl = currentPlaylist() ?: return emptyList()
        val sourceId = PlaylistIdentity.stableId(pl)
        val stalkerClient = currentStalker()
        return withContext(Dispatchers.IO) {
            try {
                val result = when {
                    pl.type == com.prelude.iptv.data.PlaylistType.XTREAM && ch.streamId.isNotEmpty() ->
                        com.prelude.iptv.source.XtreamClient.shortEpg(
                            pl.server, pl.username, pl.password, ch.streamId
                        )
                    pl.type == com.prelude.iptv.data.PlaylistType.STALKER &&
                        ch.chId.isNotEmpty() && stalkerClient != null ->
                        stalkerClient.shortEpg(ch.chId)
                    else -> emptyList()
                }
                // The caller may still be alive after a source switch. Never return
                // provider EPG for a playlist that is no longer active.
                if (currentSourceId() == sourceId) result else emptyList()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override fun onCleared() {
        catalogLoadJob?.cancel()
        allSectionsLoadJob?.cancel()
        epgCoordinator.cancelSearch()
        seriesLoadJob?.cancel()
        seriesLoader.cancel()
        stalker?.cancelPendingRequests()
        Http.cancelProviderRequests()
        super.onCleared()
    }

}
