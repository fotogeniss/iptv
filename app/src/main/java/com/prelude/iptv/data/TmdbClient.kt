package com.prelude.iptv.data

import android.content.Context
import android.util.Log
import com.prelude.iptv.net.Http
import com.prelude.iptv.net.ProviderCancellation
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

/**
 * Εμπλουτισμός ταινιών/σειρών από το TMDB: βαθμολογία, backdrop, poster,
 * περίληψη στα ελληνικά και ηθοποιοί ΜΕ φωτογραφία.
 *
 * Οι πάροχοι IPTV σπάνια δίνουν αυτά τα δεδομένα (σε M3U δεν υπάρχουν καθόλου),
 * οπότε ψάχνουμε με βάση τον τίτλο + έτος.
 *
 * API key: δωρεάν από themoviedb.org → Settings → API (v3 auth).
 */
object TmdbClient {

    private const val BASE = "https://api.themoviedb.org/3"

    /**
     * Ετικέτα διάγνωσης για την αναζήτηση τίτλου.
     *
     * Δεν είναι προσωρινός κώδικας. Το ίδιο μοτίβο (`SeriesLoad`) είναι ό,τι
     * τελικά έλυσε το πρόβλημα των επεισοδίων Stalker μετά από τρεις γύρους
     * λανθασμένων υποθέσεων· η αναζήτηση στο TMDB αποτυγχάνει με τον ίδιο
     * σιωπηλό τρόπο —κενό αποτέλεσμα, χωρίς σφάλμα— και είναι αδύνατο να
     * διαγνωστεί από την οθόνη. ΠΟΤΕ δεν καταγράφεται διεύθυνση: περιέχει το
     * κλειδί API.
     */
    private const val LOOKUP_TAG = "TmdbLookup"
    const val IMG_POSTER = "https://image.tmdb.org/t/p/w500"
    // w1280 αντί για w780: το backdrop γεμίζει οθόνη 1920px — με w780 γινόταν
    // 2.5x μεγέθυνση και «πιξέλιαζε» αισθητά στην τηλεόραση.
    const val IMG_BACKDROP = "https://image.tmdb.org/t/p/w1280"
    const val IMG_PROFILE = "https://image.tmdb.org/t/p/w185"
    /** Στιγμιότυπο επεισοδίου (16:9). w300 αρκεί για κάρτα ~245dp. */
    const val IMG_STILL = "https://image.tmdb.org/t/p/w300"

    /**
     * Πόσες κλήσεις TMDB τρέχουν ταυτόχρονα.
     *
     * Τέσσερις: αρκετές ώστε το πλέγμα να γεμίζει γρήγορα, λίγες ώστε να μη
     * χτυπάμε το όριο ρυθμού και να μην πνίγεται η σύνδεση ενός TV box τη στιγμή
     * που παίζει και βίντεο από τον ίδιο πάροχο.
     */
    private const val MAX_CONCURRENT_REQUESTS = 4

    data class Person(val name: String, val role: String, val photo: String?)

    /** Στοιχεία ενός επεισοδίου από το TMDB. Κενά πεδία = δεν υπάρχουν. */
    data class EpisodeMeta(
        val still: String = "",
        val title: String = "",
        val overview: String = "",
    )

    data class Meta(
        val tmdbId: Int = 0,
        val rating: String = "",
        val poster: String = "",
        val backdrop: String = "",
        val overview: String = "",
        val year: String = "",
        val genres: String = "",
        val cast: List<Person> = emptyList()
    )

    // @Volatile: γράφονται από το init/setKey και διαβάζονται από τα νήματα IO
    // που τρέχουν οι κάρτες. Χωρίς αυτό, ένα νήμα μπορεί να μη δει ποτέ το κλειδί.
    @Volatile private var apiKey: String = ""
    @Volatile private var lang: String = "el-GR"
    @Volatile private var prefs: android.content.SharedPreferences? = null

