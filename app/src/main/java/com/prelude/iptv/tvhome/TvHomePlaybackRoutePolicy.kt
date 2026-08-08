package com.prelude.iptv.tvhome

import java.util.UUID

internal data class TvHomePlaybackRoute(
    val route: String,
    val token: String,
)

/** Strict parser for the only externally reachable playback deep links. */
internal object TvHomePlaybackRoutePolicy {
    const val ROUTE_PLAY_NEXT = "play-next"
    const val ROUTE_MY_LIST = "my-list"
    private val supportedRoutes = setOf(ROUTE_PLAY_NEXT, ROUTE_MY_LIST)

    fun parse(scheme: String?, host: String?, pathSegments: List<String>): TvHomePlaybackRoute? {
        if (scheme != "upl" || host !in supportedRoutes || pathSegments.size != 1) return null
        val token = pathSegments.single()
        if (token != token.trim()) return null
        val uuid = runCatching { UUID.fromString(token) }.getOrNull() ?: return null
        if (uuid.toString() != token) return null
        return TvHomePlaybackRoute(route = host.orEmpty(), token = uuid.toString())
    }
}
