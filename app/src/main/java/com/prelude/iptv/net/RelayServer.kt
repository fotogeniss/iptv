package com.prelude.iptv.net

import com.prelude.iptv.data.RelayHub
import fi.iki.elonen.NanoHTTPD

/** Ελαφρύς server: /playlist.m3u και /ch/{index} (φρέσκο resolve + proxy). */
class RelayServer(port: Int) : NanoHTTPD("0.0.0.0", port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        return when {
            uri.startsWith("/playlist.m3u") -> {
                val ip = RelayHub.localIp()
                newFixedLengthResponse(
                    Response.Status.OK, "audio/x-mpegurl", RelayHub.buildM3u(ip)
                )
            }
            uri.startsWith("/ch/") -> serveChannel(uri)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404")
        }
    }

    private fun serveChannel(uri: String): Response {
        val idx = uri.removePrefix("/ch/").substringBefore('/').toIntOrNull()
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "bad id")
        val ch = RelayHub.channelAt(idx)
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "no channel")
        return try {
            val url = RelayHub.resolve(ch)
            if (url.isEmpty())
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "no url")
            // Redirect στο πραγματικό URL — ο player συνδέεται απευθείας (πιο αξιόπιστο)
            val r = newFixedLengthResponse(Response.Status.REDIRECT, "text/plain", "")
            r.addHeader("Location", url)
            r
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "upstream error")
        }
    }
}
