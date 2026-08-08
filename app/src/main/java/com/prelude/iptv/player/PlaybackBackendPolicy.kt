package com.prelude.iptv.player

/** Ποια μηχανή θα παίξει τη ροή. */
enum class PlaybackBackend {
    /** Media3/ExoPlayer — HLS, DASH, προοδευτικό HTTP. Καλύτερο frame pacing. */
    EXO,

    /** LibVLC — ό,τι δεν αγγίζει το Media3: RTSP, RTMP, UDP, MMS, «γυμνό» TS. */
    VLC,
}

/**
 * Ποιο backend αναλαμβάνει κάθε διεύθυνση.
 *
 * ---
 *
 * ΤΟ ΠΡΟΒΛΗΜΑ ΠΟΥ ΛΥΝΕΙ: το Media3 **δεν ξέρει** RTSP χωρίς το `media3-exoplayer-rtsp`,
 * και δεν ξέρει καθόλου RTMP, UDP ή MMS — ούτε με πρόσθετα. Μια λίστα IPTV που
 * περιέχει τέτοιες διευθύνσεις έπαιζε σε άλλες εφαρμογές και **όχι σε εμάς**, όχι
 * επειδή λείπει codec αλλά επειδή η μηχανή δεν ανοίγει καν τη σύνδεση.
 *
 * Το LibVLC τα ξέρει όλα αυτά μέσα από το libavformat/live555 που κουβαλά, χωρίς
 * ξεχωριστό πρόσθετο ανά μορφή.
 *
 * ---
 *
 * ΓΙΑΤΙ ΟΧΙ «VLC ΓΙΑ ΟΛΑ»: το Media3 κάνει δύο πράγματα αισθητά καλύτερα σε
 * τηλεόραση, και τα δύο τα δουλέψαμε πολύ σε αυτή την εφαρμογή —
 *
 * 1. **Frame pacing και συγχρονισμός χειλιών.** Το `setVideoSurfaceView` με
 *    `VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS` και ο έλεγχος ρυθμού
 *    ανανέωσης της οθόνης δεν έχουν αντίστοιχο στο LibVLC.
 * 2. **Επιλογή κομματιών με σταθερά αναγνωριστικά**, που κρατά τη γλώσσα ήχου
 *    όταν η ροή αλλάζει ποιότητα.
 *
 * Άρα: HLS και προοδευτικό HTTP μένουν στο Media3· ό,τι δεν μπορεί να ανοίξει
 * πάει στο LibVLC. Η επιλογή γίνεται από τη **διεύθυνση**, όχι από δοκιμή-και-
 * αποτυχία: μια αποτυχημένη προσπάθεια κοστίζει δευτερόλεπτα μαύρης οθόνης.
 *
 * ---
 *
 * ΤΙ ΚΑΛΥΠΤΕΤΑΙ ΣΥΝΟΛΙΚΑ (η λίστα που ζητήθηκε):
 *
 * | | ExoPlayer | LibVLC |
 * |---|---|---|
 * | HLS (m3u8) | ✔ | ✔ |
 * | RTMP | — | ✔ |
 * | UDP / multicast | — | ✔ |
 * | RTSP | — | ✔ |
 * | MMS | — | ✔ |
 * | MPEG-TS | ✔ | ✔ |
 * | MP4 | ✔ | ✔ |
 * | H.264 / H.265 | ✔ | ✔ |
 * | AAC / MP3 | ✔ | ✔ |
 * | AC-3 | ✔ (nextlib) | ✔ |
 */
object PlaybackBackendPolicy {

    /**
     * Σχήματα που το Media3 **δεν** μπορεί να ανοίξει σε αυτό το build.
     *
     * Δεν είναι θέμα codec — είναι θέμα μεταφοράς. Το ExoPlayer δεν έχει
     * `DataSource` γι' αυτά, οπότε αποτυγχάνει πριν καν δει βίντεο.
     */
    private val VLC_ONLY_SCHEMES = setOf(
        "rtsp", "rtsps",
        "rtmp", "rtmps", "rtmpe", "rtmpt", "rtmpte",
        "udp", "rtp",
        "mms", "mmsh", "mmst",
    )

