package com.prelude.iptv.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryCountryPolicyTest {
    @Test fun `common provider prefixes are recognised`() {
        assertEquals("GR", CategoryCountryPolicy.countryOf("GR | Αθλητικά"))
        assertEquals("UK", CategoryCountryPolicy.countryOf("UK: News"))
        assertEquals("US", CategoryCountryPolicy.countryOf("[US] Movies"))
        assertEquals("IT", CategoryCountryPolicy.countryOf("IT - Sport"))
        assertEquals("GR", CategoryCountryPolicy.countryOf("|GR| Ταινίες"))
    }

    @Test fun `titles without a country prefix are not grouped`() {
        assertNull(CategoryCountryPolicy.countryOf("Adult"))
        assertNull(CategoryCountryPolicy.countryOf("Kids - Cartoons"))
        assertNull(CategoryCountryPolicy.countryOf("VOD | Action"))
        assertNull(CategoryCountryPolicy.countryOf("HD: Sports"))
    }

    @Test fun `groups by size with rare prefixes folded into others`() {
        val options = listOf(
            CategoryOption("1", "GR | Sports"),
            CategoryOption("2", "GR | News"),
            CategoryOption("3", "GR | Kids"),
            CategoryOption("4", "UK | Sports"),
            CategoryOption("5", "UK | News"),
            CategoryOption("6", "FR | Cinema"),
            CategoryOption("7", "Adult"),
        )

        val groups = CategoryCountryPolicy.groups(options)

        assertEquals(listOf("GR", "UK", null), groups.map { it.code })
        assertEquals(listOf("6", "7"), groups.last().ids)
    }

    @Test fun `a single country is not worth a grid`() {
        val options = listOf(
            CategoryOption("1", "GR | Sports"),
            CategoryOption("2", "GR | News"),
            CategoryOption("3", "Adult"),
        )

        assertTrue(CategoryCountryPolicy.groups(options).isEmpty())
    }
}
