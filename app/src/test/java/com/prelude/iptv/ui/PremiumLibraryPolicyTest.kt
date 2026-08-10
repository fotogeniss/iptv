package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import com.prelude.iptv.ui.components.library.LibraryHubTab
import com.prelude.iptv.ui.components.library.LibraryRailLabels
import com.prelude.iptv.ui.components.library.LibrarySort
import com.prelude.iptv.ui.components.library.PremiumLibraryContent
import com.prelude.iptv.ui.components.library.initialLibraryTab
import com.prelude.iptv.ui.components.library.libraryKey
import com.prelude.iptv.ui.components.library.libraryProgress
import com.prelude.iptv.ui.components.library.libraryRails
import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumLibraryPolicyTest {
    private val movie = Channel(name = "Zulu", url = "z", kind = "vod")
    private val series = Channel(name = "Alpha", url = "a", kind = "series")
    private val history = Channel(name = "Beta", url = "b", kind = "vod")

    private val content = PremiumLibraryContent(
        myList = listOf(movie, series),
        continueWatching = listOf(series),
        history = listOf(history),
        progress = mapOf(libraryKey(series) to 0.42f)
    )

    // Plain stand-in labels: this test only asserts rail identity/order and
    // sort behavior, never label text, so the exact strings do not matter.
    private val labels = LibraryRailLabels(
        continueTitle = "Continue",
        continueSubtitleDescription = "Continue subtitle",
        continueSubtitleCount = "%d continuing",
        myListTitle = "My List",
        myListSubtitleDescription = "My list subtitle",
        myListSubtitleCount = "%d in my list",
        historyTitle = "History",
        historySubtitleDescription = "History subtitle",
        historySubtitleCount = "%d in history",
    )

    @Test
    fun all_tab_builds_expected_rails() {
        assertEquals(
            listOf("continue", "my-list", "history"),
            libraryRails(content, LibraryHubTab.ALL, LibrarySort.RECENT, labels).map { it.id }
        )
    }

    @Test
    fun title_sort_is_applied_inside_selected_tab() {
        assertEquals(
            listOf("Alpha", "Zulu"),
            libraryRails(content, LibraryHubTab.MY_LIST, LibrarySort.TITLE, labels)
                .single().items.map { it.name }
        )
    }

    @Test
    fun destination_and_progress_map_to_hub_state() {
        assertEquals(LibraryHubTab.HISTORY, initialLibraryTab(LibraryDestination.HISTORY))
        assertEquals(0.42f, libraryProgress(series, content))
    }
}
