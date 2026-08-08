package com.prelude.iptv.ui.player

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import android.graphics.Typeface
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.player.PlaybackEngine

/**
 * Οι υπότιτλοι του κοινού player.
 *
 * ΓΙΑΤΙ ΞΕΧΩΡΙΣΤΟ: το TextureView και το SurfaceView δείχνουν μόνο εικόνα. Ο
 * έτοιμος PlayerView του Media3 κρύβει από πάνω ένα [SubtitleView] και το ταΐζει
 * μόνος του — αλλά εμείς δεν τον χρησιμοποιούμε, γιατί χρειαζόμασταν έλεγχο της
 * επιφάνειας για τη μεγέθυνση και το frame pacing.
 *
 * Το κόστος αυτής της επιλογής ήταν κρυφό: το μενού «Υπότιτλοι» δούλευε, ο
 * player αποκωδικοποιούσε το κομμάτι, και δεν φαινόταν τίποτα — δεν υπήρχε
 * πουθενά να ζωγραφιστούν. Εδώ ξαναμπαίνει το κομμάτι που έλειπε, μία φορά για
 * όλες τις διαδρομές.
 */
// AndroidX OptIn: το @UnstableApi του Media3 φέρει androidx.annotation.RequiresOptIn,
// οπότε το kotlin.OptIn δεν έχει καμία ισχύ πάνω του.
@OptIn(markerClass = [UnstableApi::class])
@Suppress("unused") // Το PlaylistStore αναφέρεται μόνο σε τεκμηρίωση παραμέτρου.
@Composable
fun PlayerSubtitles(
    engine: PlaybackEngine,
    /** 70–180. Αλλάζει μέσα στην αναπαραγωγή, από τα χειριστήρια του player. */
    sizePercent: Int = 100,
    /** "none" | "shadow" | "box" — δες [PlaylistStore.subtitleBackground]. */
    background: String = "shadow",
    /** Applies equally to embedded tracks and downloaded external captions. */
    bold: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val cues by engine.cues.collectAsState()
    AndroidView(
        factory = { ctx -> SubtitleView(ctx).apply { setApplyEmbeddedStyles(false) } },
        // ΣΤΟ update ΚΑΙ ΟΧΙ ΣΤΟ factory.
        //
        // Το factory τρέχει μία φορά· ό,τι μπει εκεί παγώνει. Το μέγεθος και το
        // φόντο αλλάζουν ΤΩΡΑ, από τα χειριστήρια, και ο χρήστης πρέπει να δει το
        // αποτέλεσμα ενώ διαλέγει — αλλιώς διαλέγει στα τυφλά.
        update = { view ->
            view.setCues(cues)
            view.setStyle(captionStyle(background, bold))
            view.setFractionalTextSize(
                SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * (sizePercent / 100f)
            )
        },
        modifier = modifier
    )
}

/**
 * Λευκά γράμματα πάντα· αλλάζει μόνο το πώς ξεχωρίζουν από την εικόνα.
 *
 * - «Σκιά»: διαβάζεται σε φωτεινή ΚΑΙ σκοτεινή εικόνα χωρίς να κρύβει κάδρο.
 * - «Πλαίσιο»: καθαρότερο σε πολύ φωτεινές σκηνές, με κόστος ορατότητας εικόνας.
 * - «Χωρίς»: το πιο διακριτικό, αλλά χάνεται πάνω σε ανοιχτό φόντο.
 */
@OptIn(markerClass = [UnstableApi::class])
private fun captionStyle(background: String, bold: Boolean): CaptionStyleCompat {
    val transparent = android.graphics.Color.TRANSPARENT
    val boxed = background == "box"
    return CaptionStyleCompat(
        android.graphics.Color.WHITE,
        if (boxed) android.graphics.Color.argb(190, 0, 0, 0) else transparent,
        transparent,
        when (background) {
            "shadow" -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
            else -> CaptionStyleCompat.EDGE_TYPE_NONE
        },
        android.graphics.Color.BLACK,
        if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT,
    )
}
