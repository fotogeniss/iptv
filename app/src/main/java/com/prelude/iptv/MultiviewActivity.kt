package com.prelude.iptv

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import com.prelude.iptv.data.MultiviewLaunchStore
import com.prelude.iptv.net.Http
import com.prelude.iptv.player.MultiviewPolicy
import com.prelude.iptv.player.PlaybackStabilityPolicy
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

@OptIn(markerClass = [UnstableApi::class])
class MultiviewActivity : ComponentActivity() {
    companion object {
        const val EXTRA_LAUNCH_TOKEN = "multiview_launch_token"
        private const val MAX_PANE_RESTARTS = 5
        private const val STALL_TIMEOUT_MS = 12_000L
        private const val WATCHDOG_INTERVAL_MS = 2_000L
    }

    private val players = arrayOfNulls<ExoPlayer>(2)
    private val views = arrayOfNulls<PlayerView>(2)
    private val labels = arrayOfNulls<TextView>(2)
    private val scrims = arrayOfNulls<View>(2)
    private val restartAttempts = IntArray(2)
    private val generations = IntArray(2)
    private val lastPositions = LongArray(2) { -1L }
    private val lastProgressAt = LongArray(2) { SystemClock.elapsedRealtime() }
    private val pendingRestarts = arrayOfNulls<Runnable>(2)
    private val stableResetCallbacks = arrayOfNulls<Runnable>(2)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var streams: Array<MultiviewLaunchStore.Stream>
    private var activePane = 0
    private var stopped = false

    private val watchdog = object : Runnable {
        override fun run() {
            if (stopped || isFinishing || isDestroyed) return
            val now = SystemClock.elapsedRealtime()
            players.forEachIndexed { index, player ->
                player ?: return@forEachIndexed
                if (!player.playWhenReady) return@forEachIndexed

                val position = player.currentPosition
                if (player.isPlaying && position != lastPositions[index]) {
                    lastPositions[index] = position
                    lastProgressAt[index] = now
                } else {
                    val stalled = now - lastProgressAt[index] >= STALL_TIMEOUT_MS &&
                        (player.playbackState == Player.STATE_BUFFERING ||
                            player.playbackState == Player.STATE_IDLE ||
                            (player.playbackState == Player.STATE_READY && !player.isPlaying))
                    if (stalled) {
                        Log.w("Multiview", "Pane ${index + 1} stalled in state ${player.playbackState}")
                        schedulePaneRestart(index, "Επανασύνδεση…")
                    }
                }
            }
            handler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launch = MultiviewLaunchStore.consume(intent.getStringExtra(EXTRA_LAUNCH_TOKEN))
        if (launch == null) {
            finish()
            return
        }
        streams = arrayOf(launch.primary, launch.secondary)

        enterImmersiveMode()
        setContentView(R.layout.activity_multiview)
        views[0] = findViewById(R.id.multiview_left_player)
        views[1] = findViewById(R.id.multiview_right_player)
        labels[0] = findViewById(R.id.multiview_left_label)
        labels[1] = findViewById(R.id.multiview_right_label)
        scrims[0] = findViewById(R.id.multiview_left_scrim)
        scrims[1] = findViewById(R.id.multiview_right_scrim)

        streams.forEachIndexed { index, stream ->
            views[index]?.contentDescription = stream.title
            labels[index]?.text = stream.title
            createPlayer(index)
        }
        setActivePane(0)
    }

    override fun onStart() {
        super.onStart()
        stopped = false
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, WATCHDOG_INTERVAL_MS)
    }