    // ---------------------------------------------------------------------
    // ΤΑΥΤΟΧΡΟΝΗ ΠΡΟΣΒΑΣΗ
    // ---------------------------------------------------------------------
    // Κάθε κάρτα στο πλέγμα ζητά μόνη της την αφίσα της, από δική της κορουτίνα.
    // Ένα πλέγμα 40 καρτών σημαίνει 40 νήματα να γράφουν στο ίδιο cache.
    //
    // Ήταν σκέτο HashMap. Η ταυτόχρονη εγγραφή σε HashMap δεν δίνει απλώς λάθος
    // τιμή: μπορεί να καταστρέψει την εσωτερική του δομή και να αφήσει ένα νήμα
    // σε ατέρμονο βρόχο. Δεν είχε εμφανιστεί ακόμη επειδή θέλει συγκεκριμένο
    // συγχρονισμό — ήταν θέμα χρόνου.
    private val memCache = ConcurrentHashMap<String, Meta>()

    /** Δεδομένα επεισοδίων ανά σεζόν (μία λήψη ανά σεζόν, όχι ανά επεισόδιο). */
    private val episodeMemCache = ConcurrentHashMap<String, Map<Int, EpisodeMeta>>()

    /**
     * ΕΔΩ ΥΠΗΡΧΕ ΑΡΝΗΤΙΚΟ CACHE — ΑΦΑΙΡΕΘΗΚΕ.
     *
     * Η ιδέα ήταν να μη ξαναρωτάμε το δίκτυο για τίτλους που το TMDB δεν γνωρίζει.
     * Το ελάττωμα: ΔΕΝ μπορούσε να ξεχωρίσει «δεν υπάρχει τέτοιος τίτλος» από
     * «η κλήση απέτυχε». Οι εσωτερικοί χειριστές σφαλμάτων επιστρέφουν κενό και
     * στις δύο περιπτώσεις.
     *
     * Στο άνοιγμα μιας κατηγορίας ξεκινούν δεκάδες αναζητήσεις μαζί. Όποια
     * τύχαινε να κοπεί —χρονικό όριο, όριο ρυθμού, στιγμιαία διακοπή— σημαδευόταν
     * ΜΟΝΙΜΑ ως «άγνωστη» και η αφίσα της δεν εμφανιζόταν ποτέ ξανά σε εκείνη τη
     * συνεδρία. Ακριβώς αυτό που φαινόταν ως «τα κουτάκια δεν έχουν εικόνα».
     *
     * Το κόστος της αφαίρεσης είναι μερικές επιπλέον κλήσεις για τίτλους που
     * όντως δεν υπάρχουν. Το κόστος της διατήρησης ήταν κενές αφίσες. Η επιλογή
     * δεν είναι δύσκολη — και η βελτιστοποίηση ήταν δική μου προσθήκη, όχι
     * απαίτηση της εφαρμογής.
     */

    /**
     * Ένα κλείδωμα ανά κλειδί, ώστε δέκα κάρτες της ίδιας σειράς να κάνουν ΕΝΑ
     * αίτημα και οι υπόλοιπες εννιά να περιμένουν το αποτέλεσμα.
     */
    private val keyLocks = ConcurrentHashMap<String, Any>()

    /**
     * Όριο ταυτόχρονων κλήσεων. Χωρίς αυτό, το άνοιγμα μιας κατηγορίας άνοιγε
     * δεκάδες συνδέσεις μαζί: το TMDB απαντούσε με όριο ρυθμού και οι αφίσες
     * έμεναν κενές — ακριβώς τότε που τις χρειαζόσουν όλες.
     */
    private val networkLimit = Semaphore(MAX_CONCURRENT_REQUESTS, true)

    private fun lockFor(key: String): Any = keyLocks.computeIfAbsent(key) { Any() }

    /** Εκτελεί δικτυακή δουλειά μέσα στο όριο ταυτόχρονων κλήσεων. */
    private fun <T> throttled(block: () -> T): T {
        networkLimit.acquire()
        return try {
            block()
        } finally {
            networkLimit.release()
        }
    }

    fun init(ctx: Context) {
        val p = ctx.getSharedPreferences("upl_prefs", Context.MODE_PRIVATE)
        prefs = p
        apiKey = PlaylistStore(ctx).tmdbKey
    }

    fun setKey(ctx: Context, key: String) {
        apiKey = key
        PlaylistStore(ctx).tmdbKey = key
    }

    fun hasKey(): Boolean = apiKey.isNotBlank()

    /* ------------------------------------------------------- normalize ---- */

