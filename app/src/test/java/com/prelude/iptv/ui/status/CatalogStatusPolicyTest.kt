package com.prelude.iptv.ui.status

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogStatusPolicyTest {
    @Test
    fun blankStatusIsNone() {
        assertEquals(CatalogStatusKind.NONE, CatalogStatusPolicy.kindOf(""))
    }

    @Test
    fun legacyErrorsBecomeTypedAtOneBoundary() {
        assertEquals(
            CatalogStatusKind.ERROR,
            CatalogStatusPolicy.kindOf("Σφάλμα ανανέωσης: timeout"),
        )
    }

    @Test
    fun progressAndSuccessMessagesAreInformational() {
        assertEquals(
            CatalogStatusKind.INFO,
            CatalogStatusPolicy.kindOf("Λήψη από την πηγή…"),
        )
    }
}
