package com.prelude.iptv.tvhome

import android.content.Context
import com.prelude.iptv.data.PlaylistIdentity
import com.prelude.iptv.data.PlaylistStore

internal class TvMyListCatalogRepository(context: Context) {
    private val store = PlaylistStore(context.applicationContext)

    fun build() = if (!store.tvHomeMyListEnabled) {
        emptyList()
    } else {
        TvMyListPolicy.select(
            favorites = store.loadSourceFavorites(),
            availableSourceIds = store.loadPlaylists().mapTo(HashSet<String>()) { PlaylistIdentity.stableId(it) },
            lockedGroups = store.lockedGroups()
        )
    }
}
