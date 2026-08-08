package com.prelude.iptv

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

/** Lightweight cold-start branding. MainActivity remains the production app shell. */
class StartupActivity : AppCompatActivity() {
    private var launchedMain = false
    private var introVideo: VideoView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val introTimeout = Runnable { openMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // INTRO_ENABLED = false: η εφαρμογή ανοίγει κατευθείαν, χωρίς εισαγωγικό
        // βίντεο. Ο κώδικας του intro μένει άθικτο ώστε να επανέρχεται αλλάζοντας
        // μόνο τη σημαία.
        if (!INTRO_ENABLED || shouldSkipIntro()) {
            openMain()
            return
        }

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.lumina_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 1f
        }
        val video = VideoView(this).apply {
            setVideoURI(Uri.parse("android.resource://$packageName/${R.raw.lumina_intro}"))
            setOnPreparedListener { player ->
                player.isLooping = false
                logo.visibility = View.GONE
                if (hasWindowFocus()) start()
            }
            setOnCompletionListener { openMain() }
            setOnErrorListener { _, _, _ -> openMain(); true }
        }
        introVideo = video
        root.addView(video, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        root.addView(logo, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        setContentView(root)

        // Preparation starts when the Activity becomes interactive. The timeout is
        // armed only while resumed, so Home/background can never launch MainActivity.
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && (
                event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    event.keyCode == KeyEvent.KEYCODE_ENTER ||
                    event.keyCode == KeyEvent.KEYCODE_BACK
                )) {
            openMain()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        if (launchedMain) return
        handler.removeCallbacks(introTimeout)
        handler.postDelayed(introTimeout, INTRO_FAILSAFE_MS)
        introVideo?.start()
    }

    override fun onPause() {
        // Never let a delayed splash callback launch the app from the background.
        handler.removeCallbacks(introTimeout)
        introVideo?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(introTimeout)
        introVideo?.setOnPreparedListener(null)
        introVideo?.setOnCompletionListener(null)
        introVideo?.setOnErrorListener(null)
        introVideo?.stopPlayback()
        introVideo = null
        super.onDestroy()
    }

    private fun shouldSkipIntro(): Boolean {
        val last = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_LAST_INTRO, 0L)
        val now = SystemClock.elapsedRealtime()
        return last > 0L && now >= last && now - last < WARM_START_WINDOW_MS
    }

    private fun openMain() {
        if (launchedMain || isFinishing || isDestroyed) return
        launchedMain = true
        handler.removeCallbacks(introTimeout)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putLong(KEY_LAST_INTRO, SystemClock.elapsedRealtime()).apply()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        /**
         * Το ΠΑΛΙΟ εισαγωγικό βίντεο. Μένει κλειστό — και δεν πρέπει να ανοίξει.
         *
         * Η εισαγωγή έγινε [com.prelude.iptv.ui.splash.PreludeSplash], επίστρωση
         * μέσα στο MainActivity. Ο λόγος είναι ακριβώς αυτό που δεν μπορούσε να
         * κάνει αυτή εδώ η οθόνη: να ξέρει πόσο μένει. Το βίντεο έπαιζε σταθερό
         * χρόνο με χρονόμετρο ασφαλείας 12 δευτερολέπτων, δηλαδή είτε περίμενες
         * τζάμπα είτε έβλεπες άδεια εφαρμογή.
         *
         * Ανοίγοντάς το ξανά, ο χρήστης θα δει ΔΥΟ εισαγωγές στη σειρά.
         */
        private const val INTRO_ENABLED = false
        private const val PREFS = "startup_branding"
        private const val KEY_LAST_INTRO = "last_intro_elapsed_ms"
        private const val WARM_START_WINDOW_MS = 15 * 60 * 1000L
        private const val INTRO_FAILSAFE_MS = 12_000L
    }
}
