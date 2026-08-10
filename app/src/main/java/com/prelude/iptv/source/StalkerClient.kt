package com.prelude.iptv.source

import android.util.Log
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgEntry
import com.prelude.iptv.data.SourceProgressCallback
import com.prelude.iptv.data.SourcePartialCallback
import com.prelude.iptv.net.Http
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest

/** Ελάχιστος Stalker/Ministra client με MAC authentication + MAG emulation. */
class StalkerClient(portal: String, mac: String, userAgent: String = "") {

    private val mac: String = normalizeMac(mac)
    private val userAgent: String = userAgent.trim()
    private val portalInput: String
    private val hostRoot: String
    private var base: String? = null
    private var token: String? = null
    private var activeUa: String? = null
    private val sn: String
    private val did32: String
    var lastDiag: String = ""; private set
    @Volatile private var requestCancelled: Boolean = false

    fun cancelPendingRequests() {
        requestCancelled = true
    }

    private fun ensureRequestActive() {
        if (requestCancelled) throw java.util.concurrent.CancellationException("Stalker request cancelled")
    }

    private fun rethrowIfCancelled(error: Exception) {
        if (requestCancelled || error is java.util.concurrent.CancellationException ||
            error is java.io.InterruptedIOException ||
            error.message?.contains("canceled", ignoreCase = true) == true
        ) throw java.util.concurrent.CancellationException("Stalker request cancelled")
    }

    private fun providerText(url: String, headers: Map<String, String>): String {
        ensureRequestActive()
        return Http.providerGet(url, headers).also { ensureRequestActive() }
    }

    private fun providerFull(url: String, headers: Map<String, String>): Http.Resp {
        ensureRequestActive()
        return Http.providerGetFull(url, headers).also { ensureRequestActive() }
    }

    private fun providerProgress(
        url: String,
        headers: Map<String, String>,
        onProgress: ((Long, Long?) -> Unit)?
    ): String {
        ensureRequestActive()
        return Http.providerGetWithProgress(url, headers, onProgress).also { ensureRequestActive() }
    }

    init {
        var raw = portal.trim().trimEnd('/')
        if (!raw.startsWith("http")) raw = "http://$raw"
        portalInput = raw
        var root = raw.replace(Regex("""/[^/]*\.php$"""), "")
        root = root.replace(Regex("""/c(/.*)?$"""), "")
        hostRoot = root
        sn = md5(this.mac).uppercase().substring(0, 13)
        did32 = sha256(this.mac).substring(0, 32)   // όπως ο working Node player
    }

    companion object {
        private val PATHS = listOf(
            "/server/load.php", "/stalker_portal/server/load.php", "/c/server/load.php",
            "/portal.php", "/c/portal.php", "/stalker_portal/portal.php"
        )

        /**
         * ΕΝΑΣ κοινός executor για το παράλληλο paging. Πριν φτιαχνόταν νέο
         * pool ΑΝΑ ΚΑΤΗΓΟΡΙΑ (50 κατηγορίες = 50 pools = άσκοπο thread churn).
         * Daemon threads: δεν κρατάνε τη διεργασία ζωντανή στο κλείσιμο.
         */
        private val pagePool: java.util.concurrent.ExecutorService =
            java.util.concurrent.Executors.newFixedThreadPool(6) { r ->
                Thread(r, "stalker-page").apply { isDaemon = true }
            }

        /**
         * ΞΕΧΩΡΙΣΤΟ pool για τις ΚΑΤΗΓΟΡΙΕΣ — και πρέπει να είναι ξεχωριστό.
         *
         * Κάθε εργασία κατηγορίας καλεί το [fetchAllPages], που με τη σειρά του
         * υποβάλλει εργασίες σελίδων ΚΑΙ ΜΠΛΟΚΑΡΕΙ περιμένοντάς τες. Αν οι δύο
         * μοιράζονταν pool, όλα τα νήματα θα κρατιόνταν από κατηγορίες που
         * περιμένουν σελίδες που δεν έχουν νήμα να τρέξουν: κλασικό deadlock.
         *
         * Τρία και όχι περισσότερα: μαζί με τις 6 σελίδες δίνει έως 9 ταυτόχρονα
         * αιτήματα. Τα portals είναι συχνά μικρά μηχανήματα και πάνω από αυτό
         * αρχίζουν να απαντούν με 429/500 — που θα φαινόταν στον χρήστη ως
         * «χάθηκαν κατηγορίες», όχι ως υπερφόρτωση.
         */
        private val categoryPool: java.util.concurrent.ExecutorService =
            java.util.concurrent.Executors.newFixedThreadPool(3) { r ->
                Thread(r, "stalker-category").apply { isDaemon = true }
            }

        fun normalizeMac(mac: String): String {
            val hex = mac.filter { it.isLetterOrDigit() }.uppercase()
            return if (hex.length == 12)
                (0 until 12 step 2).joinToString(":") { hex.substring(it, it + 2) }
            else mac.trim().uppercase().replace("-", ":")
        }

        private fun md5(s: String) = hashHex(s, "MD5")
        private fun sha256(s: String) = hashHex(s, "SHA-256")
        private fun hashHex(s: String, algo: String): String {
            val d = MessageDigest.getInstance(algo).digest(s.toByteArray())
            return d.joinToString("") { "%02x".format(it) }
        }
    }

