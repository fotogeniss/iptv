package com.prelude.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentQualityPolicyTest {
    @Test
    fun `prefers the highest explicit quality token`() {
        assertEquals("4K", ContentQualityPolicy.label("Movie FHD", "Premium 4K"))
        assertEquals("2K", ContentQualityPolicy.label("Series QHD"))
    }

    @Test
    fun `recognizes common provider resolution labels`() {
        assertEquals("FHD", ContentQualityPolicy.label("Title 1080p"))
        assertEquals("HD", ContentQualityPolicy.label("Title 720"))
        assertEquals("SD", ContentQualityPolicy.label("Title 576i"))
    }

    @Test
    fun `does not invent a quality when metadata is absent`() {
        assertEquals("", ContentQualityPolicy.label("A normal movie title", "Cinema"))
        assertEquals("", ContentQualityPolicy.label("The UHDerman Story"))
    }
}
