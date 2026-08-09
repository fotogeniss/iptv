package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.ui.LibraryDestination
import com.prelude.iptv.ui.components.library.LibraryHubTab
import com.prelude.iptv.ui.components.library.LibraryRailLabels
import com.prelude.iptv.ui.components.library.LibrarySort
import com.prelude.iptv.ui.components.library.PremiumLibraryContent

@StringRes
fun LibraryHubTab.labelRes(): Int = when (this) {
    LibraryHubTab.ALL -> R.string.library_tab_all
    LibraryHubTab.MY_LIST -> R.string.library_tab_my_list
    LibraryHubTab.CONTINUE -> R.string.library_tab_continue
    LibraryHubTab.HISTORY -> R.string.library_tab_history
}

@StringRes
fun LibrarySort.labelRes(): Int = when (this) {
    LibrarySort.RECENT -> R.string.library_sort_recent
    LibrarySort.TITLE -> R.string.library_sort_title
}

/** Uppercase "eyebrow" label shown above the selected title in the library info panel. */
@StringRes
fun LibraryDestination.eyebrowRes(): Int = when (this) {
    LibraryDestination.MY_LIST -> R.string.library_eyebrow_my_list
    LibraryDestination.CONTINUE_WATCHING -> R.string.library_eyebrow_continue
    LibraryDestination.HISTORY -> R.string.library_eyebrow_history
    LibraryDestination.SEARCH -> R.string.library_eyebrow_library
}

/** Resolves the shared TV/mobile library rail titles, subtitles and locale-aware counts. */
@Composable
fun libraryRailLabels(content: PremiumLibraryContent): LibraryRailLabels = LibraryRailLabels(
    continueTitle = stringResource(R.string.library_rail_continue_title),
    continueSubtitleDescription = stringResource(R.string.library_rail_continue_subtitle),
    continueSubtitleCount = pluralStringResource(
        R.plurals.library_rail_continue_count, content.continueWatching.size, content.continueWatching.size
    ),
    myListTitle = stringResource(R.string.library_rail_my_list_title),
    myListSubtitleDescription = stringResource(R.string.library_rail_my_list_subtitle),
    myListSubtitleCount = pluralStringResource(
        R.plurals.library_rail_my_list_count, content.myList.size, content.myList.size
    ),
    historyTitle = stringResource(R.string.library_rail_history_title),
    historySubtitleDescription = stringResource(R.string.library_rail_history_subtitle),
    historySubtitleCount = pluralStringResource(
        R.plurals.library_rail_history_count, content.history.size, content.history.size
    ),
)
