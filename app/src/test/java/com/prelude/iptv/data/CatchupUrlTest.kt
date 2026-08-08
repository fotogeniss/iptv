package com.prelude.iptv.data

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatchupUrlTest {

    private val start = 1_700_000_000_000L          // κάποιο περασμένο timestamp
    private val stop = start + 30 * 60_000L          // +30'
    private val now = stop + 60_000L                 // μετά το τέλος (= παρελθόν)

    @Test fun `builds a well-formed timeshift url`() {
        val u = CatchupUrl.build("http://srv:8080", "user", "pass", "123", start, stop, now)!!
        assertTrue(u.startsWith("http://srv:8080/timeshift/user/pass/30/"))
        assertTrue(u.endsWith("/123.ts"))
    }

    @Test fun `trims trailing slash on server`() {
        val u = CatchupUrl.build("http://srv/", "u", "p", "1", start, stop, now)!!
        assertTrue(u.startsWith("http://srv/timeshift/"))   // όχι διπλό //
    }

    @Test fun `url-encodes credentials with special chars`() {
        val u = CatchupUrl.build("http://s", "a b", "p/w+d", "9", start, stop, now)!!
        assertTrue(u.contains("/a+b/") || u.contains("/a%20b/"))
        assertTrue(u.contains("p%2Fw%2Bd"))                 // / και + encoded
    }

    @Test fun `returns null for future programme`() {
        // stop στο μέλλον -> catch-up δεν ισχύει
        assertNull(CatchupUrl.build("http://s", "u", "p", "1", start, stop, start - 1))
    }

    @Test fun `returns null without streamId or server`() {
        assertNull(CatchupUrl.build("http://s", "u", "p", "", start, stop, now))
        assertNull(CatchupUrl.build("", "u", "p", "1", start, stop, now))
    }

    @Test fun `returns null for sub-minute duration`() {
        assertNull(CatchupUrl.build("http://s", "u", "p", "1", start, start + 30_000L, now))
    }
}