    private fun headers(ua: String, withToken: Boolean = true): Map<String, String> {
        val h = linkedMapOf(
            "User-Agent" to ua,
            "Accept" to "*/*",
            "Accept-Encoding" to "identity",
            "Referer" to "$hostRoot/",
            "X-User-Agent" to "Model: MAG250; Link: WiFi",
            "Cookie" to "mac=${URLEncoder.encode(mac, "UTF-8")}; stb_lang=en; timezone=Europe%2FAthens"
        )
        if (withToken && token != null) h["Authorization"] = "Bearer $token"
        return h
    }

    private fun activeHeaders(): Map<String, String> {
        val ua = activeUa ?: throw java.io.IOException("Stalker session is not connected")
        return headers(ua)
    }

    private fun endpoints(): List<String> {
        val eps = ArrayList<String>()
        if (portalInput.endsWith(".php")) eps.add(portalInput)
        PATHS.forEach { eps.add(hostRoot + it) }
        return eps.distinct()
    }

    private fun uaList(): List<String> =
        if (userAgent.isNotEmpty())
            listOf(userAgent) + Http.STB_UAS.filter { it != userAgent }
        else Http.STB_UAS

    fun connect() {
        val diag = ArrayList<String>()
        var cloudflare = false
        for (ua in uaList()) {
            for (ep in endpoints()) {
                val url = "$ep?type=stb&action=handshake&prehash=0&token=&JsHttpRequest=1-xml"
                try {
                    val (code, body) = providerFull(url, headers(ua, withToken = false))
                    if (code == 200) {
                        val tok = JSONObject(body).optJSONObject("js")?.optString("token")
                        if (!tok.isNullOrEmpty()) {
                            base = ep; token = tok; activeUa = ua
                            getProfile()
                            return
                        }
                        diag.add("$ep → 200 χωρίς token")
                    } else {
                        if (Http.looksLikeCloudflare(body)) cloudflare = true
                        diag.add("$ep → HTTP $code" + if (Http.looksLikeCloudflare(body)) " (Cloudflare)" else "")
                    }
                } catch (e: Exception) {
                    rethrowIfCancelled(e)
                    diag.add("$ep → ${e.javaClass.simpleName}")
                }
            }
        }
        lastDiag = diag.distinct().joinToString(" | ")
        val hint = when {
            cloudflare -> "\n\nΤο portal είναι πίσω από Cloudflare. Ζήτα M3U/Xtream URL από τον πάροχο."
            diag.any { it.contains("403") } ->
                "\n\nΌλα 403 (όχι Cloudflare). Δοκίμασε το User-Agent της TV, ή έλεγξε το Portal URL."
            else -> "\n\nΈλεγξε Portal URL/port και MAC."
        }
        throw RuntimeException("Δεν έγινε handshake.\n" + diag.distinct().take(6).joinToString(" | ") + hint)
    }

    private fun getProfile() {
        val ver = "ImageDescription:0.2.18-r23-pub-250;ImageDate:Fri Jan 19 18:00:00 UTC 2024;" +
            "PORTAL version:5.6.1;API Version:JS API version:340;STB API version:146;" +
            "Player Engine version:0x58c"
        val params = linkedMapOf(
            "type" to "stb", "action" to "get_profile", "hd" to "1", "ver" to ver,
            // ΤΟ sn υπολογιζόταν από το MAC αλλά στελνόταν hardcoded "test123" —
            // portals που δένουν τη συνδρομή σε serial φέρονταν απρόβλεπτα.
            "num_banks" to "2", "sn" to sn, "stb_type" to "MAG250",
            "image_version" to "218", "video_out" to "hdmi",
            "device_id" to did32, "device_id2" to did32, "signature" to "",
            "auth_second_step" to "1", "hw_version" to "1.7-BD-00",
            "not_valid_token" to "0", "JsHttpRequest" to "1-xml"
        )
        val qs = params.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
        runCatching { providerText("$base?$qs", activeHeaders()) }
    }

    /** Λίστα κατηγοριών (id, τίτλος) — για επιλογή πριν τη φόρτωση. */
    fun getGenres(): List<Pair<String, String>> {
        if (base == null) connect()
        return try {
            val g = JSONObject(providerText("$base?type=itv&action=get_genres&JsHttpRequest=1-xml", activeHeaders()))
            val arr = g.optJSONArray("js")
            val out = ArrayList<Pair<String, String>>()
            if (arr != null) for (i in 0 until arr.length()) {
                val it = arr.getJSONObject(i)
                val id = it.optString("id")
                val title = it.optString("title")
                if (id.isNotEmpty() && id != "*" && title.isNotEmpty()) out.add(id to title)
            }
            out
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            emptyList()
        }
    }

