package com.prelude.iptv.ui.profile

import com.prelude.iptv.data.PlaylistStore

sealed interface ProfileDisplayName {
    data object Primary : ProfileDisplayName
    data class Stored(val value: String) : ProfileDisplayName
}

/** Keeps the app-created primary label separate from names entered by the user. */
object ProfilePresentationPolicy {
    fun displayName(profile: PlaylistStore.Profile): ProfileDisplayName =
        if (profile.id == 0) ProfileDisplayName.Primary
        else ProfileDisplayName.Stored(profile.name)
}
