package com.prelude.iptv.data

import com.prelude.iptv.net.Http
import com.prelude.iptv.net.ProviderCancellation
import com.prelude.iptv.source.M3uParser
import com.prelude.iptv.source.StalkerClient
import com.prelude.iptv.source.XtreamClient

/** Αποτέλεσμα φόρτωσης: κανάλια + (προαιρετικά) ο ενεργός Stalker client για resolve. */
data class LoadResult(
    val channels: List<Channel>,
    val stalker: StalkerClient? = null,
    /** EPG που δηλώνει ο πάροχος μέσα στο ίδιο το M3U */
    val epgUrl: String = ""
)

object Repository {

    /** Clears only transient provider session state; catalog data is never cached. */
    fun invalidate(pl: Playlist) {
        if (pl.type == PlaylistType.XTREAM) {
            XtreamClient.invalidateSession(pl.server, pl.username)
        }
    }

    /** Φορτώνει μια playlist. Τρέχει σε background (IO) από τον caller. */
    fun load(pl: Playlist, contentType: String = "live", onProgress: SourceProgressCallback? = null): LoadResult {
        return when (pl.type) {
            PlaylistType.M3U -> {
                // ---- ΡΟΪΚΗ ΑΝΑΓΝΩΣΗ, ΧΩΡΙΣ ΟΛΟΚΛΗΡΟ ΤΟ ΑΡΧΕΙΟ ΣΤΗ ΜΝΗΜΗ ----
                //
                // Πριν, το M3U γινόταν ολόκληρο `String` και σαρωνόταν ΤΡΕΙΣ φορές:
                // `contains("#EXTINF")`, `parse()`, `epgUrlFrom()`.
                //
                // Λίστα με πλήρη κατάλογο VOD φτάνει τα 100–300 MB. Το `String` της
                // Java κρατά 2 bytes ανά χαρακτήρα, οπότε τα 150 MB γίνονται ~300 MB
                // — και όσο χτίζονται ζει ταυτόχρονα και το buffer των 150 MB.
                // Σχεδόν μισό gigabyte για ένα αρχείο κειμένου: **OutOfMemoryError**
                // σε TV box, και δεν λύνεται με μεγαλύτερο heap γιατί το επόμενο
                // αρχείο θα είναι μεγαλύτερο.
                //
                // Τώρα: ένα πέρασμα, μία γραμμή τη φορά, σταθερή μνήμη ανεξάρτητα
                // από το μέγεθος.
                onProgress?.invoke(1, "Προετοιμασία M3U…")
                var epgUrl = ""
                var sawM3uMarker = false
                val header: (String) -> Unit = { line ->
                    sawM3uMarker = true
                    if (epgUrl.isBlank()) epgUrl = M3uParser.epgUrlFromHeader(line)
                }
                val onParsed: (Int, Int) -> Unit = { processed, _ ->
                    onProgress?.invoke(null, "Ανάλυση M3U… $processed γραμμές")
                }

                // ΔΥΟ ΦΑΣΕΙΣ, ΟΧΙ ΜΙΑ: πρώτα λήψη σε αρχείο, μετά ανάλυση.
                //
                // Η ανάλυση κατευθείαν από τη ροή του δικτύου κρατούσε τη σύνδεση
                // ανοιχτή όσο κρατούσε ΟΛΟ το parsing — λεπτά, για λίστα με μισό
                // εκατομμύριο γραμμές. Ο πάροχος έκοβε τη σύνδεση και το σφάλμα
                // («Software caused connection abort») εμφανιζόταν ΑΦΟΥ είχαν
                // διαβαστεί όλες οι γραμμές: όλη η δουλειά γινόταν και πετιόταν.
                //
                // Με το αρχείο, η λήψη τρέχει με ταχύτητα δικτύου και η σύνδεση
                // κλείνει αμέσως. Η ανάλυση γίνεται μετά, τοπικά, με σταθερή μνήμη.
                var temp: java.io.File? = null
                val channels = try {
                    val file = if (pl.isUrl) {
                        // Μέσω του [CatalogDownloadManager]: κρατά τη διεργασία
                        // ζωντανή με υπηρεσία προσκηνίου, ώστε η λήψη να συνεχίζει
                        // όταν ο χρήστης αλλάξει εφαρμογή, και δεν ξεκινά δεύτερη
                        // λήψη για την ίδια διεύθυνση.
                        CatalogDownloadManager.download(
                            pl.source,
                            mapOf("User-Agent" to Http.DESKTOP_UA),
                        ) { read, total ->
                            val pct = total?.takeIf { it > 0L }?.let {
                                3 + ((read.toDouble() / it.toDouble()) * 66.0).toInt()
                            }
                            onProgress?.invoke(pct?.coerceIn(3, 69), "Λήψη M3U…")
                        }.also { temp = it }
                    } else {
                        // Τοπικό αρχείο: δεν χρειάζεται αντιγραφή, διαβάζεται όπως είναι.
                        java.io.File(pl.source)
                    }
                    file.bufferedReader(Charsets.UTF_8).use { reader ->
                        M3uParser.parse(reader.lineSequence(), onProgress = onParsed, onHeader = header)
                    }
                } finally {
                    // ΠΑΝΤΑ, ακόμη και σε σφάλμα: αλλιώς κάθε αποτυχημένη λήψη
                    // αφήνει πίσω της εκατοντάδες megabyte στην cache της συσκευής.
                    temp?.delete()
                }

                // Ο έλεγχος εγκυρότητας γίνεται ΜΕΤΑ, από το αποτέλεσμα. Πριν
                // απαιτούσε δεύτερη σάρωση του κειμένου· τώρα ξέρουμε ήδη αν
                // βρέθηκε κεφαλίδα ή έστω ένα κανάλι.
                if (channels.isEmpty() && !sawM3uMarker) {
                    throw RuntimeException("Δεν μοιάζει με έγκυρο M3U playlist.")
                }
                onProgress?.invoke(100, "Το M3U είναι έτοιμο")
                LoadResult(channels, epgUrl = epgUrl)
            }
            PlaylistType.XTREAM -> {
                val ch = when (contentType) {
                    "vod" -> XtreamClient.vod(pl.server, pl.username, pl.password, onProgress = onProgress)
                    "series" -> XtreamClient.seriesList(pl.server, pl.username, pl.password, onProgress = onProgress)
                    else -> XtreamClient.live(pl.server, pl.username, pl.password, pl.output, onProgress = onProgress)
                }
                LoadResult(ch)
            }
            PlaylistType.STALKER -> {
                val cli = StalkerClient(pl.portal, pl.mac, pl.userAgent)
                onProgress?.invoke(2, "Σύνδεση Stalker…")
                cli.connect()
                LoadResult(cli.getChannels(onProgress = onProgress), cli)
            }
        }
    }

