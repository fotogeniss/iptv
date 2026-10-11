package com.prelude.iptv.ui.player

import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.prelude.iptv.player.PlaybackEngine
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Η επιφάνεια προβολής όταν παίζει το LibVLC.
 *
 * ΓΙΑΤΙ ΧΡΕΙΑΖΕΤΑΙ ΞΕΧΩΡΙΣΤΗ: δεν υπάρχει κοινή επιφάνεια για τις δύο μηχανές. Το
 * ExoPlayer θέλει `SurfaceView` ή `TextureView` και τα οδηγεί το ίδιο· το LibVLC
 * θέλει το δικό του `VLCVideoLayout`, το οποίο **στήνει και μετρά μόνο του** την
 * επιφάνεια για να εφαρμόσει την αναλογία εικόνας.
 *
 * Δίνοντάς του γυμνό `SurfaceView` θα έπρεπε να υπολογίζουμε εμείς την αναλογία —
 * δηλαδή να ξαναγράψουμε κώδικα που υπάρχει ήδη και είναι δοκιμασμένος από τη
 * VideoLAN σε χιλιάδες συσκευές, συμπεριλαμβανομένων των ανισομερών pixel του
 * MPEG-TS.
 *
 * ΔΕΝ ΕΙΝΑΙ ΔΕΥΤΕΡΟΣ PLAYER: δεν έχει χειριστήρια, δεν πιάνει αγγίγματα, δεν ξέρει
 * πλήκτρα. Είναι ένα κουτί που δείχνει εικόνα, στην ίδια θέση όπου το
 * [PlayerVideoSurface] βάζει το `SurfaceView`. Ό,τι ζωγραφίζεται από πάνω —
 * χειριστήρια, υπότιτλοι, μενού — είναι ακριβώς το ίδιο.
 */
@Composable
internal fun PlayerVlcSurface(
    engine: PlaybackEngine,
    keepScreenOn: Boolean,
    modifier: Modifier = Modifier,
    /**
     * Λαβή για να τραβηχτεί ΕΝΑ καρέ κατά τη μετάβαση, χωρίς δεύτερη επιφάνεια.
     *
     * Το μονοπάτι του libVLC δεν την είχε ποτέ: το [PlayerVideoSurface] έβγαινε
     * με `return` πριν φτάσει εκεί. Αποτέλεσμα, στα ζωντανά δεν υπήρχε παγωμένο
     * καρέ να καλύψει τη μεγέθυνση, και φαινόταν η ωμή αλλαγή μεγέθους της
     * επιφάνειας — η εικόνα πήδαγε σε σκαλοπάτια.
     */
    frameCapture: PlayerVideoFrameCapture? = null,
) {
    val layoutRef = remember { arrayOfNulls<VLCVideoLayout>(1) }
    val captureRef = remember { arrayOfNulls<SurfaceView>(1) }

    DisposableEffect(engine) {
        onDispose {
            captureRef[0]?.let { surface -> frameCapture?.detach(surface) }
            captureRef[0] = null
            // Χωρίς αποσύνδεση, το LibVLC κρατά αναφορά σε View που έφυγε από τη
            // σύνθεση — και συνεχίζει να της στέλνει καρέ. Είναι διαρροή που
            // εμφανίζεται ως κόλλημα όταν αλλάζεις γρήγορα κανάλια.
            engine.detachVlcLayout()
            layoutRef[0] = null
        }
    }

    AndroidView(
        factory = { context ->
            VLCVideoLayout(context).also { layoutRef[0] = it }
        },
        update = { layout ->
            engine.attachVlcLayout(layout)
            // ΤΟ ΠΑΙΔΙ, ΟΧΙ ΤΟ ΔΟΧΕΙΟ: το `PixelCopy` θέλει `SurfaceView`, και το
            // `VLCVideoLayout` είναι απλώς ο γονιός του. Το πρώτο `SurfaceView`
            // του είναι το βίντεο· το δεύτερο, όταν υπάρχει, είναι οι υπότιτλοι.
            //
            // ΕΞΑΡΤΗΣΗ ΑΠΟ ΕΣΩΤΕΡΙΚΗ ΔΟΜΗ, ΜΕ ΔΙΚΤΥ: αν μελλοντική έκδοση του
            // libVLC αλλάξει την ιεραρχία, το `null` απλώς σημαίνει ότι δεν
            // υπάρχει καρέ για τη μετάβαση. Χάνεται η ομαλότητα, όχι η εικόνα.
            if (captureRef[0] == null) {
                layout.videoSurfaceView()?.let { surface ->
                    captureRef[0] = surface
                    frameCapture?.attach(surface)
                }
            }
            // Ίδιος λόγος με το SurfaceView: χωρίς αυτό η τηλεόραση μπαίνει σε
            // προφύλαξη οθόνης πάνω στην ταινία, επειδή δεν βλέπει είσοδο από το
            // τηλεχειριστήριο.
            layout.keepScreenOn = keepScreenOn
        },
        modifier = modifier
    )
}

private fun VLCVideoLayout.videoSurfaceView(): SurfaceView? {
    for (index in 0 until childCount) {
        val child = getChildAt(index)
        if (child is SurfaceView) return child
    }
    return null
}
