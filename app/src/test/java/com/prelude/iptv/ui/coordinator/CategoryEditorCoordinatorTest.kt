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
            reloadSelection = { _, ids -> reloadedIds = ids },
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
        assertEquals(2, coordinator.revision.value)
    }

    @Test fun `every change is persisted immediately without a save step`() = runTest {
        val playlist = playlist()
        val sourceId = PlaylistIdentity.stableId(playlist)
        val savedChoices = mutableMapOf<String, List<String>?>()
        var reloaded: Pair<String, List<String>?>? = null
        val coordinator = coordinator(
            playlist = { playlist },
            scope = this,
            loadCategories = { _, type ->
                if (type == "live") listOf("a" to "GR | A", "b" to "GR | B") else emptyList()
            },
            saveChoice = { key, ids -> savedChoices[key] = ids },
            reloadSelection = { type, ids -> reloaded = type to ids },
        )
        coordinator.show()
        advanceUntilIdle()

        coordinator.updateLayout("live", CategoryLayout(hidden = setOf("b")))

        assertEquals(true, coordinator.state.value.visible)
        assertEquals(listOf("a"), savedChoices["$sourceId:live"])
        assertEquals("live" to listOf("a"), reloaded)
        assertEquals(1, coordinator.revision.value)

        coordinator.updateLayout("live", CategoryLayout())

        assertNull(savedChoices["$sourceId:live"])
    }

    @Test fun `open mirrors the visibility already chosen elsewhere and revives deleted groups`() = runTest {
        val playlist = playlist()
        val sourceId = PlaylistIdentity.stableId(playlist)
        val coordinator = coordinator(
            playlist = { playlist },
            scope = this,
            loadLayout = { _, type -> if (type == "live") CategoryLayout(deleted = setOf("c")) else CategoryLayout() },
            loadCategories = { _, type ->
                if (type == "live") listOf("a" to "A", "b" to "B", "c" to "C") else emptyList()
            },
            loadChoice = { key -> if (key == "$sourceId:live") true to listOf("a") else false to null },
        )

        coordinator.open()
        advanceUntilIdle()

        val live = coordinator.state.value.section("live")
        assertEquals(setOf("b", "c"), live.layout.hidden)
        assertEquals(emptySet<String>(), live.layout.deleted)
        assertEquals(listOf("a"), live.entries.filter { it.visible }.map { it.option.id })
    }

    private fun coordinator(
        playlist: () -> Playlist,
        scope: kotlinx.coroutines.CoroutineScope,
        contentType: () -> String = { "live" },
        loadLayout: (String, String) -> CategoryLayout = { _, _ -> CategoryLayout() },
        loadCategories: suspend (Playlist, String) -> List<Pair<String, String>> = { _, _ -> emptyList() },
        saveLayout: (String, String, CategoryLayout) -> Unit = { _, _, _ -> },
        saveChoice: (String, List<String>?) -> Unit = { _, _ -> },
        reloadSelection: (String, List<String>?) -> Unit = { _, _ -> },
        loadChoice: (String) -> Pair<Boolean, List<String>?> = { false to null },
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
        loadChoice = loadChoice,
    )

    private fun playlist() = Playlist(
        name = "Test",
        type = PlaylistType.XTREAM,
        server = "https://provider.example",
        username = "user",
        password = "secret",
    )
}