    /**
     * Καθαρίζει τίτλους παρόχων: "4K Passenger (2026)" → "Passenger",
     * "GR| Ο Νονός HD [MULTI]" → "Ο Νονός".
     */
    fun cleanTitle(raw: String): String {
        var s = raw
        s = s.replace(Regex("""\((19|20)\d{2}\)"""), " ")          // έτος σε παρένθεση
        s = s.replace(Regex("""[\[\(][^\]\)]*[\]\)]"""), " ")       // [MULTI], (DUB) κλπ
        s = s.replace(Regex("""(?i)\b(4K|UHD|FHD|HD|SD|HEVC|H265|H264|1080p?|720p?|2160p?|WEB-?DL|BluRay|MULTI|DUB|SUB|VOSTFR|IMAX|EXTENDED|REMASTERED)\b"""), " ")
        s = s.replace(Regex("""^\s*[A-Z]{2,3}\s*[|:\-]\s*"""), "")   // "GR| ", "EN - "
        // Provider language tag at the end, e.g. "Hometown Cha-Cha-Cha (2021) DE".
        // Keep this case-sensitive so legitimate titles such as "It" are not removed.
        s = s.replace(
            Regex("""(?:\s*[|:\-]\s*|\s+)(?:DE|EN|ENG|GER|FR|FRE|IT|ITA|ES|SPA|EL|GR|GRE|PT|PL|RU|AR|TR|NL|CZ|RO|HU)$"""),
            " ",
        )
        s = s.replace(Regex("""(?i)\bS\d{1,2}\s*E\d{1,2}\b"""), " ") // S01E02
        s = s.replace(Regex("""[._]+"""), " ")
        s = s.replace(Regex("""\s{2,}"""), " ")
        return stripEdgeNoise(s)
    }

    /**
     * Διακοσμητικά σύμβολα που οι πάροχοι κολλούν γύρω από τον τίτλο.
     *
     * Πραγματικό παράδειγμα από λίστα: «To Spiti Dipla Sto Potami #». Ο τίτλος
     * είναι σωστός, το «#» είναι σήμανση του παρόχου — αλλά έφτανε αυτούσιο
     * στο ερώτημα προς το TMDB (κωδικοποιημένο ως %23) και δεν έβρισκε τίποτα.
     */
    private const val EDGE_NOISE = "#*~•·►▶◄★☆✦※|+=_<>«»–—-:"

    /**
     * Αφαιρεί θόρυβο ΜΟΝΟ από τα άκρα, ποτέ από το εσωτερικό.
     *
     * Η διάκριση δεν είναι λεπτομέρεια: το «M*A*S*H» χάνει την ταυτότητά του αν
     * αφαιρεθούν εσωτερικοί αστερίσκοι, και το ίδιο ισχύει για «9-1-1»,
     * «S.W.A.T.» ή «Sex/Life». Επαναληπτικά, ώστε «*** Τίτλος ***» να καθαρίζει
     * με μία κλήση. Θαυμαστικά και ερωτηματικά ΔΕΝ θεωρούνται θόρυβος: είναι
     * νόμιμο τέλος τίτλου.
     */
    private fun stripEdgeNoise(value: String): String {
        var current = value.trim()
        while (true) {
            val next = current.trim { it.isWhitespace() || it in EDGE_NOISE }
            if (next == current) return next
            current = next
        }
    }

    /** Βγάζει το έτος αν υπάρχει μέσα στον τίτλο ή στο πεδίο year. */
    fun extractYear(raw: String, fallback: String = ""): String {
        Regex("""\((19|20)(\d{2})\)""").find(raw)?.let { return it.groupValues[1] + it.groupValues[2] }
        Regex("""\b(19|20)(\d{2})\b""").find(fallback)?.let { return it.groupValues[1] + it.groupValues[2] }
        return ""
    }

    /* ------------------------------------------------------------ fetch --- */