    /**
     * Καταλήξεις που το Media3 χειρίζεται αξιόπιστα.
     *
     * Το `.ts` λείπει επίτηδες — δες [isBareTransportStream].
     */
    private val EXO_EXTENSIONS = setOf("m3u8", "mpd", "mp4", "m4v", "mkv", "webm", "mp3", "m4a", "aac")

    /**
     * Ποιο backend για αυτή τη διεύθυνση.
     *
     * @param url ό,τι έδωσε ο πάροχος, χωρίς επεξεργασία
     * @param forceVlc ρύθμιση χρήστη «πάντα LibVLC» — υπερισχύει των πάντων
     */
    fun backendFor(url: String, forceVlc: Boolean = false): PlaybackBackend {
        if (forceVlc) return PlaybackBackend.VLC
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return PlaybackBackend.EXO
        if (schemeOf(trimmed) in VLC_ONLY_SCHEMES) return PlaybackBackend.VLC
        if (isBareTransportStream(trimmed)) return PlaybackBackend.VLC
        return PlaybackBackend.EXO
    }

    /** Το σχήμα σε πεζά, ή κενό όταν η διεύθυνση δεν έχει. */
    fun schemeOf(url: String): String {
        val separator = url.indexOf("://")
        if (separator <= 0) return ""
        return url.substring(0, separator).lowercase()
    }

    /**
     * Είναι «γυμνή» ροή MPEG-TS χωρίς manifest;
     *
     * ΓΙΑΤΙ ΠΑΕΙ ΣΤΟ LIBVLC: το ExoPlayer παίζει MPEG-TS **μέσα σε HLS**, όπου το
     * manifest λέει από πού αρχίζει κάθε κομμάτι. Μια ατέρμονη ροή `.ts` πάνω από
     * HTTP δεν έχει manifest, δεν έχει διάρκεια, και συχνά ξεκινά στη μέση ενός
     * πακέτου. Το `ProgressiveMediaSource` τη δέχεται αλλά σκοντάφτει σε ελλιπή
     * PAT/PMT και σε αλλαγές ρεύματος — ακριβώς αυτό που κάνουν οι πάροχοι IPTV
     * στα ζωντανά.
     *
     * Το LibVLC ξαναδιαβάζει τους πίνακες όποτε χρειαστεί και δεν το πειράζει.
     *
     * Το ερώτημα κρίνεται από τη ΔΙΑΔΡΟΜΗ και όχι από τα query parameters: πολλοί
     * πάροχοι Xtream δίνουν `.../12345.ts?token=…`, και ένα σκέτο `endsWith`
     * θα έχανε το `.ts`.
     */
    fun isBareTransportStream(url: String): Boolean {
        val extension = extensionOf(url)
        return extension == "ts" || extension == "m2ts" || extension == "mts"
    }

    /** Η κατάληξη της διαδρομής, χωρίς query ή fragment. Πεζά, χωρίς τελεία. */
    fun extensionOf(url: String): String {
        val withoutFragment = url.substringBefore('#')
        val withoutQuery = withoutFragment.substringBefore('?')
        val lastSegment = withoutQuery.substringAfterLast('/')
        if (!lastSegment.contains('.')) return ""
        return lastSegment.substringAfterLast('.').lowercase()
    }

    /**
     * Μπορεί το Media3 να το ανοίξει με σιγουριά;
     *
     * Χρησιμεύει στη διάγνωση («γιατί δεν παίζει;») και όχι στην επιλογή — η
     * επιλογή γίνεται από το [backendFor], που είναι πιο συντηρητικό.
     */
    fun exoHandlesConfidently(url: String): Boolean =
        schemeOf(url) !in VLC_ONLY_SCHEMES && extensionOf(url) in EXO_EXTENSIONS
}
