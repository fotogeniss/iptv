package com.prelude.iptv.player

import android.content.Context
import android.os.Handler
import android.util.Log
import android.os.Looper
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.VideoSize
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue as MediaCue
import androidx.media3.common.text.CueGroup
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.net.Http
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Η ΜΙΑ μηχανή αναπαραγωγής της εφαρμογής, ανεξάρτητη από Activity.
 *
 * ΓΙΑΤΙ ΥΠΑΡΧΕΙ: μέχρι τώρα η αναπαραγωγή ζούσε αποκλειστικά μέσα στο
 * `PlayerActivity`. Κάθε φορά που χρειαζόμασταν βίντεο αλλού (π.χ. προεπισκόπηση
 * ζωντανών) φτιαχνόταν δεύτερος player — δύο υλοποιήσεις, δύο συμπεριφορές, και
 * η μία πάντα πίσω σε λειτουργίες. Εδώ η μηχανή αποδεσμεύεται από το παράθυρο:
 * ζει όσο τη χρειαζόμαστε και η ΕΠΙΦΑΝΕΙΑ προβολής προσαρτάται/αποσπάται.
 *
 * Αυτό είναι που επιτρέπει την απρόσκοπτη μετάβαση «μικρό -> πλήρης οθόνη»: δεν
 * αλλάζει player, αλλάζει μόνο το μέγεθος του δοχείου.
 *
 * Δεν κρατά UI. Εκθέτει κατάσταση με [StateFlow] ώστε να τη διαβάζει είτε Compose
 * είτε ο κλασικός κώδικας με Views.
 */
// ΤΟ AndroidX OptIn, ΟΧΙ ΤΟ KOTLIN.
//
// Το `@UnstableApi` του Media3 φέρει `androidx.annotation.RequiresOptIn`, όχι το
// αντίστοιχο του Kotlin. Ένα `@file:OptIn(...)` με το kotlin.OptIn μεταγλωττίζεται
// κανονικά αλλά δεν κάνει τίποτα — ο μεταγλωττιστής το λέει ρητά: «has no effect».
// Ο PlayerActivity το είχε ήδη σωστά· η νέα μηχανή το αντέγραψε λάθος.
//
// Στην κλάση και όχι στο αρχείο, γιατί το AndroidX OptIn δεν δέχεται στόχο αρχείου.
@OptIn(UnstableApi::class)
class PlaybackEngine(private val appContext: Context) {

    /**
     * Τεχνικά στοιχεία της ροής που παίζει. Χρησιμεύουν σε δύο πράγματα: στην
     * ένδειξη ποιότητας που βλέπει ο χρήστης, και — κυρίως — στο [frameRate], που
     * είναι αυτό που επιτρέπει στην τηλεόραση να συγχρονίσει τη συχνότητά της.
     */
    data class VideoQuality(
        val width: Int = 0,
        val height: Int = 0,
        val frameRate: Float = 0f,
        val codec: String = "",
        val bitrateBps: Int = 0,
    )

    /** Ό,τι χρειάζεται η διεπαφή για να ζωγραφίσει, χωρίς να αγγίξει τη μηχανή. */
    data class State(
        val playing: Boolean = false,
        val buffering: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val videoAspect: Float = 0f,
        val quality: VideoQuality = VideoQuality(),
        /**
         * Αυξάνεται κάθε φορά που η ΤΡΕΧΟΥΣΑ επιφάνεια βγάζει το πρώτο της καρέ.
         *
         * Μετρητής και όχι σημαία: όταν αλλάζει επιφάνεια (προεπισκόπηση ->
         * πλήρης οθόνη) το γεγονός ξανασυμβαίνει, και μια σημαία που είναι ήδη
         * true δεν θα το ανακοίνωνε. Η διεπαφή το χρειάζεται για να ξέρει πότε
         * μπορεί να πάψει να δείχνει το παγωμένο καρέ.
         */
        val renderedFrames: Int = 0,
        /**
         * Τα ΕΣΩΤΕΡΙΚΑ κομμάτια της ροής, όπως τα ξέρει τώρα ο player.
         *
         * Ζουν στην κατάσταση και όχι σε συνάρτηση που καλείται όταν ανοίγει το
         * μενού: τα κομμάτια γίνονται γνωστά ΜΕΤΑ την έναρξη, και συχνά αλλάζουν
         * όταν η ροή αλλάζει ποιότητα. Ένα μενού που τα ρωτούσε μία φορά έδειχνε
         * κενή λίστα αν είχες προλάβει να το ανοίξεις — και δεν ενημερωνόταν ποτέ.
         */
        val audioTracks: List<TrackOption> = emptyList(),
        val subtitleTracks: List<TrackOption> = emptyList(),
        val error: String? = null,
    )

    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var exo: ExoPlayer? = null
    // TextureView ή SurfaceView — δες [attachSurface]. Κρατιέται ως View ώστε να
    // μπορεί να ξαναπροσαρτηθεί σωστά όταν χτίζεται νέος player.
    private var surface: View? = null

    /** Διαγνωστικό ίχνος του ορίου επιφάνειας. Δες `docs/NEXT_CHAT_HANDOFF.md`. */
    private val SURFACE_TAG = "PlayerSurface"
    private var retryAttempt = 0
    private var currentUrl: String = ""
    private var externalSubtitleCues: List<Cue>? = null
    private var externalSubtitleLabel: String = ""
    private var lastExternalCue: Cue? = null

    /** Το MediaItem περιέχει μόνο τη ροή· οι κατεβασμένοι SRT ζωγραφίζονται χωριστά. */
    private fun buildMediaItem(url: String): MediaItem = MediaItem.fromUri(url)

    /**
     * Ενεργοποιεί ήδη αναλυμένες γραμμές SRT χωρίς αλλαγή του MediaItem.
     * Απενεργοποιείται μόνο το εσωτερικό text track, οπότε εικόνα, ήχος,
     * θέση αναπαραγωγής και buffers συνεχίζουν ακριβώς όπως βρίσκονται.
     */
    fun setExternalSubtitle(cues: List<Cue>, label: String) {
        if (cues.isEmpty()) return
        externalSubtitleCues = cues
        externalSubtitleLabel = label
        lastExternalCue = null
        val p = exo ?: return
        // The downloaded SRT is rendered by our shared subtitle layer. Only the
        // embedded text renderer is disabled; video/audio and their buffers remain
        // untouched, so switching subtitle sources never reloads or pauses playback.
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        publishExternalCue(p.currentPosition.coerceAtLeast(0L))
        publishTracks()
    }

