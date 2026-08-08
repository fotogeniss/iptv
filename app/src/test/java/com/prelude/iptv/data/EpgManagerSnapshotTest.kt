package com.prelude.iptv.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpgManagerSnapshotTest {
    @After fun cleanup() = EpgManager.clear()

    @Test fun `prepared replacement does not mutate visible guide`() {
        val active = snapshot("old", "Old guide")
        val replacement = snapshot("new", "New guide")

        EpgManager.installSnapshot(active)

        assertEquals("old", EpgManager.currentSource())
        assertEquals("Old guide", EpgManager.nowNext("old", 150L).first?.title)
        assertNull(EpgManager.nowNext("new", 150L).first)
        // Holding a parsed replacement is intentionally side-effect free.
        assertEquals("new", replacement.source)
        assertEquals("old", EpgManager.currentSource())
    }

    @Test fun `install publishes complete replacement atomically`() {
        EpgManager.installSnapshot(snapshot("old", "Old guide"))
        EpgManager.installSnapshot(snapshot("new", "New guide"))

        assertEquals("new", EpgManager.currentSource())
        assertNull(EpgManager.nowNext("old", 150L).first)
        assertEquals("New guide", EpgManager.nowNext("new", 150L).first?.title)
    }

    private fun snapshot(id: String, title: String) = EpgManager.Snapshot(
        programmes = mapOf(id to listOf(EpgManager.Prog(title, "", 100L, 200L))),
        source = id,
        loadedAtMs = 1L
    )
}
