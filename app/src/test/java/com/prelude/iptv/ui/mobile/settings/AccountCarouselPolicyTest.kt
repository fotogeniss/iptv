package com.prelude.iptv.ui.mobile.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountCarouselPolicyTest {
    @Test
    fun pageIdentitiesStayTruthfulAndOrdered() {
        assertEquals(
            listOf(
                AccountPageIdentity.LocalProfiles,
                AccountPageIdentity.DeviceLocal,
                AccountPageIdentity.EncryptedBackup,
            ),
            AccountPageIdentity.entries,
        )
    }

    @Test
    fun automaticAdvanceWrapsAtLastPage() {
        assertEquals(1, AccountCarouselPolicy.nextPage(0, 3))
        assertEquals(0, AccountCarouselPolicy.nextPage(2, 3))
    }

    @Test
    fun swipeChangesOnePageAndRespectsBounds() {
        assertEquals(1, AccountCarouselPolicy.pageAfterSwipe(0, -80f, 3))
        assertEquals(0, AccountCarouselPolicy.pageAfterSwipe(0, 80f, 3))
        assertEquals(2, AccountCarouselPolicy.pageAfterSwipe(2, -80f, 3))
        assertEquals(1, AccountCarouselPolicy.pageAfterSwipe(2, 80f, 3))
    }

    @Test
    fun shortGestureDoesNotChangePage() {
        assertEquals(1, AccountCarouselPolicy.pageAfterSwipe(1, 20f, 3))
    }
}
