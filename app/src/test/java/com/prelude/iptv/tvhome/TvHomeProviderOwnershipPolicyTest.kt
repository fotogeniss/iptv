package com.prelude.iptv.tvhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvHomeProviderOwnershipPolicyTest {
    private val token = "123e4567-e89b-12d3-a456-426614174000"

    @Test fun `accepts only owned watch next identifier`() {
        assertEquals(
            token,
            TvHomeProviderOwnershipPolicy.ownedToken(
                "${TvHomeProviderOwnershipPolicy.WATCH_NEXT_PREFIX}$token",
                TvHomeProviderOwnershipPolicy.WATCH_NEXT_PREFIX,
            )
        )
    }

    @Test fun `accepts only owned my list identifier`() {
        assertEquals(
            token,
            TvHomeProviderOwnershipPolicy.ownedToken(
                "${TvHomeProviderOwnershipPolicy.MY_LIST_PREFIX}$token",
                TvHomeProviderOwnershipPolicy.MY_LIST_PREFIX,
            )
        )
    }

    @Test fun `rejects foreign malformed and cross namespace identifiers`() {
        assertNull(TvHomeProviderOwnershipPolicy.ownedToken("other:$token", TvHomeProviderOwnershipPolicy.WATCH_NEXT_PREFIX))
        assertNull(TvHomeProviderOwnershipPolicy.ownedToken("upl:not-a-uuid", TvHomeProviderOwnershipPolicy.WATCH_NEXT_PREFIX))
        assertNull(TvHomeProviderOwnershipPolicy.ownedToken("upl:${token.uppercase()}", TvHomeProviderOwnershipPolicy.WATCH_NEXT_PREFIX))
        assertNull(TvHomeProviderOwnershipPolicy.ownedToken("upl:list:$token", TvHomeProviderOwnershipPolicy.WATCH_NEXT_PREFIX))
        assertNull(TvHomeProviderOwnershipPolicy.ownedToken("upl:$token", TvHomeProviderOwnershipPolicy.MY_LIST_PREFIX))
    }
}
