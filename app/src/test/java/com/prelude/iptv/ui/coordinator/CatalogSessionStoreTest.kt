package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.LoadResult
import com.prelude.iptv.data.SessionCatalogSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSessionStoreTest {
    @Test
    fun sourceInvalidationClearsCatalogAndM3uTogether() {
        val store = CatalogSessionStore(maxCatalogEntries = 4, maxM3uEntries = 2)
        val key = store.catalogKey("source-a", "live", listOf("sports"))
        store.putCatalog(key, SessionCatalogSnapshot(listOf(Channel("A")), listOf("sports")))
        store.rememberM3u("source-a", LoadResult(listOf(Channel("A")), epgUrl = "https://epg/a.xml"))

        store.invalidateSource("source-a")

        assertNull(store.getCatalog(key))
        assertNull(store.getM3u("source-a"))
    }

    @Test
    fun sectionInvalidationDoesNotDropOtherSections() {
        val store = CatalogSessionStore(maxCatalogEntries = 4)
        val live = store.catalogKey("source-a", "live", null)
        val vod = store.catalogKey("source-a", "vod", null)
        store.putCatalog(live, SessionCatalogSnapshot(listOf(Channel("Live")), listOf("All")))
        store.putCatalog(vod, SessionCatalogSnapshot(listOf(Channel("Movie", kind = "vod")), listOf("All")))

        store.invalidateSection("source-a", "live")

        assertNull(store.getCatalog(live))
        assertEquals("Movie", store.getCatalog(vod)?.channels?.single()?.name)
    }

    @Test
    fun m3uPayloadCacheIsBoundedAndUsesLruOrder() {
        val store = CatalogSessionStore(maxM3uEntries = 2)
        store.rememberM3u("a", LoadResult(listOf(Channel("A"))))
        store.rememberM3u("b", LoadResult(listOf(Channel("B"))))
        assertEquals("A", store.getM3u("a")?.channels?.single()?.name)
        store.rememberM3u("c", LoadResult(listOf(Channel("C"))))

        assertNull(store.getM3u("b"))
        assertEquals("A", store.getM3u("a")?.channels?.single()?.name)
        assertEquals("C", store.getM3u("c")?.channels?.single()?.name)
    }

    @Test
    fun clearWorkingSetDoesNotEraseCachedCatalogs() {
        val store = CatalogSessionStore()
        val key = store.catalogKey("source-a", "series", null)
        store.putCatalog(key, SessionCatalogSnapshot(listOf(Channel("Series", kind = "series")), listOf("All")))
        store.liveChannels = listOf(Channel("Live"))
        store.seriesEpisodes = mapOf("s" to listOf("Season 1" to listOf(Channel("Episode", kind = "series_ep"))))

        store.clearWorkingSet()

        assertTrue(store.liveChannels.isEmpty())
        assertTrue(store.seriesEpisodes.isEmpty())
        assertEquals("Series", store.getCatalog(key)?.channels?.single()?.name)
    }
}
