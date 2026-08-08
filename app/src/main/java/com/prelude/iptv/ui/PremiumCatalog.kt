package com.prelude.iptv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prelude.iptv.data.Channel

/**
 * Public catalog entry point. It intentionally delegates to different mobile
 * and television implementations instead of shrinking one layout for both.
 */
@Composable
fun PremiumContentRail(
    section: CatalogRailSection,
    favoriteKeys: Set<String>,
    onOpen: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isTvDevice()) {
        TvMediaRail(section, favoriteKeys, onOpen, modifier)
    } else {
        MobileMediaRail(section, favoriteKeys, onOpen, modifier)
    }
}
