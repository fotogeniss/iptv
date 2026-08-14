package com.prelude.iptv.player

import android.content.Context
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Η μηχανή LibVLC, πίσω από την ίδια συμπεριφορά με το ExoPlayer.
 *
 * ΤΙ ΕΙΝΑΙ ΚΑΙ ΤΙ ΔΕΝ ΕΙΝΑΙ: δεν είναι δεύτερος player. Δεν έχει διεπαφή, δεν
 * ξέρει από χειριστήρια, δεν χειρίζεται πλήκτρα. Είναι ΜΟΝΟ η αναπαραγωγή, και
 * την ελέγχει το [PlaybackEngine] — που είναι το μόνο που βλέπει ο υπόλοιπος
 * κώδικας.
 *
 * Αυτή η διάκριση είναι όλη η διαφορά με το παλιό `PlayerActivity`, που είχε δικό
 * του VLC μαζί με δικά του μενού και δικές του χειρονομίες. Εκείνο έπρεπε να
 * φύγει· αυτό όχι.
 *
 * ---
 *
 * ΟΛΑ ΤΑ ΓΕΓΟΝΟΤΑ ΕΡΧΟΝΤΑΙ ΣΕ ΝΗΜΑ ΤΟΥ LIBVLC, ΟΧΙ ΣΤΟ ΚΥΡΙΟ. Ο καλών παίρνει
 * ενημερώσεις μέσω [onState] και είναι **δική του ευθύνη** να τις μεταφέρει όπου
 * πρέπει. Δεν το κάνουμε εδώ γιατί το [PlaybackEngine] έχει ήδη `Handler` κύριου
 * νήματος και δύο μηχανισμοί θα ήταν ένας παραπάνω.
 */
/**
 * ΤΟ LIBVLC ΖΩΓΡΑΦΙΖΕΙ ΣΕ TextureView, ΟΠΩΣ ΚΑΙ ΤΟ EXOPLAYER.
 *
 * Αυτό είναι όλη η διαφορά ανάμεσα στις ταινίες, που δούλευαν πάντα, και στα
 * ζωντανά, που έβγαζαν ήχο χωρίς εικόνα μέσα στη μαζεμένη λωρίδα.
 *
 * Το `SurfaceView` ζει σε ΞΕΧΩΡΙΣΤΟ system layer, με δική του γεωμετρία που δεν
 * ακολουθεί το resize, το clipping και το z-order του Compose. Όταν ο player
 * μάζευε σε 121x68dp, η επιφάνεια έμενε στη γεωμετρία της πλήρους οθόνης και
 * δεν φαινόταν τίποτα. Το `TextureView` συντίθεται στο ΙΔΙΟ layer με το
 * υπόλοιπο UI και ακολουθεί σωστά όλα τα παραπάνω.
 *
 * Το ίδιο συμπέρασμα είχε ήδη καταγραφεί για το ExoPlayer στο
 * [com.prelude.iptv.ui.player.PlayerVideoSurface] («ακούγεται ήχος αλλά δεν
 * φαίνεται εικόνα») — απλώς δεν είχε εφαρμοστεί ποτέ και στο LibVLC.
 */
private const val VLC_USE_TEXTURE_VIEW = true

