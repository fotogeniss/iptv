package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCatalogCacheTest {
    private fun snapshot(name: String) = SessionCatalogSnapshot(
        channels = listOf(Channel(name = name)),
        groups = listOf("Όλα"),
        categories = listOf("provider-id" to "Provider category"),
    )

    @Test
    fun categorySignature_isStableAcrossOrderAndDuplicates() {
        assertEquals(
            SessionCatalogCache.categorySignature(listOf("2", "1", "2")),
            SessionCatalogCache.categorySignature(listOf("1", "2"))
        )
        assertEquals(SessionCatalogCache.ALL_CATEGORIES, SessionCatalogCache.categorySignature(null))
        assertEquals(SessionCatalogCache.NO_CATEGORIES, SessionCatalogCache.categorySignature(emptyList()))
    }

    @Test
    fun catalogsAreIsolatedBySourceTypeAndSelection() {
        val cache = SessionCatalogCache()
        val live = SessionCatalogCache.key("source-a", "live", null)
        val movies = SessionCatalogCache.key("source-a", "vod", null)
        val otherSource = SessionCatalogCache.key("source-b", "live", null)
        val selected = SessionCatalogCache.key("source-a", "live", listOf("sports"))

        val liveSnapshot = snapshot("Live A")
        cache.put(live, liveSnapshot)
        cache.put(movies, snapshot("Movie A"))
        cache.put(otherSource, snapshot("Live B"))
        cache.put(selected, snapshot("Sports A"))

        assertEquals("Live A", cache.get(live)?.channels?.single()?.name)
        assertEquals(listOf("provider-id" to "Provider category"), cache.get(live)?.categories)
        assertEquals("Movie A", cache.get(movies)?.channels?.single()?.name)
        assertEquals("Live B", cache.get(otherSource)?.channels?.single()?.name)
        assertEquals("Sports A", cache.get(selected)?.channels?.single()?.name)
    }

    @Test
    fun invalidateSourceDoesNotTouchOtherSources() {
        val cache = SessionCatalogCache()
        val a = SessionCatalogCache.key("a", "live", null)
        val b = SessionCatalogCache.key("b", "live", null)
        cache.put(a, snapshot("A"))
        cache.put(b, snapshot("B"))

        cache.invalidateSource("a")

        assertNull(cache.get(a))
        assertEquals("B", cache.get(b)?.channels?.single()?.name)
    }

    @Test
    fun cacheEvictsLeastRecentlyUsedEntry() {
        val cache = SessionCatalogCache(maxEntries = 2)
        val a = SessionCatalogCache.key("a", "live", null)
        val b = SessionCatalogCache.key("b", "live", null)
        val c = SessionCatalogCache.key("c", "live", null)
        cache.put(a, snapshot("A"))
        cache.put(b, snapshot("B"))
        cache.get(a) // A becomes most recently used.
        cache.put(c, snapshot("C"))

        assertEquals("A", cache.get(a)?.channels?.single()?.name)
        assertNull(cache.get(b))
        assertEquals("C", cache.get(c)?.channels?.single()?.name)
        assertEquals(2, cache.size())
    }
}
