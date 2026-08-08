package com.prelude.iptv.ui.navigation

/**
 * The five destinations that are always one action away on phone and TV.
 *
 * Secondary destinations (favorites, history, EPG, sources and settings) live
 * inside the screen that owns them instead of competing with content browsing.
 */
enum class PrimaryContentDestination(
    val route: String,
) {
    HOME("home"),
    LIVE("live"),
    MOVIES("movies"),
    SERIES("series"),
    SEARCH("search"),
    ;

    companion object {
        val ordered: List<PrimaryContentDestination> = entries.toList()

        fun fromRoute(route: String?): PrimaryContentDestination? =
            entries.firstOrNull { it.route == route }

        /** Home owns secondary library views that are not primary destinations. */
        fun selectionRoute(route: String?): String =
            fromRoute(route)?.route ?: HOME.route

        /**
         * Resolves the one TV rail item that owns selection and Back focus.
         * Secondary library views belong to Home; the EPG belongs to Live via
         * its current content type.
         */
        fun resolveTvSelection(
            currentContentType: String,
            homeSelected: Boolean,
            searchSelected: Boolean,
            secondaryLibrarySelected: Boolean,
        ): PrimaryContentDestination = when {
            searchSelected -> SEARCH
            homeSelected || secondaryLibrarySelected -> HOME
            currentContentType == "live" -> LIVE
            currentContentType == "vod" -> MOVIES
            currentContentType == "series" -> SERIES
            else -> HOME
        }
    }
}
