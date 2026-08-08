package com.prelude.iptv.source

import android.util.Base64
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgEntry
import com.prelude.iptv.data.SourceProgressCallback
import com.prelude.iptv.data.SourcePartialCallback
import com.prelude.iptv.net.Http
import com.prelude.iptv.net.ProviderCancellation
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object XtreamClient {

    /** GET με έλεγχο ότι επιστρέφει JSON· δοκιμάζει διάφορα UA + https. */
    private fun fetchX(
        url: String,
        onProgress: SourceProgressCallback? = null,
        rangeStart: Int = 0,
        rangeEnd: Int = 100,
        stage: String = "Λήψη δεδομένων…"
    ): String {
        val urls = if (url.startsWith("http://"))
            listOf(url, url.replaceFirst("http://", "https://")) else listOf(url)
        val uas = listOf(
            "IPTVSmartersPlayer",
            Http.DESKTOP_UA,
            "VLC/3.0.20 LibVLC/3.0.20",
            "okhttp/4.12.0"
        )
        var last = ""
        for (u in urls) {
            for (ua in uas) {
                val body = try {
                    Http.providerGetWithProgress(u, mapOf("User-Agent" to ua)) { read, total ->
                        val percent = total?.takeIf { it > 0L }?.let {
                            val fraction = (read.toDouble() / it.toDouble()).coerceIn(0.0, 1.0)
                            (rangeStart + ((rangeEnd - rangeStart) * fraction)).toInt()
                        }
                        onProgress?.invoke(percent, stage)
                    }
                } catch (error: Exception) {
                    // Explicit cancellation must stop UA/HTTPS retries immediately.
                    ProviderCancellation.rethrow(error, "Xtream request cancelled")
                    // Νεκρός host / δεν συνδέεται: άλλο UA δεν θα αλλάξει τίποτα.
                    if (error is java.net.UnknownHostException || error is java.net.ConnectException) break
                    ""
                }
                val t = body.trimStart()
                if (t.startsWith("{") || t.startsWith("[")) {
                    onProgress?.invoke(rangeEnd, stage)
                    return body
                }
                if (body.isNotEmpty()) last = body
            }
        }
        if (Http.looksLikeCloudflare(last))
            throw RuntimeException("Ο server είναι πίσω από Cloudflare — δεν απαντά στο API.")
        throw RuntimeException("Ο server επέστρεψε HTML αντί για JSON (έλεγξε URL/θύρα/στοιχεία).")
    }

    private fun base(server: String): String {
        var s = server.trim().trimEnd('/')
        if (!s.startsWith("http")) s = "http://$s"
        return s
    }

    private fun q(user: String, pass: String) =
        "username=${enc(user)}&password=${enc(pass)}"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /**
     * Encode για path segment. Το URLEncoder βγάζει '+' για το κενό (σωστό μόνο
     * σε query string) — στο path θέλει %20. ΧΩΡΙΣ encode: password με / # ? +
     * περνούσε το auth (εκεί γινόταν enc) αλλά έσπαγε τα stream URLs — ύπουλο,
     * γιατί το «Δοκιμή σύνδεσης» έλεγε OK και η αναπαραγωγή αποτύγχανε.
     */
    private fun pathEnc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    /**
     * Επιτυχημένα auth ανά (server|user): το auth() έτρεχε πριν από ΚΑΘΕ κλήση
     * (κατηγορίες, live, vod, series…) = 1 άχρηστο round-trip τη φορά. Το
     * «Φόρτωσε τα πάντα» έκανε 3 auth για τα ίδια στοιχεία. Τώρα: μία φορά.
     * Αν τα στοιχεία λήξουν στη μέση, η επόμενη κλήση δεδομένων θα σκάσει
     * ούτως ή άλλως με καθαρό μήνυμα — δεν χάνουμε τίποτα σε ασφάλεια.
     */
    private val authOk = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /** Forces the next refresh to authenticate again instead of reusing session state. */
    fun invalidateSession(server: String, user: String) {
        authOk.remove("${base(server)}|$user")
    }

    private fun auth(base: String, user: String, pass: String, onProgress: SourceProgressCallback? = null) {
        val k = "$base|$user"
        if (k in authOk) {
            onProgress?.invoke(10, "Σύνδεση έτοιμη")
            return
        }
        val info = JSONObject(fetchX(
            "$base/player_api.php?${q(user, pass)}",
            onProgress = onProgress,
            rangeStart = 1,
            rangeEnd = 10,
            stage = "Σύνδεση Xtream…"
        ))
        val ok = info.optJSONObject("user_info")?.optInt("auth", 0) ?: 0
        if (ok != 1) throw RuntimeException("Απέτυχε η σύνδεση Xtream (λάθος στοιχεία ή server).")
        authOk.add(k)
        onProgress?.invoke(10, "Σύνδεση έτοιμη")
    }

    private fun categories(
        base: String,
        user: String,
        pass: String,
        action: String,
        onProgress: SourceProgressCallback? = null,
        rangeStart: Int = 10,
        rangeEnd: Int = 25
    ): Map<String, String> {
        val out = HashMap<String, String>()
        try {
            val arr = JSONArray(fetchX(
                "$base/player_api.php?${q(user, pass)}&action=$action",
                onProgress = onProgress,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                stage = "Λήψη κατηγοριών…"
            ))
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                out[c.optString("category_id")] = c.optString("category_name")
            }
        } catch (error: Exception) {
            ProviderCancellation.rethrow(error, "Xtream category request cancelled")
        }
        return out
    }

    /**
     * ΓΡΗΓΟΡΟ τεστ: μόνο το player_api (user_info) + οι κατηγορίες.
     * Πριν κατέβαζε ΟΛΑ τα live κανάλια (μπορεί 30.000+) για να πει «OK».
     */
    fun test(server: String, user: String, pass: String): Pair<Boolean, String> {
        return try {
            val b = base(server)
            val info = JSONObject(fetchX("$b/player_api.php?${q(user, pass)}"))
            val ui = info.optJSONObject("user_info")
            if ((ui?.optInt("auth", 0) ?: 0) != 1)
                return false to "Λάθος στοιχεία ή server."
            val status = ui?.optString("status") ?: ""
            val exp = ui?.optString("exp_date") ?: ""
            val expTxt = exp.toLongOrNull()?.let {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(it * 1000))
            }
            val cats = try {
                categories(b, user, pass, "get_live_categories").size
            } catch (error: Exception) {
                ProviderCancellation.rethrow(error, "Xtream connection test cancelled")
                0
            }
            val parts = ArrayList<String>()
            if (status.isNotBlank()) parts.add(status)
            if (cats > 0) parts.add("$cats κατηγορίες")
            if (expTxt != null) parts.add("λήγει $expTxt")
            true to ("Σύνδεση OK" + if (parts.isEmpty()) "" else " — " + parts.joinToString(", "))
        } catch (error: Exception) {
            ProviderCancellation.rethrow(error, "Xtream connection test cancelled")
            false to (error.message ?: "άγνωστο σφάλμα")
        }
    }

    fun liveCategories(
        server: String, user: String, pass: String,
        onProgress: SourceProgressCallback? = null
    ): List<Pair<String, String>> {
        val b = base(server); auth(b, user, pass, onProgress)
        val cats = categories(b, user, pass, "get_live_categories", onProgress, 10, 95)
        onProgress?.invoke(100, "Οι κατηγορίες Live είναι έτοιμες")
        return cats.map { it.key to it.value }
    }

    fun live(
        server: String, user: String, pass: String, output: String,
        categoryIds: List<String>? = null,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> {
        val b = base(server); auth(b, user, pass, onProgress)
        val cats = categories(b, user, pass, "get_live_categories", onProgress, 10, 22)
        val requests: List<String?> = categoryIds?.filter { it.isNotBlank() }?.distinct()?.map { it }
            ?.takeIf { it.isNotEmpty() } ?: listOf(null)
        val out = ArrayList<Channel>()
        val seen = HashSet<String>()
        requests.forEachIndexed { requestIndex, categoryId ->
            val start = 22 + requestIndex * 60 / requests.size
            val end = 22 + (requestIndex + 1) * 60 / requests.size
            val categoryQuery = categoryId?.let { "&category_id=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
            val arr = JSONArray(fetchX(
                "$b/player_api.php?${q(user, pass)}&action=get_live_streams$categoryQuery",
                onProgress, start, end,
                if (categoryId == null) "Λήψη καναλιών Live…"
                else "Λήψη Live · κατηγορία ${requestIndex + 1}/${requests.size}"
            ))
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val sid = item.opt("stream_id")?.toString() ?: continue
                val catId = item.optString("category_id")
                if (categoryId != null && catId != categoryId) continue
                if (!seen.add(sid)) continue
                out.add(
                    Channel(
                        name = item.optString("name", "Άγνωστο"),
                        group = cats[catId] ?: "Live",
                        logo = item.optString("stream_icon"),
                        tvgId = item.optString("epg_channel_id"),
                        streamId = sid,
                        kind = "live",
                        url = "$b/live/${pathEnc(user)}/${pathEnc(pass)}/$sid.$output"
                    )
                )
                if (out.size % 200 == 0) onPartial?.invoke(out)
            }
            if (out.isNotEmpty()) onPartial?.invoke(out)
        }
        onProgress?.invoke(100, "Τα κανάλια Live είναι έτοιμα")
        return out
    }

    fun vodCategories(
        server: String, user: String, pass: String,
        onProgress: SourceProgressCallback? = null
    ): List<Pair<String, String>> {
        val b = base(server); auth(b, user, pass, onProgress)
        val result = categories(b, user, pass, "get_vod_categories", onProgress, 10, 95).map { it.key to it.value }
        onProgress?.invoke(100, "Οι κατηγορίες ταινιών είναι έτοιμες")
        return result
    }

    fun vod(
        server: String, user: String, pass: String,
        categoryIds: List<String>? = null,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> {
        val b = base(server); auth(b, user, pass, onProgress)
        val cats = categories(b, user, pass, "get_vod_categories", onProgress, 10, 22)
        val requests: List<String?> = categoryIds?.filter { it.isNotBlank() }?.distinct()?.map { it }
            ?.takeIf { it.isNotEmpty() } ?: listOf(null)
        val out = ArrayList<Channel>()
        val seen = HashSet<String>()
        requests.forEachIndexed { requestIndex, categoryId ->
            val start = 22 + requestIndex * 60 / requests.size
            val end = 22 + (requestIndex + 1) * 60 / requests.size
            val categoryQuery = categoryId?.let { "&category_id=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
            val arr = JSONArray(fetchX(
                "$b/player_api.php?${q(user, pass)}&action=get_vod_streams$categoryQuery",
                onProgress, start, end,
                if (categoryId == null) "Λήψη ταινιών…"
                else "Λήψη ταινιών · κατηγορία ${requestIndex + 1}/${requests.size}"
            ))
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val sid = item.opt("stream_id")?.toString() ?: continue
                val catId = item.optString("category_id")
                if (categoryId != null && catId != categoryId) continue
                if (!seen.add(sid)) continue
                val ext = item.optString("container_extension", "mp4")
                out.add(
                    Channel(
                        name = item.optString("name", "Ταινία"),
                        group = cats[catId] ?: "Ταινίες",
                        logo = item.optString("stream_icon"),
                        streamId = sid,
                        kind = "vod",
                        url = "$b/movie/${pathEnc(user)}/${pathEnc(pass)}/$sid.$ext"
                    )
                )
                if (out.size % 200 == 0) onPartial?.invoke(out)
            }
            if (out.isNotEmpty()) onPartial?.invoke(out)
        }
        onProgress?.invoke(100, "Οι ταινίες είναι έτοιμες")
        return out
    }

    fun seriesCategories(
        server: String, user: String, pass: String,
        onProgress: SourceProgressCallback? = null
    ): List<Pair<String, String>> {
        val b = base(server); auth(b, user, pass, onProgress)
        val result = categories(b, user, pass, "get_series_categories", onProgress, 10, 95).map { it.key to it.value }
        onProgress?.invoke(100, "Οι κατηγορίες σειρών είναι έτοιμες")
        return result
    }

    fun seriesList(
        server: String, user: String, pass: String,
        categoryIds: List<String>? = null,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> {
        val b = base(server); auth(b, user, pass, onProgress)
        val cats = categories(b, user, pass, "get_series_categories", onProgress, 10, 22)
        val requests: List<String?> = categoryIds?.filter { it.isNotBlank() }?.distinct()?.map { it }
            ?.takeIf { it.isNotEmpty() } ?: listOf(null)
        val out = ArrayList<Channel>()
        val seen = HashSet<String>()
        requests.forEachIndexed { requestIndex, categoryId ->
            val start = 22 + requestIndex * 60 / requests.size
            val end = 22 + (requestIndex + 1) * 60 / requests.size
            val categoryQuery = categoryId?.let { "&category_id=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
            val arr = JSONArray(fetchX(
                "$b/player_api.php?${q(user, pass)}&action=get_series$categoryQuery",
                onProgress, start, end,
                if (categoryId == null) "Λήψη σειρών…"
                else "Λήψη σειρών · κατηγορία ${requestIndex + 1}/${requests.size}"
            ))
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val sid = item.opt("series_id")?.toString() ?: continue
                val catId = item.optString("category_id")
                if (categoryId != null && catId != categoryId) continue
                if (!seen.add(sid)) continue
                out.add(
                    Channel(
                        name = item.optString("name", "Σειρά"),
                        group = cats[catId] ?: "Σειρές",
                        logo = item.optString("cover"),
                        seriesId = sid,
                        kind = "series"
                    )
                )
                if (out.size % 200 == 0) onPartial?.invoke(out)
            }
            if (out.isNotEmpty()) onPartial?.invoke(out)
        }
        onProgress?.invoke(100, "Οι σειρές είναι έτοιμες")
        return out
    }

    fun seriesEpisodes(server: String, user: String, pass: String, seriesId: String): List<Pair<String, List<Channel>>> {
        val b = base(server)
        val data = JSONObject(
            fetchX("$b/player_api.php?${q(user, pass)}&action=get_series_info&series_id=$seriesId")
        )
        val episodes = data.optJSONObject("episodes") ?: return emptyList()
        val seasons = ArrayList<Pair<String, List<Channel>>>()
        val keys = episodes.keys().asSequence().toList()
            .sortedBy { it.toIntOrNull() ?: 0 }
        for (season in keys) {
            val eps = ArrayList<Channel>()
            val arr = episodes.optJSONArray(season) ?: JSONArray()
            for (i in 0 until arr.length()) {
                val ep = arr.getJSONObject(i)
                val eid = ep.opt("id")?.toString() ?: continue
                val ext = ep.optString("container_extension", "mp4")
                eps.add(
                    Channel(
                        name = ep.optString("title", "Επεισόδιο ${ep.optString("episode_num")}"),
                        group = "Season $season",
                        kind = "series_ep",
                        streamId = eid,
                        seriesId = seriesId,
                        url = "$b/series/${pathEnc(user)}/${pathEnc(pass)}/$eid.$ext"
                    )
                )
            }
            seasons.add("Season $season" to eps)
        }
        return seasons
    }

    fun vodInfo(server: String, user: String, pass: String, streamId: String): Map<String, String> {
        val b = base(server)
        return try {
            val data = JSONObject(fetchX("$b/player_api.php?${q(user, pass)}&action=get_vod_info&vod_id=$streamId"))
            val info = data.optJSONObject("info") ?: JSONObject()
            mapOf(
                "plot" to info.optString("plot").ifEmpty { info.optString("description") },
                "cast" to info.optString("cast").ifEmpty { info.optString("actors") },
                "director" to info.optString("director"),
                "genre" to info.optString("genre"),
                "year" to (info.optString("releasedate").take(4).ifEmpty { info.optString("year") }),
                "duration" to info.optString("duration")
            )
        } catch (error: Exception) {
            ProviderCancellation.rethrow(error, "Xtream metadata request cancelled")
            emptyMap()
        }
    }

    fun shortEpg(server: String, user: String, pass: String, streamId: String, limit: Int = 8): List<EpgEntry> {
        val b = base(server)
        val data = JSONObject(
            fetchX("$b/player_api.php?${q(user, pass)}&action=get_short_epg&stream_id=$streamId&limit=$limit")
        )
        val arr = data.optJSONArray("epg_listings") ?: return emptyList()
        val out = ArrayList<EpgEntry>()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            out.add(
                EpgEntry(
                    title = b64OrPlain(e.optString("title")),
                    desc = b64OrPlain(e.optString("description")),
                    start = e.optString("start"),
                    end = e.optString("end")
                )
            )
        }
        return out
    }

    private fun b64OrPlain(s: String): String {
        if (s.isEmpty()) return ""
        return try {
            val decoded = String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8)
            val printable = decoded.count { it.isLetterOrDigit() || it.isWhitespace() || it in " .,:!?-–—()" }
            if (decoded.isNotEmpty() && printable.toDouble() / decoded.length > 0.6) decoded else s
        } catch (e: Exception) {
            s
        }
    }
}