    /**
     * Ψάχνει και επιστρέφει metadata. [isSeries] → αναζήτηση σε TV, αλλιώς σε ταινίες.
     * Επιστρέφει null αν δεν υπάρχει key ή δεν βρεθεί τίποτα.
     * ΠΡΟΣΟΧΗ: κάνει δικτυακή κλήση — κάλεσέ το σε IO dispatcher.
     */
    fun fetch(rawTitle: String, isSeries: Boolean, yearHint: String = ""): Meta? {
        if (!hasKey()) return null
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return null
        val year = extractYear(rawTitle, yearHint)
        val cacheKey = "${if (isSeries) "tv" else "mv"}:$title:$year"

        memCache[cacheKey]?.let { return it }
        loadDisk(cacheKey)?.let { memCache[cacheKey] = it; return it }

        // Ένα αίτημα ανά τίτλο. Οι υπόλοιπες κάρτες της ίδιας σειράς μπαίνουν
        // εδώ, περιμένουν, και βρίσκουν το αποτέλεσμα έτοιμο στον επανέλεγχο.
        synchronized(lockFor(cacheKey)) {
            memCache[cacheKey]?.let { return it }

            val meta = try {
                throttled { doFetch(title, isSeries, year) }
            } catch (error: Exception) {
                ProviderCancellation.rethrow(error, "TMDB request cancelled")
                null
            } ?: return null

            memCache[cacheKey] = meta
            saveDisk(cacheKey, meta)
            return meta
        }
    }

    /**
     * Στιγμιότυπα επεισοδίων (still frames) για μια σεζόν: αριθμός επεισοδίου -> URL.
     *
     * Τα IPTV playlists σχεδόν ποτέ δεν έχουν εικόνα ανά επεισόδιο, γι' αυτό οι
     * κάρτες έμεναν κενές. Η εξαγωγή καρέ από το ίδιο το stream θα απαιτούσε
     * αποκωδικοποίηση βίντεο (αργή, και συχνά αδύνατη σε live/timeshift πηγές).
     * Το TMDB δίνει επίσημα stills ανά επεισόδιο — καθαρή και γρήγορη λύση.
     *
     * Επιστρέφει κενό map χωρίς κλειδί ή όταν το TMDB δεν έχει τη σεζόν.
     */
    fun episodeMeta(rawTitle: String, yearHint: String, season: Int): Map<Int, EpisodeMeta> {
        if (!hasKey() || season <= 0) return emptyMap()
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return emptyMap()
        val year = extractYear(rawTitle, yearHint)
        val cacheKey = "ep:$title:$year:$season"
        episodeMemCache[cacheKey]?.let { return it }
        loadEpisodesDisk(cacheKey)?.let { episodeMemCache[cacheKey] = it; return it }

        // Μία λήψη ανά σεζόν: όλα τα επεισόδια μιας σειράς ζητούν το ΙΔΙΟ κλειδί,
        // οπότε χωρίς κλείδωμα δώδεκα κάρτες κατέβαζαν δώδεκα φορές τη σεζόν.
        synchronized(lockFor(cacheKey)) {
            episodeMemCache[cacheKey]?.let { return it }

            val result = try {
                throttled {
                    val id = searchId("tv", title, year, isSeries = true)
                    Log.d(
                        LOOKUP_TAG,
                        "episodeMeta raw=«$rawTitle» clean=«$title» year=«$year» " +
                            "season=$season -> tmdbId=$id",
                    )
                    if (id == 0) return@throttled emptyMap()

                    // Βάση στα αγγλικά: εκεί υπάρχουν σχεδόν πάντα still_path και σύνοψη.
                    val base = parseSeason(Http.get("$BASE/tv/$id/season/$season?api_key=$apiKey&language=en-US"))
                    // Από πάνω, ό,τι υπάρχει μεταφρασμένο (τίτλος/σύνοψη επεισοδίου).
                    val localized = if (lang.startsWith("en")) emptyMap() else runCatching {
                        parseSeason(Http.get("$BASE/tv/$id/season/$season?api_key=$apiKey&language=$lang"))
                    }.getOrDefault(emptyMap())

                    val merged = HashMap<Int, EpisodeMeta>()
                    (base.keys + localized.keys).forEach { number ->
                        val en = base[number]
                        val el = localized[number]
                        merged[number] = EpisodeMeta(
                            still = el?.still?.ifBlank { null } ?: en?.still.orEmpty(),
                            title = el?.title?.ifBlank { null } ?: en?.title.orEmpty(),
                            overview = el?.overview?.ifBlank { null } ?: en?.overview.orEmpty()
                        )
                    }
                    Log.d(
                        LOOKUP_TAG,
                        "episodeMeta season $season of tmdbId=$id -> ${merged.size} επεισόδια, " +
                            "με περιγραφή: ${merged.count { it.value.overview.isNotBlank() }}",
                    )
                    merged
                }
            } catch (error: Exception) {
                ProviderCancellation.rethrow(error, "TMDB episode metadata cancelled")
                Log.w(LOOKUP_TAG, "episodeMeta απέτυχε για «$title» σεζόν $season", error)
                emptyMap()
            }
            // ΚΕΝΟ ΑΠΟΤΕΛΕΣΜΑ ΔΕΝ ΑΠΟΘΗΚΕΥΕΤΑΙ ΠΟΥΘΕΝΑ — ΟΥΤΕ ΣΤΗ ΜΝΗΜΗ.
            //
            // Ο δίσκος προστατευόταν ήδη, η μνήμη όχι: το `result` γραφόταν στο
            // `episodeMemCache` ακόμη και κενό. Αυτό είναι ακριβώς το αρνητικό
            // cache που περιγράφεται παραπάνω ως αφαιρεμένο — απλώς είχε
            // αφαιρεθεί μόνο από το `fetch()` και είχε μείνει εδώ. Το κενό δεν
            // ξεχωρίζει το «το TMDB δεν έχει αυτή τη σεζόν» από το «η κλήση
            // κόπηκε», οπότε μια στιγμιαία αποτυχία —όριο ρυθμού όταν ανοίγουν
            // δώδεκα κάρτες μαζί, χαμένο δίκτυο— σημάδευε τη σειρά ως άγνωστη
            // για ΟΛΗ τη ζωή της διεργασίας: οι περιγραφές επεισοδίων δεν
            // εμφανίζονταν ποτέ ξανά μέχρι να κλείσει τελείως η εφαρμογή.
            if (result.isEmpty()) return result
            episodeMemCache[cacheKey] = result
            saveEpisodesDisk(cacheKey, result)
            return result
        }
    }

