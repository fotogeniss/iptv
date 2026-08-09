package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.category.CategoryEditorFailure
import com.prelude.iptv.category.CategoryLayout
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.data.PlaylistType
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryEditorCoordinatorTest {
    @Test fun `load failure is exposed as typed presentation state`() = runTest {
        val playlist = playlist()
        val coordinator = coordinator(
            playlist = { playlist },
            scope = this,
            loadCategories = { _, _ -> error("provider detail must not become display copy") },
        )

        coordinator.open()
        advanceUntilIdle()

        coordinator.state.value.sections.values.forEach { section ->
            assertEquals(CategoryEditorFailure.LoadFailed, section.error)
        }
    }

    @Test fun `open loads all category sections while preserving stored layouts`() = runTest {
        val playlist = playlist()
        val sourceId = PlaylistIdentity.stableId(playlist)
        val liveLayout = CategoryLayout(order = listOf("sports", "news"))
        val coordinator = coordinator(
            playlist = { playlist },
            scope = this,
            loadLayout = { id, type ->
                if (id == sourceId && type == "live") liveLayout else CategoryLayout()
            },
            loadCategories = { _, type -> listOf("${type}_1" to "$type One") },
        )

        coordinator.open()
        advanceUntilIdle()

        assertEquals(sourceId, coordinator.state.value.sourceId)
        assertEquals(setOf("live", "vod", "series"), coordinator.state.value.sections.keys)
        assertEquals(liveLayout, coordinator.state.value.section("live").layout)
        coordinator.state.value.sections.values.forEach { section ->
            assertFalse(section.loading)
            assertNull(section.error)
            assertEquals(1, section.available.size)
        }
    }

    @Test fun `save normalizes order persists selection and reloads active type`() = runTest {
        val playlist = playlist()
        val sourceId = PlaylistIdentity.stableId(playlist)
        val savedLayouts = mutableMapOf<Pair<String, String>, CategoryLayout>()
        val savedChoices = mutableMapOf<String, List<String>?>()
        var reloadedIds: List<String>? = null
        val coordinator = coordinator(
            playlist = { playlist },
            scope = this,
            contentType = { "vod" },
            loadCategories = { _, type ->
                if (type == "vod") listOf("v1" to "First", "v2" to "Second") else emptyList()
            },
            saveLayout = { id, type, layout -> savedLayouts[id to type] = layout },
            saveChoice = { key, ids -> savedChoices[key] = ids },
            reloadSelection = { reloadedIds = it },
        )
        coordinator.open()
        advanceUntilIdle()
        coordinator.updateLayout(
            "vod",
            CategoryLayout(order = listOf("v2", "v1"), hidden = setOf("v1")),
        )

        coordinator.save()

        val saved = savedLayouts.getValue(sourceId to "vod")
        assertEquals(listOf("v2", "v1"), saved.order)
        assertEquals(listOf("Second", "First"), saved.orderedTitles)
        assertEquals(listOf("v2"), savedChoices["$sourceId:vod"])
        assertEquals(listOf("v2"), reloadedIds)
        assertEquals(1, coordinator.revision.value)
    }

    private fun coordinator(
        playlist: () -> Playlist,
        scope: kotlinx.coroutines.CoroutineScope,
        contentType: () -> String = { "live" },
        loadLayout: (String, String) -> CategoryLayout = { _, _ -> CategoryLayout() },
        loadCategories: suspend (Playlist, String) -> List<Pair<String, String>> = { _, _ -> emptyList() },
        saveLayout: (String, String, CategoryLayout) -> Unit = { _, _, _ -> },
        saveChoice: (String, List<String>?) -> Unit = { _, _ -> },
        reloadSelection: (List<String>?) -> Unit = {},
    ): CategoryEditorCoordinator = CategoryEditorCoordinator(
        scope = scope,
        currentPlaylist = playlist,
        currentSourceId = { PlaylistIdentity.stableId(playlist()) },
        currentContentType = contentType,
        loadLayout = loadLayout,
        loadCategories = loadCategories,
        saveLayout = saveLayout,
        saveChoice = saveChoice,
        reloadSelection = reloadSelection,
    )

    private fun playlist() = Playlist(
        name = "Test",
        type = PlaylistType.XTREAM,
        server = "https://provider.example",
        username = "user",
        password = "secret",
    )
}
