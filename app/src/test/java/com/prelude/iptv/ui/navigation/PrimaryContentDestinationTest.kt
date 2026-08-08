package com.prelude.iptv.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrimaryContentDestinationTest {
    @Test
    fun primary_navigation_has_the_approved_order() {
        assertEquals(
            listOf("home", "live", "movies", "series", "search"),
            PrimaryContentDestination.ordered.map { it.route },
        )
    }

    @Test
    fun routes_resolve_without_absorbing_secondary_destinations() {
        assertEquals(
            PrimaryContentDestination.MOVIES,
            PrimaryContentDestination.fromRoute("movies"),
        )
        assertNull(PrimaryContentDestination.fromRoute("library"))
        assertNull(PrimaryContentDestination.fromRoute("settings"))
        assertEquals("home", PrimaryContentDestination.selectionRoute("library"))
    }

    @Test
    fun tv_selection_keeps_back_focus_on_the_owning_primary_destination() {
        fun resolve(
            type: String = "vod",
            home: Boolean = false,
            search: Boolean = false,
            library: Boolean = false,
        ) = PrimaryContentDestination.resolveTvSelection(type, home, search, library)

        assertEquals(PrimaryContentDestination.HOME, resolve(home = true))
        assertEquals(PrimaryContentDestination.LIVE, resolve(type = "live"))
        assertEquals(PrimaryContentDestination.MOVIES, resolve(type = "vod"))
        assertEquals(PrimaryContentDestination.SERIES, resolve(type = "series"))
        assertEquals(PrimaryContentDestination.SEARCH, resolve(search = true, library = true))
        assertEquals(PrimaryContentDestination.HOME, resolve(library = true))
        assertEquals(PrimaryContentDestination.HOME, resolve(type = "unknown"))
    }
}
