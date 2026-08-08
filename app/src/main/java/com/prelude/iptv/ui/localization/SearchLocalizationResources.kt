package com.prelude.iptv.ui.localization

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prelude.iptv.R
import com.prelude.iptv.ui.PremiumSearchFilter
import com.prelude.iptv.ui.SearchCategory
import com.prelude.iptv.ui.SearchHeading
import com.prelude.iptv.ui.SearchKeyboardAction

@StringRes
fun PremiumSearchFilter.labelRes(): Int = when (this) {
    PremiumSearchFilter.ALL -> R.string.search_filter_all
    PremiumSearchFilter.MOVIES -> R.string.search_filter_movies
    PremiumSearchFilter.SERIES -> R.string.search_filter_series
    PremiumSearchFilter.LIVE -> R.string.search_filter_live
    PremiumSearchFilter.SPORTS -> R.string.search_filter_sports
    PremiumSearchFilter.DOCUMENTARIES -> R.string.search_filter_documentaries
}

@Composable
fun localizedSearchHeading(heading: SearchHeading): String = when (heading) {
    is SearchHeading.Query -> stringResource(R.string.search_results_for, heading.query)
    is SearchHeading.Filter -> stringResource(heading.filter.labelRes())
    SearchHeading.Popular -> stringResource(R.string.search_popular_now)
}

@Composable
fun localizedSearchCategory(category: SearchCategory): String = when (category) {
    SearchCategory.Live -> stringResource(R.string.search_category_live)
    SearchCategory.Series -> stringResource(R.string.search_category_series)
    SearchCategory.Movie -> stringResource(R.string.search_category_movie)
    SearchCategory.Content -> stringResource(R.string.search_category_content)
    is SearchCategory.Provider -> category.label
}

@StringRes
fun SearchKeyboardAction.labelRes(): Int = when (this) {
    SearchKeyboardAction.SPACE -> R.string.search_keyboard_space
    SearchKeyboardAction.CLEAR -> R.string.search_keyboard_clear
    SearchKeyboardAction.GREEK -> R.string.search_keyboard_greek
    SearchKeyboardAction.LATIN -> R.string.search_keyboard_latin
    SearchKeyboardAction.NUMERIC -> R.string.search_keyboard_numeric
    SearchKeyboardAction.BACKSPACE -> R.string.search_keyboard_backspace
    SearchKeyboardAction.CHARACTER -> error("Character keys own their input glyph")
}
