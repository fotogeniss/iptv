package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class PlayerSubtitlePolicyTest {

    /* ---- size clamping ---- */

    @Test
    fun coerceClampsToSupportedRange() {
        assertEquals(PlayerSubtitlePolicy.MIN_SIZE_PERCENT, PlayerSubtitlePolicy.coerceSizePercent(0))
        assertEquals(PlayerSubtitlePolicy.MAX_SIZE_PERCENT, PlayerSubtitlePolicy.coerceSizePercent(999))
        assertEquals(100, PlayerSubtitlePolicy.coerceSizePercent(100))
    }

    @Test
    fun growAndShrinkRespectBoundaries() {
        assertFalse(PlayerSubtitlePolicy.canShrink(PlayerSubtitlePolicy.MIN_SIZE_PERCENT))
        assertTrue(PlayerSubtitlePolicy.canShrink(PlayerSubtitlePolicy.MIN_SIZE_PERCENT + 1))
        assertFalse(PlayerSubtitlePolicy.canGrow(PlayerSubtitlePolicy.MAX_SIZE_PERCENT))
        assertTrue(PlayerSubtitlePolicy.canGrow(PlayerSubtitlePolicy.MAX_SIZE_PERCENT - 1))
    }

    /* ---- scaling ---- */

    @Test
    fun scaleIsOneAtHundredPercent() {
        assertEquals(1f, PlayerSubtitlePolicy.scale(100), 0.0001f)
        assertEquals(1.8f, PlayerSubtitlePolicy.scale(180), 0.0001f)
    }

    @Test
    fun baseSpDependsOnFormFactor() {
        assertEquals(PlayerSubtitlePolicy.TV_BASE_SP, PlayerSubtitlePolicy.baseSp(isTv = true), 0.0001f)
        assertEquals(PlayerSubtitlePolicy.MOBILE_BASE_SP, PlayerSubtitlePolicy.baseSp(isTv = false), 0.0001f)
    }

    @Test
    fun exoFractionMatchesLegacyFormula() {
        // Legacy PlayerActivity: EXO_BASE_FRACTION * (percent / 100f)
        assertEquals(PlayerSubtitlePolicy.EXO_BASE_FRACTION, PlayerSubtitlePolicy.exoFraction(100), 0.00001f)
        assertEquals(PlayerSubtitlePolicy.EXO_BASE_FRACTION * 1.5f, PlayerSubtitlePolicy.exoFraction(150), 0.00001f)
    }

    /* ---- active cue selection ---- */

    private fun cue(start: Long, end: Long, text: String) = Cue(start, end, text)

    @Test
    fun activeCueMatchesInclusiveBounds() {
        val cues = listOf(cue(1000, 2000, "a"))
        assertEquals("a", PlayerSubtitlePolicy.activeCue(cues, 1000)?.text)
        assertEquals("a", PlayerSubtitlePolicy.activeCue(cues, 1500)?.text)
        assertEquals("a", PlayerSubtitlePolicy.activeCue(cues, 2000)?.text)
    }

    @Test
    fun activeCueReturnsNullOutsideAnyCue() {
        val cues = listOf(cue(1000, 2000, "a"), cue(3000, 4000, "b"))
        assertNull(PlayerSubtitlePolicy.activeCue(cues, 500))
        assertNull(PlayerSubtitlePolicy.activeCue(cues, 2500))
        assertNull(PlayerSubtitlePolicy.activeCue(emptyList(), 1500))
    }

    @Test
    fun activeCueReturnsFirstMatchWhenCuesOverlap() {
        val cues = listOf(cue(1000, 3000, "first"), cue(2000, 4000, "second"))
        assertEquals("first", PlayerSubtitlePolicy.activeCue(cues, 2500)?.text)
    }

    /* ---- encoding fallback ---- */

    @Test
    fun decodeKeepsValidUtf8() {
        val text = "Καλησπέρα — γεια"
        assertEquals(text, PlayerSubtitlePolicy.decodeSubtitleBytes(text.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun decodeFallsBackToWindows1253ForInvalidUtf8() {
        // 0xE1 is an invalid stand-alone UTF-8 byte, but is 'α' in Windows-1253.
        val decoded = PlayerSubtitlePolicy.decodeSubtitleBytes(byteArrayOf(0xE1.toByte()))
        assertFalse("must not leak the UTF-8 replacement char", decoded.contains('\uFFFD'))
        assertEquals("α", decoded) // GREEK SMALL LETTER ALPHA
    }

    @Test
    fun decodeUnpacksGzipSubtitleResponse() {
        val srt = "1\n00:00:01,000 --> 00:00:03,000\nΚαλησπέρα\n"
        val packed = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(srt.toByteArray(Charsets.UTF_8)) }
        }.toByteArray()

        assertEquals(srt, PlayerSubtitlePolicy.decodeSubtitleBytes(packed))
    }
}
