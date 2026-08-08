package com.prelude.iptv.ui

import com.prelude.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchUiPolicyTest {
    @Test fun groupsByKind() {
        val items = listOf(
            Channel(name = "Live", kind = "live"),
            Channel(name = "Movie", kind = "vod"),
            Channel(name = "Series", kind = "series")
        )
        val groups = SearchUiPolicy.group(items)
        assertEquals(1, groups.live.size)
        assertEquals(1, groups.movies.size)
        assertEquals(1, groups.series.size)
        assertEquals(3, groups.total)
    }

    @Test fun suggestionsAreDistinctAndOrdered() {
        val items = listOf(
            Channel(name = "A", genre = "Drama"),
            Channel(name = "B", genre = "Drama"),
            Channel(name = "C", group = "Sports")
        )
        assertEquals(listOf("Drama", "Sports"), SearchUiPolicy.suggestions(items))
    }
    @Test fun premiumFiltersKeepSearchEngineResultsPure() {
        val items = listOf(
            Channel(name = "Movie", kind = "vod", genre = "Drama"),
            Channel(name = "Series", kind = "series", genre = "Crime"),
            Channel(name = "Formula Racing", kind = "live", group = "Sports"),
            Channel(name = "Wild Earth", kind = "vod", genre = "Documentary")
        )
        assertEquals(listOf("Movie"), SearchUiPolicy.filter(items, PremiumSearchFilter.MOVIES).map { it.name })
        assertEquals(listOf("Series"), SearchUiPolicy.filter(items, PremiumSearchFilter.SERIES).map { it.name })
        assertEquals(listOf("Formula Racing"), SearchUiPolicy.filter(items, PremiumSearchFilter.SPORTS).map { it.name })
        assertEquals(listOf("Wild Earth"), SearchUiPolicy.filter(items, PremiumSearchFilter.DOCUMENTARIES).map { it.name })
    }

    @Test fun discoveryPrefersVisualVodAndSeries() {
        val items = listOf(
            Channel(name = "Live", kind = "live", logo = "live"),
            Channel(name = "Movie", kind = "vod", logo = "movie"),
            Channel(name = "Series", kind = "series", logo = "series"),
            Channel(name = "No art", kind = "vod")
        )
        assertEquals(listOf("Movie", "Series", "Live"), SearchUiPolicy.discovery(items).map { it.name })
    }

    @Test fun headingsAndCategoriesRemainTypedOutsideAndroidResources() {
        assertEquals(
            SearchHeading.Query("matrix"),
            SearchUiPolicy.heading("  matrix  ", PremiumSearchFilter.ALL)
        )
        assertEquals(
            SearchHeading.Filter(PremiumSearchFilter.SERIES),
            SearchUiPolicy.heading("", PremiumSearchFilter.SERIES)
        )
        assertEquals(SearchHeading.Popular, SearchUiPolicy.heading("", PremiumSearchFilter.ALL))
        assertEquals(
            SearchCategory.Movie,
            SearchUiPolicy.category(Channel(name = "Movie", kind = "vod"))
        )
        assertEquals(
            SearchCategory.Provider("Concerts"),
            SearchUiPolicy.category(Channel(name = "Other", kind = "other", group = "Concerts"))
        )
    }

    @Test fun missingDescriptionStaysTypedAsMissing() {
        assertEquals(null, SearchUiPolicy.description(Channel(name = "Movie"), null))
    }

}
