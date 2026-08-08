package com.prelude.iptv.ui.coordinator

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

/** Owns category-editor loading, draft state and persistence normalization. */
class CategoryEditorCoordinator(
    private val scope: CoroutineScope,
    private val currentPlaylist: () -> Playlist?,
    private val currentSourceId: () -> String,
    private val currentContentType: () -> String,
    private val loadLayout: (sourceId: String, type: String) -> CategoryLayout,
    private val loadCategories: suspend (playlist: Playlist, type: String) -> List<Pair<String, String>>,
    private val saveLayout: (sourceId: String, type: String, layout: CategoryLayout) -> Unit,
    private val saveChoice: (key: String, ids: List<String>?) -> Unit,
    private val reloadSelection: (ids: List<String>?) -> Unit,
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
                CategoryEditorSection(layout = loadLayout(sourceId, type), loading = true)
            },
        )
        scope.launch {
            contentTypes.forEach { type ->
                runCatching { loadCategories(playlist, type) }
                    .onSuccess { categories ->
                        if (_state.value.sourceId != sourceId) return@onSuccess
                        updateSection(type) { current ->
                            current.copy(
                                available = categories.map { CategoryOption(it.first, it.second) },
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
                                error = error.message ?: "Αποτυχία φόρτωσης",
                            )
                        }
                    }
            }
        }
    }

    fun updateLayout(type: String, layout: CategoryLayout) {
        updateSection(type) { it.copy(layout = layout) }
    }

    fun save() {
        val playlist = currentPlaylist() ?: return
        val sourceId = PlaylistIdentity.stableId(playlist)
        if (_state.value.sourceId != sourceId) return

        _state.value.sections.forEach { (type, section) ->
            if (section.loading || section.error != null) return@forEach
            val entries = CategoryLayoutPolicy.resolve(section.available, section.layout)
            saveLayout(
                sourceId,
                type,
                section.layout.copy(
                    order = entries.map { it.option.id },
                    orderedTitles = entries.map { it.option.title },
                ),
            )
            saveChoice("$sourceId:$type", CategoryLayoutPolicy.selectedIds(entries))
        }

        _revision.value += 1
        val active = _state.value.section(currentContentType())
        if (active.available.isNotEmpty()) {
            reloadSelection(CategoryLayoutPolicy.selectedIds(active.entries))
        }
    }

    private fun updateSection(
        type: String,
        transform: (CategoryEditorSection) -> CategoryEditorSection,
    ) {
        _state.update { current ->
            current.copy(sections = current.sections + (type to transform(current.section(type))))
        }
    }
}