    /** Δίνει το playable URL για ένα κανάλι (κάνει resolve αν είναι Stalker). */
    /**
     * ΓΡΗΓΟΡΟ τεστ M3U: διαβάζει μόνο την αρχή του αρχείου.
     * Δεν έχει νόημα να κατέβουν 50MB για να δούμε αν λέει #EXTM3U.
     */
    fun testM3u(url: String, isUrl: Boolean = true): Pair<Boolean, String> = try {
        val head = if (isUrl) Http.probe(url, 4096)
        else java.io.File(url).inputStream().use { st ->
            val b = ByteArray(4096); val n = st.read(b); String(b, 0, maxOf(n, 0))
        }
        if (head.contains("#EXTM3U") || head.contains("#EXTINF")) {
            val epg = M3uParser.epgUrlFrom(head)
            true to ("Έγκυρο M3U" + if (epg.isNotBlank()) " — βρέθηκε και EPG" else "")
        } else {
            false to "Δεν μοιάζει με M3U (λείπει το #EXTM3U στην αρχή)."
        }
    } catch (error: Exception) {
        ProviderCancellation.rethrow(error, "M3U probe cancelled")
        false to (error.message ?: "άγνωστο σφάλμα")
    }

    /** Το standard EPG endpoint του Xtream Codes. */
    fun xtreamXmltvUrl(pl: Playlist): String {
        if (pl.server.isBlank()) return ""
        val base = pl.server.trim().trimEnd('/')
        val u = java.net.URLEncoder.encode(pl.username, "UTF-8")
        val p = java.net.URLEncoder.encode(pl.password, "UTF-8")
        return "$base/xmltv.php?username=$u&password=$p"
    }

