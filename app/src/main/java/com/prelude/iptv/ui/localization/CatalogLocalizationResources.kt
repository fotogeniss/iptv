package com.prelude.iptv.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.ui.UiState
import java.text.NumberFormat

/** Localizes only app-owned synthetic groups; provider labels remain provider data. */
@Composable
fun localizedCatalogGroupLabel(group: String, contentType: String): String = when (group) {
    UiState.ALL_GROUP -> when (contentType) {
        "vod" -> stringResource(R.string.catalog_all_movies)
        "series" -> stringResource(R.string.catalog_all_series)
        "live" -> stringResource(R.string.live_all_channels)
        else -> group
    }
    UiState.FAV_GROUP -> if (contentType == "live") {
        stringResource(R.string.live_favorites)
    } else {
        stringResource(R.string.catalog_favorites)
    }
    else -> group
}

@Composable
fun localizedCatalogProgress(percent: Int?): String {
    if (percent == null) return stringResource(R.string.catalog_loading)
    val locale = LocalConfiguration.current.locales[0]
    val formatted = NumberFormat.getPercentInstance(locale).format(percent / 100.0)
    return stringResource(R.string.catalog_loading_with_progress, formatted)
}