    private fun parseSeason(body: String): Map<Int, EpisodeMeta> {
        val arr = JSONObject(body).optJSONArray("episodes") ?: return emptyMap()
        val out = HashMap<Int, EpisodeMeta>()
        for (i in 0 until arr.length()) {
            val ep = arr.optJSONObject(i) ?: continue
            val number = ep.optInt("episode_number", -1)
            if (number <= 0) continue
            val still = ep.optString("still_path", "").let {
                if (it.isBlank() || it == "null") "" else IMG_STILL + it
            }
            out[number] = EpisodeMeta(
                still = still,
                title = ep.optString("name", "").takeIf { it != "null" }.orEmpty(),
                overview = ep.optString("overview", "").takeIf { it != "null" }.orEmpty()
            )
        }
        return out
    }

    /**
     * Εναλλακτικές μορφές τίτλου για αναζήτηση, από την πιο πιθανή στη λιγότερο.
     *
     * Οι ελληνικές λίστες γράφουν τους τίτλους με τρόπους που το TMDB δεν βρίσκει
     * αυτούσιους: «ΣΕΙΡΑ - ΕΠΕΙΣΟΔΙΟ», ΚΕΦΑΛΑΙΑ ΜΕ ΤΟΝΟΥΣ, προθέματα παρόχου
     * («ΕΛΛΗΝΙΚΕΣ | ...»). Γι' αυτό ενώ ο τίτλος υπάρχει στο TMDB, η εφαρμογή δεν
     * έβρισκε τίποτα.
     */
    private fun titleCandidates(title: String): List<String> {
        val out = LinkedHashSet<String>()
        fun add(value: String) {
            val v = value.trim().trim('-', '|', ':', '·').trim()
            if (v.length >= 2) out += v
        }
        add(title)
        // Το κομμάτι ΠΡΙΝ από διαχωριστικό: «Η Αίθουσα του Θρόνου - Επεισόδιο 1».
        Regex("""\s+[-|·:]\s+""").split(title).firstOrNull()?.let(::add)
        // Χωρίς τόνους: τα ΚΕΦΑΛΑΙΑ με τόνους μπερδεύουν το ταίριασμα.
        add(stripAccents(title))
        Regex("""\s+[-|·:]\s+""").split(stripAccents(title)).firstOrNull()?.let(::add)
        // Πρόθεμα παρόχου πριν από «|» (και με ελληνικά γράμματα, όχι μόνο λατινικά).
        title.substringAfter('|', "").takeIf { it.isNotBlank() }?.let(::add)
        return out.toList()
    }

