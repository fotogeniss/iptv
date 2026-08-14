package com.prelude.iptv.ui.player

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
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
) {
    val layoutRef = remember { arrayOfNulls<VLCVideoLayout>(1) }
    // Η ΕΞΟΔΟΣ ΞΑΝΑΧΤΙΖΕΤΑΙ ΜΟΝΟ ΟΤΑΝ Η ΕΠΙΦΑΝΕΙΑ ΟΝΤΩΣ ΚΑΤΑΣΤΡΑΦΕΙ.
    //
    // Τρεις σκανδάλες δοκιμάστηκαν πριν από αυτή, και οι δύο πρώτες ήταν λάθος
    // ερώτημα:
    //
    // - «άλλαξε το μέγεθος» (1.75.0/1.81.0): ξαναχτίζει και όταν δεν χρειάζεται.
    //   Στην τηλεόραση η επιφάνεια ΔΕΝ καταστρέφεται σε αλλαγή μεγέθους, οπότε
    //   κάθε μετάβαση μικρής/μεγάλης οθόνης πλήρωνε άδικα έναν πλήρη restart.
    // - «η View ξαναμπήκε στο παράθυρο» (1.79.0): πολύ νωρίς. Η επιφάνεια του
    //   SurfaceView δημιουργείται αργότερα και ασύγχρονα, οπότε το `attachViews`
    //   δενόταν στο κενό — το logcat δεν έδειχνε ποτέ `vout display`.
    //
    // Το `SurfaceHolder` λέει ακριβώς το ζητούμενο: πότε χάθηκε η επιφάνεια και
    // πότε υπάρχει ξανά. Ξαναχτίζουμε στο `surfaceCreated`, και μόνο εφόσον έχει
    // προηγηθεί `surfaceDestroyed`. Έτσι η αλλαγή μεγέθους από μόνη της δεν
    // κοστίζει τίποτα. Δες `docs/PLAYER_SURFACE_DECISIONS.md`.
    val surfaceWasDestroyed = remember { booleanArrayOf(false) }
    DisposableEffect(engine) {
        onDispose {
            // Χωρίς αποσύνδεση, το LibVLC κρατά αναφορά σε View που έφυγε από τη
            // σύνθεση — και συνεχίζει να της στέλνει καρέ. Είναι διαρροή που
            // εμφανίζεται ως κόλλημα όταν αλλάζεις γρήγορα κανάλια.
            // ΜΟΝΟ ΤΗ ΔΙΚΗ ΤΗΣ επιφάνεια. Χωρίς τον έλεγχο ταυτότητας, η
            // πλήρης οθόνη που φεύγει ξήλωνε τη λωρίδα που μόλις προσαρτήθηκε.
            layoutRef[0]?.let { engine.detachVlcLayout(it) }
            layoutRef[0] = null
        }
    }

    AndroidView(
        factory = { context ->
            VLCVideoLayout(context).also { layout ->
                layoutRef[0] = layout
                layout.surfaceViews().forEach { surfaceView ->
                    surfaceView.holder.addCallback(
                        object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                if (!surfaceWasDestroyed[0]) return
                                surfaceWasDestroyed[0] = false
                                engine.reattachVlcLayout(layout)
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int,
                            ) = Unit

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                surfaceWasDestroyed[0] = true
                            }
                        }
                    )
                }
            }
        },
        update = { layout ->
            engine.attachVlcLayout(layout)
            // Ίδιος λόγος με το SurfaceView: χωρίς αυτό η τηλεόραση μπαίνει σε
            // προφύλαξη οθόνης πάνω στην ταινία, επειδή δεν βλέπει είσοδο από το
            // τηλεχειριστήριο.
            layout.keepScreenOn = keepScreenOn
        },
        modifier = modifier
    )
}

/**
 * Τα [SurfaceView] που κρύβει μέσα του ένα `VLCVideoLayout`.
 *
 * Το layout στήνεται από τη VideoLAN και τα παιδιά του δεν είναι δικό μας
 * συμβόλαιο, οπότε ψάχνονται με αναζήτηση τύπου αντί για id. Είναι δύο, εικόνα
 * και υπότιτλοι· ο κύκλος ζωής τους είναι κοινός, άρα η παρακολούθηση και των
 * δύο δεν αλλάζει το αποτέλεσμα.
 */
private fun View.surfaceViews(): List<SurfaceView> = when (this) {
    is SurfaceView -> listOf(this)
    is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).surfaceViews() }
    else -> emptyList()
}