    /** Το τρέχον instance — για προχωρημένες ρυθμίσεις (tracks, ταχύτητα). */
    val player: ExoPlayer? get() = exo

    /**
     * Δημιουργεί τη μηχανή αν δεν υπάρχει. Η ίδια ρύθμιση με τον υπάρχοντα
     * player: ffmpeg renderers για εξωτικούς κωδικοποιητές, desktop user-agent
     * (πολλοί πάροχοι μπλοκάρουν αλλιώς), και audio focus ώστε να μη
     * συνυπάρχουν δύο ήχοι όταν έρθει κλήση ή ειδοποίηση.
     */
    private fun ensurePlayer(): ExoPlayer {
        exo?.let { return it }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Http.DESKTOP_UA)
            .setAllowCrossProtocolRedirects(true)
            // ΧΡΟΝΙΚΑ ΟΡΙΑ ΔΙΚΤΥΟΥ.
            //
            // Οι προεπιλογές είναι 8 δευτ. — γραμμένες για γρήγορα CDN. Ένας
            // πάροχος IPTV που αργεί 9 δευτ. να απαντήσει θεωρούνταν νεκρός: ο
            // player πετούσε σφάλμα και ξεκινούσε από την αρχή. Αυτό ακριβώς
            // φαίνεται σαν «κολλάει πολλές φορές».
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        val renderers = NextRenderersFactory(appContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            // ΣΥΓΧΡΟΝΙΣΜΟΣ ΕΙΚΟΝΑΣ - ΗΧΟΥ ΣΤΗΝ ΕΝΑΡΞΗ.
            //
            // Η προεπιλογή είναι 5000 ms: για πέντε ολόκληρα δευτερόλεπτα μετά
            // την έναρξη ή την αλλαγή καναλιού, ο renderer βγάζει καρέ ΧΩΡΙΣ να
            // τα συγχρονίζει με τον ήχο — ώστε η εικόνα να εμφανιστεί γρήγορα.
            //
            // Σε ταινία που παίζει μία ώρα δεν το προσέχει κανείς. Σε ζωντανά,
            // όπου αλλάζεις κανάλι συνέχεια, βρίσκεσαι μονίμως μέσα σε αυτό το
            // παράθυρο — τα χείλη δεν κουμπώνουν ποτέ.
            //
            // Με 0 ο συγχρονισμός ισχύει από το πρώτο καρέ. Κόστος: η εικόνα
            // εμφανίζεται ελάχιστα αργότερα.
            .setAllowedVideoJoiningTimeMs(0)
        val created = ExoPlayer.Builder(appContext, renderers)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(httpFactory)
                    // Έξι προσπάθειες πριν παραδοθεί: μια στιγμιαία αναταραχή
                    // δικτύου δεν πρέπει να διακόπτει την ταινία.
                    .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(6))
            )
            .setLoadControl(buildLoadControl())
            .setVideoChangeFrameRateStrategy(
                // Η ρύθμιση του χρήστη διαβάζεται ΕΔΩ, τη στιγμή που χτίζεται ο
                // player, και όχι από τη διεπαφή: η στρατηγική κλειδώνεται στον
                // constructor, οπότε μια εκ των υστέρων αλλαγή θα απαιτούσε
                // release() — δηλαδή διακοπή της ροής που ήδη παίζει.
                if (frameRateMatchingEnabled()) {
                    C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
                } else {
                    C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF
                }
            )
            .build()
        created.setAudioAttributes(AudioAttributes.DEFAULT, true)
        runCatching {
            val preferences = PlaylistStore(appContext)
            val selection = created.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !preferences.startWithSubtitles)
            preferences.preferredAudioLanguage.takeIf { it.isNotBlank() }
                ?.let { selection.setPreferredAudioLanguage(it) }
            preferences.preferredSubtitleLanguage.takeIf { it.isNotBlank() }
                ?.let { selection.setPreferredTextLanguage(it) }
            created.trackSelectionParameters = selection.build()
        }
        created.addListener(listener)
        exo = created
        // Η επιφάνεια μπορεί να είναι είτε του ενός είτε του άλλου τύπου.
        when (val existing = surface) {
            is SurfaceView -> created.setVideoSurfaceView(existing)
            is TextureView -> created.setVideoTextureView(existing)
            else -> Unit
        }
        return created
    }

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            // Ίδια πολιτική με τον πλήρη player: τα παροδικά σφάλματα δικτύου
            // ξαναδοκιμάζονται αντί να πετούν τον χρήστη έξω.
            val retry = PlaybackStabilityPolicy.shouldRetryTransientIo(
                attempt = retryAttempt,
                isIoFailure = PlaybackStabilityPolicy.hasIoCause(error),
                requestStillCurrent = true,
            )
            if (retry) {
                val delayMs = PlaybackStabilityPolicy.retryDelayMs(retryAttempt)
                val resumeAt = exo?.currentPosition?.coerceAtLeast(0L) ?: 0L
                retryAttempt++
                handler.postDelayed({ open(currentUrl, resumeAt) }, delayMs)
                return
            }

            // ---- ΠΤΩΣΗ ΣΤΟ LIBVLC ----
            //
            // ΓΙΑΤΙ ΧΡΕΙΑΖΕΤΑΙ, ΑΦΟΥ ΥΠΑΡΧΕΙ ΗΔΗ Η ΕΠΙΛΟΓΗ ΑΠΟ ΤΗ ΔΙΕΥΘΥΝΣΗ:
            //
            // Η διεύθυνση προδίδει το ΠΡΩΤΟΚΟΛΛΟ, όχι το περιεχόμενο. Μια ταινία
            // `…/12345.mkv` πάει σωστά στο ExoPlayer — και μπορεί να αποτύχει
            // επειδή μέσα έχει κωδικοποιητή που η συσκευή δεν ξέρει, επειδή ο
            // πάροχος δεν στέλνει Content-Length, επειδή το MKV δεν είναι
            // αναζητήσιμο, ή για δέκα άλλους λόγους που κανείς δεν μαντεύει από
            // το URL.
            //
            // Αυτό ακριβώς κάνουν οι εφαρμογές που «παίζουν τα πάντα»: δεν ξέρουν
            // κάτι που δεν ξέρουμε — απλώς έχουν εφεδρεία.
            //
            // ΜΙΑ ΦΟΡΑ ΑΝΑ ΔΙΕΥΘΥΝΣΗ: το `vlcFallbackUrl` εμποδίζει τον βρόχο
            // ExoPlayer → σφάλμα → LibVLC → σφάλμα → ExoPlayer.
            if (backend == PlaybackBackend.EXO && currentUrl != vlcFallbackUrl) {
                vlcFallbackUrl = currentUrl
                val resumeAt = exo?.currentPosition?.coerceAtLeast(0L) ?: 0L
                handler.post { openWithVlc(currentUrl, resumeAt) }
                return
            }

            _state.value = _state.value.copy(error = "Σφάλμα αναπαραγωγής", playing = false)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Ξεκίνησε: το δίχτυ δεν χρειάζεται πια.
            if (isPlaying) handler.removeCallbacks(exoStartTimeout)
            if (isPlaying) retryAttempt = 0
            _state.value = _state.value.copy(playing = isPlaying, error = null)
            if (isPlaying) startProgressTicker() else stopProgressTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.value = _state.value.copy(
                buffering = playbackState == Player.STATE_BUFFERING,
                durationMs = exo?.duration?.takeIf { it > 0 } ?: 0L,
            )
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.height > 0) {
                _state.value = _state.value.copy(
                    videoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                )
            }
            publishQuality()
        }

        override fun onTracksChanged(tracks: Tracks) {
            // Ο κωδικοποιητής και ο ρυθμός καρέ γίνονται γνωστά μόνο αφού
            // επιλεγούν τα κομμάτια — όχι στο onVideoSizeChanged.
            publishQuality()
            _state.value = _state.value.copy(
                audioTracks = tracksOf(C.TRACK_TYPE_AUDIO),
                subtitleTracks = tracksOf(C.TRACK_TYPE_TEXT),
            )
        }

        override fun onCues(cueGroup: CueGroup) {
            if (externalSubtitleCues == null) _cues.value = cueGroup.cues
        }

        override fun onRenderedFirstFrame() {
            _state.value = _state.value.copy(
                renderedFrames = _state.value.renderedFrames + 1
            )
        }
    }

    /**
     * ΟΙ ΥΠΟΤΙΤΛΟΙ ΔΕΝ ΖΩΓΡΑΦΙΖΟΝΤΑΙ ΜΟΝΟΙ ΤΟΥΣ.
     *
     * Το TextureView και το SurfaceView δείχνουν ΜΟΝΟ εικόνα. Ο έτοιμος
     * PlayerView του Media3 κρύβει ένα SubtitleView από πάνω και το ταΐζει — εμείς
     * όμως δεν τον χρησιμοποιούμε, γιατί χρειαζόμασταν έλεγχο της επιφάνειας για
     * τη μεγέθυνση και το frame pacing.
     *
     * Χάθηκε έτσι κάτι που κανείς δεν πρόσεξε: το μενού «Υπότιτλοι» επέλεγε
     * κανονικά κομμάτι, ο player το αποκωδικοποιούσε — και δεν φαινόταν τίποτα,
     * επειδή δεν υπήρχε πουθενά να ζωγραφιστεί. Εδώ βγαίνουν οι γραμμές προς τα
     * έξω, και η διεπαφή τις εμφανίζει.
     */
    private val _cues = MutableStateFlow<List<MediaCue>>(emptyList())
    val cues: StateFlow<List<MediaCue>> = _cues.asStateFlow()

    private fun publishExternalCue(positionMs: Long) {
        val subtitles = externalSubtitleCues ?: return
        val active = PlayerSubtitlePolicy.activeCue(subtitles, positionMs)
        if (active == lastExternalCue) return
        lastExternalCue = active
        _cues.value = active?.let {
            listOf(MediaCue.Builder().setText(it.text).build())
        }.orEmpty()
    }

    private fun publishTracks() {
        _state.value = _state.value.copy(
            audioTracks = tracksOf(C.TRACK_TYPE_AUDIO),
            subtitleTracks = tracksOf(C.TRACK_TYPE_TEXT),
        )
    }

    /**
     * Διαβάζει τα τεχνικά στοιχεία από το ενεργό format και τα δημοσιεύει.
     *
     * Ο ρυθμός καρέ είναι το σημαντικό: χωρίς αυτόν η τηλεόραση δεν μπορεί να
     * κλειδώσει στη σωστή συχνότητα και η κίνηση σπάει.
     */
    private fun publishQuality() {
        val format: Format = exo?.videoFormat ?: return
        val quality = VideoQuality(
            width = format.width.takeIf { it > 0 } ?: 0,
            height = format.height.takeIf { it > 0 } ?: 0,
            frameRate = format.frameRate.takeIf { it > 0f } ?: 0f,
            codec = format.sampleMimeType.orEmpty(),
            bitrateBps = format.bitrate.takeIf { it > 0 }
                ?: format.averageBitrate.takeIf { it > 0 } ?: 0,
        )
        if (quality == _state.value.quality) return
        _state.value = _state.value.copy(quality = quality)
    }

    /**
     * ΑΠΟΘΕΜΑ ΠΡΙΝ ΤΗΝ ΑΝΑΠΑΡΑΓΩΓΗ.
     *
     * Οι προεπιλογές του ExoPlayer είναι γραμμένες για βίντεο κατά παραγγελία σε
     * σταθερή γραμμή. Σε ροή IPTV δίνουμε μεγαλύτερο απόθεμα και ζητάμε ρητά να
     * μετριέται σε ΧΡΟΝΟ αντί σε bytes: μια ροή 20 Mbps γέμιζε το όριο μεγέθους
     * έχοντας μόλις λίγα δευτερόλεπτα εικόνας μπροστά.
     */
    /**
     * Έχει ζητήσει ο χρήστης αντιστοίχιση συχνότητας;
     *
     * Διαβάζεται αμυντικά: αν για οποιονδήποτε λόγο δεν υπάρχει αποθηκευμένη
     * ρύθμιση, δεν εμποδίζουμε την αναπαραγωγή για μια προτίμηση.
     */
    private fun frameRateMatchingEnabled(): Boolean = try {
        AutoFrameRateMode.fromStorage(PlaylistStore(appContext).autoFrameRateMode) !=
            AutoFrameRateMode.OFF
    } catch (_: Exception) {
        false
    }

    private fun buildLoadControl(): LoadControl {
        val profile = try {
            BufferPolicy.fromStorage(PlaylistStore(appContext).bufferProfile)
        } catch (_: Exception) {
            BufferProfile.NORMAL
        }
        val d = BufferPolicy.durationsFor(profile)
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(d.minMs, d.maxMs, d.forPlaybackMs, d.afterRebufferMs)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * ΔΥΟ ΕΙΔΗ ΕΠΙΦΑΝΕΙΑΣ, ΓΙΑ ΔΥΟ ΔΙΑΦΟΡΕΤΙΚΕΣ ΔΟΥΛΕΙΕΣ.
     *
     * [TextureView]: αλλάζει μέγεθος ομαλά, άρα κάνει δυνατή τη μεγέθυνση από
     * προεπισκόπηση σε πλήρη οθόνη χωρίς αναβόσβημα. Περνά όμως από τον
     * compositor του GPU με έναν επιπλέον buffer, οπότε η στιγμή που εμφανίζεται
     * κάθε καρέ δεν κλειδώνει τέλεια στο vsync — ορατό ως judder σε κίνηση.
     *
     * [SurfaceView]: συντίθεται απευθείας από το σύστημα, με ακριβές frame
     * pacing, αλλά δεν συμπεριφέρεται το ίδιο καλά όσο αλλάζει μέγεθος.
     *
     * Καμία από τις δύο δεν είναι «η σωστή» — γι' αυτό η μηχανή δέχεται και τις
     * δύο και η διεπαφή διαλέγει: μικρή προεπισκόπηση όπου το judder δεν
     * φαίνεται -> TextureView· πλήρης οθόνη όπου φαίνεται -> SurfaceView.
     */
    fun attachSurface(view: TextureView) {
        if (surface === view) {
            Log.d(SURFACE_TAG, "attach TextureView#${System.identityHashCode(view)} ήδη ενεργή")
            return
        }
        surface = view
        exo?.setVideoTextureView(view)
        Log.d(
            SURFACE_TAG,
            "attach TextureView#${System.identityHashCode(view)} " +
                "μηχανή=${_renderer.value} exo=${exo != null}"
        )
    }

    /**
     * Σύντομη ταυτότητα της ενεργής επιφάνειας, ΜΟΝΟ για το διαγνωστικό του QA.
     *
     * Δεν το διαβάζει καμία απόφαση αναπαραγωγής· υπάρχει για να απαντηθεί με
     * ένα screenshot το «σε ποια επιφάνεια ζωγραφίζει τώρα».
     */
    fun attachedSurfaceLabel(): String = when (val current = surface) {
        null -> "—"
        else -> "${current.javaClass.simpleName.take(4)}#${System.identityHashCode(current) % 10000}"
    }

    /** Detaches only if [view] is still the active surface. */
    fun detachSurface(view: TextureView) {
        if (surface !== view) {
            Log.d(
                SURFACE_TAG,
                "detach TextureView#${System.identityHashCode(view)} ΑΓΝΟΗΘΗΚΕ, " +
                    "ενεργή είναι #${surface?.let { System.identityHashCode(it) }}"
            )
            return
        }
        exo?.clearVideoTextureView(view)
        surface = null
        Log.d(SURFACE_TAG, "detach TextureView#${System.identityHashCode(view)} ΕΓΙΝΕ")
    }

    /** Δες [attachSurface] για το γιατί υπάρχουν δύο. */
    fun attachSurface(view: SurfaceView) {
        if (surface === view) return
        surface = view
        exo?.setVideoSurfaceView(view)
    }

    /** Detaches only if [view] is still the active surface. */
    fun detachSurface(view: SurfaceView) {
        if (surface !== view) return
        exo?.clearVideoSurfaceView(view)
        surface = null
    }

    /**
     * Τα κομμάτια βίντεο μιας πολλαπλής ροής (π.χ. HLS με 1080p/720p/480p).
     *
     * Δική του υλοποίηση και όχι [tracksOf]: εκεί η ετικέτα βγαίνει από το
     * `label`/`language`, που για βίντεο είναι σχεδόν πάντα κενά — θα έβγαινε
     * «Κομμάτι 1, Κομμάτι 2, Κομμάτι 3» και δεν θα ήξερες τι διαλέγεις. Εδώ η
     * ετικέτα είναι η ανάλυση, που είναι το μόνο που ενδιαφέρει.
     */
    fun videoTracks(): List<TrackOption> {
        val p = exo ?: return emptyList()
        val out = ArrayList<TrackOption>()
        p.currentTracks.groups.forEach { group ->
            if (group.type != C.TRACK_TYPE_VIDEO) return@forEach
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val resolution = PlaybackQualityPolicy.resolutionLabel(format.height)
                val bitrate = PlaybackQualityPolicy.bitrateLabel(
                    format.bitrate.takeIf { it > 0 } ?: format.averageBitrate
                )
                out += TrackOption(
                    id = "${group.mediaTrackGroup.id}:$i",
                    label = listOf(resolution, bitrate)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                        .ifBlank { "Κομμάτι ${out.size + 1}" },
                    selected = group.isTrackSelected(i),
                )
            }
        }
        // Από την υψηλότερη ανάλυση προς τα κάτω: έτσι τη διαβάζει ο άνθρωπος.
        return out.reversed()
    }

    fun selectVideo(id: String?) {
        if (id == null) {
            // null = αυτόματη επιλογή. Καθαρίζουμε την υπέρβαση αντί να
            // απενεργοποιήσουμε το βίντεο — αλλιώς θα έμενε μαύρη οθόνη.
            val p = exo ?: return
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .build()
            return
        }
        selectTrack(C.TRACK_TYPE_VIDEO, id)
    }

    /** Ξεκινά (ή αλλάζει) ροή. Η ίδια μηχανή, άρα καμία διακοπή στην επιφάνεια. */
    /* ------------------------- επιλογή μηχανής ------------------------- */

    /**
     * Ποια μηχανή παίζει ΤΩΡΑ. Αποφασίζεται στο [open] από τη διεύθυνση.
     *
     * Η υπόλοιπη εφαρμογή δεν το βλέπει ποτέ: το `PlayerHost`, το
     * `TvPlaybackOverlay` και το `MobilePlaybackOverlay` καλούν τις ίδιες
     * μεθόδους, με την ίδια σημασία, ό,τι κι αν παίζει από κάτω.
     */
    private var backend: PlaybackBackend = PlaybackBackend.EXO
    private var vlc: VlcBackend? = null

    /**
     * Ποιο είδος επιφάνειας χρειάζεται η τρέχουσα ροή.
     *
     * ΤΟ ΜΟΝΟ ΠΟΥ ΔΙΑΡΡΕΕΙ ΠΡΟΣ ΤΑ ΕΞΩ, ΚΑΙ ΜΟΝΟ ΣΕ ΕΝΑ ΣΗΜΕΙΟ: το
     * `PlayerVideoSurface` πρέπει να ξέρει αν θα φτιάξει `SurfaceView` (ExoPlayer)
     * ή `VLCVideoLayout` (LibVLC) — δεν υπάρχει κοινή επιφάνεια για τα δύο.
     *
     * Δεν είναι διαρροή πολιτικής: είναι γεγονός σχεδίασης. Κανένα άλλο αρχείο
     * διεπαφής δεν το διαβάζει, και κανένα δεν αλλάζει συμπεριφορά ανάλογα με
     * αυτό — χειριστήρια, μενού και πλήκτρα μένουν ίδια.
     */
    private val _renderer = MutableStateFlow(PlaybackBackend.EXO)
    val renderer: StateFlow<PlaybackBackend> = _renderer.asStateFlow()

    /** Προσαρτά την επιφάνεια του LibVLC. Αγνοείται όταν παίζει το ExoPlayer. */
    fun attachVlcLayout(layout: org.videolan.libvlc.util.VLCVideoLayout) {
        Log.d(SURFACE_TAG, "attach VLCVideoLayout#${System.identityHashCode(layout)} vlc=${vlc != null}")
        vlc?.attachLayout(layout)
    }

    /** Ενημερώνει τη γεωμετρία της ζωντανής εξόδου, χωρίς ξήλωμα. Δες [reattachVlcLayout]. */
    fun updateVlcWindowSize(width: Int, height: Int) {
        Log.d(SURFACE_TAG, "vlc window size -> ${width}x$height")
        vlc?.updateWindowSize(width, height)
    }

    /** true όσο το LibVLC αναφέρει ενεργή έξοδο βίντεο. Δίχτυ ασφαλείας, όχι απόφαση. */
    fun vlcVideoOutputActive(): Boolean = vlcFrameWasRendered

    /** Ξαναδένει την ίδια επιφάνεια μετά από ουσιαστική αλλαγή μεγέθους. */
    fun reattachVlcLayout(layout: org.videolan.libvlc.util.VLCVideoLayout) {
        Log.d(SURFACE_TAG, "reattach VLCVideoLayout#${System.identityHashCode(layout)} λόγω αλλαγής μεγέθους")
        vlc?.reattachLayout(layout)
    }

    /** Αποσυνδέει μόνο αν το [layout] είναι ακόμη το ενεργό. Δες [detachSurface]. */
    fun detachVlcLayout(layout: org.videolan.libvlc.util.VLCVideoLayout) {
        Log.d(SURFACE_TAG, "detach VLCVideoLayout#${System.identityHashCode(layout)}")
        vlc?.detachLayout(layout)
    }

    /** true όταν η τρέχουσα ροή είναι ζωντανή — καθορίζει την αποθήκευση του VLC. */
    private var liveStream: Boolean = false

    /** Ρύθμιση χρήστη «πάντα LibVLC», για πηγές που το Media3 χειρίζεται άσχημα. */
    var forceVlc: Boolean = false

    /**
     * Η διεύθυνση για την οποία έχει ήδη γίνει πτώση στο LibVLC.
     *
     * Φρουρός βρόχου: χωρίς αυτόν, μια ροή που αποτυγχάνει και στις δύο μηχανές θα
     * πηγαινοερχόταν για πάντα, με τον χρήστη να βλέπει μαύρη οθόνη που «σκέφτεται».
     */
    private var vlcFallbackUrl: String = ""
    private var vlcFrameWasRendered: Boolean = false

    /**
     * Ανοίγει ρητά με LibVLC, χωρίς να ξαναρωτηθεί η πολιτική.
     *
     * Χρησιμοποιείται μόνο ως εφεδρεία μετά από αποτυχία του ExoPlayer — δες
     * [Player.Listener.onPlayerError].
     */
    /**
     * Πόσο περιμένουμε το ExoPlayer να ΑΡΧΙΣΕΙ πριν δοκιμάσουμε LibVLC.
     *
     * 7 δευτερόλεπτα: αρκετά για αργό διακομιστή σε 4G, λίγα ώστε ο χρήστης να μη
     * νομίσει ότι κόλλησε. Το κόστος πληρώνεται ΜΟΝΟ από ροές που ούτως ή άλλως
     * δεν θα έπαιζαν.
     */
    private val exoStartTimeout = Runnable {
        // ΤΟ ΣΦΑΛΜΑ ΔΕΝ ΕΙΝΑΙ ΠΑΝΤΑ ΣΦΑΛΜΑ.
        //
        // Η εφεδρεία στο `onPlayerError` καλύπτει μόνο όσα το ExoPlayer αναγνωρίζει
        // ως αποτυχία. Πολλοί διακομιστές IPTV δεν δίνουν ποτέ σφάλμα: κρατούν τη
        // σύνδεση ανοιχτή, στέλνουν κεφαλίδες που το Media3 δεν καταλαβαίνει, και
        // ο player μένει σε BUFFERING **για πάντα**.
        //
        // Ο χρήστης βλέπει μαύρη οθόνη με κυκλάκι — και καμία εφεδρεία δεν
        // πυροδοτείται, γιατί τυπικά δεν έχει συμβεί τίποτα κακό.
        //
        // Ακριβώς αυτό συνέβαινε στις ταινίες: το `onPlayerError` δεν καλούνταν
        // ποτέ.
        if (backend != PlaybackBackend.EXO) return@Runnable
        if (_state.value.playing) return@Runnable
        if (currentUrl.isBlank() || currentUrl == vlcFallbackUrl) return@Runnable
        vlcFallbackUrl = currentUrl
        openWithVlc(currentUrl, exo?.currentPosition?.coerceAtLeast(0L) ?: 0L)
    }

    private fun openWithVlc(url: String, resumeMs: Long) {
        handler.removeCallbacks(exoStartTimeout)
        exo?.stop()
        handler.removeCallbacks(stallWatchdog)
        handler.removeCallbacks(progressTick)
        backend = PlaybackBackend.VLC
        _renderer.value = PlaybackBackend.VLC
        _state.value = _state.value.copy(error = null, buffering = true)
        ensureVlc().open(url, live = liveStream, resumeMs = resumeMs, playWhenReady = true)
    }

    private fun ensureVlc(): VlcBackend = vlc ?: VlcBackend(appContext) { snapshot ->
        // Τα γεγονότα του LibVLC έρχονται σε δικό του νήμα. Η κατάσταση διαβάζεται
        // από το Compose, οπότε η μετάβαση στο κύριο νήμα γίνεται ΕΔΩ, μία φορά.
        handler.post {
            val engine = vlc ?: return@post
            val renderedFrameAdvanced = snapshot.renderedFrame && !vlcFrameWasRendered
            vlcFrameWasRendered = snapshot.renderedFrame
            _state.value = VlcStateMapper.merge(
                previous = _state.value,
                snapshot = snapshot,
                audioTracks = engine.audioTracks(),
                subtitleTracks = engine.subtitleTracks(),
                renderedFrameAdvanced = renderedFrameAdvanced,
            )
        }
    }.also { vlc = it }

    /**
     * @param live ζωντανό κανάλι. Καθορίζει την αποθήκευση δικτύου του LibVLC —
     *   1,5 δευτ. στα ζωντανά για γρήγορη αλλαγή, 3 δευτ. στις ταινίες για ομαλή
     *   ροή. Προεπιλογή `false`: μια ταινία με ρύθμιση ζωντανού κολλάει, ένα
     *   κανάλι με ρύθμιση ταινίας απλώς αργεί λίγο να ανοίξει.
     */
    fun open(
        url: String,
        resumeMs: Long = 0L,
        playWhenReady: Boolean = true,
        live: Boolean = false,
    ) {
        if (url.isBlank()) return
        liveStream = live
        // Νέο περιεχόμενο σημαίνει ότι ο εξωτερικός υπότιτλος του προηγούμενου
        // δεν ισχύει πια. Χωρίς αυτό, οι υπότιτλοι μιας ταινίας θα κουβαλιόνταν
        // στο επόμενο επεισόδιο.
        if (url != currentUrl) {
            val hadExternalSubtitle = externalSubtitleCues != null
            externalSubtitleCues = null
            externalSubtitleLabel = ""
            lastExternalCue = null
            if (hadExternalSubtitle) {
                exo?.let { player ->
                    val startEmbedded = runCatching {
                        PlaylistStore(appContext).startWithSubtitles
                    }.getOrDefault(true)
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !startEmbedded)
                        .build()
                }
            }
        }
        currentUrl = url
        if (externalSubtitleCues != null) lastExternalCue = null
        // Αλλιώς η τελευταία γραμμή του προηγούμενου καναλιού μένει καρφωμένη
        // στην οθόνη μέχρι να έρθει η πρώτη του καινούργιου.
        _cues.value = emptyList()

        // ---- Η ΜΟΝΗ ΔΙΑΚΛΑΔΩΣΗ ----
        //
        // Αποφασίζεται από τη ΔΙΕΥΘΥΝΣΗ, πριν ανοίξει σύνδεση. Δοκιμή-και-αποτυχία
        // θα κόστιζε δευτερόλεπτα μαύρης οθόνης σε κάθε RTSP/RTMP κανάλι.
        // ΚΑΘΕ ΑΝΟΙΓΜΑ ΞΕΚΙΝΑ ΚΑΘΑΡΟ.
        //
        // Ο φρουρός υπάρχει για να μη γίνει βρόχος ΜΕΣΑ σε ένα άνοιγμα. Αν έμενε
        // αναμμένος, η ίδια ταινία θα αποτύγχανε για πάντα μετά την πρώτη πτώση:
        // δεύτερο άνοιγμα -> ExoPlayer -> σφάλμα -> ο φρουρός μπλοκάρει την
        // εφεδρεία -> μαύρη οθόνη. Βρόχος δεν μπορεί να προκύψει, γιατί η
        // επιστροφή στο ExoPlayer γίνεται μόνο από νέο [open].
        vlcFallbackUrl = ""
        backend = PlaybackBackendPolicy.backendFor(url, forceVlc)
        // Ενημερώνεται ΠΡΙΝ ανοίξει η ροή, ώστε η επιφάνεια να έχει ήδη αλλάξει
        // όταν βγει το πρώτο καρέ. Αλλιώς το LibVLC ζωγραφίζει σε επιφάνεια που
        // δεν υπάρχει ακόμη και το πρώτο δευτερόλεπτο είναι μαύρο.
        _renderer.value = backend
        if (backend == PlaybackBackend.VLC) {
            // Το ExoPlayer σταματά αλλά ΔΕΝ καταστρέφεται: η επόμενη ταινία μπορεί
            // να είναι HLS, και το ξαναστήσιμο κοστίζει.
            exo?.stop()
            // Ο φύλακας κολλήματος και ο μετρητής προόδου ανήκουν στο ExoPlayer:
            // ρωτούν το `exo` για θέση. Αφημένοι ζωντανοί, θα έβλεπαν θέση που δεν
            // προχωρά (γιατί παίζει το LibVLC) και θα «διόρθωναν» επανεκκινώντας
            // ροή που δεν τρέχει.
            handler.removeCallbacks(stallWatchdog)
            handler.removeCallbacks(progressTick)
            _state.value = _state.value.copy(error = null, buffering = true)
            ensureVlc().open(url, live = liveStream, resumeMs = resumeMs, playWhenReady = playWhenReady)
            return
        }
        // Γυρίσαμε σε ροή που ξέρει το Media3: το LibVLC σταματά για να μην κρατά
        // δίκτυο και αποκωδικοποιητή ανοιχτά στο παρασκήνιο.
        vlc?.stop()

        val p = ensurePlayer()
        p.setMediaItem(buildMediaItem(url))
        p.prepare()
        if (resumeMs > 1000L) p.seekTo(resumeMs)
        p.playWhenReady = playWhenReady
        _state.value = _state.value.copy(error = null)
        if (playWhenReady) {
            startStallWatchdog()
            // Δίχτυ για ροές που δεν ξεκινούν ΚΑΙ δεν δίνουν σφάλμα.
            handler.removeCallbacks(exoStartTimeout)
            handler.postDelayed(exoStartTimeout, EXO_START_TIMEOUT_MS)
        }
    }

    /* ------------------------------ πρόοδος ---------------------------- */

    // Η θέση δεν έρχεται με callback: πρέπει να τη ρωτάμε. Το ticker τρέχει ΜΟΝΟ
    // όσο παίζει, ώστε να μη ξυπνά η συσκευή χωρίς λόγο σε παύση.
    private val progressTick = object : Runnable {
        override fun run() {
            val p = exo ?: return
            val position = p.currentPosition.coerceAtLeast(0L)
            _state.value = _state.value.copy(
                positionMs = position,
                durationMs = p.duration.takeIf { it > 0 } ?: 0L,
            )
            publishExternalCue(position)
            handler.postDelayed(
                this,
                if (externalSubtitleCues == null) PROGRESS_INTERVAL_MS else EXTERNAL_SUBTITLE_INTERVAL_MS
            )
        }
    }

    private fun startProgressTicker() {
        handler.removeCallbacks(progressTick)
        handler.post(progressTick)
    }

    private fun stopProgressTicker() {
        handler.removeCallbacks(progressTick)
    }

    /* ------------------------------ επιτήρηση κολλήματος --------------- */

    // ΓΙΑΤΙ ΧΡΕΙΑΖΕΤΑΙ ΞΕΧΩΡΙΣΤΗ ΕΠΙΤΗΡΗΣΗ:
    //
    // Η επανάληψη γινόταν ΜΟΝΟ στο onPlayerError. Αλλά μια ζωντανή ροή που
    // παγώνει συχνά δεν βγάζει σφάλμα: ο πάροχος κρατά τη σύνδεση ανοιχτή και
    // απλώς σταματά να στέλνει δεδομένα. Ο player μένει για πάντα σε «φόρτωση»,
    // ικανοποιημένος, και κανείς δεν του λέει να ξαναπροσπαθήσει.
    //
    // Ο παλιός player και το multiview είχαν ακριβώς αυτόν τον φύλακα. Ο νέος
    // τον έχασε στη μετάβαση — γι' αυτό «δεν επανέρχεται όπως έκανε παλιά».
    private var lastKnownPosition = -1L
    private var lastProgressAt = 0L
    private var stallRestarts = 0
    private var healthyTicks = 0

    private val stallWatchdog = object : Runnable {
        override fun run() {
            val p = exo
            if (p != null && p.playWhenReady) {
                val now = android.os.SystemClock.elapsedRealtime()
                val position = p.currentPosition
                if (p.isPlaying && position != lastKnownPosition) {
                    lastKnownPosition = position
                    lastProgressAt = now
                    // Σταθερή αναπαραγωγή για αρκετή ώρα: ξεχνάμε τις παλιές
                    // αποτυχίες. Αλλιώς ένα κανάλι που κόλλησε δύο φορές μέσα σε
                    // μια ώρα θα είχε εξαντλήσει τις προσπάθειες την τρίτη, ώρες
                    // αργότερα, χωρίς κανένα λόγο.
                    healthyTicks++
                    if (healthyTicks >= HEALTHY_TICKS_TO_FORGIVE) {
                        stallRestarts = 0
                        healthyTicks = 0
                    }
                } else if (lastProgressAt > 0L && now - lastProgressAt >= STALL_TIMEOUT_MS) {
                    healthyTicks = 0
                    if (stallRestarts < MAX_STALL_RESTARTS) {
                        stallRestarts++
                        lastProgressAt = now
                        // Ζωντανά: πάντα από την άκρη. Επιστροφή στην παλιά θέση
                        // θα ζητούσε δεδομένα που ο πάροχος δεν κρατά πια.
                        val resumeAt = if (durationMs() > 0) position.coerceAtLeast(0L) else 0L
                        open(currentUrl, resumeMs = resumeAt)
                        // ΕΞΟΔΟΣ ΧΩΡΙΣ ΝΕΟ ΠΡΟΓΡΑΜΜΑΤΙΣΜΟ: το open() ξαναστήνει
                        // τον φύλακα μόνο του. Αν συνεχίζαμε, θα έτρεχαν δύο
                        // αντίγραφα — και μετά από λίγα κολλήματα, οκτώ.
                        return
                    } else {
                        _state.value = _state.value.copy(
                            error = "Η ροή σταμάτησε και δεν επανήλθε",
                            playing = false,
                        )
                    }
                }
            }
            handler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    private fun startStallWatchdog() {
        handler.removeCallbacks(stallWatchdog)
        lastKnownPosition = -1L
        lastProgressAt = android.os.SystemClock.elapsedRealtime()
        handler.postDelayed(stallWatchdog, WATCHDOG_INTERVAL_MS)
    }

    /* ------------------------------ κομμάτια ήχου / υποτίτλων ---------- */

    /** Μια επιλογή ήχου ή υποτίτλων, έτοιμη για εμφάνιση σε μενού. */
    data class TrackOption(
        val id: String,
        val label: String,
        val selected: Boolean,
    )

    private fun tracksOf(type: Int): List<TrackOption> {
        val p = exo ?: return emptyList()
        val out = ArrayList<TrackOption>()
        if (type == C.TRACK_TYPE_TEXT && externalSubtitleCues != null) {
            out += TrackOption(
                id = EXTERNAL_SUBTITLE_TRACK_ID,
                label = externalSubtitleLabel.ifBlank { "OpenSubtitles" },
                selected = true,
            )
        }
        var fallbackIndex = 1
        p.currentTracks.groups.forEach { group ->
            if (group.type != type) return@forEach
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                out += TrackOption(
                    id = "${group.mediaTrackGroup.id}:$i",
                    // Η ΓΛΩΣΣΑ ΜΠΡΟΣΤΑ. Πριν, η ετικέτα ήταν το `label` του
                    // παρόχου — που είναι συνήθως κενό — και το μενού έδειχνε
                    // «Κομμάτι 1, Κομμάτι 2»: έπρεπε να τα δοκιμάσεις ένα-ένα για
                    // να βρεις ποιο είναι στα ελληνικά.
                    label = appContext.playerTrackLabel(
                        language = format.language,
                        providerLabel = format.label,
                        fallbackIndex = fallbackIndex++,
                    ),
                    selected = (type != C.TRACK_TYPE_TEXT || externalSubtitleCues == null) &&
                        group.isTrackSelected(i),
                )
            }
        }
        return out
    }

    fun audioTracks(): List<TrackOption> =
        if (backend == PlaybackBackend.VLC) vlc?.audioTracks().orEmpty()
        else tracksOf(C.TRACK_TYPE_AUDIO)

    fun subtitleTracks(): List<TrackOption> =
        if (backend == PlaybackBackend.VLC) vlc?.subtitleTracks().orEmpty()
        else tracksOf(C.TRACK_TYPE_TEXT)

    private fun selectTrack(type: Int, id: String?) {
        val p = exo ?: return
        if (id == null) {
            // null = απενεργοποίηση (χρήσιμο για «Χωρίς υποτίτλους»).
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(type, true)
                .build()
            return
        }
        val groupId = id.substringBeforeLast(':')
        val index = id.substringAfterLast(':').toIntOrNull() ?: return
        val group = p.currentTracks.groups.firstOrNull {
            it.type == type && it.mediaTrackGroup.id == groupId
        } ?: return
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index))
            .build()
    }

    fun selectAudio(id: String) {
        if (backend == PlaybackBackend.VLC) { vlc?.selectAudio(id); return }
        selectTrack(C.TRACK_TYPE_AUDIO, id)
    }

    /** [id] = null απενεργοποιεί τους υποτίτλους. */
    fun selectSubtitle(id: String?) {
        if (id == EXTERNAL_SUBTITLE_TRACK_ID) return
        if (backend == PlaybackBackend.VLC) { vlc?.selectSubtitle(id); return }
        externalSubtitleCues = null
        externalSubtitleLabel = ""
        lastExternalCue = null
        _cues.value = emptyList()
        selectTrack(C.TRACK_TYPE_TEXT, id)
        publishTracks()
    }

    // ---- ΧΕΙΡΙΣΜΟΣ: ΙΔΙΑ ΣΗΜΑΣΙΑ, ΟΠΟΙΑ ΜΗΧΑΝΗ ΚΙ ΑΝ ΠΑΙΖΕΙ ----
    //
    // Κάθε μία δρομολογεί με μία γραμμή. Η εναλλακτική —να ρωτά ο καλών ποια
    // μηχανή τρέχει— θα σκόρπιζε τη γνώση σε έξι αρχεία διεπαφής, και ένα από
    // αυτά θα την ξεχνούσε.

    fun play() {
        if (backend == PlaybackBackend.VLC) vlc?.play() else exo?.play()
    }

    fun pause() {
        if (backend == PlaybackBackend.VLC) vlc?.pause() else exo?.pause()
    }

    fun togglePlay() {
        if (backend == PlaybackBackend.VLC) vlc?.togglePlay()
        else exo?.let { if (it.isPlaying) it.pause() else it.play() }
    }
    fun seekTo(positionMs: Long) {
        if (backend == PlaybackBackend.VLC) {
            // Το VlcBackend αγνοεί μόνο του τη μετακίνηση σε ροή που δεν
            // επιτρέπει seek (ζωντανά, RTMP, UDP) — δεν χρειάζεται έλεγχος εδώ.
            vlc?.seekTo(positionMs)
            return
        }
        val p = exo ?: return
        val duration = p.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        p.seekTo(positionMs.coerceIn(0L, duration))
        _state.value = _state.value.copy(positionMs = p.currentPosition.coerceAtLeast(0L))
        publishExternalCue(p.currentPosition.coerceAtLeast(0L))
    }

    /** Σχετική μετακίνηση (π.χ. ±10 δευτερόλεπτα). */
    fun seekBy(deltaMs: Long) = seekTo(currentPositionMs() + deltaMs)

    fun setSpeed(speed: Float) {
        if (backend == PlaybackBackend.VLC) vlc?.setSpeed(speed) else exo?.setPlaybackSpeed(speed)
    }

    /** Τρέχουσα θέση — για δείκτες προόδου που ρωτούν περιοδικά. */
    fun currentPositionMs(): Long =
        if (backend == PlaybackBackend.VLC) vlc?.currentPositionMs() ?: 0L
        else exo?.currentPosition?.coerceAtLeast(0L) ?: 0L

    fun durationMs(): Long =
        if (backend == PlaybackBackend.VLC) vlc?.durationMs() ?: 0L
        else exo?.duration?.takeIf { it > 0 } ?: 0L

    /** Σταματά και ελευθερώνει. Ο καλών είναι υπεύθυνος για τον κύκλο ζωής. */
    fun release() {
        stopProgressTicker()
        handler.removeCallbacks(stallWatchdog)
        handler.removeCallbacksAndMessages(null)
        stallRestarts = 0
        healthyTicks = 0
        lastProgressAt = 0L
        exo?.let { p ->
            p.removeListener(listener)
            p.stop()
            p.release()
        }
        exo = null
        // ΚΑΙ ΤΑ ΔΥΟ, ΠΑΝΤΑ. Ελευθερώνοντας μόνο το ενεργό backend, το άλλο μένει
        // ζωντανό με ανοιχτή σύνδεση και αποκωδικοποιητή — διαρροή που φαίνεται ως
        // «η μπαταρία πέφτει ενώ δεν παίζει τίποτα».
        vlc?.release()
        vlc = null
        externalSubtitleCues = null
        externalSubtitleLabel = ""
        lastExternalCue = null
        _state.value = State()
        _cues.value = emptyList()
    }

    private companion object {
        /** Αρκετά συχνά για ομαλή μπάρα, αρκετά αραιά για να μη φορτώνει τη CPU. */
        const val PROGRESS_INTERVAL_MS = 500L
        const val EXTERNAL_SUBTITLE_INTERVAL_MS = 200L
        const val EXTERNAL_SUBTITLE_TRACK_ID = "external:downloaded"

        /** Κάθε πότε ελέγχεται αν προχωρά η αναπαραγωγή. */
        const val WATCHDOG_INTERVAL_MS = 2_000L

        /**
         * Πόσο περιμένουμε το ExoPlayer να ΞΕΚΙΝΗΣΕΙ πριν δοκιμαστεί το LibVLC.
         *
         * Μικρότερο από το [STALL_TIMEOUT_MS], γιατί εδώ δεν μιλάμε για διακοπή σε
         * ροή που έπαιζε — μιλάμε για ροή που δεν άρχισε ποτέ. Εκεί η αναμονή δεν
         * πρόκειται να ανταμειφθεί.
         */
        const val EXO_START_TIMEOUT_MS = 7_000L

        /**
         * Πόση ακινησία θεωρείται κόλλημα.
         *
         * 12 δευτ.: αρκετά ώστε μια κανονική αναπλήρωση αποθέματος σε αργή γραμμή
         * να προλάβει να τελειώσει μόνη της, αρκετά λίγα ώστε να μην κάθεται
         * κανείς να κοιτάζει παγωμένη εικόνα.
         */
        const val STALL_TIMEOUT_MS = 12_000L

        /** Πόσες φορές ξαναπροσπαθούμε πριν το πούμε στον χρήστη. */
        const val MAX_STALL_RESTARTS = 5

        /** 15 × 2 δευτ. = 30 δευτ. ομαλής ροής σβήνουν το ιστορικό αποτυχιών. */
        const val HEALTHY_TICKS_TO_FORGIVE = 15
    }
}