    /** Αφαιρεί τόνους/διακριτικά (ά -> α, é -> e) κρατώντας τα γράμματα. */
    private fun stripAccents(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")

    /**
     * Αναζήτηση με διαδοχικές προσπάθειες: κάθε υποψήφιος τίτλος πρώτα ΜΕ έτος
     * (ακριβέστερο) και μετά ΧΩΡΙΣ (οι πάροχοι βάζουν συχνά λάθος χρονιά).
     * Σταματά στο πρώτο αποτέλεσμα, οπότε στη συνήθη περίπτωση κάνει μία κλήση.
     *
     * ΤΟ ΕΛΛΗΝΙΚΟ ΕΡΩΤΗΜΑ ΜΠΑΙΝΕΙ ΤΕΛΕΥΤΑΙΟ ΚΑΙ ΜΕ ΑΥΣΤΗΡΟΤΕΡΟ ΚΡΙΤΗΡΙΟ.
     *
     * Οι υποψήφιοι του [titleCandidates] είναι ο ΠΡΑΓΜΑΤΙΚΟΣ τίτλος του
     * παρόχου σε παραλλαγές, οπότε κρατούν τη μέχρι τώρα συμπεριφορά: δέχονται
     * το πρώτο αποτέλεσμα. Ο τίτλος που παράγει το [GreeklishTitlePolicy.toGreek]
     * είναι ΥΠΟΘΕΣΗ — μια μεταγραφή που μπορεί να πέσει έξω — και δεν
     * επιτρέπεται να επιστρέψει άσχετη σειρά με λάθος περιλήψεις επεισοδίων.
     * Γι' αυτό απαιτεί επαλήθευση: το αποτέλεσμα γίνεται δεκτό μόνο αν ο
     * σκελετός του ταιριάζει με τον σκελετό του τίτλου της λίστας.
     */
    private fun searchId(type: String, title: String, year: String, isSeries: Boolean): Int {
        val yearParam = when {
            year.isBlank() -> ""
            isSeries -> "&first_air_date_year=$year"
            else -> "&year=$year"
        }
        for (candidate in titleCandidates(title)) {
            val q = URLEncoder.encode(candidate, "UTF-8")
            if (yearParam.isNotEmpty()) {
                val withYear = firstId(Http.get("$BASE/search/$type?api_key=$apiKey&language=$lang&query=$q$yearParam"))
                if (withYear != 0) return withYear
            }
            val withoutYear = firstId(Http.get("$BASE/search/$type?api_key=$apiKey&language=$lang&query=$q"))
            if (withoutYear != 0) return withoutYear
        }
        return greeklishId(type, title, yearParam)
    }

    /**
     * Τελευταία προσπάθεια για τίτλο γραμμένο σε greeklish.
     *
     * Φτάνει εδώ μόνο ό,τι απέτυχε με κάθε άλλη μορφή, οπότε οι δύο επιπλέον
     * κλήσεις χρεώνονται σε αναζητήσεις που ήδη δεν έφερναν τίποτα.
     */
    private fun greeklishId(type: String, title: String, yearParam: String): Int {
        if (!GreeklishTitlePolicy.looksGreeklish(title)) {
            Log.d(LOOKUP_TAG, "greeklish: «$title» δεν θεωρήθηκε greeklish, τέλος")
            return 0
        }
        val expected = GreeklishTitlePolicy.latinSkeleton(title)
        if (expected.isBlank()) return 0
        val greek = GreeklishTitlePolicy.toGreek(title)
        if (greek.isBlank()) return 0
        Log.d(LOOKUP_TAG, "greeklish: «$title» -> ερώτημα «$greek», σκελετός «$expected»")
        val q = URLEncoder.encode(greek, "UTF-8")
        if (yearParam.isNotEmpty()) {
            val withYear = verifiedId(
                Http.get("$BASE/search/$type?api_key=$apiKey&language=$lang&query=$q$yearParam"),
                expected,
            )
            if (withYear != 0) return withYear
        }
        return verifiedId(
            Http.get("$BASE/search/$type?api_key=$apiKey&language=$lang&query=$q"),
            expected,
        )
    }

    /**
     * Το πρώτο αποτέλεσμα του οποίου ΚΑΠΟΙΟΣ τίτλος ταυτίζεται με τον
     * αναμενόμενο σκελετό — όχι απλώς το πρώτο αποτέλεσμα.
     *
     * Ελέγχονται και ο τοπικός και ο πρωτότυπος τίτλος: το ερώτημα φεύγει στα
     * ελληνικά, αλλά το TMDB μπορεί να απαντήσει με αγγλικό `name` και τον
     * ελληνικό μόνο στο `original_name`.
     */
    private fun verifiedId(json: String, expectedSkeleton: String): Int = try {
        val results = JSONObject(json).optJSONArray("results")
        var found = 0
        var i = 0
        val seen = StringBuilder()
        while (results != null && i < results.length() && found == 0) {
            val row = results.optJSONObject(i)
            if (row != null) {
                val names = listOf(
                    row.optString("name"), row.optString("original_name"),
                    row.optString("title"), row.optString("original_title"),
                ).filter { it.isNotBlank() }.distinct()
                names.forEach { name ->
                    seen.append("\n  «$name» -> ${GreeklishTitlePolicy.latinSkeleton(name)}")
                }
                val hit = names.any { name ->
                    GreeklishTitlePolicy.isSameTitle(
                        GreeklishTitlePolicy.latinSkeleton(name),
                        expectedSkeleton,
                    )
                }
                if (hit) found = row.optInt("id", 0)
            }
            i++
        }
        // Το ΠΟΙΟΣ τίτλος γύρισε και με τι σκελετό είναι ακριβώς η πληροφορία
        // που χρειάζεται για να κριθεί αν φταίει το ερώτημα ή η σύγκριση.
        // Ποτέ δεν καταγράφεται διεύθυνση: περιέχει το κλειδί API.
        Log.d(
            LOOKUP_TAG,
            "greeklish: αναμενόμενο «$expectedSkeleton», " +
                "${results?.length() ?: 0} αποτελέσματα, ταίριασμα=$found$seen",
        )
        found
    } catch (e: Exception) {
        Log.w(LOOKUP_TAG, "greeklish: αδύνατη ανάγνωση αποτελεσμάτων", e)
        0
    }

    private fun doFetch(title: String, isSeries: Boolean, year: String): Meta? {
        val type = if (isSeries) "tv" else "movie"
        val id = searchId(type, title, year, isSeries)
        if (id == 0) return null

        val detUrl = "$BASE/$type/$id?api_key=$apiKey&language=$lang&append_to_response=credits"
        val o = JSONObject(Http.get(detUrl))

        var overview = o.optString("overview", "")
        var genres = (0 until (o.optJSONArray("genres")?.length() ?: 0)).joinToString(", ") {
            o.getJSONArray("genres").getJSONObject(it).optString("name", "")
        }

        // ΕΦΕΔΡΙΚΑ ΑΓΓΛΙΚΑ.
        //
        // Ζητάμε τα δεδομένα σε el-GR. Όταν το TMDB δεν έχει ελληνική μετάφραση
        // (ισχύει για τη ΜΕΓΑΛΗ πλειοψηφία των τίτλων) επιστρέφει ΚΕΝΗ περιγραφή,
        // ενώ η αγγλική υπάρχει κανονικά — γι' αυτό στο Google έβλεπες σύνοψη και
        // στην εφαρμογή τίποτα. Εδώ, αν λείπει, τη ζητάμε ξανά στα αγγλικά.
        if (overview.isBlank() || genres.isBlank()) {
            runCatching {
                val en = JSONObject(Http.get("$BASE/$type/$id?api_key=$apiKey&language=en-US"))
                if (overview.isBlank()) overview = en.optString("overview", "")
                if (genres.isBlank()) {
                    genres = (0 until (en.optJSONArray("genres")?.length() ?: 0)).joinToString(", ") {
                        en.getJSONArray("genres").getJSONObject(it).optString("name", "")
                    }
                }
            }
        }

        val vote = o.optDouble("vote_average", 0.0)
        val date = o.optString(if (isSeries) "first_air_date" else "release_date", "")
        val poster = o.optString("poster_path", "").let { if (it.isBlank() || it == "null") "" else IMG_POSTER + it }
        val backdrop = o.optString("backdrop_path", "").let { if (it.isBlank() || it == "null") "" else IMG_BACKDROP + it }

        val cast = ArrayList<Person>()
        o.optJSONObject("credits")?.optJSONArray("cast")?.let { arr ->
            for (i in 0 until minOf(arr.length(), 15)) {
                val c = arr.getJSONObject(i)
                val photo = c.optString("profile_path", "").let {
                    if (it.isBlank() || it == "null") null else IMG_PROFILE + it
                }
                cast.add(Person(c.optString("name", ""), c.optString("character", ""), photo))
            }
        }

        return Meta(
            tmdbId = id,
            rating = if (vote > 0) String.format("%.1f", vote) else "",
            poster = poster, backdrop = backdrop, overview = overview,
            year = date.take(4), genres = genres, cast = cast
        )
    }

    private fun firstId(json: String): Int = try {
        JSONObject(json).optJSONArray("results")?.optJSONObject(0)?.optInt("id", 0) ?: 0
    } catch (e: Exception) { 0 }

    /* ------------------------------------------------------------ cache --- */

    private fun loadDisk(key: String): Meta? {
        val raw = prefs?.getString("tmdb_c_$key", null) ?: return null
        return try {
            val o = JSONObject(raw)
            val cast = ArrayList<Person>()
            val arr = o.optJSONArray("cast") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                cast.add(Person(c.optString("n"), c.optString("r"), c.optString("p").ifBlank { null }))
            }
            Meta(
                o.optInt("id"), o.optString("rt"), o.optString("po"), o.optString("bd"),
                o.optString("ov"), o.optString("yr"), o.optString("gn"), cast
            )
        } catch (e: Exception) { null }
    }

