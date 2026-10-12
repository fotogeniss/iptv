package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.category.CategoryEditorFailure
import com.prelude.iptv.category.CategoryEditorSection
import com.prelude.iptv.category.CategoryEditorState
import com.prelude.iptv.category.CategoryLayout
import com.prelude.iptv.category.CategoryLayoutPolicy
import com.prelude.iptv.category.CategoryOption
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns category-editor loading, state and persistence normalization.
 *
 * Από την 2.2.0 κάθε αλλαγή αποθηκεύεται ΑΜΕΣΩΣ ([updateLayout]): το κουμπί
 * «Αποθήκευση» έφυγε, γιατί όποιος το ξεχνούσε έχανε όλη τη δουλειά. Η αναίρεση
 * γίνεται από το UI, ξαναστέλνοντας το προηγούμενο layout.
 */
class CategoryEditorCoordinator(
    private val scope: CoroutineScope,
    private val currentPlaylist: () -> Playlist?,
    private val currentSourceId: () -> String,
    private val currentContentType: () -> String,
    private val loadLayout: (sourceId: String, type: String) -> CategoryLayout,
    private val loadCategories: suspend (playlist: Playlist, type: String) -> List<Pair<String, String>>,
    private val saveLayout: (sourceId: String, type: String, layout: CategoryLayout) -> Unit,
    private val saveChoice: (key: String, ids: List<String>?) -> Unit,
    /** Εφαρμόζει την ορατότητα στον κατάλογο που ήδη φαίνεται (ίδια διαδρομή με το παλιό φίλτρο). */
    private val reloadSelection: (type: String, ids: List<String>?) -> Unit,
    /** Η επιλογή που ισχύει ήδη: (υπάρχει, ids) — `ids == null` σημαίνει «όλες». */
    private val loadChoice: (key: String) -> Pair<Boolean, List<String>?> = { false to null },
    private val loadCounts: (playlist: Playlist, type: String) -> Map<String, Int> = { _, _ -> emptyMap() },
) {
    private val contentTypes = listOf("live", "vod", "series")
    private val _state = MutableStateFlow(CategoryEditorState())
    val state: StateFlow<CategoryEditorState> = _state.asStateFlow()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun titlesInOrder(type: String): List<String> =
        currentSourceId().takeIf { it.isNotBlank() }
            ?.let { loadLayout(it, type).orderedTitles }
            .orEmpty()

    fun open() {
        val playlist = currentPlaylist() ?: return
        val sourceId = PlaylistIdentity.stableId(playlist)
        _state.value = CategoryEditorState(
            sourceId = sourceId,
            sections = contentTypes.associateWith { type ->
                CategoryEditorSection(
                    layout = CategoryLayoutPolicy.normalized(loadLayout(sourceId, type)),
                    loading = true,
                )
            },
            visible = _state.value.visible,
        )
        scope.launch {
            contentTypes.forEach { type ->
                runCatching { loadCategories(playlist, type) }
                    .onSuccess { categories ->
                        if (_state.value.sourceId != sourceId) return@onSuccess
                        val options = categories.map { CategoryOption(it.first, it.second) }
                        val (hasChoice, ids) = loadChoice("$sourceId:$type")
                        val counts = runCatching { loadCounts(playlist, type) }.getOrDefault(emptyMap())
                        updateSection(type) { current ->
                            current.copy(
                                available = options,
                                layout = if (hasChoice) CategoryLayoutPolicy.withChoice(current.layout, options, ids)
                                else current.layout,
                                counts = counts,
                                loading = false,
                                error = null,
                            )
                        }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        if (_state.value.sourceId != sourceId) return@onFailure
                        updateSection(type) { current ->
                            current.copy(
                                loading = false,
                                error = CategoryEditorFailure.LoadFailed,
                            )
                        }
                    }
            }
        }
    }

    /** Φορτώνει και ΔΕΙΧΝΕΙ τον επεξεργαστή. */
    fun show() {
        _state.update { it.copy(visible = true) }
        open()
    }

    fun close() {
        _state.update { it.copy(visible = false) }
    }

    /** Εφαρμόζει και αποθηκεύει αμέσως μία αλλαγή του χρήστη. */
    fun updateLayout(type: String, layout: CategoryLayout) {
        updateSection(type) { it.copy(layout = CategoryLayoutPolicy.normalized(layout)) }
        val section = _state.value.section(type)
        if (section.loading || section.error != null) return
        if (!persist(type, section)) return
        _revision.value += 1
        reloadSelection(type, choiceOf(section.entries))
    }

    fun save() {
        val playlist = currentPlaylist() ?: return
        val sourceId = PlaylistIdentity.stableId(playlist)
        if (_state.value.sourceId != sourceId) return

        _state.value.sections.forEach { (type, section) ->
            if (section.loading || section.error != null) return@forEach
            persist(type, section)
        }

        _revision.value += 1
        val type = currentContentType()
        val active = _state.value.section(type)
        if (active.available.isNotEmpty()) {
            reloadSelection(type, choiceOf(active.entries))
        }
    }

    private fun persist(type: String, section: CategoryEditorSection): Boolean {
        val playlist = currentPlaylist() ?: return false
        val sourceId = PlaylistIdentity.stableId(playlist)
        if (_state.value.sourceId != sourceId) return false
        val entries = CategoryLayoutPolicy.resolve(section.available, section.layout)
        saveLayout(
            sourceId,
            type,
            section.layout.copy(
                order = entries.map { it.option.id },
                orderedTitles = entries.map { it.option.title },
            ),
        )
        saveChoice("$sourceId:$type", choiceOf(entries))
        return true
    }

    // null = «όλες»: έτσι μια κατηγορία που προσθέτει αργότερα ο πάροχος φαίνεται.
    private fun choiceOf(entries: List<com.prelude.iptv.category.CategoryEntry>): List<String>? =
        CategoryLayoutPolicy.selectedIds(entries).takeUnless { it.size == entries.size }

    private fun updateSection(
        type: String,
        transform: (CategoryEditorSection) -> CategoryEditorSection,
    ) {
        _state.update { current ->
            current.copy(sections = current.sections + (type to transform(current.section(type))))
        }
    }
}