    /**
     * Η διεύθυνση που θα δοθεί στον player.
     *
     * ΤΟ `vod` ΔΕΝ ΕΙΝΑΙ ΛΕΠΤΟΜΕΡΕΙΑ: για ταινίες και επεισόδια το Stalker
     * **απαιτεί** `create_link` σε κάθε αναπαραγωγή. Ο σύνδεσμος μέσα στο `cmd`
     * είναι ανενεργός και ο πάροχος απαντά HTTP 404 — που μοιάζει με χαλασμένο
     * αρχείο, ενώ είναι ανενεργό token. Δες [StalkerClient.resolve].
     */
    fun playableUrl(ch: Channel, stalker: StalkerClient?): String {
        return if (ch.cmd.isNotEmpty() && stalker != null) {
            stalker.resolve(ch.cmd, vod = ch.kind != "live")
        } else ch.url
    }

    // -------- επιλογή κατηγοριών (για να μη φορτώνει τα πάντα) --------

    fun stalkerConnect(pl: Playlist): StalkerClient {
        val cli = StalkerClient(pl.portal, pl.mac, pl.userAgent)
        cli.connect()
        return cli
    }

    fun stalkerCategories(cli: StalkerClient, contentType: String): List<Pair<String, String>> =
        when (contentType) {
            "vod" -> cli.getVodCategories()
            "series" -> cli.getSeriesCategories()
            else -> cli.getGenres()
        }

    fun stalkerLoad(
        cli: StalkerClient,
        contentType: String,
        ids: List<String>?,
        onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> = when (contentType) {
        "vod" -> cli.getVodChannels(ids, onProgress, onPartial)
        "series" -> cli.getSeriesChannels(ids, onProgress, onPartial)
        else -> cli.getChannels(ids, onProgress, onPartial)
    }

    fun xtreamLiveCategories(pl: Playlist, onProgress: SourceProgressCallback? = null): List<Pair<String, String>> =
        XtreamClient.liveCategories(pl.server, pl.username, pl.password, onProgress)

    fun xtreamLiveSelected(
        pl: Playlist, ids: List<String>?, onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> =
        XtreamClient.live(pl.server, pl.username, pl.password, pl.output, ids, onProgress, onPartial)

    fun xtreamVodCategories(pl: Playlist, onProgress: SourceProgressCallback? = null): List<Pair<String, String>> =
        XtreamClient.vodCategories(pl.server, pl.username, pl.password, onProgress)

    fun xtreamVodSelected(
        pl: Playlist, ids: List<String>?, onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> =
        XtreamClient.vod(pl.server, pl.username, pl.password, ids, onProgress, onPartial)

    fun xtreamSeriesCategories(pl: Playlist, onProgress: SourceProgressCallback? = null): List<Pair<String, String>> =
        XtreamClient.seriesCategories(pl.server, pl.username, pl.password, onProgress)

    fun xtreamSeriesSelected(
        pl: Playlist, ids: List<String>?, onProgress: SourceProgressCallback? = null,
        onPartial: SourcePartialCallback? = null
    ): List<Channel> =
        XtreamClient.seriesList(pl.server, pl.username, pl.password, ids, onProgress, onPartial)

    fun xtreamVodInfo(pl: Playlist, streamId: String): Map<String, String> =
        XtreamClient.vodInfo(pl.server, pl.username, pl.password, streamId)
}
