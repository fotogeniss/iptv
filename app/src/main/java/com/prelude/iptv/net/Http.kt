package com.prelude.iptv.net

import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import java.util.concurrent.TimeUnit

object Http {
    const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"

    val STB_UAS = listOf(
        "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) " +
            "MAG250 stbapp ver: 2 rev: 250 Safari/533.3",
        "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) " +
            "MAG254 stbapp ver: 4 rev: 2721 Safari/533.3",
        "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) " +
            "MAG200 stbapp ver: 4 rev: 2721 Safari/533.3"
    )

    private fun newClient(): OkHttpClient = OkHttpClient.Builder()
        // connect 10s: αν δεν άνοιξε σύνδεση σε 10", δεν θα ανοίξει και σε 20 —
        // απλά κρατούσε τον χρήστη να περιμένει διπλάσια σε νεκρά portals.
        // Το read μένει 40s για μεγάλα M3U/EPG αρχεία.
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** General app traffic: subtitles, metadata, EPG and diagnostics. */
    private val client = newClient()

    /**
     * Catalog/provider traffic only. Keeping a dedicated client lets the
     * ViewModel cancel an obsolete Live/VOD/Series request without interrupting
     * subtitle downloads, TMDB metadata or unrelated app traffic.
     */
    private val providerClient = newClient()

    fun cancelProviderRequests() {
        providerClient.dispatcher.cancelAll()
    }

    data class Resp(val code: Int, val body: String)

    private fun freshRequest(url: String): Request.Builder = Request.Builder().url(url)
        .header("Cache-Control", "no-cache, no-store, max-age=0")
        .header("Pragma", "no-cache")

    /**
     * GET που επιστρέφει ΡΟΗ — για μεγάλα αρχεία (XMLTV EPG 10-40MB).
     * Το get() θα τα έφερνε ολόκληρα ως String στη μνήμη = OOM σε TV box.
     * Ο καλών ΠΡΕΠΕΙ να κάνει close (use { }).
     */
    fun stream(url: String, headers: Map<String, String> = mapOf("User-Agent" to DESKTOP_UA)): java.io.InputStream {
        val req = freshRequest(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        val r = client.newCall(req).execute()
        if (!r.isSuccessful) { r.close(); throw RuntimeException("HTTP ${r.code}") }
        return r.body?.byteStream() ?: run { r.close(); throw RuntimeException("Κενή απόκριση") }
    }

    /** GET που επιστρέφει body· πετάει exception σε δικτυακό ΚΑΙ HTTP σφάλμα. */
    fun get(url: String, headers: Map<String, String> = mapOf("User-Agent" to DESKTOP_UA)): String {
        val req = freshRequest(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        client.newCall(req).execute().use { r ->
            // Πριν, ένα 403/404 επέστρεφε το HTML της σελίδας σφάλματος ως
            // «επιτυχία» και πιο κάτω έβγαινε παραπλανητικό «Δεν μοιάζει με M3U».
            if (!r.isSuccessful) throw RuntimeException("HTTP ${r.code}")
            return r.body?.string() ?: ""
        }
    }

    /**
     * GET με πραγματική πρόοδο bytes όταν ο server δηλώνει Content-Length.
     * Αν το μήκος είναι άγνωστο, το total είναι null και το UI δείχνει
     * indeterminate progress αντί για ψεύτικο ποσοστό.
     */
    fun getWithProgress(
        url: String,
        headers: Map<String, String> = mapOf("User-Agent" to DESKTOP_UA),
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): String {
        val req = freshRequest(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw RuntimeException("HTTP ${r.code}")
            val body = r.body ?: return ""
            val total = body.contentLength().takeIf { it > 0L }
            val out = java.io.ByteArrayOutputStream(
                total?.coerceAtMost(8L * 1024L * 1024L)?.toInt() ?: 32 * 1024
            )
            body.byteStream().use { input ->
                val buffer = ByteArray(32 * 1024)
                var readTotal = 0L
                var lastPercent = -1
                var lastUnknownReport = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    readTotal += read
                    if (total != null) {
                        val percent = ((readTotal * 100L) / total).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress?.invoke(readTotal, total)
                        }
                    } else if (readTotal - lastUnknownReport >= 256 * 1024L || lastUnknownReport == 0L) {
                        lastUnknownReport = readTotal
                        onProgress?.invoke(readTotal, null)
                    }
                }
                onProgress?.invoke(readTotal, total)
            }
            return out.toString(Charsets.UTF_8.name())
        }
    }

