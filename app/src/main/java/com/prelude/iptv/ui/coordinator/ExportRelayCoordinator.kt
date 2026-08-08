package com.prelude.iptv.ui.coordinator

import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.RelayHub
import com.prelude.iptv.source.StalkerClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns relay lifecycle state and resolved M3U export preparation. */
internal class ExportRelayCoordinator(
    private val currentStalker: () -> StalkerClient?,
    private val currentChannels: () -> List<Channel>,
    private val resolvePlayableUrl: suspend (Channel) -> String,
    private val startRelayService: () -> Unit,
    private val stopRelayService: () -> Unit,
    private val publishRelayState: (running: Boolean, url: String) -> Unit,
) {
    fun startRelay(selected: List<Channel>): String {
        RelayHub.channels = selected.filter { it.kind != "series" }
        RelayHub.stalker = currentStalker()
        RelayHub.port = RELAY_PORT
        startRelayService()
        val url = "http://${RelayHub.localIp()}:${RelayHub.port}/playlist.m3u"
        publishRelayState(true, url)
        return url
    }

    fun stopRelay() {
        stopRelayService()
        publishRelayState(false, "")
    }

    fun exportableChannels(): List<Channel> =
        currentChannels().filter { it.kind != "series" }

    suspend fun buildResolvedM3u(channels: List<Channel>): String = withContext(Dispatchers.IO) {
        val output = StringBuilder("#EXTM3U\n")
        for (channel in channels) {
            val url = try {
                resolvePlayableUrl(channel)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ""
            }
            if (url.isNotEmpty()) {
                output.append(
                    "#EXTINF:-1 tvg-id=\"${cleanAttribute(channel.tvgId)}\" " +
                        "tvg-logo=\"${cleanAttribute(channel.logo)}\" " +
                        "group-title=\"${cleanAttribute(channel.group)}\",${channel.name}\n"
                )
                output.append("$url\n")
            }
        }
        output.toString()
    }

    private fun cleanAttribute(value: String): String = value.replace("\"", "")

    private companion object {
        const val RELAY_PORT = 8899
    }
}
