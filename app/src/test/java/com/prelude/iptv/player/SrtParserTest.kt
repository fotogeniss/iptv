package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtParserTest {

    @Test fun `parses a basic cue with correct millis`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:04,500
            Γεια σου κόσμε
        """.trimIndent()
        val cues = SrtParser.parse(srt)
        assertEquals(1, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals(4500L, cues[0].endMs)
        assertEquals("Γεια σου κόσμε", cues[0].text)
    }

    @Test fun `parses multiple cues separated by blank lines`() {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\nA\n\n2\n00:00:03,000 --> 00:00:04,000\nB"
        val cues = SrtParser.parse(srt)
        assertEquals(2, cues.size)
        assertEquals("A", cues[0].text)
        assertEquals("B", cues[1].text)
    }

    @Test fun `strips html-style tags`() {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\n<i>κλάψε</i> <b>τώρα</b>"
        assertEquals("κλάψε τώρα", SrtParser.parse(srt)[0].text)
    }

    @Test fun `handles CRLF line endings`() {
        val srt = "1\r\n00:00:01,000 --> 00:00:02,000\r\nHello\r\n"
        val cues = SrtParser.parse(srt)
        assertEquals(1, cues.size)
        assertEquals("Hello", cues[0].text)
    }

    @Test fun `joins multi-line subtitle text`() {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\nΓραμμή 1\nΓραμμή 2"
        assertEquals("Γραμμή 1\nΓραμμή 2", SrtParser.parse(srt)[0].text)
    }

    @Test fun `hour component is counted`() {
        val srt = "1\n01:02:03,004 --> 01:02:04,005\nx"
        val c = SrtParser.parse(srt)[0]
        assertEquals(3723004L, c.startMs)   // 1h2m3s004
        assertEquals(3724005L, c.endMs)
    }

    @Test fun `malformed blocks are skipped, not crashing`() {
        val srt = "garbage no timecode\n\n1\n00:00:01,000 --> 00:00:02,000\nok"
        val cues = SrtParser.parse(srt)
        assertEquals(1, cues.size)
        assertEquals("ok", cues[0].text)
    }

    @Test fun `empty input yields empty list`() {
        assertTrue(SrtParser.parse("").isEmpty())
    }
}
