package com.prelude.iptv.data

import com.prelude.iptv.source.StalkerClient
import java.net.NetworkInterface

/** Κοινό σημείο ανάμεσα σε app και RelayService: κανάλια + resolve. */
object RelayHub {
    @Volatile var channels: List<Channel> = emptyList()
    @Volatile var stalker: StalkerClient? = null
    @Volatile var port: Int = 8899
    @Volatile var running: Boolean = false

    /** Φρέσκο playable URL για κανάλι (κάνει resolve + retry αν λήξει το token). */
    fun resolve(ch: Channel): String {
        val st = stalker
        // Ίδιος κανόνας με το [Repository.playableUrl]: ταινίες και επεισόδια
        // ΠΡΕΠΕΙ να περάσουν από create_link. Δεύτερο σημείο που καλεί την ίδια
        // συνάρτηση — αν έμενε πίσω, το relay θα σέρβιρε τους ίδιους νεκρούς
        // συνδέσμους που μόλις διορθώσαμε στον player.
        val vod = ch.kind != "live"
        // chId κρατά τον πραγματικό αριθμό επεισοδίου· το streamId ταυτοποιεί
        // το επεισόδιο μόνιμα (ιστορικό/αγαπημένα) και επαναλαμβάνεται ανά
        // σεζόν, άρα δεν είναι ασφαλές εδώ — δες Repository.playableUrl.
        val episodeNum = if (ch.kind == "series_ep") ch.chId else ""
        return if (ch.cmd.isNotEmpty() && st != null) {
            try {
                st.resolve(ch.cmd, vod = vod, episodeNum = episodeNum)
            } catch (e: Exception) {
                // token μπορεί να έληξε — ξανασυνδέσου και δοκίμασε ξανά
                st.connect()
                st.resolve(ch.cmd, vod = vod, episodeNum = episodeNum)
            }
        } else ch.url
    }

    fun buildM3u(ip: String): String {
        val sb = StringBuilder("#EXTM3U\n")
        channels.forEachIndexed { i, ch ->
            sb.append(
                "#EXTINF:-1 tvg-id=\"${ch.tvgId}\" tvg-logo=\"${ch.logo}\" " +
                    "group-title=\"${ch.group}\",${ch.name}\n"
            )
            sb.append("http://$ip:$port/ch/$i\n")
        }
        return sb.toString()
    }

    fun channelAt(index: Int): Channel? = channels.getOrNull(index)

    /** Βρίσκει την τοπική IP (LAN) της συσκευής. */
    fun localIp(): String {
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()
            for (iface in ifaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains('.') == true) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "127.0.0.1"
    }
}
