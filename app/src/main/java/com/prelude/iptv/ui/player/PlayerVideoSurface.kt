package com.prelude.iptv.ui.player

import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.prelude.iptv.player.PlaybackBackend
import com.prelude.iptv.player.PlaybackEngine

/**
 * Η επιφάνεια προβολής του κοινού player.
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΟ: ο ίδιος κώδικας ήταν γραμμένoς δύο φορές — στο [PlayerHost]
 * για την τηλεόραση και στο MobilePlaybackOverlay για το κινητό. Είναι ακριβώς
 * το μοτίβο που μας κόστισε επανειλημμένα: όταν διορθώναμε τη μία υλοποίηση, η
 * άλλη έμενε πίσω. Η αλλαγή σε SurfaceView, για παράδειγμα, χρειάστηκε να γίνει
 * δύο φορές· η επόμενη διόρθωση θα χρειαζόταν πάλι δύο.
 *
 * @param preferSmoothResize διάλεξε [TextureView] αντί για [SurfaceView].
 *   Χρειάζεται ΜΟΝΟ όπου η επιφάνεια αλλάζει μέγεθος με κίνηση (μεγέθυνση από
 *   προεπισκόπηση). Παντού αλλού το SurfaceView δίνει ακριβέστερο frame pacing —
 *   δες [PlaybackEngine.attachSurface].
 */
@Composable
fun PlayerVideoSurface(
    engine: PlaybackEngine,
    keepScreenOn: Boolean,
    modifier: Modifier = Modifier,
    preferSmoothResize: Boolean = false,
    frameCapture: PlayerVideoFrameCapture? = null,
    /**
     * Το τελευταίο καρέ της επιφάνειας τη στιγμή που φεύγει από τη σύνθεση.
     *
     * Χρησιμεύει για να καλυφθεί η εναλλαγή TextureView -> SurfaceView: χωρίς
     * αυτό, ανάμεσα στις δύο επιφάνειες μεσολαβεί ένα κενό καρέ, που στη μέση
     * μιας μεγέθυνσης φαίνεται σαν αναβόσβημα.
     *
     * Μόνο το [TextureView] μπορεί να το δώσει — το SurfaceView δεν διαβάζεται
     * έτσι. Γι' αυτό καλύπτεται η μεγέθυνση αλλά όχι η σμίκρυνση.
     */
    onLastFrame: ((android.graphics.Bitmap?) -> Unit)? = null,
) {
    // ---- ΠΟΙΑ ΜΗΧΑΝΗ ΖΩΓΡΑΦΙΖΕΙ ----
    //
    // Το μόνο σημείο της διεπαφής που το ρωτά, και μόνο επειδή δεν υπάρχει κοινή
    // επιφάνεια για τις δύο μηχανές. Χειριστήρια, μενού, πλήκτρα και focus δεν
    // αλλάζουν σε τίποτα — δες [PlayerVlcSurface].
    val renderer by engine.renderer.collectAsState()
    if (renderer == PlaybackBackend.VLC) {
        PlayerVlcSurface(engine = engine, keepScreenOn = keepScreenOn, modifier = modifier)
        return
    }

    if (preferSmoothResize) {
        // Κρατάμε αναφορά στη View ώστε να τη ρωτήσουμε ΤΗ ΣΤΙΓΜΗ που φεύγει.
        val textureRef = remember { arrayOfNulls<TextureView>(1) }
        val currentOnLastFrame = rememberUpdatedState(onLastFrame)
        DisposableEffect(engine) {
            onDispose {
                val view = textureRef[0]
                // Capture before detaching; after TextureView leaves the window its
                // SurfaceTexture may already be unavailable.
                currentOnLastFrame.value?.invoke(
                    runCatching { view?.takeIf { it.isAvailable }?.bitmap }.getOrNull()
                )
                view?.let { current ->
                    frameCapture?.detach(current)
                    engine.detachSurface(current)
                }
                textureRef[0] = null
            }
        }
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also {
                    // TextureView explicitly rejects background drawables on Android.
                    // setBackgroundColor() creates a ColorDrawable and crashes here
                    // with UnsupportedOperationException on affected devices. The
                    // Compose parent already paints the required black background.
                    textureRef[0] = it
                    frameCapture?.attach(it)
                }
            },
            update = { view ->
                engine.attachSurface(view)
                // ΚΡΑΤΑ ΤΗΝ ΟΘΟΝΗ ΑΝΟΙΧΤΗ όσο παίζει.
                //
                // Χωρίς αυτό, η τηλεόραση θεωρεί ότι δεν γίνεται τίποτα (δεν
                // υπάρχει είσοδος από το τηλεχειριστήριο) και μετά από λίγα λεπτά
                // μπαίνει σε προφύλαξη οθόνης πάνω στην ταινία. Δένεται στην
                // κατάσταση αναπαραγωγής, ώστε σε παύση να μη μένει η οθόνη
                // αναμμένη χωρίς λόγο.
                view.keepScreenOn = keepScreenOn
            },
            modifier = modifier
        )
    } else {
        val surfaceRef = remember { arrayOfNulls<SurfaceView>(1) }
        DisposableEffect(engine) {
            onDispose {
                surfaceRef[0]?.let { current ->
                    frameCapture?.detach(current)
                    engine.detachSurface(current)
                }
                surfaceRef[0] = null
            }
        }
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    // ΠΡΟΣΟΧΗ: Μην ορίζεις setBackgroundColor(Color.BLACK) εδώ.
                    // Το SurfaceView τρυπάει το παράθυρο για να δείξει το βίντεο
                    // από πίσω. Αν του βάλεις φόντο, το φόντο μπαίνει ΠΑΝΩ από
                    // το βίντεο και βλέπεις μόνο μαύρο.
                    surfaceRef[0] = this
                    frameCapture?.attach(this)
                }
            },
            update = { view ->
                engine.attachSurface(view)
                view.keepScreenOn = keepScreenOn
            },
            modifier = modifier
        )
    }
}
