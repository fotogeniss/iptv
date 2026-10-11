package com.prelude.iptv.source

import android.util.Log
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgEntry
import com.prelude.iptv.data.SourceCategoriesCallback
import com.prelude.iptv.data.SourceCategoryTotalsCallback
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

    private fun providerProgress(
        url: String,
        headers: Map<String, String>,
        onProgress: ((Long, Long?) -> Unit)?
    ): String {
        ensureRequestActive()
        return Http.providerGetWithProgress(url, headers, onProgress).also { ensureRequestActive() }
    }

    private fun providerFull(url: String, headers: Map<String, String>): Http.Resp {
        ensureRequestActive()
        return Http.providerGetFull(url, headers).also { ensureRequestActive() }
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
         *
         * ΕΞΙ. ΔΟΚΙΜΑΣΤΗΚΑΝ ΔΩΔΕΚΑ ΚΑΙ ΕΠΕΣΤΡΕΨΑΝ ΧΑΛΑΣΜΕΝΑ ΔΕΔΟΜΕΝΑ.
         *
         * Η αύξηση σε 12 δούλεψε σε ταχύτητα ακριβώς όπως προβλέφθηκε — 27,1s σε
         * 14,3s για τις ίδιες 14 κατηγορίες. Και ήταν λάθος.
         *
         * Με 12 νήματα, το portal δεν απαντούσε με σφάλμα· απαντούσε με
         * **σκελετούς**. Η ίδια ταινία (`id 1813440`) γύρισε δύο φορές, με τα
         * ίδια αιτήματα, εντελώς διαφορετικό περιεχόμενο:
         *
         * ```
         * 6 νήματα : rating_imdb=6.7  tmdb_id=1284465  description=«Na een leven…»  time=123
         * 12 νήματα: rating_imdb=N/A  tmdb_id=«»       description=N/A             time=1
         * ```
         *
         * Μαζί με 30 σελίδες που απέτυχαν σιωπηλά στις ταινίες και 2-3 στα
         * ζωντανά. Δηλαδή η «επιτάχυνση» αγόραζε χρόνο πληρώνοντας με βαθμολογίες,
         * περιγραφές, αφίσες και TMDB id — ακριβώς τα πεδία που μόλις είχαμε
         * δουλέψει για να εμφανίζονται σωστά.
         *
         * ΜΗΝ ΤΟ ΞΑΝΑΝΕΒΑΣΕΙΣ ΧΩΡΙΣ ΝΑ ΚΟΙΤΑΞΕΙΣ ΤΟ ΠΕΡΙΕΧΟΜΕΝΟ, όχι μόνο τον
         * χρόνο. Ο μετρητής `ΑΠΟΤΥΧΙΕΣ` στη γραμμή `ΣΥΝΟΨΗ` πιάνει τις χαμένες
         * σελίδες, αλλά ΔΕΝ πιάνει τις γραμμές που ήρθαν άδειες: αυτές μοιάζουν
         * απόλυτα φυσιολογικές μέχρι να δεις «N/A» στην οθόνη.
         */
        private val pagePool: java.util.concurrent.ExecutorService =
            java.util.concurrent.Executors.newFixedThreadPool(PAGE_THREADS) { r ->
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
         * ΜΕΝΕΙ ΣΤΑ ΤΡΙΑ ΕΠΕΙΔΗ ΤΟ PORTAL ΤΟ ΕΠΕΒΑΛΕ, ΟΧΙ ΑΠΟ ΣΥΝΗΘΕΙΑ.
         * Στο 1.87.0 ανέβηκαν στα έξι με το σκεπτικό ότι με **456 κατηγορίες**
         * η καθυστέρηση δικτύου πληρώνεται 152 φορές στη σειρά — σωστή
         * αριθμητική, λάθος συμπέρασμα. Δες [CATEGORY_THREADS].
         */
        private val categoryPool: java.util.concurrent.ExecutorService =
            java.util.concurrent.Executors.newFixedThreadPool(CATEGORY_THREADS) { r ->
                Thread(r, "stalker-category").apply { isDaemon = true }
            }

        /**
         * ΠΟΣΑ ΤΑΥΤΟΧΡΟΝΑ ΑΙΤΗΜΑΤΑ, ΚΑΙ ΓΙΑΤΙ ΑΥΤΑ.
         *
         * `CATEGORY_THREADS + PAGE_THREADS` ΠΡΕΠΕΙ να μένει ίσο ή κάτω από το
         * `maxRequestsPerHost` του `Http.providerClient`, που είναι 16. Πάνω από
         * αυτό, τα επιπλέον αιτήματα περιμένουν σιωπηλά στην ουρά του OkHttp —
         * χωρίς σφάλμα, χωρίς ένδειξη, και ο πραγματικός παραλληλισμός πέφτει
         * κάτω από αυτόν που νομίζεις ότι ρύθμισες.
         *
         * ΑΝΕΒΗΚΑΝ ΣΤΟ 1.87.0 ΣΕ 6+10 ΚΑΙ ΞΑΝΑΚΑΤΕΒΗΚΑΝ ΣΤΟ 1.88.0. ΓΙΑΤΙ:
         *
         * Η αιτιολόγηση ήταν **0 ΑΠΟΤΥΧΙΕΣ σε 1345 αιτήματα** στα εννέα
         * ταυτόχρονα. Ο αριθμός ήταν αληθινός αλλά **από τον δρόμο των ζωντανών**,
         * που από το 1.87.0 στέλνει ένα μόνο αίτημα (`get_all_channels`) και άρα
         * δεν πιέζει καθόλου. Ο δρόμος του καταλόγου ταινιών, που ΔΕΝ μετρήθηκε,
         * απάντησε στα δεκαέξι ταυτόχρονα με **HTTP 503 μέσα στο πρώτο
         * δευτερόλεπτο**. Το portal βάζει οροφή στις ταυτόχρονες συνδέσεις.
         *
         * ΤΟ ΜΑΘΗΜΑ: μια μέτρηση ισχύει για τον δρόμο που μέτρησε. Τα εννέα είναι
         * ο μόνος αριθμός που έχει τρέξει ποτέ σε αυτό το portal με μηδέν
         * αποτυχίες, και μέχρι νέα μέτρηση είναι ο μόνος που δικαιολογείται.
         * ΜΗΝ ΤΑ ΞΑΝΑΝΕΒΑΣΕΙΣ ΓΙΑ ΤΑΧΥΤΗΤΑ — η ταχύτητα του καταλόγου δεν
         * βρίσκεται εδώ, βρίσκεται στο πόσα αιτήματα ζητάμε συνολικά.
         *
         * ΤΟ ΟΡΙΟ ΤΟ ΟΡΙΖΕΙ Ο ΜΕΤΡΗΤΗΣ, ΟΧΙ Η ΓΝΩΜΗ: αν σε κάποια πηγή αρχίσουν
         * να εμφανίζονται «ΑΠΟΤΥΧΙΕΣ» ή «άδεια απόκριση» στη γραμμή ΣΥΝΟΨΗ, το
         * portal λυγίζει και αυτοί οι αριθμοί πρέπει να κατέβουν. Άδειες σελίδες
         * χάνουν δεδομένα ΣΙΩΠΗΛΑ — μοιάζουν φυσιολογικές μέχρι να δεις «N/A».
         */
        private const val PAGE_THREADS = 6
        private const val CATEGORY_THREADS = 3

        /**
         * ΠΟΣΕΣ ΚΑΤΗΓΟΡΙΕΣ ΕΠΙΤΡΕΠΕΤΑΙ ΝΑ ΕΙΝΑΙ «ΤΕΛΕΙΩΜΕΝΕΣ ΚΑΙ ΑΠΑΡΑΔΟΤΕΣ».
         *
         * ΑΥΤΟ ΕΙΝΑΙ Η ΑΙΤΙΑ ΤΟΥ `OutOfMemoryError`, ΟΧΙ Η ΤΑΧΥΤΗΤΑ. Το
         * [forEachCategoryParallel] υπέβαλλε **και τις 456 κατηγορίες μαζί** και
         * τις παρέδιδε με τη σειρά. Όποια τελείωνε πριν έρθει η σειρά της κρατούσε
         * τις γραμμές της ζωντανές μέσα στο `Future`. Αν αργούσε η πρώτη, στη
         * μνήμη υπήρχε **ΟΛΟΚΛΗΡΟΣ Ο ΚΑΤΑΛΟΓΟΣ ΣΕ JSONObject** — δηλαδή δεύτερο,
         * βαρύτερο αντίγραφο πάνω από τη λίστα [Channel] που χτιζόταν ταυτόχρονα.
         *
         * ΜΕΤΡΗΜΕΝΟ ΣΤΟ log του ιδιοκτήτη (emulator Android TV, όριο σωρού 192MB):
         * η μνήμη ανέβαινε 19 → 45 → … → 191MB στη μέση των ταινιών, οι παύσεις
         * GC έφταναν το ένα δευτερόλεπτο (`WaitForGcToComplete blocked Alloc
         * 4.905s`), και μετά `FATAL EXCEPTION: main java.lang.OutOfMemoryError`,
         * ANR στα 5002ms και `Killing com.prelude.iptv.qa (bg anr)`.
         *
         * ΤΟ ΠΑΡΑΘΥΡΟ ΔΕΝ ΚΟΣΤΙΖΕΙ ΤΑΧΥΤΗΤΑ: τα νήματα είναι [CATEGORY_THREADS],
         * άρα πάνω από τόσες κατηγορίες δεν δουλεύονται ποτέ ταυτόχρονα. Το
         * διπλάσιο κρατάει την ουρά γεμάτη ώστε κανένα νήμα να μη μείνει άεργο,
         * και ταυτόχρονα βάζει **οροφή** στη μνήμη: το πολύ τόσες κατηγορίες σε
         * γραμμές, όχι όλος ο κατάλογος.
         */
        private const val CATEGORY_WINDOW = CATEGORY_THREADS * 2

        /**
         * ΠΟΣΕΣ ΣΕΛΙΔΕΣ ΚΑΤΕΒΑΙΝΟΥΝ ΑΝΑ ΚΑΤΗΓΟΡΙΑ ΣΤΗΝ ΕΚΚΙΝΗΣΗ.
         *
         * ΔΥΟ, ΚΑΙ Ο ΛΟΓΟΣ ΕΙΝΑΙ ΑΡΙΘΜΗΤΙΚΟΣ: μια ράγα της Αρχικής δείχνει
         * `HomeRailContentPolicy.RAIL_LIMIT = 20` στοιχεία, και το portal δίνει
         * `max_page_items = 14`. Με μία σελίδα κάθε ράγα θα έδειχνε 14 από 20 —
         * ορατή υποβάθμιση. Με δύο, 28 φορτωμένα καλύπτουν τα 20.
         *
         * ΜΕΤΡΗΜΕΝΟ ΚΟΣΤΟΣ: ~7 δευτερόλεπτα ανά πέρασμα σε 60 κατηγορίες, άρα
         * 12 → 19 δευτερόλεπτα συνολικά. Το προηγούμενο μοντέλο ήθελε 5,5 λεπτά.
         *
         * ΜΗΝ ΤΟ ΑΝΕΒΑΣΕΙΣ ΓΙΑ «ΝΑ ΕΧΟΥΜΕ ΠΕΡΙΣΣΟΤΕΡΑ»: κάθε μονάδα κοστίζει άλλο
         * ένα αίτημα ΑΝΑ ΚΑΤΗΓΟΡΙΑ. Τα υπόλοιπα έρχονται κατ' απαίτηση.
         */
        private const val HEAD_PAGES = 2

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
            // ΧΩΡΙΣ ρητό Accept-Encoding εδώ, ΕΠΙΤΗΔΕΣ: πριν όριζε "identity",
            // που απενεργοποιεί εντελώς τη διαφανή gzip αποσυμπίεση του OkHttp
            // (OkHttp προσθέτει μόνο του "Accept-Encoding: gzip" και αποσυμπιέζει
            // αθόρυβα ΟΤΑΝ η εφαρμογή δεν ορίζει η ίδια την κεφαλίδα — αλλιώς
            // σβήνει αυτή τη συμπεριφορά εντελώς). Οι κλήσεις Xtream/M3U/TMDB
            // ποτέ δεν όριζαν Accept-Encoding, άρα ήδη έπαιρναν gzip· μόνο το
            // Stalker κατέβαζε ασυμπίεστο JSON σε κάθε σελίδα κάθε κατηγορίας.
            // Καμία τεκμηρίωση/CHANGELOG δεν εξηγούσε γιατί μπήκε το "identity" —
            // αφαιρέθηκε ως πιθανή αιτία του «οι λίστες αργούν συγκριτικά με
            // άλλα players», και επιβεβαιώθηκε σε πραγματικό portal (κινητό +
            // τηλεόραση). Αν κάποιο άλλο portal στείλει σπασμένο gzip, θα φανεί
            // ως σφάλμα ανάλυσης JSON (όχι σιωπηλά λάθος δεδομένα).
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
        onPartial: SourcePartialCallback? = null,
        onCategories: SourceCategoriesCallback? = null,
    ): List<Channel> {
        if (base == null) connect()
        val genreList = getGenres()
        onCategories?.invoke(genreList)
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

        val liveStartedAtMs = System.currentTimeMillis()
        resetLoadCounters()

        // ---- ΕΝΑ ΑΙΤΗΜΑ ΓΙΑ ΟΛΑ ΤΑ ΚΑΝΑΛΙΑ. ΠΡΩΤΑ, ΟΧΙ ΩΣ ΕΦΕΔΡΕΙΑ. ----
        //
        // ΜΕΤΡΗΜΕΝΟ, ΟΧΙ ΕΙΚΑΣΙΑ. Σε πραγματικό portal του ιδιοκτήτη, ο δρόμος
        // κατηγορία-κατηγορία έδωσε:
        //
        //   ΣΥΝΟΨΗ live — 15838 στοιχεία σε 132.52s · 1345 αιτήματα σελίδας ·
        //   0 ΑΠΟΤΥΧΙΕΣ · 456 κατηγορίες · 120 στοιχεία/δευτ. · 10 αιτήματα/δευτ.
        //
        // Δηλαδή 11,8 στοιχεία ανά αίτημα: πληρώναμε ένα ολόκληρο round-trip για
        // δεκατέσσερα κανάλια, 1345 φορές, δύο λεπτά και δώδεκα δευτερόλεπτα.
        //
        // ΚΑΙ ΔΕΝ ΧΡΕΙΑΖΟΤΑΝ ΚΑΘΟΛΟΥ: το [append] διαβάζει την κατηγορία από το
        // `tv_genre_id` ΤΗΣ ΙΔΙΑΣ ΤΗΣ ΓΡΑΜΜΗΣ, και το `genre=$gid` του αιτήματος
        // είναι μόνο εφεδρεία γι' αυτό. Άρα οι 456 κλήσεις ανά κατηγορία δεν
        // πρόσθεταν καμία πληροφορία που δεν ήταν ήδη στο περιεχόμενο.
        //
        // Το `get_all_channels` υπήρχε στο αρχείο, αλλά καλούνταν μόνο όταν ο
        // αργός δρόμος γύριζε άδειος — δηλαδή σχεδόν ποτέ.
        val restrictTo = genreIds
            ?.filter { it.isNotBlank() && it != "*" }
            ?.toHashSet()
            ?.takeIf { it.isNotEmpty() }
        val singleShot = try {
            onProgress?.invoke(20, "Λήψη όλων των καναλιών…")
            pageRequests.incrementAndGet()
            // ΠΡΟΣΠΑΘΕΙΑ ΠΟΥ ΓΥΡΙΣΕ ΠΙΣΩ (1.90.0 → 1.92.0). Αυτό το σημείο
            // ξαναγράφτηκε ώστε να κατεβάζει σε αρχείο και να διαβάζει με
            // `JsonReader`, για να μη ζει ο κατάλογος δύο φορές στη μνήμη. Η
            // ιδέα ήταν σωστή· η εκτέλεση ΔΕΝ ΕΠΙΒΕΒΑΙΩΘΗΚΕ ΠΟΤΕ σε συσκευή και
            // τα ζωντανά έσπασαν στο πρώτο build που την περιείχε.
            //
            // ΜΗΝ ΤΟ ΞΑΝΑΔΟΚΙΜΑΣΕΙΣ ΧΩΡΙΣ ΜΕΤΡΗΣΗ ΠΡΩΤΑ. Η μνήμη ΔΕΝ ήταν το
            // πρόβλημα: μέτρηση 2026-08-15 έδειξε σωρό 94MB από 121MB και ΚΑΜΙΑ
            // `OutOfMemoryError` σε 80.000 γραμμές logcat. Ο κώδικας παρακάτω
            // είναι αυτός που έδωσε «4638 στοιχεία σε 7.351s · 1 αίτημα ·
            // 0 ΑΠΟΤΥΧΙΕΣ» και είναι ο μόνος επιβεβαιωμένος.
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
            val rows = if (data == null) emptyList() else {
                (0 until data.length())
                    .map { data.getJSONObject(it) }
                    // Ο χρήστης μπορεί να έχει διαλέξει ΥΠΟΣΥΝΟΛΟ κατηγοριών. Το
                    // φιλτράρουμε από το περιεχόμενο, χωρίς ούτε ένα επιπλέον
                    // αίτημα: ένα αίτημα εξυπηρετεί και την πλήρη λήψη και την
                    // επιλεκτική.
                    .filter { row ->
                        if (restrictTo == null) return@filter true
                        val gid = row.optString("tv_genre_id")
                        gid.isBlank() || gid in restrictTo
                    }
            }
            if (rows.isEmpty()) false else {
                append(rows)
                if (result.isNotEmpty()) onPartial?.invoke(result)
                true
            }
        } catch (e: Exception) {
            // Η ακύρωση ΔΕΝ είναι αποτυχία του portal: αν την καταπιούμε εδώ, ο
            // αργός δρόμος από κάτω θα ξεκινούσε 456 αιτήματα για δεδομένα που
            // κανείς δεν θα δει.
            rethrowIfCancelled(e)
            notePageFailure("σφάλμα, get_all_channels", e)
            false
        }

        // ---- ΕΦΕΔΡΕΙΑ: portals που δεν υλοποιούν το get_all_channels ----
        //
        // Οι κατηγορίες κατεβαίνουν ΠΑΡΑΛΛΗΛΑ και παραδίδονται με τη σειρά. Μόλις
        // ολοκληρωθεί μία, δημοσιεύεται ενώ οι επόμενες συνεχίζουν να κατεβαίνουν.
        // Χειρότερη περίπτωση: ό,τι έκανε η εφαρμογή μέχρι σήμερα.
        if (!singleShot) {
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
        }

        logLoadSummary("live", ids.size, result.size, liveStartedAtMs)
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

    /**
     * ΜΙΑ ΑΠΟΤΥΧΙΑ ΕΔΩ ΔΕΝ ΕΠΙΤΡΕΠΕΤΑΙ ΝΑ ΜΟΙΑΖΕΙ ΜΕ ΑΔΕΙΟ PORTAL.
     *
     * ΜΕΤΡΗΜΕΝΟ, 2026-08-15, τηλέφωνο του ιδιοκτήτη:
     *
     *   ΣΥΝΟΨΗ live   — 4638 στοιχεία σε 7.351s · 1 αίτημα · 0 ΑΠΟΤΥΧΙΕΣ
     *   ΣΥΝΟΨΗ vod    — 0 στοιχεία σε 0.001s · 0 αιτήματα · 0 κατηγορίες
     *   ΣΥΝΟΨΗ series — 0 στοιχεία σε 0.001s · 0 αιτήματα · 0 κατηγορίες
     *
     * Οι ταινίες και οι σειρές δεν κόλλησαν — **δεν ξεκίνησαν ποτέ**, επειδή αυτή
     * η μέθοδος γύρισε άδεια λίστα. Και μέχρι το 1.91.0 γύριζε άδεια λίστα και
     * όταν το portal απαντούσε «καμία κατηγορία» ΚΑΙ όταν το αίτημα έσκαγε: ένα
     * `catch` που κατάπινε τα πάντα. Οι δύο περιπτώσεις θέλουν ΑΝΤΙΘΕΤΕΣ
     * κινήσεις — η πρώτη «δεν έχει ταινίες αυτό το portal», η δεύτερη «ξαναρώτα»
     * — και ήταν αδύνατο να ξεχωριστούν από έξω. Είναι το ίδιο μάθημα που το
     * [notePageFailure] είχε ήδη μάθει για τις σελίδες, ένα επίπεδο πιο πάνω.
     *
     * ΔΕΝ πετάει: η οθόνη δεν έχει σχεδιασμένο μήνυμα σφάλματος γι' αυτό, και
     * μια τέτοια αλλαγή θέλει δικό της preview. Εδώ γίνεται μόνο ΘΟΡΥΒΩΔΗΣ.
     */
    private fun getCategories(type: String): List<Pair<String, String>> {
        if (base == null) connect()
        val url = "$base?type=$type&action=get_categories&JsHttpRequest=1-xml"
        val raw = try {
            providerText(url, activeHeaders())
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            // ΔΕΥΤΕΡΗ ΠΡΟΣΠΑΘΕΙΑ, ΟΠΩΣ ΚΑΙ Η 1η ΣΕΛΙΔΑ ΚΑΤΗΓΟΡΙΑΣ. Όταν χάνεται
            // αυτή δεν χάνονται δεκατέσσερα στοιχεία, χάνεται ΟΛΟΚΛΗΡΗ η ενότητα.
            val retried = if (isTransientFailure(e)) {
                try {
                    providerText(url, activeHeaders())
                } catch (second: Exception) {
                    rethrowIfCancelled(second)
                    null
                }
            } else null
            if (retried == null) {
                Log.w(
                    "CatalogLoad",
                    "ΧΑΘΗΚΕ Η ΛΙΣΤΑ ΚΑΤΗΓΟΡΙΩΝ «$type» — η ενότητα θα εμφανιστεί ΑΔΕΙΑ " +
                        "χωρίς να φταίει ο κατάλογος. Παροδικό=${isTransientFailure(e)}: " +
                        "${e.javaClass.simpleName}",
                    e,
                )
                return emptyList()
            }
            Log.w("CatalogLoad", "ΣΩΘΗΚΕ η λίστα κατηγοριών «$type» στη δεύτερη προσπάθεια")
            retried
        }

        val arr = try {
            JSONObject(raw).optJSONArray("js")
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            null
        }
        if (arr == null) {
            // ΤΟ ΣΧΗΜΑ ΤΗΣ ΑΠΟΚΡΙΣΗΣ ΕΙΝΑΙ Η ΜΟΝΗ ΠΛΗΡΟΦΟΡΙΑ ΠΟΥ ΛΕΙΠΕΙ ΕΔΩ.
            // Χωρίς αυτό, «λείπει το js» και «το js δεν είναι πίνακας» φαίνονται
            // ίδια, ενώ το δεύτερο σημαίνει portal με άλλο σχήμα, όχι σφάλμα.
            Log.w(
                "CatalogLoad",
                "ΑΚΑΤΑΝΟΗΤΗ ΛΙΣΤΑ ΚΑΤΗΓΟΡΙΩΝ «$type» — δεν βρέθηκε πίνακας «js». " +
                    "ΩΜΗ ΑΠΟΚΡΙΣΗ (πρώτοι 400 χαρακτήρες): ${raw.take(400)}",
            )
            return emptyList()
        }

        val out = ArrayList<Pair<String, String>>()
        for (i in 0 until arr.length()) {
            val it = arr.optJSONObject(i) ?: continue
            val id = it.optString("id"); val title = it.optString("title")
            if (id.isNotEmpty() && id != "*" && title.isNotEmpty()) out.add(id to title)
        }
        if (out.isEmpty()) {
            Log.w(
                "CatalogLoad",
                "ΜΗΔΕΝ ΕΓΚΥΡΕΣ ΚΑΤΗΓΟΡΙΕΣ «$type» από ${arr.length()} γραμμές — το " +
                    "portal απάντησε, αλλά καμία γραμμή δεν είχε «id» και «title». " +
                    "ΩΜΗ ΑΠΟΚΡΙΣΗ (πρώτοι 400 χαρακτήρες): ${raw.take(400)}",
            )
        } else {
            Log.d("CatalogLoad", "ΚΑΤΗΓΟΡΙΕΣ «$type» — ${out.size} έγκυρες από ${arr.length()}")
        }
        return out
    }

    fun getVodChannels(
        catIds: List<String>?,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null,
        onCategories: SourceCategoriesCallback? = null,
        onCategoryTotals: SourceCategoryTotalsCallback? = null,
    ): List<Channel> = getVodLike("vod", catIds, onProgress, onPartial, onCategories, onCategoryTotals)

    fun getSeriesChannels(
        catIds: List<String>?,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null,
        onCategories: SourceCategoriesCallback? = null,
        onCategoryTotals: SourceCategoryTotalsCallback? = null,
    ): List<Channel> = getVodLike("series", catIds, onProgress, onPartial, onCategories, onCategoryTotals)

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
            for (i in 0 until data.length()) {
                val row = data.getJSONObject(i)
                val label = row.optString("name").takeIf { it.isNotBlank() } ?: "Season ${i + 1}"
                val seasonCmd = row.optString("cmd")
                if (seasonCmd.isBlank()) continue
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
                if (episodes.isNotEmpty()) seasons += label to episodes
            }

            // ΕΔΩ ΖΗΤΟΥΝΤΑΝ ΠΕΡΙΓΡΑΦΗ ΑΝΑ ΕΠΕΙΣΟΔΙΟ. ΑΦΑΙΡΕΘΗΚΕ.
            //
            // Το portal ΑΓΝΟΕΙ τα `season_id` και `episode_id` σε αυτό το βάθος:
            // επιβεβαιώθηκε με Logcat όπου αιτήματα για episode=25, 28, 33, 39…
            // επέστρεψαν ΟΛΑ την ίδια απάντηση — τη λίστα σεζόν, με το
            // `description` της ΣΕΙΡΑΣ. Γι' αυτό κάθε επεισόδιο εμφάνιζε την ίδια
            // σύνοψη: δεν ήταν σφάλμα προβολής, ήταν ίδια δεδομένα.
            //
            // Το κόστος ήταν πραγματικό: μία σειρά 81 επεισοδίων έκανε 81
            // αιτήματα, το καθένα κατέβαζε ολόκληρη τη λίστα σεζόν, σε pool 3
            // νημάτων ΚΟΙΝΟ με τις σελίδες κατηγοριών — 4,5 δευτερόλεπτα
            // αναμονής πριν φανεί η λίστα, και ισάριθμα αιτήματα κλεμμένα από
            // τον κατάλογο και από το create_link της αναπαραγωγής.
            //
            // Το `plot` του επεισοδίου μένει ό,τι έδωσε η λίστα σεζόν. Οι
            // περιγραφές ανά επεισόδιο έρχονται από το TMDB, που είναι και η
            // μόνη πηγή που τις έχει πραγματικά.
            seasons
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            emptyList()
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
        // Η γραμμή ΣΕΖΟΝ κουβαλάει το tmdb_id της σειράς. Μπαίνει και στο
        // επεισόδιο ώστε οι οθόνες που δείχνουν επεισόδιο ΧΩΡΙΣ τον γονέα
        // δίπλα — το πάνελ πληροφοριών του player — να μπορούν κι αυτές να
        // ζητήσουν μεταδεδομένα χωρίς αναζήτηση με τίτλο.
        tmdbId = row.optString("tmdb_id").ifEmpty { row.optString("tmdb") },
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
    /**
     * Πόσα αιτήματα σελίδας έγιναν από την τελευταία μηδένιση.
     *
     * ΓΙΑΤΙ ΜΕΤΡΑΜΕ: ο πάροχος σερβίρει 14 στοιχεία ανά σελίδα, οπότε κατάλογος
     * δέκα χιλιάδων ταινιών σημαίνει εφτακόσια round trips. Το «αργεί» δεν
     * βελτιώνεται, μετριέται — και χωρίς αυτόν τον αριθμό κάθε ρύθμιση
     * παραλληλισμού θα ήταν εικασία, με ρίσκο να αρχίσει το portal να απαντά 429.
     */
    private val pageRequests = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Σελίδες που ΑΠΕΤΥΧΑΝ και καταπιώθηκαν.
     *
     * Το `catch` γύρω από κάθε σελίδα επιστρέφει κενή λίστα, ώστε μια στιγμιαία
     * αποτυχία να μη ρίξει ολόκληρη τη φόρτωση. Το τίμημα είναι ότι η απώλεια
     * είναι ΑΟΡΑΤΗ: ο χρήστης βλέπει λιγότερες ταινίες και κανένα σφάλμα. Μετά
     * την αύξηση του παραλληλισμού μια πραγματική φόρτωση έδωσε 1.981 σειρές
     * εκεί που πριν έδινε 2.304 — χωρίς αυτόν τον μετρητή δεν υπάρχει τρόπος να
     * ξεχωρίσεις «το portal μας κόβει» από «άλλαξε ο κατάλογος».
     */
    private val pageFailures = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Μία γραμμή στο τέλος κάθε ενότητας: πόσα αιτήματα, πόσος χρόνος, τι ρυθμός.
     *
     * Ο ρυθμός («στοιχεία/δευτ.») είναι το νούμερο που κρίνει κάθε επόμενη
     * βελτίωση: αν ανεβάσουμε τον παραλληλισμό και δεν ανέβει αυτός, το φράγμα
     * είναι το portal και όχι εμείς — και τότε σταματάμε αντί να το πιέζουμε
     * μέχρι να αρχίσει να απαντά 429.
     */
    /**
     * Η ΠΡΩΤΗ σελίδα μιας κατηγορίας που αποτυγχάνει χάνει ΟΛΟΚΛΗΡΗ την κατηγορία.
     *
     * Το [fetchAllPages] επιστρέφει κενή λίστα αν πέσει το πρώτο αίτημα ή αν
     * λείπει το `js`/`data`, και μέχρι τώρα ΔΕΝ το μετρούσε πουθενά: ο μετρητής
     * [pageFailures] πιάνει μόνο τις παράλληλες σελίδες 2..N. Με 271 κατηγορίες
     * στις σειρές, μια χαμένη πρώτη σελίδα είναι αόρατη απώλεια άγνωστου μεγέθους
     * — χειρότερη από τις 14 μετρημένες, γιατί δεν ξέρουμε καν πόσα στοιχεία είχε.
     */
    private val firstPageFailures = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Πόσα δείγματα αιτίας έχουν καταγραφεί σε αυτή την ενότητα.
     *
     * ΦΡΑΓΜΕΝΟ ΣΤΑ 3 ΕΠΙΤΗΔΕΣ. Δεκατέσσερα stack traces δεν λένε περισσότερα από
     * τρία, και πνίγουν το logcat ακριβώς όταν το διαβάζεις. Ο συνολικός αριθμός
     * ζει ήδη στη γραμμή `ΣΥΝΟΨΗ`· εδώ θέλουμε μόνο το ΕΙΔΟΣ της αποτυχίας.
     */
    private val failureSamplesLogged = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Πόσες σελίδες ΣΩΘΗΚΑΝ στο δεύτερο, σειριακό πέρασμα.
     *
     * ΓΙΑΤΙ ΜΕΤΡΑΕΙ ΞΕΧΩΡΙΣΤΑ: μέχρι το 1.88.0 μια σελίδα που γύριζε HTTP 503
     * ΧΑΝΟΤΑΝ ΟΡΙΣΤΙΚΑ. Καμία επανάληψη, κανένα σφάλμα στην οθόνη — ο χρήστης
     * έβλεπε λιγότερες ταινίες και δεν είχε τρόπο να το ξέρει. Αν αυτός ο
     * μετρητής είναι μεγάλος, το portal λυγίζει και τα νήματα πρέπει να πέσουν.
     */
    private val pageRecoveries = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Έχει καταγραφεί το «κεφάλι» μιας κατηγορίας σε αυτή την ενότητα;
     *
     * ΜΙΑ ΦΟΡΑ ΑΝΑ ΕΝΟΤΗΤΑ, ΟΧΙ ΑΝΑ ΚΑΤΗΓΟΡΙΑ: με 145 κατηγορίες η ίδια γραμμή
     * 145 φορές πνίγει το logcat ακριβώς όταν το διαβάζεις. Το νούμερο που
     * χρειάζεται (`total_items`) είναι το ίδιο για όλες.
     */
    private val loggedHeadSample = java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * Αξίζει δεύτερη προσπάθεια αυτό το σφάλμα;
     *
     * ΤΟ 503 ΕΙΝΑΙ ΠΑΡΟΔΙΚΟ, ΤΟ 401 ΔΕΝ ΕΙΝΑΙ. Το `Http.providerGet` πετάει
     * `RuntimeException("HTTP <κωδικός>")`, οπότε ο κωδικός διαβάζεται από το
     * μήνυμα. Σφάλμα δικτύου χωρίς κωδικό μετράει ως παροδικό: αυτό είναι
     * κομμένη σύνδεση, όχι άρνηση του παρόχου.
     */
    private fun isTransientFailure(error: Exception): Boolean {
        val message = error.message.orEmpty()
        val marker = message.indexOf("HTTP ")
        if (marker < 0) return true
        val code = message.substring(marker + 5).take(3).toIntOrNull() ?: return true
        return code == 408 || code == 429 || code >= 500
    }

    /**
     * Καταγράφει ΓΙΑΤΙ χάθηκε μια σελίδα — τη μία πληροφορία που ο μετρητής δεν
     * κρατά και που καθορίζει την επόμενη κίνηση.
     *
     * «άδεια απόκριση» και «σφάλμα» οδηγούν σε ΑΝΤΙΘΕΤΕΣ διορθώσεις: το πρώτο
     * σημαίνει ότι το portal απάντησε χωρίς δεδομένα, δηλαδή η ίδια υποβάθμιση
     * υπό πίεση που ακύρωσε τα 12 νήματα, και τότε μια επανάληψη απλώς προσθέτει
     * πίεση. Το δεύτερο είναι παροδικό και μια φραγμένη επανάληψη το σώζει.
     * ΜΗΝ ΓΡΑΨΕΙΣ RETRY ΠΡΙΝ ΔΕΙΣ ΠΟΙΟ ΑΠΟ ΤΑ ΔΥΟ ΕΙΝΑΙ.
     */
    private fun notePageFailure(reason: String, error: Exception? = null) {
        if (failureSamplesLogged.incrementAndGet() > 3) return
        val message = "ΑΙΤΙΑ ΑΠΟΤΥΧΙΑΣ ΣΕΛΙΔΑΣ ($reason)"
        if (error == null) Log.w("CatalogLoad", message)
        else Log.w("CatalogLoad", "$message: ${error.javaClass.simpleName}", error)
    }

    private fun resetLoadCounters() {
        pageRequests.set(0)
        pageFailures.set(0)
        firstPageFailures.set(0)
        failureSamplesLogged.set(0)
        pageRecoveries.set(0)
        loggedHeadSample.set(false)
    }

    private fun logLoadSummary(type: String, categories: Int, items: Int, startedAtMs: Long) {
        val elapsedMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(1)
        val requests = pageRequests.get()
        val failures = pageFailures.get()
        Log.d(
            "CatalogLoad",
            "ΣΥΝΟΨΗ $type — $items στοιχεία σε ${elapsedMs / 1000.0}s · " +
                "$requests αιτήματα σελίδας · $failures ΑΠΟΤΥΧΙΕΣ · " +
                "${pageRecoveries.get()} ΣΩΘΗΚΑΝ · $categories κατηγορίες · " +
                "${"%.0f".format(items * 1000.0 / elapsedMs)} στοιχεία/δευτ. · " +
                "${"%.0f".format(requests * 1000.0 / elapsedMs)} αιτήματα/δευτ.",
        )
        if (failures > 0) {
            Log.w(
                "CatalogLoad",
                "ΧΑΘΗΚΑΝ ΔΕΔΟΜΕΝΑ: $failures σελίδες του «$type» απέτυχαν και " +
                    "αγνοήθηκαν σιωπηλά — έως ${failures * 14} στοιχεία λείπουν. " +
                    "Δες τις γραμμές «ΑΙΤΙΑ ΑΠΟΤΥΧΙΑΣ ΣΕΛΙΔΑΣ» παραπάνω: «άδεια " +
                    "απόκριση» σημαίνει ότι το portal λυγίζει υπό πίεση και ο " +
                    "παραλληλισμός (pagePool) είναι πολύ ψηλά· «σφάλμα» σημαίνει " +
                    "παροδική αποτυχία δικτύου.",
            )
        }
        val lostCategories = firstPageFailures.get()
        if (lostCategories > 0) {
            Log.w(
                "CatalogLoad",
                "ΧΑΘΗΚΑΝ ΚΑΤΗΓΟΡΙΕΣ: σε $lostCategories κατηγορίες του «$type» απέτυχε " +
                    "η ΠΡΩΤΗ σελίδα, οπότε χάθηκε ολόκληρη η κατηγορία. Το πλήθος των " +
                    "στοιχείων που λείπουν είναι άγνωστο — η απάντηση που θα το έλεγε " +
                    "είναι αυτή που δεν ήρθε.",
            )
        }
    }

    /** Η 1η σελίδα μιας κατηγορίας, μαζί με ό,τι δηλώνει για το σύνολό της. */
    private class CategoryHead(
        val rows: List<JSONObject>,
        val total: Int,
        val per: Int,
    )

    /**
     * ΜΟΝΟ η 1η σελίδα μιας κατηγορίας. `null` σημαίνει «χάθηκε ολόκληρη».
     *
     * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΑ ΑΠΟ ΤΙΣ ΥΠΟΛΟΙΠΕΣ: επιτρέπει να κατέβει Η ΠΡΩΤΗ ΣΕΛΙΔΑ
     * ΚΑΘΕ ΚΑΤΗΓΟΡΙΑΣ πρώτα, και οι υπόλοιπες μετά. Δες
     * [forEachCategoryBreadthFirst] για το γιατί αυτό αλλάζει τα πάντα στην
     * αίσθηση χωρίς να αλλάζει τίποτα στο αποτέλεσμα.
     */
    /** Τι απομένει να κατέβει από μια κατηγορία, αφού ήρθε η 1η σελίδα της. */
    private class CategoryPlan(val id: String, val total: Int, val per: Int)

    private fun awaitHead(future: java.util.concurrent.Future<CategoryHead?>): CategoryHead? =
        try {
            future.get()
        } catch (wrapped: java.util.concurrent.ExecutionException) {
            val cause = wrapped.cause
            if (cause is Exception) rethrowIfCancelled(cause)
            null
        }

    /**
     * ΠΡΩΤΑ Η ΣΕΛΙΔΑ 1 ΚΑΘΕ ΚΑΤΗΓΟΡΙΑΣ. ΜΕΤΑ ΟΛΕΣ ΟΙ ΥΠΟΛΟΙΠΕΣ.
     *
     * ΔΕΝ ΜΕΙΩΝΕΙ ΤΗ ΔΟΥΛΕΙΑ — ΑΛΛΑΖΕΙ ΤΗ ΣΕΙΡΑ ΤΗΣ. Στο τέλος κατεβαίνουν
     * ακριβώς τα ίδια στοιχεία με πριν. Αλλάζει μόνο πότε γίνεται περιηγήσιμος ο
     * κατάλογος.
     *
     * ΜΕΤΡΗΜΕΝΟ στο portal του ιδιοκτήτη:
     *
     *   ΣΥΝΟΨΗ vod — 43057 στοιχεία σε 263.518s · 3402 αιτήματα · 145 κατηγορίες
     *
     * Ο παλιός δρόμος τελείωνε ΜΙΑ κατηγορία τη φορά, μαζί με όλες τις σελίδες
     * της. Άρα στο δευτερόλεπτο 16 ο χρήστης είχε δεκαπέντε κατηγορίες πλήρεις
     * και εκατόν τριάντα άδειες — δηλαδή μια οθόνη που δεν χρησίμευε. Με τα ίδια
     * 145 πρώτα αιτήματα, το πρώτο πέρασμα δίνει **και τις 145 κατηγορίες με
     * δεκατέσσερα στοιχεία η καθεμία**, που είναι όσα χωράει μια ράγα της αρχικής
     * (το `buildCatalogRailSections` κόβει στα 20). Η οθόνη είναι πλήρης και
     * περιηγήσιμη, και οι υπόλοιπες 3257 σελίδες συμπληρώνονται από κάτω.
     *
     * ΓΙΑΤΙ ΔΕΝ ΑΛΛΑΖΕΙ ΤΙΠΟΤΑ ΣΤΗΝ ΟΘΟΝΗ: η σειρά των ραγών, οι ράγες «Νέα» και
     * «Κορυφαία» και το «δες όλα» βασίζονται σε ό,τι υπάρχει στη μνήμη. Επειδή
     * το δεύτερο πέρασμα φέρνει τα υπόλοιπα, το τελικό περιεχόμενο είναι
     * πανομοιότυπο. ΑΝ ΚΑΠΟΤΕ ΚΟΠΕΙ ΤΟ ΔΕΥΤΕΡΟ ΠΕΡΑΣΜΑ, αυτά τα τρία αλλάζουν
     * νόημα — και τότε θέλουν δικά τους αιτήματα με `sortby`, όχι σιωπηλή ανοχή.
     */
    private fun forEachCategoryBreadthFirst(
        ids: List<String>,
        urlFor: (categoryId: String, page: Int) -> String,
        /**
         * Το `total_items` της 1ης σελίδας, μόλις γίνει γνωστό. Καλείται ΜΙΑ φορά
         * ανά κατηγορία, μέσα στο 1ο πέρασμα — δηλαδή στα πρώτα δευτερόλεπτα,
         * πολύ πριν κατέβει ο κατάλογος.
         */
        onCategoryTotal: (categoryId: String, totalItems: Int, pageSize: Int) -> Unit = { _, _, _ -> },
        onRows: (categoryId: String, rows: List<JSONObject>, done: Int, total: Int, firstPass: Boolean) -> Unit,
    ) {
        if (ids.isEmpty()) return
        val plans = ArrayList<CategoryPlan>(ids.size)

        // ---- ΠΕΡΑΣΜΑ 1: η σελίδα 1 κάθε κατηγορίας ----
        val heads = ArrayDeque<Pair<String, java.util.concurrent.Future<CategoryHead?>>>()
        var nextHead = 0
        var deliveredHeads = 0
        fun submitHead() {
            if (nextHead >= ids.size) return
            val id = ids[nextHead++]
            heads.addLast(
                id to categoryPool.submit<CategoryHead?> {
                    fetchFirstPage({ page -> urlFor(id, page) }, HEAD_PAGES)
                }
            )
        }
        try {
            while (heads.size < CATEGORY_WINDOW && nextHead < ids.size) submitHead()
            while (heads.isNotEmpty()) {
                ensureRequestActive()
                val (id, future) = heads.removeFirst()
                deliveredHeads++
                val head = awaitHead(future)
                if (head != null && head.total > 0) onCategoryTotal(id, head.total, head.per)
                if (head != null && head.rows.isNotEmpty()) {
                    // `total <= 0` σημαίνει ότι το portal δεν δηλώνει σύνολο: τότε
                    // το δεύτερο πέρασμα ΠΡΕΠΕΙ να τρέξει, γιατί μόνο αυτό μπορεί
                    // να ανακαλύψει πού τελειώνει η κατηγορία.
                    if (head.total <= 0 || head.total > head.rows.size) {
                        plans.add(CategoryPlan(id, head.total, head.per))
                    }
                    onRows(id, head.rows, deliveredHeads, ids.size, true)
                } else {
                    onRows(id, emptyList(), deliveredHeads, ids.size, true)
                }
                submitHead()
            }
        } catch (error: Throwable) {
            heads.forEach { (_, future) -> future.cancel(true) }
            throw error
        }

        // ---- ΤΟ ΠΕΡΑΣΜΑ 2 ΔΕΝ ΤΡΕΧΕΙ ΣΤΗΝ ΕΚΚΙΝΗΣΗ ----
        //
        // ΜΕΤΡΗΜΕΝΟ: το 1ο πέρασμα τελειώνει σε ~7s ανά ενότητα· το 2ο πρόσθετε
        // 270s για τις ταινίες και 44s για τις σειρές. Δηλαδή τα 5,5 λεπτά που
        // ο χρήστης περίμενε ήταν ΜΟΝΟ αυτό.
        //
        // ΔΕΝ ΓΙΝΕΤΑΙ ΓΡΗΓΟΡΟΤΕΡΟ ΚΑΙ ΔΕΝ ΧΡΕΙΑΖΕΤΑΙ: το portal δίνει 14 στοιχεία
        // ανά απάντηση, άρα 61.210 ταινίες = 4.372 αιτήματα για οποιονδήποτε.
        // Οι υπόλοιπες σελίδες κατεβαίνουν πλέον ΚΑΤ' ΑΠΑΙΤΗΣΗ, όταν ο χρήστης
        // ανοίξει την κατηγορία — δες [categoryPage].
        //
        // Το `plans` μένει γιατί λέει ΠΟΙΕΣ κατηγορίες έχουν κι άλλα, και αυτό
        // είναι ακριβώς η πληροφορία που χρειάζεται η οθόνη για να ξέρει αν
        // αξίζει να ζητήσει επόμενη σελίδα.
        Log.d(
            "CatalogLoad",
            "ΠΕΡΑΣΜΑ 1 ΤΕΛΟΣ — ${plans.size} από ${ids.size} κατηγορίες έχουν κι άλλες " +
                "σελίδες, που θα κατέβουν κατ' απαίτηση",
        )
        return
    }

    private fun fetchFirstPage(urlFor: (Int) -> String, headPages: Int = 1): CategoryHead? {
        pageRequests.incrementAndGet()
        val first = try {
            JSONObject(providerText(urlFor(1), activeHeaders()))
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            // Η 1η ΣΕΛΙΔΑ ΑΞΙΖΕΙ ΜΙΑ ΔΕΥΤΕΡΗ ΠΡΟΣΠΑΘΕΙΑ, ΟΙ ΑΛΛΕΣ ΟΧΙ ΤΟΣΟ: όταν
            // χάνεται αυτή, χάνεται Η ΟΛΟΚΛΗΡΗ ΚΑΤΗΓΟΡΙΑ, όχι δεκατέσσερα
            // στοιχεία. Η επανάληψη είναι σειριακή εδώ, δηλαδή έρχεται όταν το
            // παράθυρο έχει ήδη αδειάσει και το portal δεν πιέζεται.
            val retried = if (isTransientFailure(e)) {
                pageRequests.incrementAndGet()
                try {
                    JSONObject(providerText(urlFor(1), activeHeaders()))
                } catch (second: Exception) {
                    rethrowIfCancelled(second)
                    null
                }
            } else null
            if (retried == null) {
                firstPageFailures.incrementAndGet()
                notePageFailure("σφάλμα, 1η σελίδα κατηγορίας", e)
                return null
            }
            pageRecoveries.incrementAndGet()
            retried
        }
        val js = first.optJSONObject("js") ?: run {
            firstPageFailures.incrementAndGet()
            notePageFailure("άδεια απόκριση, 1η σελίδα κατηγορίας — λείπει το js")
            return null
        }
        val data = js.optJSONArray("data") ?: run {
            firstPageFailures.incrementAndGet()
            notePageFailure("άδεια απόκριση, 1η σελίδα κατηγορίας — λείπει το data")
            return null
        }
        val rows = ArrayList<JSONObject>(data.length())
        for (i in 0 until data.length()) rows.add(data.getJSONObject(i))
        val total = js.optInt("total_items", 0)
        val per = js.optInt("max_page_items", data.length()).coerceAtLeast(1)
        // ΟΙ ΕΠΟΜΕΝΕΣ ΣΕΛΙΔΕΣ ΤΟΥ ΚΕΦΑΛΙΟΥ. Σειριακά και μέσα στο ίδιο νήμα
        // κατηγορίας: είναι μία-δύο σελίδες, δεν αξίζει δεύτερο επίπεδο
        // παραλληλισμού, και το portal ήδη δέχεται CATEGORY_THREADS ταυτόχρονα.
        var page = 2
        while (page <= headPages && total > rows.size) {
            ensureRequestActive()
            pageRequests.incrementAndGet()
            val more = try {
                JSONObject(providerText(urlFor(page), activeHeaders()))
                    .optJSONObject("js")?.optJSONArray("data")
            } catch (e: Exception) {
                rethrowIfCancelled(e)
                pageFailures.incrementAndGet()
                notePageFailure("σφάλμα, σελίδα $page κεφαλιού")
                null
            } ?: break
            if (more.length() == 0) break
            for (i in 0 until more.length()) rows.add(more.getJSONObject(i))
            page++
        }

        // ΤΟ `total_items` ΕΙΝΑΙ ΤΟ ΝΟΥΜΕΡΟ ΠΟΥ ΚΡΑΤΑΕΙ ΟΛΟ ΤΟ ΜΟΝΤΕΛΟ ΟΡΘΙΟ:
        // δίνει το πραγματικό πλήθος της κατηγορίας από 14 στοιχεία, και ορίζει
        // πόσες σελίδες υπάρχουν για το κατ' απαίτηση κατέβασμα. Όταν λείπει, η
        // κατηγορία δεν ξέρει πού τελειώνει.
        if (loggedHeadSample.compareAndSet(false, true)) {
            Log.d(
                "CatalogLoad",
                "ΚΕΦΑΛΙ ΚΑΤΗΓΟΡΙΑΣ — total_items=$total · max_page_items=$per · " +
                    "γραμμές=${rows.size} · δρόμος=" +
                    (if (total <= 0) "ΣΕΙΡΙΑΚΟΣ (έως 500 σελίδες μία-μία)" else "παράλληλος"),
            )
        }
        return CategoryHead(rows = rows, total = total, per = per)
    }

    /**
     * ΜΙΑ γραμμή καταλόγου → [Channel]. ΤΟ ΜΟΝΑΔΙΚΟ ΣΗΜΕΙΟ ΠΟΥ ΤΟ ΚΑΝΕΙ.
     *
     * ΓΙΑΤΙ ΒΓΗΚΕ ΕΞΩ: το χτίσιμο ζούσε μέσα σε τοπική συνάρτηση του
     * [getVodLike] και δεν ήταν προσβάσιμο από αλλού. Με το κατ' απαίτηση
     * κατέβασμα υπάρχουν πλέον ΔΥΟ δρόμοι που φτιάχνουν [Channel] από γραμμή
     * παρόχου, και αν ο δεύτερος έγραφε τα δικά του `optString`, μια αλλαγή
     * πεδίου θα τον άφηνε ΣΙΩΠΗΛΑ πίσω. Σιωπηλές αποκλίσεις σε αυτό το αρχείο
     * έχουν ήδη κοστίσει εκδόσεις.
     */
    private fun vodRowToChannel(ch: JSONObject, type: String, group: String): Channel {
        val rawCmd = ch.optString("cmd")
        // Σε αυτό το portal το "series_id" είναι κενό στη γραμμή της σειράς — το
        // πραγματικό αναγνωριστικό είναι το "id". Διαβάζοντας μόνο "series_id",
        // ο normalizer έπεφτε σε τοπικό (local:) hash και τα επεισόδια δεν
        // βρίσκονταν ποτέ.
        val seriesId = if (type == "series") {
            ch.optString("id").ifBlank { ch.optString("series_id") }
        } else ""
        return Channel(
            name = ch.optString("name", "Άγνωστο"),
            group = group,
            logo = ch.optString("screenshot_uri").ifEmpty { ch.optString("logo") },
            // Μια σειρά ΔΕΝ παίζεται η ίδια — μόνο τα επεισόδιά της. Το cmd μένει
            // κενό ώστε ο normalizer να την αναγνωρίζει ως container (ίδια
            // σύμβαση με Xtream/M3U).
            cmd = if (type == "series") "" else rawCmd,
            streamId = ch.optString("id").ifEmpty { ch.optString("movie_id") },
            kind = if (type == "series") "series" else "vod",
            seriesId = seriesId,
            plot = ch.optString("description"),
            cast = ch.optString("actors"),
            director = ch.optString("director"),
            genre = ch.optString("genres_str").ifEmpty { ch.optString("genre") },
            year = ch.optString("year"),
            duration = ch.optString("time").ifEmpty { ch.optString("duration") },
            // Ο πάροχος ΞΕΡΕΙ το TMDB id και το στέλνει σε δύο πεδία με την ίδια
            // τιμή («tmdb_id» και «tmdb»), ώστε η αναζήτηση με τίτλο να είναι
            // εφεδρεία και όχι ο κανόνας.
            tmdbId = ch.optString("tmdb_id").ifEmpty { ch.optString("tmdb") },
            // Τα δύο πεδία που κάνουν τις ράγες «Νέα» και «Κορυφαία» να σημαίνουν
            // κάτι. Χωρίς αυτά, η πρώτη ταξινομεί με το έτος παραγωγής και η
            // δεύτερη με τίποτα.
            addedAt = ch.optString("added"),
            rating = ch.optString("rating_imdb").ifEmpty { ch.optString("rating_kinopoisk") },
        )
    }

    /**
     * ΜΙΑ σελίδα μιας κατηγορίας, κατ' απαίτηση. Ο πυρήνας του νέου μοντέλου.
     *
     * ΤΙ ΑΝΤΙΚΑΘΙΣΤΑ: μέχρι το 1.95.0 ο μόνος τρόπος να δει ο χρήστης τη 300ή
     * ταινία μιας κατηγορίας ήταν να κατέβει ΟΛΟΚΛΗΡΟΣ ο κατάλογος — 4.372
     * αιτήματα, 4,5 λεπτά. Τώρα κατεβαίνει η σελίδα που κοιτάζει: ένα αίτημα,
     * ~0,15 δευτερόλεπτα, 14 στοιχεία.
     *
     * Χτίζει [Channel] μέσω του κοινού [vodRowToChannel], ποτέ με δικά του
     * `optString`. Κενή λίστα σημαίνει «τέλος» ή «απέτυχε» — ο καλών δεν πρέπει
     * να τα ξεχωρίζει, γιατί και στις δύο περιπτώσεις δεν υπάρχει τι να δείξει.
     *
     * ΔΕΝ ΑΓΓΙΖΕΙ ΤΟΥΣ ΜΕΤΡΗΤΕΣ ΤΗΣ ΦΟΡΤΩΣΗΣ: η γραμμή `ΣΥΝΟΨΗ` περιγράφει την
     * εκκίνηση· αν προσθέταμε εδώ, τα νούμερα της εκκίνησης θα μεγάλωναν για
     * πάντα όσο ο χρήστης σκρολάρει και δεν θα σήμαιναν τίποτα.
     */
    fun categoryPage(
        type: String,
        categoryId: String,
        categoryTitle: String,
        page: Int,
    ): List<Channel> {
        if (base == null) connect()
        if (page < 1) return emptyList()
        val url = if (type == "itv") {
            "$base?type=itv&action=get_ordered_list&genre=$categoryId&fav=0&sortby=number&p=$page&JsHttpRequest=1-xml"
        } else {
            "$base?type=$type&action=get_ordered_list&category=$categoryId&p=$page&JsHttpRequest=1-xml"
        }
        val arr = try {
            JSONObject(providerText(url, activeHeaders()))
                .optJSONObject("js")?.optJSONArray("data")
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            Log.w("CatalogLoad", "ΚΑΤ' ΑΠΑΙΤΗΣΗ: σελίδα $page της «$categoryId» απέτυχε", e)
            return emptyList()
        } ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            .map { vodRowToChannel(it, type, categoryTitle) }
    }

    /** Οι σελίδες 2..N μιας κατηγορίας. Η 1η έχει ήδη παραδοθεί. */
    private fun fetchRestOfPages(urlFor: (Int) -> String, total: Int, per: Int): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        if (total <= 0) {
            // δεν δηλώνει σύνολο (σπάνιο): σειριακά μέχρι κενή σελίδα, με δικλείδα
            //
            // ΑΥΤΟΣ Ο ΔΡΟΜΟΣ ΔΕΝ ΧΡΗΣΙΜΟΠΟΙΕΙ ΚΑΘΟΛΟΥ ΤΟ [pagePool]. Έξι νήματα
            // κάθονται άεργα ενώ ένα κατεβάζει σελίδα-σελίδα, και η μπάρα δεν
            // κουνιέται μέχρι να τελειώσει ΟΛΟΚΛΗΡΗ η κατηγορία.
            Log.w(
                "CatalogLoad",
                "ΣΕΙΡΙΑΚΟ ΚΑΤΕΒΑΣΜΑ ΚΑΤΗΓΟΡΙΑΣ: το portal δεν δήλωσε «total_items», " +
                    "οπότε οι σελίδες 2..500 κατεβαίνουν ΜΙΑ-ΜΙΑ χωρίς παραλληλισμό. " +
                    "Αν αυτή η γραμμή εμφανίζεται πολλές φορές, εκεί πάει ο χρόνος.",
            )
            var p = 2
            while (p <= 500) {
                ensureRequestActive()
                pageRequests.incrementAndGet()
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

        val wanted = (total + per - 1) / per
        val pages = wanted.coerceAtMost(500)
        if (wanted > pages) {
            // ΣΙΩΠΗΛΗ ΑΠΩΛΕΙΑ ΠΕΡΙΕΧΟΜΕΝΟΥ. Το όριο των 500 σελίδων υπάρχει ως
            // δικλείδα, αλλά όταν χτυπηθεί ο χρήστης χάνει στοιχεία χωρίς κανένα
            // σφάλμα. Με 14 στοιχεία/σελίδα, 500 σελίδες είναι 7.000 στοιχεία —
            // ένας μεγάλος κατάλογος το περνάει άνετα.
            Log.w(
                "CatalogLoad",
                "ΟΡΙΟ ΣΕΛΙΔΩΝ: ζητήθηκαν $wanted σελίδες ($total στοιχεία, $per ανά σελίδα) " +
                    "αλλά κατεβαίνουν μόνο $pages. Χάνονται στοιχεία.",
            )
        }
        if (pages <= 1) return out
        // ΟΙ ΣΕΛΙΔΕΣ ΠΟΥ ΕΠΕΣΑΝ ΠΑΡΟΔΙΚΑ ΔΕΝ ΧΑΝΟΝΤΑΙ ΠΙΑ.
        //
        // ΜΕΤΡΗΜΕΝΟ στο portal του ιδιοκτήτη στο 1.87.0: στη λήψη ταινιών
        // εμφανίστηκαν «σφάλμα, σελίδα 4 / σελίδα 6: HTTP 503» μέσα στο πρώτο
        // δευτερόλεπτο. Το 503 σημαίνει «τώρα δεν μπορώ», όχι «δεν υπάρχει» —
        // και μέχρι τώρα αυτές οι σελίδες πετάγονταν σιωπηλά.
        val transientPages = java.util.Collections.synchronizedList(ArrayList<Int>())
        val futures = (2..pages).map { p ->
            pagePool.submit<List<JSONObject>> {
                try {
                    ensureRequestActive()
                    pageRequests.incrementAndGet()
                    val arr = JSONObject(providerText(urlFor(p), activeHeaders()))
                        .optJSONObject("js")?.optJSONArray("data")
                    if (arr == null) {
                        pageFailures.incrementAndGet()
                        notePageFailure("άδεια απόκριση, σελίδα $p")
                        emptyList()
                    } else (0 until arr.length()).map { arr.getJSONObject(it) }
                } catch (e: Exception) {
                    rethrowIfCancelled(e)
                    pageFailures.incrementAndGet()
                    notePageFailure("σφάλμα, σελίδα $p", e)
                    if (isTransientFailure(e)) transientPages.add(p)
                    emptyList()
                }
            }
        }
        futures.forEach { future ->
            ensureRequestActive()
            out.addAll(future.get())
        }

        // ΔΕΥΤΕΡΟ ΠΕΡΑΣΜΑ, ΣΕΙΡΙΑΚΟ, ΧΩΡΙΣ ΚΑΜΙΑ ΑΝΑΜΟΝΗ.
        //
        // ΓΙΑΤΙ ΣΕΙΡΙΑΚΟ ΚΑΙ ΟΧΙ BACKOFF: το `Thread.sleep` είναι απαγορευμένο σε
        // αυτό το repo και ο έλεγχος `risk_inventory.py` το επιβάλλει. Δεν
        // χρειάζεται όμως: το δεύτερο πέρασμα τρέχει ΑΦΟΥ έχει αδειάσει το
        // παράλληλο παράθυρο, άρα ήδη βρίσκει το portal σε ένα αίτημα τη φορά.
        // Αυτό είναι η ίδια εκτόνωση που θα έδινε μια αναμονή, χωρίς αναμονή.
        val pending = transientPages.toList().sorted()
        for (p in pending) {
            ensureRequestActive()
            pageRequests.incrementAndGet()
            val arr = try {
                JSONObject(providerText(urlFor(p), activeHeaders()))
                    .optJSONObject("js")?.optJSONArray("data")
            } catch (e: Exception) {
                rethrowIfCancelled(e)
                null
            } ?: continue
            for (i in 0 until arr.length()) out.add(arr.getJSONObject(i))
            pageRecoveries.incrementAndGet()
        }
        return out
    }

    private fun fetchAllPages(urlFor: (Int) -> String): List<JSONObject> {
        val head = fetchFirstPage(urlFor) ?: return emptyList()
        if (head.rows.isEmpty()) return emptyList()
        val out = ArrayList<JSONObject>(head.rows)
        out.addAll(fetchRestOfPages(urlFor, head.total, head.per))
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
        // ΚΥΛΙΟΜΕΝΟ ΠΑΡΑΘΥΡΟ, ΟΧΙ ΟΛΕΣ ΜΑΖΙ. Δες [CATEGORY_WINDOW] για το γιατί:
        // το `ids.map { submit }` που ήταν εδώ κρατούσε ολόκληρο τον κατάλογο σε
        // JSONObject και έσκαγε από μνήμη στη μέση των ταινιών.
        val inFlight = ArrayDeque<Pair<String, java.util.concurrent.Future<List<JSONObject>>>>()
        var next = 0
        var delivered = 0
        fun submitNext() {
            if (next >= ids.size) return
            val id = ids[next++]
            inFlight.addLast(
                id to categoryPool.submit<List<JSONObject>> {
                    fetchAllPages(urlFor = { page -> urlFor(id, page) })
                }
            )
        }
        try {
            while (inFlight.size < CATEGORY_WINDOW && next < ids.size) submitNext()
            while (inFlight.isNotEmpty()) {
                ensureRequestActive()
                val (id, future) = inFlight.removeFirst()
                delivered++
                // Η παράδοση γίνεται σε ΞΕΧΩΡΙΣΤΗ ΜΕΘΟΔΟ επίτηδες: όταν το frame
                // της φύγει, φεύγει και η τελευταία αναφορά στις γραμμές, ώστε ο
                // GC να τις πάρει ΠΡΙΝ υποβάλουμε την επόμενη κατηγορία.
                deliverCategory(id, future, delivered, ids.size, onCategoryDone)
                submitNext()
            }
        } catch (error: Throwable) {
            // Ακύρωση ή σφάλμα: μη αφήνεις τα υπόλοιπα αιτήματα να τρέχουν και να
            // καταναλώνουν δίκτυο για δεδομένα που κανείς δεν θα δει.
            inFlight.forEach { (_, future) -> future.cancel(true) }
            throw error
        }
    }

    /**
     * Παραδίδει ΜΙΑ κατηγορία και αφήνει τις γραμμές της να πεθάνουν με το frame.
     *
     * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΗ ΜΕΘΟΔΟΣ ΚΑΙ ΟΧΙ ΤΟΠΙΚΗ ΜΕΤΑΒΛΗΤΗ: μια τοπική `val rows`
     * μέσα στον βρόχο μένει ζωντανή για τον GC μέχρι το τέλος της επανάληψης,
     * δηλαδή και ΜΕΤΑ την υποβολή της επόμενης κατηγορίας. Σε κατάλογο που
     * ακουμπάει το όριο του σωρού, αυτή η επικάλυψη είναι ακριβώς η κατανομή που
     * δεν χωράει.
     */
    private fun deliverCategory(
        id: String,
        future: java.util.concurrent.Future<List<JSONObject>>,
        done: Int,
        total: Int,
        onCategoryDone: (categoryId: String, rows: List<JSONObject>, done: Int, total: Int) -> Unit,
    ) {
        val rows = try {
            future.get()
        } catch (wrapped: java.util.concurrent.ExecutionException) {
            // Το ExecutionException κρύβει το πραγματικό σφάλμα. Το
            // ξετυλίγουμε ώστε η ακύρωση να αναγνωρίζεται ως ακύρωση.
            val cause = wrapped.cause
            if (cause is Exception) rethrowIfCancelled(cause)
            emptyList()
        }
        onCategoryDone(id, rows, done, total)
    }

    private fun getVodLike(
        type: String,
        catIds: List<String>?,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null,
        onCategories: SourceCategoriesCallback? = null,
        onCategoryTotals: SourceCategoryTotalsCallback? = null,
    ): List<Channel> {
        if (base == null) connect()
        onProgress?.invoke(5, "Σύνδεση Stalker έτοιμη")
        val allCats = getCategories(type)
        onCategories?.invoke(allCats)
        val cats = allCats.toMap()
        val ids = catIds ?: allCats.map { it.first }
        onProgress?.invoke(12, "Βρέθηκαν ${ids.size} κατηγορίες")
        val fallbackGroup = if (type == "vod") "Ταινίες" else "Σειρές"
        val result = ArrayList<Channel>()
        val seen = HashSet<String>()
        // ΜΙΑ γραμμή δείγματος ανά ενότητα, όχι ανά στοιχείο.
        //
        // Η μόνη πραγματική απάντηση αυτού του portal που έχει δει κανείς είναι
        // η λίστα ΣΕΖΟΝ, και από εκεί διαβάστηκαν τα ονόματα πεδίων `rating_imdb`
        // και `added`. Αν οι σελίδες ΚΑΤΑΛΟΓΟΥ δεν τα στέλνουν — ή τα στέλνουν με
        // άλλο όνομα — οι ράγες «Κορυφαίες» και «Νέα» γυρνούν σιωπηλά στην παλιά
        // συμπεριφορά και το σύμπτωμα είναι ακριβώς «όλα τυχαία», χωρίς κανένα
        // σφάλμα πουθενά. Αυτή η γραμμή είναι η διαφορά ανάμεσα στο να το ξέρουμε
        // και στο να το μαντεύουμε.
        var loggedSample = false

        /**
         * ΤΟ ΠΡΑΓΜΑΤΙΚΟ ΠΛΗΘΟΣ ΚΑΘΕ ΚΑΤΗΓΟΡΙΑΣ, ΓΝΩΣΤΟ ΑΠΟ 14 ΣΤΟΙΧΕΙΑ.
         *
         * Η 1η σελίδα κάθε κατηγορίας κουβαλάει `total_items`. Το διαβάζαμε ήδη
         * για να υπολογίσουμε πόσες σελίδες λείπουν, και μετά το πετούσαμε — ενώ
         * είναι ακριβώς το νούμερο που κάνει τα πλήθη και τη σειρά των ραγών να
         * είναι ΣΩΣΤΑ όσο ο κατάλογος κατεβαίνει ακόμη.
         */
        val categoryTotals = java.util.LinkedHashMap<String, Int>()

        fun append(rows: List<JSONObject>, fallbackCategoryId: String) {
            rows.forEach { ch ->
                val stableKey = ch.optString("id").ifBlank {
                    ch.optString("movie_id").ifBlank { ch.optString("series_id").ifBlank { ch.optString("name") } }
                }
                if (!seen.add(stableKey)) return@forEach
                if (!loggedSample) {
                    loggedSample = true
                    Log.d(
                        "CatalogLoad",
                        "δείγμα γραμμής καταλόγου ($type) — rating_imdb=«${ch.optString("rating_imdb")}» " +
                            "rating_kinopoisk=«${ch.optString("rating_kinopoisk")}» " +
                            "added=«${ch.optString("added")}» year=«${ch.optString("year")}» " +
                            "tmdb_id=«${ch.optString("tmdb_id")}»\nΩΜΗ ΓΡΑΜΜΗ: $ch",
                    )
                }
                // Η ομάδα βγαίνει από το `category_id` ΤΗΣ ΙΔΙΑΣ ΤΗΣ ΓΡΑΜΜΗΣ· το
                // `fallbackCategoryId` είναι μόνο εφεδρεία για portals που δεν το
                // στέλνουν μέσα στη γραμμή.
                val categoryId = ch.optString("category_id").ifBlank { fallbackCategoryId }
                result.add(vodRowToChannel(ch, type, cats[categoryId] ?: fallbackGroup))
                // Επεισόδια: ΟΧΙ εδώ. Το get_ordered_list της κατηγορίας δεν
                // κουβαλάει ποτέ επεισόδια σε αυτό το API (επιβεβαιωμένο σε
                // πραγματικό portal: πάντα "cmd":"" και "series":[] σε αυτή τη
                // γραμμή) — χρειάζεται το ξεχωριστό αίτημα σε [seriesEpisodes].
                if (result.size % 100 == 0) onPartial?.invoke(result)
            }
        }

        val label = if (type == "vod") "ταινιών" else "σειρών"
        val startedAtMs = System.currentTimeMillis()
        resetLoadCounters()
        val catCount = ids.count { it.isNotBlank() }
        forEachCategoryBreadthFirst(
            ids = ids.filter { it.isNotBlank() },
            urlFor = { cid, page ->
                "$base?type=$type&action=get_ordered_list&category=$cid&p=$page&JsHttpRequest=1-xml"
            },
            onCategoryTotal = { cid, totalItems, pageSize ->
                // ΔΗΜΟΣΙΕΥΕΤΑΙ ΟΣΟ ΕΡΧΕΤΑΙ, ΟΧΙ ΣΤΟ ΤΕΛΟΣ. Τα πλήθη έχουν αξία
                // ακριβώς όσο ο κατάλογος κατεβαίνει· στο τέλος τα ξέρει ήδη
                // όποιος μέτρησε τα φορτωμένα.
                synchronized(categoryTotals) {
                    categoryTotals[cid] = totalItems
                    // Το μέγεθος σελίδας το ορίζει ο πάροχος και είναι ίδιο για
                    // όλες τις κατηγορίες. Ταξιδεύει μαζί με τα πλήθη ώστε η οθόνη
                    // να βγάζει την ΕΠΟΜΕΝΗ σελίδα από το πόσα έχει ήδη.
                    onCategoryTotals?.invoke(LinkedHashMap(categoryTotals), pageSize)
                }
            },
        ) { cid, rows, done, total, firstPass ->
            append(rows, cid)
            // ΤΟ ΠΡΩΤΟ ΠΕΡΑΣΜΑ ΠΑΙΡΝΕΙ ΣΧΕΔΟΝ ΤΟ ΜΙΣΟ ΤΗΣ ΜΠΑΡΑΣ ΓΙΑ ΤΟ 4% ΤΩΝ
            // ΑΙΤΗΜΑΤΩΝ, ΣΚΟΠΙΜΑ: εκεί τελειώνει η αναμονή που νιώθει ο χρήστης.
            // Στο δεύτερο πέρασμα το κείμενο μένει στο «$catCount/$catCount» —
            // η αρίθμηση ΔΕΝ ξαναρχίζει, γιατί καμία νέα κατηγορία δεν εμφανίζεται
            // και μια μπάρα που γυρίζει πίσω μοιάζει με σφάλμα.
            val overall = if (firstPass) {
                12 + ((done.toDouble() / total.coerceAtLeast(1)) * 43.0).toInt()
            } else {
                55 + ((done.toDouble() / total.coerceAtLeast(1)) * 44.0).toInt()
            }
            val shown = if (firstPass) done else catCount
            onProgress?.invoke(overall.coerceIn(12, 99), "Λήψη $label · κατηγορία $shown/$catCount")
            if (result.isNotEmpty()) onPartial?.invoke(result)
        }
        logLoadSummary(type, ids.size, result.size, startedAtMs)
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
