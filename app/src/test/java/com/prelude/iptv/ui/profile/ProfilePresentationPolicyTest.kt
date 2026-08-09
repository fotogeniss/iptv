package com.prelude.iptv.ui.profile

import com.prelude.iptv.data.PlaylistStore
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfilePresentationPolicyTest {
    @Test
    fun primaryProfileUsesAppOwnedDisplayIdentity() {
        val profile = PlaylistStore.Profile(id = 0, name = PlaylistStore.LEGACY_PRIMARY_PROFILE_NAME, protected = false)

        assertEquals(ProfileDisplayName.Primary, ProfilePresentationPolicy.displayName(profile))
    }

    @Test
    fun userProfileNameRemainsExactDataEvenWhenItLooksLikeAppCopy() {
        val name = "Κύριο"
        val profile = PlaylistStore.Profile(id = 7, name = name, protected = true)

        assertEquals(ProfileDisplayName.Stored(name), ProfilePresentationPolicy.displayName(profile))
    }
}