    /** GET που επιστρέφει (code, body) — και σε HTTP error, για διαγνωστικά. */
    fun getFull(url: String, headers: Map<String, String>): Resp {
        val req = freshRequest(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        client.newCall(req).execute().use { r ->
            return Resp(r.code, r.body?.string() ?: "")
        }
    }

    /**
     * Κατεβάζει ΜΟΝΟ τα πρώτα bytes και κλείνει τη σύνδεση.
     * Για γρήγορο έλεγχο M3U: δεν έχει νόημα να κατέβουν 50MB για να δούμε
     * αν η πρώτη γραμμή λέει #EXTM3U.
     */
    fun probe(url: String, maxBytes: Int = 4096): String {
        val req = freshRequest(url)
            .header("User-Agent", DESKTOP_UA)
            .header("Range", "bytes=0-$maxBytes")
            .build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful && r.code != 206) throw RuntimeException("HTTP ${r.code}")
            val src = r.body?.source() ?: return ""
            val buf = okio.Buffer()
            src.read(buf, maxBytes.toLong())
            return buf.readUtf8()
        }
    }

    fun postJson(url: String, jsonBody: String, headers: Map<String, String>): String {
        val body = okhttp3.RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), jsonBody
        )
        val req = freshRequest(url).post(body).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        client.newCall(req).execute().use { r ->
            return r.body?.string() ?: ""
        }
    }

    /** Provider-only GET. These calls can be cancelled as one catalog request family. */
    fun providerGet(
        url: String,
        headers: Map<String, String> = mapOf("User-Agent" to DESKTOP_UA)
    ): String {
        val req = freshRequest(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        providerClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code}")
            return response.body?.string() ?: ""
        }
    }

    fun providerGetFull(url: String, headers: Map<String, String>): Resp {
        val req = freshRequest(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        providerClient.newCall(req).execute().use { response ->
            return Resp(response.code, response.body?.string() ?: "")
        }
    }

    /**
     * Κατεβάζει provider απόκριση **σε αρχείο**, χωρίς να περάσει από τη μνήμη.
     *
     * ΓΙΑΤΙ ΑΡΧΕΙΟ ΚΑΙ ΟΧΙ ΚΑΤΕΥΘΕΙΑΝ ΑΝΑΛΥΣΗ:
     *
     * Η ανάλυση κατευθείαν από τη ροή λύνει τη μνήμη αλλά κρατά τη **σύνδεση
     * ανοιχτή όσο κρατά όλο το parsing** — λεπτά, για λίστα με μισό εκατομμύριο
     * γραμμές. Οι πάροχοι IPTV κόβουν τέτοιες συνδέσεις, και το αποτέλεσμα είναι
     * «Software caused connection abort» **αφού** έχουν διαβαστεί όλες οι γραμμές:
     * η δουλειά γίνεται και μετά πετιέται.
     *
     * Το αρχείο τα λύνει και τα δύο: η λήψη τρέχει με ταχύτητα δικτύου και η
     * σύνδεση κλείνει αμέσως· η ανάλυση γίνεται μετά, τοπικά, με σταθερή μνήμη.
     *
     * Κόστος: προσωρινός χώρος στον δίσκο όσο το αρχείο. Ασύγκριτα φθηνότερο από
     * μισό gigabyte RAM σε TV box.
     */
    fun providerDownloadTo(
        url: String,
        target: java.io.File,
        headers: Map<String, String> = mapOf("User-Agent" to DESKTOP_UA),
        /**
         * Πόσες φορές ξαναδοκιμάζει όταν η σύνδεση κοπεί στη μέση.
         *
         * Δεν είναι πολυτέλεια: μια λήψη 200 MB σε 4G ή σε φορτωμένο πάροχο IPTV
         * **θα** κοπεί. Το «unexpected end of stream» δεν σημαίνει χαλασμένη πηγή —
         * σημαίνει ότι το TCP πέθανε στο 60%.
         */
        maxRetries: Int = 5,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null,
    ) {
        var attempt = 0
        var total: Long? = null

        while (true) {
            // ΣΥΝΕΧΙΣΗ, ΟΧΙ ΕΠΑΝΕΚΚΙΝΗΣΗ.
            //
            // Ξαναρχίζοντας από το μηδέν, μια λήψη που κόβεται στο 90% δεν
            // τελειώνει ποτέ σε ασταθές δίκτυο: κάθε προσπάθεια είναι εξίσου
            // πιθανό να κοπεί, και ο χρήστης κοιτά μια μπάρα που γυρίζει πίσω.
            val already = if (target.exists()) target.length() else 0L
            val request = freshRequest(url).apply {
                headers.forEach { (k, v) -> header(k, v) }
                if (already > 0L) header("Range", "bytes=$already-")
            }.build()

            try {
                providerClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code}")
                    val body = response.body ?: throw RuntimeException("Κενή απόκριση")

                    // 206 = ο διακομιστής δέχτηκε τη συνέχιση. Οτιδήποτε άλλο
                    // (τυπικά 200) σημαίνει ότι στέλνει ΞΑΝΑ από την αρχή — τότε
                    // πρέπει να γράψουμε από την αρχή, αλλιώς το αρχείο γίνεται
                    // δύο μισά κολλημένα μεταξύ τους και σκάει στην ανάλυση.
                    val resumed = response.code == 206 && already > 0L
                    val offset = if (resumed) already else 0L
                    if (total == null) {
                        total = body.contentLength().takeIf { it > 0L }?.plus(offset)
                    }

                    body.byteStream().use { input ->
                        java.io.FileOutputStream(target, resumed).buffered().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var written = offset
                            var lastReport = offset
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                written += read
                                if (written - lastReport > 512 * 1024) {
                                    lastReport = written
                                    onProgress?.invoke(written, total)
                                }
                            }
                            onProgress?.invoke(written, total)
                        }
                    }
                }
                return
            } catch (interrupted: InterruptedException) {
                throw interrupted
            } catch (error: java.io.IOException) {
                attempt++
                // Χωρίς πρόοδο ΚΑΙ χωρίς προσπάθειες: παραδινόμαστε. Αν όμως
                // κάτι κατέβηκε, αξίζει να συνεχίσουμε — η επόμενη προσπάθεια
                // ξεκινά από εκεί.
                if (attempt > maxRetries) throw error
                // Μικρή αναμονή που μεγαλώνει: ένας διακομιστής που μόλις έκοψε
                // τη σύνδεση συνήθως χρειάζεται μια στιγμή.
                Thread.sleep((500L * attempt).coerceAtMost(4_000L))
            }
        }
    }

    /**
     * Provider GET που δίνει τις γραμμές ως **ροή**, χωρίς να φέρει το σώμα στη μνήμη.
     *
     * ΓΙΑΤΙ ΥΠΑΡΧΕΙ: το [providerGetWithProgress] χτίζει ολόκληρο το σώμα σε
     * `ByteArrayOutputStream` και μετά το κάνει `String`. Μια λίστα M3U με πλήρη
     * κατάλογο VOD φτάνει τα 100–300 MB· το `String` της Java κρατά **2 bytes ανά
     * χαρακτήρα**, οπότε τα 150 MB γίνονται ~300 MB — και όσο χτίζονται, ζει
     * ταυτόχρονα και το buffer των 150 MB από το οποίο προέκυψαν.
     *
     * Σχεδόν μισό gigabyte για ένα αρχείο κειμένου. Σε TV box με 1–2 GB συνολικά,
     * αυτό είναι `OutOfMemoryError` — και δεν διορθώνεται με μεγαλύτερο heap, γιατί
     * το επόμενο αρχείο θα είναι μεγαλύτερο.
     *
     * Εδώ δεν υπάρχει ποτέ στη μνήμη περισσότερο από μία γραμμή.
     *
     * Η [block] πρέπει να καταναλώσει τη [Sequence] ΜΕΣΑ στην κλήση: μόλις
     * επιστρέψει, η σύνδεση κλείνει.
     */
    fun <T> providerReadLines(
        url: String,
        headers: Map<String, String> = mapOf("User-Agent" to DESKTOP_UA),
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null,
        block: (Sequence<String>) -> T,
    ): T {
        val req = freshRequest(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        providerClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code}")
            val body = response.body ?: throw RuntimeException("Κενή απόκριση")
            val total = body.contentLength().takeIf { it > 0L }
            var readTotal = 0L
            var lastReport = 0L
            return body.byteStream().bufferedReader(Charsets.UTF_8).use { reader ->
                block(
                    reader.lineSequence().onEach { line ->
                        // Κατά προσέγγιση: μετράει χαρακτήρες, όχι bytes. Για μπάρα
                        // προόδου αρκεί — η ακριβής μέτρηση θα απαιτούσε δικό μας
                        // wrapper πάνω στο InputStream για κέρδος που δεν φαίνεται.
                        readTotal += line.length + 1
                        if (readTotal - lastReport > 512 * 1024) {
                            lastReport = readTotal
                            onProgress?.invoke(readTotal, total)
                        }
                    }
                )
            }
        }
    }

    fun providerGetWithProgress(
        url: String,
        headers: Map<String, String> = mapOf("User-Agent" to DESKTOP_UA),
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): String {
        val req = freshRequest(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        providerClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code}")
            val body = response.body ?: return ""
            val total = body.contentLength().takeIf { it > 0L }
            val output = java.io.ByteArrayOutputStream(
                total?.coerceAtMost(8L * 1024L * 1024L)?.toInt() ?: 32 * 1024
            )
            body.byteStream().use { input ->
                val buffer = ByteArray(32 * 1024)
                var readTotal = 0L
                var lastPercent = -1
                var lastUnknownReport = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    readTotal += read
                    if (total != null) {
                        val percent = ((readTotal * 100L) / total).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress?.invoke(readTotal, total)
                        }
                    } else if (readTotal - lastUnknownReport >= 256 * 1024L || lastUnknownReport == 0L) {
                        lastUnknownReport = readTotal
                        onProgress?.invoke(readTotal, null)
                    }
                }
                onProgress?.invoke(readTotal, total)
            }
            return output.toString(Charsets.UTF_8.name())
        }
    }

    fun looksLikeCloudflare(body: String): Boolean {
        if (body.isEmpty()) return false
        val low = body.lowercase()
        return listOf(
            "just a moment", "cf-browser-verification", "cf-chl", "cloudflare",
            "attention required", "__cf_", "challenge-platform"
        ).any { low.contains(it) }
    }
}
