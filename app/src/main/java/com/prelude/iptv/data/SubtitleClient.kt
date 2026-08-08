package com.prelude.iptv.data

import android.content.Context
import android.net.Uri
import com.prelude.iptv.net.Http
import com.prelude.iptv.net.ProviderCancellation
import com.prelude.iptv.player.PlayerSubtitlePolicy
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.URLEncoder

/**
 * Υπότιτλοι μέσω OpenSubtitles API v1.
 * ΧΡΕΙΑΖΕΤΑΙ ΔΩΡΕΑΝ API KEY από https://www.opensubtitles.com/consumers
 * (κάνε λογαριασμό → New Consumer → πάρε το Api-Key). Βάλ' το στο API_KEY.
 * Για λήψη (download) χρειάζεται και login (username/password) — δωρεάν quota.
 */
object SubtitleClient {

    // Ρυθμίζονται από τα Settings (αποθηκεύονται τοπικά)
    @Volatile var apiKey: String = ""
    @Volatile var osUser: String = ""
    @Volatile var osPass: String = ""
    private const val BASE = "https://api.opensubtitles.com/api/v1"
    private const val UA = "PreludeIPTV v1.40.11"

    @Volatile private var token: String? = null

    data class Sub(
        val fileId: Int,
        val name: String,
        val lang: String,
        val release: String,
        val downloads: Int,
        val featureTitle: String = "",
        val year: Int? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val matchPercent: Int = 0,
    )

    private fun headers(auth: Boolean = false): Map<String, String> {
        val h = linkedMapOf(
            "Api-Key" to apiKey,
            "User-Agent" to UA,
            "Accept" to "application/json",
            "Content-Type" to "application/json"
        )
        if (auth && token != null) h["Authorization"] = "Bearer $token"
        return h
    }

    fun hasKey(): Boolean = apiKey.isNotBlank()

    private fun ensureLogin() {
        if (token != null) return
        if (osUser.isNotBlank() && osPass.isNotBlank()) login(osUser, osPass)
    }

    /** Login για download quota. */
    fun login(username: String, password: String): Boolean {
        return try {
            val body = JSONObject().put("username", username).put("password", password).toString()
            val resp = JSONObject(Http.postJson("$BASE/login", body, headers()))
            token = resp.optString("token").ifEmpty { null }
            token != null
        } catch (error: Exception) {
            ProviderCancellation.rethrow(error, "Subtitle login cancelled")
            false
        }
    }

    /** Αναζήτηση με ακριβή ταυτότητα media + γλώσσα. */
    fun search(request: SubtitleSearchRequest, lang: String): List<Sub> {
        if (!hasKey() || request.title.isBlank()) return emptyList()
        val encoded = request.apiParameters(lang).entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
        val resp = JSONObject(Http.get("$BASE/subtitles?$encoded", headers()))
        val data: JSONArray = resp.optJSONArray("data") ?: return emptyList()
        val out = ArrayList<Sub>()
        for (i in 0 until data.length()) {
            val attr = data.getJSONObject(i).optJSONObject("attributes") ?: continue
            val files = attr.optJSONArray("files") ?: continue
            if (files.length() == 0) continue
            val file = files.getJSONObject(0)
            val fileId = file.optInt("file_id", -1)
            if (fileId < 0) continue
            val feature = attr.optJSONObject("feature_details")
            val candidate = Sub(
                    fileId = fileId,
                    name = file.optString("file_name", "subtitle"),
                    lang = attr.optString("language", lang),
                    release = attr.optString("release", ""),
                    downloads = attr.optInt("download_count", 0),
                    featureTitle = feature?.optString("title", "").orEmpty(),
                    year = feature?.optInt("year", 0)?.takeIf { it > 0 },
                    season = feature?.optInt("season_number", 0)?.takeIf { it > 0 },
                    episode = feature?.optInt("episode_number", 0)?.takeIf { it > 0 },
                )
            if (!SubtitleMatchPolicy.accepts(
                    request = request,
                    fileName = candidate.name,
                    release = candidate.release,
                    featureTitle = candidate.featureTitle,
                    year = candidate.year,
                    season = candidate.season,
                    episode = candidate.episode,
                )
            ) continue
            out += candidate.copy(
                matchPercent = SubtitleMatchPolicy.percent(
                    request = request,
                    fileName = candidate.name,
                    release = candidate.release,
                    featureTitle = candidate.featureTitle,
                    year = candidate.year,
                    season = candidate.season,
                    episode = candidate.episode,
                    downloads = candidate.downloads,
                )
            )
        }
        return out.sortedWith(
            compareByDescending<Sub> { it.matchPercent }
                .thenByDescending { it.downloads }
        )
    }

    /** Compatibility path for manual free-text search. */
    fun search(query: String, lang: String): List<Sub> =
        search(SubtitleSearchPolicy.generic(query), lang)

    /** Κατεβάζει τον υπότιτλο σε αρχείο cache και επιστρέφει Uri (ή null). */
    fun download(context: Context, fileId: Int): Uri? {
        if (!hasKey()) return null
        ensureLogin()
        return try {
            val body = JSONObject().put("file_id", fileId).toString()
            val resp = JSONObject(Http.postJson("$BASE/download", body, headers(auth = true)))
            val link = resp.optString("link")
            if (link.isBlank()) return null
            // ΑΠΟΚΩΔΙΚΟΠΟΙΗΣΗ ΠΡΙΝ ΤΗΝ ΑΠΟΘΗΚΕΥΣΗ.
            //
            // Πριν, εδώ υπήρχε readText(), που υποθέτει UTF-8. Πολλά ελληνικά SRT
            // στο OpenSubtitles είναι Windows-1253: το αρχείο γραφόταν με
            // κατεστραμμένους χαρακτήρες και ο υπότιτλος έβγαινε γεμάτος «».
            //
            // Ο παλιός player το παρέκαμπτε διαβάζοντας ξανά τα bytes μόνος του —
            // δηλαδή η διόρθωση ζούσε στον καταναλωτή, όχι στην πηγή. Όποιος
            // άλλος κατέβαζε υπότιτλο έπαιρνε τα χαλασμένα δεδομένα.
            val bytes = URL(link).readBytes()
            val dir = File(context.cacheDir, "subs").apply { mkdirs() }
            val f = File(dir, "sub_$fileId.srt")
            // decodeSubtitleBytes also unwraps gzip responses before charset conversion.
            f.writeText(PlayerSubtitlePolicy.decodeSubtitleBytes(bytes), Charsets.UTF_8)
            Uri.fromFile(f)
        } catch (error: Exception) {
            ProviderCancellation.rethrow(error, "Subtitle download cancelled")
            null
        }
    }

    /** Βολικό: ψάξε Ελληνικά, αλλιώς Αγγλικά, κατέβασε το καλύτερο match. */
    fun autoFetch(
        context: Context,
        request: SubtitleSearchRequest,
        preferredLanguage: String = ""
    ): Pair<Uri, String>? {
        for (lang in PlaybackPreferencePolicy.subtitleSearchLanguages(preferredLanguage)) {
            val res = try {
                search(request, lang)
            } catch (error: Exception) {
                ProviderCancellation.rethrow(error, "Subtitle search cancelled")
                emptyList()
            }
            val first = res.firstOrNull() ?: continue
            val uri = download(context, first.fileId) ?: continue
            return uri to lang
        }
        return null
    }

    fun autoFetch(context: Context, title: String): Pair<Uri, String>? =
        autoFetch(context, SubtitleSearchPolicy.generic(title))
}