class VlcBackend(
    private val appContext: Context,
    /** Καλείται σε ΚΑΘΕ αλλαγή. Το νήμα δεν είναι εγγυημένα το κύριο. */
    private val onState: (Snapshot) -> Unit,
) {

    /**
     * Ό,τι ξέρει η μηχανή για τη ροή που παίζει.
     *
     * Καθρεφτίζει τα πεδία του [PlaybackEngine.State] που μπορεί να γεμίσει το
     * LibVLC. Δεν είναι το ίδιο αντικείμενο επίτηδες: αν ήταν, το backend θα
     * μπορούσε να γράψει σε πεδία που ανήκουν στο ExoPlayer.
     */
    data class Snapshot(
        val playing: Boolean = false,
        val buffering: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val videoAspect: Float = 0f,
        val width: Int = 0,
        val height: Int = 0,
        val frameRate: Float = 0f,
        val renderedFrame: Boolean = false,
        val ended: Boolean = false,
        val error: String? = null,
    )

    private var libVlc: LibVLC? = null
    private var player: MediaPlayer? = null
    private var attachedLayout: VLCVideoLayout? = null

    // @Volatile: γράφεται από το νήμα γεγονότων του LibVLC και από το κύριο νήμα
    // στο [open]. Χωρίς αυτό, το κύριο νήμα μπορεί να δει παλιό αντίγραφο.
    @Volatile
    private var snapshot = Snapshot()
    private var liveStream = true

    /** Το LibVLC ξεκινά ΜΙΑ φορά ανά είδος ροής: τα ορίσματα δεν αλλάζουν εν πτήσει. */
    private fun ensureVlc(live: Boolean): LibVLC {
        val existing = libVlc
        if (existing != null && live == liveStream) return existing
        // Αλλαγή ζωντανό <-> ταινία σημαίνει άλλη αποθήκευση, άρα νέο LibVLC.
        // Η εναλλακτική —μία ρύθμιση για όλα— κάνει είτε την αλλαγή καναλιού αργή
        // είτε τις ταινίες να κολλάνε.
        releaseInternal()
        liveStream = live
        return LibVLC(appContext, ArrayList(VlcOptionsPolicy.startupOptions(live = live)))
            .also { libVlc = it }
    }

    /**
     * Ανοίγει μια ροή.
     *
     * @param live ζωντανό κανάλι — καθορίζει την αποθήκευση δικτύου
     * @param resumeMs θέση εκκίνησης· αγνοείται στα ζωντανά
     */
    fun open(url: String, live: Boolean, resumeMs: Long = 0L, playWhenReady: Boolean = true) {
        val vlc = ensureVlc(live)
        val existingPlayer = player ?: MediaPlayer(vlc).also { created ->
            player = created
            created.setEventListener(::onEvent)
            attachedLayout?.let { created.attachViews(it, null, false, VLC_USE_TEXTURE_VIEW) }
        }

        val media = Media(vlc, android.net.Uri.parse(url)).apply {
            // ΕΠΙΤΑΧΥΝΣΗ ΥΛΙΚΟΥ ΜΕ ΔΙΚΤΥ ΑΣΦΑΛΕΙΑΣ.
            //
            // `force = false`: αν ο αποκωδικοποιητής της συσκευής αρνηθεί το προφίλ
            // —συνηθισμένο σε HEVC 10-bit σε φθηνά TV box— το LibVLC πέφτει μόνο
            // του σε λογισμικό. Με `force = true` θα έμενε μαύρη οθόνη.
            setHWDecoderEnabled(true, false)

            // ΙΔΙΑ ΤΑΥΤΟΤΗΤΑ ΜΕ ΤΟ EXOPLAYER.
            //
            // Το ExoPlayer στέλνει `Http.DESKTOP_UA`· το LibVLC στέλνει το δικό
            // του «LibVLC/3.0.x». Πολλοί πάροχοι IPTV φιλτράρουν με βάση τον
            // User-Agent και απαντούν 403 ή **404** σε ό,τι δεν αναγνωρίζουν.
            //
            // Δύο μηχανές που παρουσιάζονται διαφορετικά σημαίνει ότι η εφεδρεία
            // μπορεί να αποτύχει για λόγο άσχετο με την ικανότητά της — και
            // ψάχνεις codec ενώ φταίει μια κεφαλίδα.
            addOption(":http-user-agent=${com.prelude.iptv.net.Http.DESKTOP_UA}")
            addOption(":http-reconnect")

            addOption(":network-caching=${if (live) VlcOptionsPolicy.LIVE_CACHING_MS else VlcOptionsPolicy.VOD_CACHING_MS}")
            if (!live && resumeMs > 0L) {
                // Σε δευτερόλεπτα, όχι χιλιοστά — το LibVLC δέχεται δεκαδικά εδώ.
                addOption(":start-time=${resumeMs / 1000.0}")
            }
        }

        existingPlayer.media = media
        // Το Media κρατιέται από τον player· η δική μας αναφορά πρέπει να φύγει,
        // αλλιώς διαρρέει native μνήμη σε κάθε αλλαγή καναλιού.
        media.release()

        snapshot = Snapshot(buffering = true)
        publish()
        if (playWhenReady) existingPlayer.play()
    }

    // ---------------------------------------------------------------- έλεγχος

    fun play() { player?.play() }

    fun pause() { player?.pause() }

    fun togglePlay() {
        val current = player ?: return
        if (current.isPlaying) current.pause() else current.play()
    }

    /**
     * Μετακίνηση σε θέση.
     *
     * Το LibVLC δέχεται `time` σε ms αλλά **αγνοεί** την εντολή όταν η ροή δεν
     * είναι seekable — ζωντανά κανάλια, RTMP, UDP. Ο έλεγχος γίνεται εδώ ώστε ο
     * καλών να μη χρειάζεται να ξέρει το είδος της ροής.
     */
    fun seekTo(positionMs: Long) {
        val current = player ?: return
        if (!current.isSeekable) return
        current.time = positionMs.coerceAtLeast(0L)
    }

    fun setSpeed(speed: Float) { player?.rate = speed }

    fun currentPositionMs(): Long = player?.time?.coerceAtLeast(0L) ?: 0L

    /** `length` είναι −1 όσο δεν έχει διαβαστεί, και 0 στα ζωντανά. */
    fun durationMs(): Long = player?.length?.takeIf { it > 0L } ?: 0L

    fun isSeekable(): Boolean = player?.isSeekable ?: false

    // ---------------------------------------------------------------- κομμάτια

    /**
     * Κομμάτια ήχου, με τη γλώσσα μπροστά.
     *
     * Περνά από το ίδιο [TrackLabelPolicy] με το ExoPlayer ώστε το ίδιο κανάλι να
     * γράφει «Ελληνικά» και στις δύο μηχανές. Διαφορετικές ετικέτες για το ίδιο
     * πράγμα είναι ο πιο σίγουρος τρόπος να μοιάζει η εφαρμογή ασυνεπής.
     */
    fun audioTracks(): List<PlaybackEngine.TrackOption> =
        trackOptions(player?.audioTracks, player?.audioTrack ?: -1)

    fun subtitleTracks(): List<PlaybackEngine.TrackOption> =
        trackOptions(player?.spuTracks, player?.spuTrack ?: -1)

    private fun trackOptions(
        tracks: Array<MediaPlayer.TrackDescription>?,
        selectedId: Int,
    ): List<PlaybackEngine.TrackOption> {
        val list = tracks ?: return emptyList()
        var fallbackIndex = 1
        return list
            // Το LibVLC βάζει πάντα ένα «Disable» με id −1. Η απενεργοποίηση
            // υπάρχει ήδη ως ρητή επιλογή στα μενού μας — εδώ θα ήταν διπλή.
            .filter { it.id >= 0 }
            .map { description ->
                PlaybackEngine.TrackOption(
                    id = description.id.toString(),
                    label = appContext.playerTrackLabel(
                        language = description.name,
                        providerLabel = description.name,
                        fallbackIndex = fallbackIndex++,
                    ),
                    selected = description.id == selectedId,
                )
            }
    }

    fun selectAudio(id: String) {
        player?.audioTrack = id.toIntOrNull() ?: return
    }

    /** null = απενεργοποίηση. Το LibVLC θέλει −1 γι' αυτό. */
    fun selectSubtitle(id: String?) {
        player?.spuTrack = id?.toIntOrNull() ?: -1
    }

    // ---------------------------------------------------------------- επιφάνεια

    /**
     * Προσαρτά την επιφάνεια βίντεο.
     *
     * ΓΙΑΤΙ `VLCVideoLayout` ΚΑΙ ΟΧΙ ΣΚΕΤΟ SurfaceView: το LibVLC θέλει να ελέγχει
     * το μέγεθος της επιφάνειας για να εφαρμόσει την αναλογία εικόνας. Το
     * `attachViews` με ένα γυμνό `SurfaceView` υπάρχει, αλλά αφήνει σε εμάς τον
     * υπολογισμό της αναλογίας — δηλαδή ξαναγράφουμε κώδικα που υπάρχει ήδη και
     * είναι δοκιμασμένος από τη VideoLAN σε χιλιάδες συσκευές.
     */
    fun attachLayout(layout: VLCVideoLayout) {
        if (attachedLayout === layout) return
        detachLayout()
        attachedLayout = layout
        player?.attachViews(layout, null, false, VLC_USE_TEXTURE_VIEW)
    }

    fun detachLayout() {
        if (attachedLayout == null) return
        player?.detachViews()
        attachedLayout = null
    }

    /**
     * Αποσυνδέει ΜΟΝΟ αν το [layout] είναι ακόμη το ενεργό.
     *
     * ΓΙΑΤΙ ΥΠΑΡΧΕΙ: όταν ο player μαζεύεται σε λωρίδα, η νέα μικρή επιφάνεια
     * προσαρτάται ΠΡΙΝ φύγει από τη σύνθεση η παλιά πλήρης — έτσι δουλεύει η
     * σειρά του Compose. Η παλιά, φεύγοντας, καλούσε [detachLayout] χωρίς όρο
     * και ξήλωνε την ΚΑΙΝΟΥΡΓΙΑ επιφάνεια: ο ήχος συνέχιζε, η εικόνα χανόταν.
     * Το ExoPlayer μονοπάτι είχε ήδη αυτόν τον έλεγχο ταυτότητας
     * ([PlaybackEngine.detachSurface])· το LibVLC δεν τον είχε ποτέ.
     */
    fun detachLayout(layout: VLCVideoLayout) {
        if (attachedLayout !== layout) return
        detachLayout()
    }

    /**
     * Ξαναχτίζει την έξοδο πάνω στην ΙΔΙΑ επιφάνεια, παρακάμπτοντας τον έλεγχο
     * ταυτότητας του [attachLayout].
     *
     * ΓΙΑΤΙ ΕΙΝΑΙ ΑΠΑΡΑΙΤΗΤΟ: όταν η View φεύγει από το παράθυρο —και αυτό
     * ακριβώς κάνει το μάζεμα, αφού η επιφάνεια μετακομίζει σε άλλον γονέα— το
     * σύστημα καταστρέφει την επιφάνειά της. Το LibVLC χάνει την έξοδό του και
     * δεν την ξαναχτίζει μόνο του. Επειδή η ταυτότητα του layout δεν αλλάζει,
     * το [attachLayout] επέστρεφε αμέσως και η έξοδος έμενε νεκρή για πάντα:
     * μαύρο στη λωρίδα ΚΑΙ μαύρο όταν ο player ξαναμεγάλωνε.
     */
    fun reattachLayout(layout: VLCVideoLayout) {
        detachLayout()
        attachedLayout = layout
        player?.attachViews(layout, null, false, VLC_USE_TEXTURE_VIEW)
    }

    // ---------------------------------------------------------------- γεγονότα

    private fun onEvent(event: MediaPlayer.Event) {
        // Πότε αξίζει να ξαναδιαβαστεί η μορφή του βίντεο.
        //
        // ΟΧΙ ΣΕ ΚΑΘΕ ΓΕΓΟΝΟΣ: το TimeChanged έρχεται τέσσερις φορές το
        // δευτερόλεπτο, και το [readVideoFormat] ζητά αναφορά στο Media από το
        // native επίπεδο. Διαβάζοντάς το εκεί, πληρώναμε native κλήση συνεχώς για
        // δεδομένα που αλλάζουν μία φορά ανά ροή.
        val formatMayHaveChanged = when (event.type) {
            MediaPlayer.Event.Vout,
            MediaPlayer.Event.ESAdded,
            MediaPlayer.Event.ESDeleted,
            MediaPlayer.Event.LengthChanged -> true
            else -> false
        }

        snapshot = when (event.type) {
            MediaPlayer.Event.Playing ->
                snapshot.copy(playing = true, buffering = false, error = null)

            MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped ->
                snapshot.copy(playing = false)

            // Το `buffering` έρχεται με ποσοστό 0..100. Μόνο το 100 σημαίνει
            // «γέμισε»· οτιδήποτε άλλο είναι ακόμη σε εξέλιξη.
            MediaPlayer.Event.Buffering ->
                snapshot.copy(buffering = event.buffering < 100f)

            MediaPlayer.Event.TimeChanged ->
                snapshot.copy(positionMs = event.timeChanged.coerceAtLeast(0L))

            MediaPlayer.Event.LengthChanged ->
                snapshot.copy(durationMs = event.lengthChanged.takeIf { it > 0L } ?: 0L)

            MediaPlayer.Event.Vout ->
                // Ο αριθμός των εξόδων βίντεο· >0 σημαίνει ότι βγήκε καρέ στην
                // οθόνη. Είναι το αντίστοιχο του `onRenderedFirstFrame`.
                snapshot.copy(renderedFrame = event.voutCount > 0, buffering = false)

            MediaPlayer.Event.EndReached ->
                snapshot.copy(playing = false, ended = true)

            MediaPlayer.Event.EncounteredError ->
                snapshot.copy(
                    playing = false,
                    buffering = false,
                    error = "Η ροή δεν είναι διαθέσιμη",
                )

            MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted ->
                // Νέο κομμάτι ήχου/υποτίτλων. Η διεπαφή πρέπει να ξαναρωτήσει —
                // αρκεί μια δημοσίευση χωρίς αλλαγή πεδίου.
                snapshot

            else -> return
        }
        if (formatMayHaveChanged) readVideoFormat()
        publish()
    }

    /**
     * Διαστάσεις και ρυθμός καρέ, όταν γίνουν γνωστά.
     *
     * Διαβάζονται από το `IMedia.VideoTrack` και όχι από το `MediaPlayer`: το
     * δεύτερο δίνει το μέγεθος της ΕΠΙΦΑΝΕΙΑΣ, που είναι το μέγεθος του κουτιού
     * μας και όχι της ροής. Με αυτό, μια ταινία 2.39:1 θα φαινόταν 16:9.
     */
    private fun readVideoFormat() {
        // ΤΟ `player.media` ΕΠΙΣΤΡΕΦΕΙ ΚΡΑΤΗΜΕΝΗ ΑΝΑΦΟΡΑ.
        //
        // Το binding του LibVLC κάνει retain πριν σου το δώσει. Χωρίς `release()`
        // στο τέλος, κάθε κλήση κρατά ένα native αντικείμενο ζωντανό για πάντα —
        // διαρροή που δεν φαίνεται στο Java heap και δεν την πιάνει ο profiler του
        // Android Studio.
        val media = player?.media ?: return
        try {
            for (index in 0 until media.trackCount) {
                val track = media.getTrack(index) as? IMedia.VideoTrack ?: continue
                if (track.width <= 0 || track.height <= 0) continue
                // Το sar (sample aspect ratio) διορθώνει τα ανισομερή pixel του
                // MPEG-TS. Χωρίς αυτό, ροές 720x576 φαίνονται στενές.
                val sar = if (track.sarNum > 0 && track.sarDen > 0) {
                    track.sarNum.toFloat() / track.sarDen
                } else 1f
                snapshot = snapshot.copy(
                    width = track.width,
                    height = track.height,
                    videoAspect = track.width * sar / track.height,
                    frameRate = if (track.frameRateDen > 0) {
                        track.frameRateNum.toFloat() / track.frameRateDen
                    } else 0f,
                )
                return
            }
        } finally {
            media.release()
        }
    }

    private fun publish() = onState(snapshot)

    // ---------------------------------------------------------------- τέλος

    /**
     * Σταματά τη ροή αλλά κρατά τη μηχανή, για γρήγορη αλλαγή καναλιού.
     *
     * Ξαναστήνοντας το LibVLC σε κάθε κανάλι, η αλλαγή παίρνει πάνω από ένα
     * δευτερόλεπτο επιπλέον — το native φόρτωμα δεν είναι φθηνό.
     */
    fun stop() {
        player?.stop()
        snapshot = Snapshot()
        publish()
    }

    fun release() {
        releaseInternal()
    }

    private fun releaseInternal() {
        player?.let {
            it.setEventListener(null)
            it.detachViews()
            it.stop()
            it.release()
        }
        player = null
        attachedLayout = null
        libVlc?.release()
        libVlc = null
    }
}