    /** Κανάλια. Αν genreIds != null, φορτώνει ΜΟΝΟ αυτές τις κατηγορίες. */
    fun getChannels(
        genreIds: List<String>? = null,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> {
        if (base == null) connect()
        val genreList = getGenres()
        val genres = genreList.toMap()
        val ids = genreIds ?: genreList.map { it.first }
        val result = ArrayList<Channel>()
        val seen = HashSet<String>()

        fun append(rows: List<JSONObject>, fallbackGenreId: String? = null) {
            rows.forEach { ch ->
                val stableKey = ch.optString("id").ifBlank {
                    ch.optString("cmd").ifBlank { ch.optString("name") }
                }
                if (!seen.add(stableKey)) return@forEach
                val genreId = ch.optString("tv_genre_id").ifBlank { fallbackGenreId.orEmpty() }
                result.add(Channel(
                    name = ch.optString("name", "Άγνωστο"),
                    group = genres[genreId] ?: "Κανάλια",
                    logo = ch.optString("logo"),
                    tvgId = ch.optString("xmltv_id"),
                    chId = ch.optString("id"),
                    cmd = ch.optString("cmd").ifEmpty {
                        ch.optJSONArray("cmds")?.optJSONObject(0)?.optString("url") ?: ""
                    }
                ))
                if (result.size % 100 == 0) onPartial?.invoke(result)
            }
        }

        // Οι κατηγορίες κατεβαίνουν ΠΑΡΑΛΛΗΛΑ και παραδίδονται με τη σειρά. Μόλις
        // ολοκληρωθεί μία, δημοσιεύεται ενώ οι επόμενες συνεχίζουν να κατεβαίνουν.
        forEachCategoryParallel(
            ids = ids.filter { it.isNotBlank() && it != "*" },
            urlFor = { gid, page ->
                "$base?type=itv&action=get_ordered_list&genre=$gid&fav=0&sortby=number&p=$page&JsHttpRequest=1-xml"
            }
        ) { gid, rows, done, total ->
            append(rows, gid)
            val overall = 12 + ((done.toDouble() / total.coerceAtLeast(1)) * 73.0).toInt()
            onProgress?.invoke(overall.coerceIn(12, 85), "Λήψη Live · κατηγορία $done/$total")
            if (result.isNotEmpty()) onPartial?.invoke(result)
        }

        // Fallback for portals that do not expose ordered category lists.
        if (result.isEmpty()) {
            runCatching {
                onProgress?.invoke(20, "Λήψη όλων των καναλιών…")
                val response = JSONObject(providerProgress(
                    "$base?type=itv&action=get_all_channels&JsHttpRequest=1-xml",
                    activeHeaders()
                ) { read, total ->
                    val pct = total?.takeIf { it > 0L }?.let { 20 + ((read.toDouble() / it) * 65.0).toInt() }
                    onProgress?.invoke(pct?.coerceIn(20, 85), "Λήψη όλων των καναλιών…")
                })
                val js = response.opt("js")
                val data = when (js) {
                    is org.json.JSONObject -> js.optJSONArray("data")
                    is org.json.JSONArray -> js
                    else -> null
                }
                if (data != null) {
                    val rows = (0 until data.length()).map { data.getJSONObject(it) }
                    append(rows)
                    if (result.isNotEmpty()) onPartial?.invoke(result)
                }
            }
        }

        onProgress?.invoke(100, "Τα κανάλια Live είναι έτοιμα")
        return result
    }

    /**
     * ΓΡΗΓΟΡΟ τεστ: handshake + λίστα κατηγοριών (μικρή).
     * Πριν κατέβαζε ΟΛΑ τα κανάλια του portal για να πει «OK».
     */
    fun testConnection(): Pair<Boolean, String> {
        return try {
            connect()
            val g = try {
                getGenres().size
            } catch (e: Exception) {
                rethrowIfCancelled(e)
                0
            }
            true to ("Σύνδεση OK" + if (g > 0) " — $g κατηγορίες" else "")
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            false to (e.message ?: "άγνωστο σφάλμα")
        }
    }

    /** Κατηγορίες VOD (ταινίες) ή Series από MAC portal. */
    fun getVodCategories(): List<Pair<String, String>> = getCategories("vod")
    fun getSeriesCategories(): List<Pair<String, String>> = getCategories("series")

    private fun getCategories(type: String): List<Pair<String, String>> {
        if (base == null) connect()
        return try {
            val g = JSONObject(providerText("$base?type=$type&action=get_categories&JsHttpRequest=1-xml", activeHeaders()))
            val arr = g.optJSONArray("js")
            val out = ArrayList<Pair<String, String>>()
            if (arr != null) for (i in 0 until arr.length()) {
                val it = arr.getJSONObject(i)
                val id = it.optString("id"); val title = it.optString("title")
                if (id.isNotEmpty() && id != "*" && title.isNotEmpty()) out.add(id to title)
            }
            out
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            emptyList()
        }
    }

    fun getVodChannels(
        catIds: List<String>?,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> = getVodLike("vod", catIds, onProgress, onPartial)

    fun getSeriesChannels(
        catIds: List<String>?,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> = getVodLike("series", catIds, onProgress, onPartial)

    /**
     * Πραγματικά επεισόδια μιας σειράς, ΟΛΩΝ των σεζόν.
     *
     * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΗ ΣΥΝΑΡΤΗΣΗ: το `get_ordered_list` της κατηγορίας (βλ.
     * [getVodLike]) ΔΕΝ κουβαλάει ποτέ επεισόδια σε αυτό το API — επιβεβαιωμένο
     * σε πραγματικό portal: κάθε γραμμή σειράς έχει `"series":[]` και `"cmd":""`.
     * Το Ministra API θέλει ξεχωριστό αίτημα με `movie_id`.
     *
     * ΜΙΑ ΚΛΗΣΗ ΑΡΚΕΙ — επιβεβαιωμένο σε πραγματικό portal, `season_id=0`:
     * κάθε γραμμή της απάντησης είναι μια ΣΕΖΟΝ, όχι επεισόδιο. Το δικό της
     * `cmd` ΔΕΝ είναι έτοιμο stream (είναι base64 περιγραφέας της σεζόν,
     * `"has_files":0`) — είναι το ΚΟΙΝΟ cmd που παίρνουν ΟΛΑ τα επεισόδια της
     * σεζόν στο create_link, με τον αριθμό επεισοδίου στην παράμετρο `series=`
     * (δες [resolve]). Ο πίνακας `"series":[1,2,3,...]` ΤΗΣ ΓΡΑΜΜΗΣ ΣΕΖΟΝ (όχι
     * της αρχικής γραμμής σειράς, εκείνη είναι πάντα κενή) λέει ποιοι αριθμοί
     * επεισοδίων υπάρχουν. Δεν χρειάζεται δεύτερο αίτημα ανά σεζόν.
     */
    fun seriesEpisodes(seriesId: String): List<Pair<String, List<Channel>>> {
        if (base == null) connect()
        return try {
            val url = "$base?type=series&action=get_ordered_list&movie_id=" +
                "${URLEncoder.encode(seriesId, "UTF-8")}&category=*&season_id=0&episode_id=0&JsHttpRequest=1-xml"
            val raw = providerText(url, activeHeaders())
            Log.d("SeriesLoad", "seriesEpisodes raw response (seriesId=$seriesId): $raw")
            val response = JSONObject(raw)
            val js = response.opt("js")
            val data = when (js) {
                is org.json.JSONArray -> js
                is JSONObject -> js.optJSONArray("data")
                else -> null
            } ?: return emptyList()

            val seasons = ArrayList<Pair<String, List<Channel>>>()
            // (κανάλι, seasonRowId, episodeNum) ώστε να ζητηθεί η περιγραφή
            // κάθε επεισοδίου παράλληλα, ΑΦΟΥ χτιστεί όλη η λίστα — το request
            // δεν πρέπει να καθυστερήσει την εμφάνιση της λίστας επεισοδίων.
            val pendingDescriptions = ArrayList<Triple<Channel, String, String>>()
            for (i in 0 until data.length()) {
                val row = data.getJSONObject(i)
                val label = row.optString("name").takeIf { it.isNotBlank() } ?: "Season ${i + 1}"
                val seasonCmd = row.optString("cmd")
                if (seasonCmd.isBlank()) continue
                val seasonRowId = row.optString("id").ifBlank { "0" }
                val episodeNumbers = row.optJSONArray("series")
                val episodes = if (episodeNumbers != null && episodeNumbers.length() > 0) {
                    // Φάκελος σεζόν: ένα cmd, πολλά επεισόδια μέσω series=.
                    (0 until episodeNumbers.length()).mapNotNull { epIndex ->
                        val num = episodeNumbers.optString(epIndex).trim()
                        if (num.isEmpty()) null
                        else buildEpisodeChannel(seriesId, seasonCmd, num, label, row)
                    }
                } else {
                    // Χωρίς πίνακα επεισοδίων: η ίδια η γραμμή είναι ήδη ένα
                    // μεμονωμένο επεισόδιο (σειρά χωρίς φακέλους σεζόν).
                    listOf(buildEpisodeChannel(seriesId, seasonCmd, (i + 1).toString(), label, row))
                }
                if (episodes.isNotEmpty()) {
                    seasons += label to episodes
                    episodes.forEach { pendingDescriptions += Triple(it, seasonRowId, it.chId) }
                }
            }

            // Η ίδια η λίστα δεν κουβαλάει περιγραφή ανά επεισόδιο (μόνο τον
            // αριθμό) — χρειάζεται ένα ακόμη αίτημα ΑΝΑ επεισόδιο. Παράλληλα
            // στο ίδιο μικρό pool με τις σελίδες κατηγοριών, ώστε μια σειρά με
            // πολλά επεισόδια να μη γίνεται πολλά δευτερόλεπτα αναμονής.
            val descriptions = pendingDescriptions.map { (channel, seasonRowId, episodeNum) ->
                channel to categoryPool.submit<String> {
                    fetchEpisodeDescription(seriesId, seasonRowId, episodeNum)
                }
            }
            val withPlot = descriptions.associate { (channel, future) ->
                val plot = try {
                    future.get()
                } catch (e: Exception) {
                    ""
                }
                channel to plot
            }
            seasons.map { (label, episodes) ->
                label to episodes.map { ch -> ch.copy(plot = withPlot[ch] ?: ch.plot) }
            }
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            emptyList()
        }
    }

    /**
     * Περιγραφή ενός συγκεκριμένου επεισοδίου. ΔΕΝ έχει επιβεβαιωθεί σε
     * πραγματικό portal το ακριβές σχήμα της απάντησης σε αυτό το βάθος
     * (`episode_id=<αριθμός>`) — καταγράφεται ολόκληρη η ωμή απάντηση ώστε αν
     * η υπόθεση για το πεδίο `"description"` είναι λάθος να φανεί αμέσως.
     */
    private fun fetchEpisodeDescription(seriesId: String, seasonRowId: String, episodeNum: String): String {
        return try {
            val url = "$base?type=series&action=get_ordered_list&movie_id=" +
                "${URLEncoder.encode(seriesId, "UTF-8")}&category=*&season_id=" +
                "${URLEncoder.encode(seasonRowId, "UTF-8")}&episode_id=" +
                "${URLEncoder.encode(episodeNum, "UTF-8")}&JsHttpRequest=1-xml"
            val raw = providerText(url, activeHeaders())
            Log.d(
                "SeriesLoad",
                "seriesEpisodes episode detail (seriesId=$seriesId, season=$seasonRowId, episode=$episodeNum): $raw",
            )
            val response = JSONObject(raw)
            val js = response.opt("js")
            val row = when (js) {
                is JSONObject -> js.optJSONArray("data")?.optJSONObject(0) ?: js
                is org.json.JSONArray -> js.optJSONObject(0)
                else -> null
            }
            row?.optString("description").orEmpty()
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            ""
        }
    }

    private fun buildEpisodeChannel(
        seriesId: String,
        cmd: String,
        episodeNum: String,
        seasonLabel: String,
        row: JSONObject,
    ): Channel = Channel(
        name = "Επεισόδιο $episodeNum",
        group = seasonLabel,
        logo = row.optString("screenshot_uri").ifEmpty { row.optString("logo") },
        cmd = cmd,
        // ΠΡΟΣΟΧΗ, δύο διαφορετικά πράγματα: το streamId ταυτοποιεί το
        // επεισόδιο μόνιμα (ιστορικό/αγαπημένα το χρησιμοποιούν ως κλειδί —
        // δες PlaybackHistoryStore.historyMatchKey) και ΠΡΕΠΕΙ να είναι
        // μοναδικό σε ΟΛΗ τη σειρά, όχι μόνο μέσα σε μία σεζόν — ο αριθμός
        // επεισοδίου επαναλαμβάνεται σε κάθε σεζόν. Ο αριθμός επεισοδίου
        // μένει στο chId (αχρησιμοποίητο για series_ep παντού αλλού) — αυτόν
        // χρειάζεται το create_link ως series= στο
        // resolve()/Repository.playableUrl/RelayHub.
        streamId = "${row.optString("id")}:$episodeNum",
        chId = episodeNum,
        kind = "series_ep",
        seriesId = seriesId,
        year = row.optString("year"),
    )

    /**
     * Κατεβάζει ΟΛΕΣ τις σελίδες μιας λίστας του portal.
     *
     * ΓΙΑΤΙ: το Stalker σερβίρει ~14 items/σελίδα. Μια κατηγορία 3.000 ταινιών
     * ήταν ~215 ΣΕΙΡΙΑΚΑ round-trips — λεπτά αναμονής. Η σελίδα 1 φέρνει το
     * total_items, οπότε ξέρουμε πόσες σελίδες υπάρχουν και κατεβάζουμε τις
     * υπόλοιπες ΠΑΡΑΛΛΗΛΑ σε 6 νήματα (≈6x πιο γρήγορα, χωρίς να «βομβαρδίζουμε»
     * τον server). Η σειρά των σελίδων διατηρείται (futures με τη σειρά τους).
     */
    private fun fetchAllPages(urlFor: (Int) -> String): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        val first = try {
            JSONObject(providerText(urlFor(1), activeHeaders()))
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            return out
        }
        val js = first.optJSONObject("js") ?: return out
        val data = js.optJSONArray("data") ?: return out
        if (data.length() == 0) return out
        for (i in 0 until data.length()) out.add(data.getJSONObject(i))

        val total = js.optInt("total_items", 0)
        val per = js.optInt("max_page_items", data.length()).coerceAtLeast(1)

        if (total <= 0) {
            // δεν δηλώνει σύνολο (σπάνιο): σειριακά μέχρι κενή σελίδα, με δικλείδα
            var p = 2
            while (p <= 500) {
                ensureRequestActive()
                val arr = try {
                    JSONObject(providerText(urlFor(p), activeHeaders()))
                        .optJSONObject("js")?.optJSONArray("data")
                } catch (e: Exception) {
                    rethrowIfCancelled(e)
                    null
                } ?: break
                if (arr.length() == 0) break
                for (i in 0 until arr.length()) out.add(arr.getJSONObject(i))
                p++
            }
            return out
        }

        val pages = ((total + per - 1) / per).coerceAtMost(500)
        if (pages <= 1) return out
        val futures = (2..pages).map { p ->
            pagePool.submit<List<JSONObject>> {
                try {
                    ensureRequestActive()
                    val arr = JSONObject(providerText(urlFor(p), activeHeaders()))
                        .optJSONObject("js")?.optJSONArray("data")
                    if (arr == null) emptyList()
                    else (0 until arr.length()).map { arr.getJSONObject(it) }
                } catch (e: Exception) {
            rethrowIfCancelled(e)
            emptyList()
        }
            }
        }
        futures.forEach { future ->
            ensureRequestActive()
            out.addAll(future.get())
        }
        return out
    }

    /**
     * Κατεβάζει ΟΛΕΣ τις κατηγορίες παράλληλα, αλλά τις παραδίδει ΜΕ ΤΗ ΣΕΙΡΑ.
     *
     * ΓΙΑΤΙ ΥΠΑΡΧΕΙ: πριν, οι κατηγορίες κατέβαιναν η μία μετά την άλλη. Οι
     * σελίδες ΜΕΣΑ σε μια κατηγορία ήταν παράλληλες, αλλά μια κατηγορία με δύο
     * σελίδες χρησιμοποιούσε ένα νήμα από τα έξι — τα υπόλοιπα πέντε κάθονταν.
     * Με 60 κατηγορίες, η καθυστέρηση δικτύου πληρωνόταν 60 φορές στη σειρά.
     *
     * Η ΠΑΡΑΔΟΣΗ ΜΕΝΕΙ ΣΕΙΡΙΑΚΗ ΕΠΙΤΗΔΕΣ: ο καλών προσθέτει σε κοινή λίστα και
     * σε κοινό `HashSet` διπλοτύπων, που δεν είναι thread-safe. Επιπλέον, έτσι η
     * σειρά των κατηγοριών στην οθόνη είναι σταθερή και δεν αλλάζει από το ποιο
     * αίτημα έτυχε να γυρίσει πρώτο.
     */
    private fun forEachCategoryParallel(
        ids: List<String>,
        urlFor: (categoryId: String, page: Int) -> String,
        onCategoryDone: (categoryId: String, rows: List<JSONObject>, done: Int, total: Int) -> Unit
    ) {
        if (ids.isEmpty()) return
        val futures = ids.map { id ->
            id to categoryPool.submit<List<JSONObject>> {
                fetchAllPages(urlFor = { page -> urlFor(id, page) })
            }
        }
        try {
            futures.forEachIndexed { index, (id, future) ->
                ensureRequestActive()
                val rows = try {
                    future.get()
                } catch (wrapped: java.util.concurrent.ExecutionException) {
                    // Το ExecutionException κρύβει το πραγματικό σφάλμα. Το
                    // ξετυλίγουμε ώστε η ακύρωση να αναγνωρίζεται ως ακύρωση.
                    val cause = wrapped.cause
                    if (cause is Exception) rethrowIfCancelled(cause)
                    emptyList()
                }
                onCategoryDone(id, rows, index + 1, ids.size)
            }
        } catch (error: Throwable) {
            // Ακύρωση ή σφάλμα: μη αφήνεις τα υπόλοιπα αιτήματα να τρέχουν και να
            // καταναλώνουν δίκτυο για δεδομένα που κανείς δεν θα δει.
            futures.forEach { (_, future) -> future.cancel(true) }
            throw error
        }
    }

    private fun getVodLike(
        type: String,
        catIds: List<String>?,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> {
        if (base == null) connect()
        onProgress?.invoke(5, "Σύνδεση Stalker έτοιμη")
        val allCats = getCategories(type)
        val cats = allCats.toMap()
        val ids = catIds ?: allCats.map { it.first }
        onProgress?.invoke(12, "Βρέθηκαν ${ids.size} κατηγορίες")
        val fallbackGroup = if (type == "vod") "Ταινίες" else "Σειρές"
        val result = ArrayList<Channel>()
        val seen = HashSet<String>()

        fun append(rows: List<JSONObject>, fallbackCategoryId: String) {
            rows.forEach { ch ->
                val stableKey = ch.optString("id").ifBlank {
                    ch.optString("movie_id").ifBlank { ch.optString("series_id").ifBlank { ch.optString("name") } }
                }
                if (!seen.add(stableKey)) return@forEach
                val categoryId = ch.optString("category_id").ifBlank { fallbackCategoryId }
                val rawCmd = ch.optString("cmd")
                // Ίδια σειρά προτεραιότητας με το stableKey/streamId παραπάνω: σε
                // αυτό το portal το "series_id" είναι κενό στη γραμμή της σειράς —
                // το πραγματικό αναγνωριστικό είναι το "id". Όταν διαβάζαμε μόνο
                // "series_id", ο normalizer έπεφτε πάντα σε τοπικό (local:) hash
                // αντί για το πραγματικό id, οπότε το επόμενο fetch (λεπτομέρειες
                // σειράς) δεν έβρισκε ποτέ τα επεισόδια που μόλις χτίσαμε παρακάτω.
                val seriesId = if (type == "series") {
                    ch.optString("id").ifBlank { ch.optString("series_id") }
                } else ""
                result.add(Channel(
                    name = ch.optString("name", "Άγνωστο"),
                    group = cats[categoryId] ?: fallbackGroup,
                    logo = ch.optString("screenshot_uri").ifEmpty { ch.optString("logo") },
                    // Μια σειρά ΔΕΝ παίζεται η ίδια — μόνο τα επεισόδιά της. Το cmd
                    // μένει εδώ κενό ώστε ο normalizer να την αναγνωρίζει ως
                    // container (ίδια σύμβαση με Xtream/M3U), το πραγματικό cmd
                    // πάει στα επεισόδια παρακάτω.
                    cmd = if (type == "series") "" else rawCmd,
                    streamId = ch.optString("id").ifEmpty { ch.optString("movie_id") },
                    kind = if (type == "series") "series" else "vod",
                    seriesId = seriesId,
                    plot = ch.optString("description"),
                    cast = ch.optString("actors"),
                    director = ch.optString("director"),
                    genre = ch.optString("genres_str").ifEmpty { ch.optString("genre") },
                    year = ch.optString("year"),
                    duration = ch.optString("time").ifEmpty { ch.optString("duration") }
                ))
                // Επεισόδια: ΟΧΙ εδώ. Το get_ordered_list της κατηγορίας δεν
                // κουβαλάει ποτέ επεισόδια σε αυτό το API (επιβεβαιωμένο σε
                // πραγματικό portal: πάντα "cmd":"" και "series":[] σε αυτή τη
                // γραμμή) — χρειάζεται το ξεχωριστό αίτημα σε [seriesEpisodes].
                if (result.size % 100 == 0) onPartial?.invoke(result)
            }
        }

        val label = if (type == "vod") "ταινιών" else "σειρών"
        forEachCategoryParallel(
            ids = ids.filter { it.isNotBlank() },
            urlFor = { cid, page ->
                "$base?type=$type&action=get_ordered_list&category=$cid&p=$page&JsHttpRequest=1-xml"
            }
        ) { cid, rows, done, total ->
            append(rows, cid)
            val overall = 12 + ((done.toDouble() / total.coerceAtLeast(1)) * 87.0).toInt()
            onProgress?.invoke(overall.coerceIn(12, 99), "Λήψη $label · κατηγορία $done/$total")
            if (result.isNotEmpty()) onPartial?.invoke(result)
        }
        onProgress?.invoke(100, if (type == "vod") "Οι ταινίες είναι έτοιμες" else "Οι σειρές είναι έτοιμες")
        return result
    }

    /** playable URL. Πολλά portals δίνουν το URL μέσα στο cmd· αλλιώς create_link. */
    /**
     * Μετατρέπει ένα `cmd` του portal σε διεύθυνση που παίζει.
     *
     * @param vod ταινία ή επεισόδιο. **Αλλάζει ριζικά τη συμπεριφορά** — δες κάτω.
     *
     * ---
     *
     * ΤΟ ΣΦΑΛΜΑ ΠΟΥ ΔΙΟΡΘΩΝΕΙ ΤΟ `vod`:
     *
     * Η συνάρτηση ξεκινούσε με μια συντόμευση: «αν το `cmd` περιέχει ήδη http,
     * δώσ' το κατευθείαν». Για **ζωντανά** αυτό είναι σωστό — το `cmd` του itv
     * είναι συχνά ο τελικός σύνδεσμος και το `create_link` απλώς τον επιστρέφει.
     *
     * Για **ταινίες και επεισόδια είναι λάθος.** Εκεί το `cmd` περιέχει σύνδεσμο
     * με token που παρήχθη τη στιγμή που κατέβηκε ο ΚΑΤΑΛΟΓΟΣ. Ο σύνδεσμος
     * ενεργοποιείται μόνο όταν περάσει από `create_link`· χωρίς αυτό ο πάροχος
     * απαντά **HTTP 404** — δεν λέει «απαγορεύεται», λέει «δεν υπάρχει», και έτσι
     * μοιάζει με χαλασμένο αρχείο αντί για ανενεργό σύνδεσμο.
     *
     * Αυτό εξηγεί ακριβώς το σύμπτωμα: **τα ζωντανά έπαιζαν, οι ταινίες όχι**, στην
     * ίδια πηγή, με τον ίδιο διακομιστή, με κάθε μηχανή αναπαραγωγής.
     *
     * Η συντόμευση μένει για τα ζωντανά: εκεί γλιτώνει ένα round-trip σε κάθε
     * αλλαγή καναλιού, που είναι η πιο συχνή ενέργεια σε IPTV.
     */
    fun resolve(cmd: String, vod: Boolean = false, episodeNum: String = ""): String {
        // Το URL μέσα στο cmd. Χρήσιμο και ως έσχατη λύση παρακάτω.
        val direct = Regex("https?://\\S+").find(cmd)?.value ?: ""
        val usableDirect = direct.isNotEmpty() &&
            !direct.contains("localhost") &&
            !direct.contains("127.0.0.1")

        // ΖΩΝΤΑΝΑ: συντόμευση. ΤΑΙΝΙΕΣ: ποτέ — πρέπει να περάσει από create_link.
        if (!vod && usableDirect) return direct

        if (base == null) connect()

        // Η σειρά αντιστρέφεται για VOD: ο τύπος «vod» είναι ο σωστός για ταινίες
        // και επεισόδια, και δοκιμάζοντας πρώτα «itv» χάναμε ένα round-trip σε
        // κάθε ταινία — και σε κάποιους portal το λάθος type επιστρέφει σκουπίδια
        // αντί για κενό.
        val order = if (vod) listOf("vod", "itv") else listOf("itv", "vod")

        val attempts = LinkedHashMap<String, String>()
        for (type in order) {
            val raw = createLinkRaw(type, cmd, episodeNum)
            attempts[type] = raw
            val http = extractHttp(raw)
            if (http.isNotEmpty()) return http
        }

        // Το create_link δεν έδωσε τίποτα. Αν το cmd είχε URL, καλύτερα να
        // δοκιμάσει ο player και να αποτύχει, παρά να μη δοκιμάσει καθόλου.
        if (direct.isNotEmpty()) return direct

        throw RuntimeException(
            "create_link κενό (vod=$vod).\ncmd=${cmd.take(70)}\n" +
                attempts.entries.joinToString("\n") { "${it.key}=${it.value.take(160)}" }
        )
    }

    private fun createLinkRaw(type: String, cmd: String, episodeNum: String = ""): String {
        return try {
            // encodeURIComponent-style (space → %20, όχι +)
            val enc = URLEncoder.encode(cmd, "UTF-8").replace("+", "%20")
            // Ένα σειραϊκό cmd είναι κοινό για όλα τα επεισόδια· το portal ξέρει
            // ΠΟΙΟ επεισόδιο θέλουμε από αυτή την παράμετρο (κενή = ταινία/live,
            // όπως πριν).
            val seriesParam = if (episodeNum.isNotBlank()) URLEncoder.encode(episodeNum, "UTF-8") else ""
            val extra = "&series=$seriesParam&forced_storage=undefined&disable_ad=0&download=0&force_ch_link_check=0"
            val url = "$base?type=$type&action=create_link&cmd=$enc$extra&JsHttpRequest=1-xml"
            providerText(url, activeHeaders())
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            "ERR: ${e.message}"
        }
    }

    private fun extractHttp(raw: String): String {
        // πρώτα από το js.cmd, αλλιώς σκέτο regex στο raw
        val cmdField = try {
            JSONObject(raw).optJSONObject("js")?.optString("cmd") ?: ""
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            ""
        }
        Regex("https?://\\S+").find(cmdField)?.let { return it.value }
        Regex("https?://[^\"\\s\\\\]+").find(raw)?.let { return it.value }
        return ""
    }

    fun shortEpg(chId: String, limit: Int = 8): List<EpgEntry> {
        if (base == null) connect()
        val url = "$base?type=itv&action=get_short_epg&ch_id=$chId&size=$limit&JsHttpRequest=1-xml"
        val data = JSONObject(providerText(url, activeHeaders()))
        val arr = data.optJSONArray("js") ?: return emptyList()
        val out = ArrayList<EpgEntry>()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            out.add(
                EpgEntry(
                    title = e.optString("name").ifEmpty { e.optString("t_time") },
                    desc = e.optString("descr"),
                    start = e.optString("t_time"),
                    end = e.optString("t_time_to")
                )
            )
        }
        return out
    }
}
