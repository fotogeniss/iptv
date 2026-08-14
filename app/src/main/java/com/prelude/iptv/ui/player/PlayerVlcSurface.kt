package com.prelude.iptv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
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
    // Η ΕΞΟΔΟΣ ΞΑΝΑΧΤΙΖΕΤΑΙ ΟΤΑΝ ΑΛΛΑΞΕΙ ΟΥΣΙΑΣΤΙΚΑ ΤΟ ΜΕΓΕΘΟΣ.
    //
    // Το μάζεμα μετακομίζει την επιφάνεια σε άλλον γονέα, το σύστημα καταστρέφει
    // την επιφάνεια του SurfaceView στο ενδιάμεσο, και το LibVLC δεν ξαναχτίζει
    // την έξοδό του μόνο του — η ταυτότητα του layout δεν αλλάζει, οπότε το
    // `attachLayout` επιστρέφει αμέσως. Πρέπει να του το πούμε εμείς.
    //
    // ΓΙΑΤΙ ΜΕ ΤΟ ΜΕΓΕΘΟΣ ΚΑΙ ΟΧΙ ΜΕ ΤΟ `onViewAttachedToWindow`: το δεύτερο
    // δοκιμάστηκε στο 1.79.0 και απέτυχε, με απόδειξη από logcat. Πυροδοτείται
    // όταν μπαίνει η VIEW στο παράθυρο, αλλά η πραγματική επιφάνεια του
    // SurfaceView δημιουργείται αργότερα και ασύγχρονα· το `attachViews`
    // δενόταν στο κενό και δεν εμφανιζόταν ποτέ γραμμή `vout display` ή
    // `decoder: output` στο log. Η αλλαγή μεγέθους έρχεται ΜΕΤΑ το layout, όταν
    // η επιφάνεια υπάρχει, και είναι η μόνη σκανδάλη που έχει επιβεβαιωθεί στη
    // συσκευή ότι επαναφέρει την εικόνα. Δες `docs/PLAYER_SURFACE_DECISIONS.md`.
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    val reattachedFor = remember { intArrayOf(0, 0) }
    LaunchedEffect(surfaceSize, engine) {
        val layout = layoutRef[0] ?: return@LaunchedEffect
        if (surfaceSize.width <= 0 || surfaceSize.height <= 0) return@LaunchedEffect
        val previousWidth = reattachedFor[0]
        val previousHeight = reattachedFor[1]
        val materiallyDifferent = previousWidth == 0 || previousHeight == 0 ||
            kotlin.math.abs(surfaceSize.width - previousWidth) * 5 > previousWidth ||
            kotlin.math.abs(surfaceSize.height - previousHeight) * 5 > previousHeight
        if (!materiallyDifferent) return@LaunchedEffect
        reattachedFor[0] = surfaceSize.width
        reattachedFor[1] = surfaceSize.height
        engine.reattachVlcLayout(layout)
    }
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
            VLCVideoLayout(context).also { layoutRef[0] = it }
        },
        update = { layout ->
            engine.attachVlcLayout(layout)
            // Ίδιος λόγος με το SurfaceView: χωρίς αυτό η τηλεόραση μπαίνει σε
            // προφύλαξη οθόνης πάνω στην ταινία, επειδή δεν βλέπει είσοδο από το
            // τηλεχειριστήριο.
            layout.keepScreenOn = keepScreenOn
        },
        modifier = modifier.onSizeChanged { surfaceSize = it }
    )
}
