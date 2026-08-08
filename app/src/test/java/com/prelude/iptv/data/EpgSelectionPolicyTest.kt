package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgSelectionPolicyTest {
    @Test fun `accepts http and https remote guides`() {
        assertEquals("https://example.com/guide.xml", EpgSelectionPolicy.normalizeRemoteUrl(" https://example.com/guide.xml "))
        assertEquals("http://example.com/guide.xml.gz", EpgSelectionPolicy.normalizeRemoteUrl("http://example.com/guide.xml.gz"))
    }

    @Test fun `rejects local and malformed guide locations`() {
        assertNull(EpgSelectionPolicy.normalizeRemoteUrl(""))
        assertNull(EpgSelectionPolicy.normalizeRemoteUrl("file:///sdcard/guide.xml"))
        assertNull(EpgSelectionPolicy.normalizeRemoteUrl("content://guide.xml"))
        assertNull(EpgSelectionPolicy.normalizeRemoteUrl("not a url"))
    }

    @Test fun `commits only for the active source and exact loaded url`() {
        assertTrue(
            EpgSelectionPolicy.shouldCommit(
                requestSourceId = "source-a",
                currentSourceId = "source-a",
                requestedUrl = "https://example.com/guide.xml",
                loadedUrl = "https://example.com/guide.xml"
            )
        )
        assertFalse(
            EpgSelectionPolicy.shouldCommit(
                requestSourceId = "source-a",
                currentSourceId = "source-b",
                requestedUrl = "https://example.com/guide.xml",
                loadedUrl = "https://example.com/guide.xml"
            )
        )
        assertFalse(
            EpgSelectionPolicy.shouldCommit(
                requestSourceId = "source-a",
                currentSourceId = "source-a",
                requestedUrl = "https://example.com/guide.xml",
                loadedUrl = "https://other.example/guide.xml"
            )
        )
    }
}
