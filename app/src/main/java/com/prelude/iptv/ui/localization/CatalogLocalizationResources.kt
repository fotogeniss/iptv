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

/**
 * Η γραμμή προόδου του καταλόγου: ΠΟΣΟ και ΤΙ.
 *
 * Το [contentType] είναι η σταθερή ταυτότητα ενότητας του παρόχου («live»,
 * «vod», «series») — αποθηκευμένο κλειδί, ΟΧΙ κείμενο προς εμφάνιση. Η
 * μετάφραση γίνεται εδώ, στο σύνορο του Compose, ώστε ο κατάλογος να μη
 * χρειάζεται να ξέρει σε ποια γλώσσα μιλάει η εφαρμογή.
 *
 * Άγνωστο [contentType] πέφτει στη γενική διατύπωση αντί να δείξει το ωμό
 * κλειδί: μια νέα ενότητα αύριο δεν πρέπει να τυπώσει «vod2» στην οθόνη.
 */
@Composable
fun localizedCatalogProgress(percent: Int?, contentType: String? = null): String {
    val section = when (contentType) {
        "live" -> stringResource(R.string.catalog_loading_live)
        "vod" -> stringResource(R.string.catalog_loading_movies)
        "series" -> stringResource(R.string.catalog_loading_series)
        else -> null
    }
    if (percent == null) return section ?: stringResource(R.string.catalog_loading)
    val locale = LocalConfiguration.current.locales[0]
    val formatted = NumberFormat.getPercentInstance(locale).format(percent / 100.0)
    return if (section == null) {
        stringResource(R.string.catalog_loading_with_progress, formatted)
    } else {
        stringResource(R.string.catalog_loading_section_with_progress, formatted, section)
    }
}
