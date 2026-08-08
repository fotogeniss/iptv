package com.prelude.iptv.data

import org.json.JSONObject

/** Ένα κανάλι/στοιχείο από οποιαδήποτε πηγή. */
data class Channel(
    val name: String,
    val group: String = "",
    val logo: String = "",
    val tvgId: String = "",
    val url: String = "",          // άμεσο URL (M3U/Xtream)
    val cmd: String = "",          // Stalker cmd (χρειάζεται resolve)
    val chId: String = "",         // Stalker channel id (για EPG)
    val streamId: String = "",     // Xtream stream id (για EPG)
    val kind: String = "live",     // live | vod | series | series_ep
    val seriesId: String = "",
    // πληροφορίες ταινίας/σειράς
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val year: String = "",
    val duration: String = ""
)

enum class PlaylistType { M3U, XTREAM, STALKER }

/** Ορισμός μιας αποθηκευμένης λίστας. */
data class Playlist(
    val name: String,
    val type: PlaylistType,
    // M3U
    val source: String = "",
    val isUrl: Boolean = true,
    // Xtream
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val output: String = "ts",
    // Stalker
    val portal: String = "",
    val mac: String = "",
    val userAgent: String = "",
    // κοινό
    val epgUrl: String = "",
    /** δείκτης εικονιδίου/χρώματος προφίλ (0..5) */
    val avatar: Int = 0,
    // ---- Πλήθη καταλόγου, για την κάρτα στις «Πηγές» ----
    // -1 = άγνωστο (δεν έχει φορτωθεί ποτέ αυτή η ενότητα). Ενημερώνονται μόνο
    // όταν το MainViewModel ολοκληρώσει επιτυχώς τη φόρτωση της αντίστοιχης
    // ενότητας — δεν κάνουμε δίκτυο μόνο για να γεμίσουμε μια κάρτα.
    val liveCount: Int = -1,
    val vodCount: Int = -1,
    val seriesCount: Int = -1
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("type", type.name)
        put("source", source)
        put("isUrl", isUrl)
        put("server", server)
        put("username", username)
        put("password", password)
        put("output", output)
        put("portal", portal)
        put("mac", mac)
        put("userAgent", userAgent)
        put("epgUrl", epgUrl)
        put("avatar", avatar)
        put("liveCount", liveCount)
        put("vodCount", vodCount)
        put("seriesCount", seriesCount)
    }

    companion object {
        fun fromJson(o: JSONObject) = Playlist(
            name = o.optString("name"),
            type = runCatching { PlaylistType.valueOf(o.optString("type", "M3U")) }
                .getOrDefault(PlaylistType.M3U),
            source = o.optString("source"),
            isUrl = o.optBoolean("isUrl", true),
            server = o.optString("server"),
            username = o.optString("username"),
            password = o.optString("password"),
            output = o.optString("output", "ts"),
            portal = o.optString("portal"),
            mac = o.optString("mac"),
            userAgent = o.optString("userAgent"),
            avatar = o.optInt("avatar", 0),
            epgUrl = o.optString("epgUrl"),
            liveCount = o.optInt("liveCount", -1),
            vodCount = o.optInt("vodCount", -1),
            seriesCount = o.optInt("seriesCount", -1)
        )
    }
}

/** Μια εγγραφή EPG. */
data class EpgEntry(
    val title: String,
    val desc: String = "",
    val start: String = "",
    val end: String = ""
)