    private fun createPlayer(index: Int) {
        if (stopped || isFinishing || isDestroyed) return
        releasePlayer(index)
        val stream = streams[index]
        val generation = ++generations[index]
        lastPositions[index] = -1L
        lastProgressAt[index] = SystemClock.elapsedRealtime()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Http.DESKTOP_UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        val renderers = NextRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val player = ExoPlayer.Builder(this, renderers)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(httpFactory)
                    .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(6))
            )
            .build()

        players[index] = player
        views[index]?.player = player
        player.setAudioAttributes(AudioAttributes.DEFAULT, false)
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (generation != generations[index] || player !== players[index]) return
                Log.w("Multiview", "Pane ${index + 1} playback error: ${error.errorCodeName}", error)
                val message = if (PlaybackStabilityPolicy.hasIoCause(error)) {
                    "Προσωρινό σφάλμα — επανασύνδεση…"
                } else {
                    "Σφάλμα stream — επανασύνδεση…"
                }
                schedulePaneRestart(index, message)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (generation != generations[index] || player !== players[index]) return
                when (playbackState) {
                    Player.STATE_READY -> {
                        lastProgressAt[index] = SystemClock.elapsedRealtime()
                        labels[index]?.text = stream.title
                    }
                    Player.STATE_ENDED -> schedulePaneRestart(index, "Το stream έκλεισε — επανασύνδεση…")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (generation != generations[index] || player !== players[index]) return
                if (isPlaying) {
                    lastPositions[index] = player.currentPosition
                    lastProgressAt[index] = SystemClock.elapsedRealtime()
                    scheduleStableRetryReset(index, generation)
                }
            }
        })
        player.setMediaItem(MediaItem.fromUri(stream.url))
        applyAudioIsolation(index, player)
        player.playWhenReady = true
        player.prepare()
    }

    private fun scheduleStableRetryReset(index: Int, generation: Int) {
        stableResetCallbacks[index]?.let(handler::removeCallbacks)
        val callback = Runnable {
            if (!stopped && generation == generations[index] && players[index]?.isPlaying == true) {
                restartAttempts[index] = 0
            }
        }
        stableResetCallbacks[index] = callback
        handler.postDelayed(callback, 30_000L)
    }

    private fun schedulePaneRestart(index: Int, status: String) {
        if (stopped || isFinishing || isDestroyed || pendingRestarts[index] != null) return
        if (restartAttempts[index] >= MAX_PANE_RESTARTS) {
            // ΔΙΑΓΝΩΣΗ: αν το ΑΛΛΟ παράθυρο παίζει κανονικά ενώ αυτό απέτυχε
            // επανειλημμένα, η υπογραφή δείχνει όριο ταυτόχρονων συνδέσεων του
            // παρόχου — όχι χαλασμένο κανάλι. Οι περισσότερες συνδρομές IPTV
            // επιτρέπουν ΜΙΑ ροή ανά λογαριασμό, οπότε το δεύτερο stream κόβεται.
            //
            // Χωρίς αυτή τη διάκριση, ο χρήστης έβλεπε «μη διαθέσιμο» σε κανάλι
            // που παίζει μια χαρά μόνο του, και το έψαχνε σαν σφάλμα της εφαρμογής.
            val otherIsPlaying = players.getOrNull(1 - index)?.isPlaying == true
            labels[index]?.text = if (otherIsPlaying) {
                "${streams[index].title} · μη διαθέσιμο — ο πάροχος μάλλον επιτρέπει μία ροή"
            } else {
                "${streams[index].title} · μη διαθέσιμο"
            }
            return
        }
        labels[index]?.text = "${streams[index].title} · $status"
        val attempt = restartAttempts[index]++
        val delayMs = when (attempt) {
            0 -> 500L
            1 -> 1_200L
            2 -> 2_500L
            else -> 5_000L
        }
        val restart = Runnable {
            pendingRestarts[index] = null
            if (!stopped && !isFinishing && !isDestroyed) createPlayer(index)
        }
        pendingRestarts[index] = restart
        handler.postDelayed(restart, delayMs)
    }

    private fun setActivePane(index: Int) {
        activePane = index.coerceIn(0, 1)
        players.forEachIndexed { pane, player ->
            player ?: return@forEachIndexed
            applyAudioIsolation(pane, player)
            // Σκίαση με ξεχωριστό View αντί για alpha στο PlayerView: με
            // SurfaceView η επιφάνεια συντίθεται ξεχωριστά από το παράθυρο και
            // δεν υπακούει αξιόπιστα στη διαφάνεια της View.
            scrims[pane]?.visibility = if (pane == activePane) View.GONE else View.VISIBLE
            views[pane]?.isSelected = pane == activePane
        }
        views[activePane]?.requestFocus()
    }

    private fun applyAudioIsolation(index: Int, player: ExoPlayer) {
        val active = index == activePane
        player.volume = if (active) 1f else 0f
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, !active)
            .build()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) {
            return super.dispatchKeyEvent(event)
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                setActivePane(MultiviewPolicy.nextPane(activePane, event.keyCode))
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                setActivePane(activePane)
                true
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                finish()
                true
            }
            else -> super.dispatchKeyEvent(event)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onStop() {
        stopped = true
        handler.removeCallbacks(watchdog)
        releasePlayers()
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        releasePlayers()
        super.onDestroy()
    }

    private fun releasePlayer(index: Int) {
        pendingRestarts[index]?.let(handler::removeCallbacks)
        pendingRestarts[index] = null
        stableResetCallbacks[index]?.let(handler::removeCallbacks)
        stableResetCallbacks[index] = null
        views[index]?.player = null
        players[index]?.release()
        players[index] = null
    }

    private fun releasePlayers() {
        players.indices.forEach(::releasePlayer)
    }

    @Suppress("DEPRECATION")
    private fun enterImmersiveMode() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}