    private fun saveDisk(key: String, m: Meta) {
        try {
            val arr = JSONArray()
            m.cast.forEach {
                arr.put(JSONObject().put("n", it.name).put("r", it.role).put("p", it.photo ?: ""))
            }
            val o = JSONObject()
                .put("id", m.tmdbId).put("rt", m.rating).put("po", m.poster).put("bd", m.backdrop)
                .put("ov", m.overview).put("yr", m.year).put("gn", m.genres).put("cast", arr)
            prefs?.edit()?.putString("tmdb_c_$key", o.toString())?.apply()
        } catch (e: Exception) { /* το cache δεν είναι κρίσιμο */ }
    }

    /**
     * Τα στοιχεία επεισοδίων ζούσαν ΜΟΝΟ στη μνήμη: σε κάθε εκκίνηση της
     * εφαρμογής ξανακατέβαιναν ολόκληρες σεζόν, ενώ οι αφίσες ταινιών —
     * αποθηκευμένες σωστά — έρχονταν αμέσως. Ίδια ανάγκη, δύο συμπεριφορές.
     */
    private fun loadEpisodesDisk(key: String): Map<Int, EpisodeMeta>? {
        val raw = prefs?.getString("tmdb_c_$key", null) ?: return null
        return try {
            val o = JSONObject(raw)
            val out = HashMap<Int, EpisodeMeta>()
            o.keys().forEach { numberKey ->
                val number = numberKey.toIntOrNull() ?: return@forEach
                val e = o.getJSONObject(numberKey)
                out[number] = EpisodeMeta(
                    still = e.optString("st"),
                    title = e.optString("ti"),
                    overview = e.optString("ov")
                )
            }
            out.takeIf { it.isNotEmpty() }
        } catch (e: Exception) { null }
    }

    private fun saveEpisodesDisk(key: String, episodes: Map<Int, EpisodeMeta>) {
        try {
            val o = JSONObject()
            episodes.forEach { (number, meta) ->
                o.put(
                    number.toString(),
                    JSONObject()
                        .put("st", meta.still)
                        .put("ti", meta.title)
                        .put("ov", meta.overview)
                )
            }
            prefs?.edit()?.putString("tmdb_c_$key", o.toString())?.apply()
        } catch (e: Exception) { /* το cache δεν είναι κρίσιμο */ }
    }

    fun clearCache() {
        memCache.clear()
        episodeMemCache.clear()
        keyLocks.clear()
        prefs?.let { p ->
            val e = p.edit()
            p.all.keys.filter { it.startsWith("tmdb_c_") }.forEach { e.remove(it) }
            e.apply()
        }
    }
}
