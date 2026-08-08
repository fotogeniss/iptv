package com.prelude.iptv.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.player.AutoFrameRateMode
import com.prelude.iptv.player.DisplayFrameRateController

/**
 * Ζητά από την τηλεόραση να κλειδώσει στη συχνότητα του περιεχομένου.
 *
 * ΓΙΑΤΙ ΥΠΑΡΧΕΙ: μια ζωντανή μετάδοση ποδοσφαίρου στην Ευρώπη είναι 50 καρέ. Η
 * τηλεόραση, όσο δεν της ζητήσει κανείς αλλιώς, δείχνει στα 60 Hz. Το 50 δεν
 * διαιρεί το 60: κάθε πέμπτο καρέ μένει διπλή ώρα στην οθόνη. Το βίντεο δεν
 * χάνει τίποτα και δεν κολλάει — απλώς η κίνηση προχωρά άνισα. Σε στατική σκηνή
 * δεν φαίνεται· σε παίκτη που τρέχει οριζόντια είναι αυτό που περιγράφεται ως
 * «ρομποτικό».
 *
 * Η υποδομή υπήρχε ολόκληρη ([DisplayFrameRateController], ρύθμιση στις
 * επιλογές) αλλά ο νέος player δεν την καλούσε ποτέ — έμεινε πίσω στο
 * `PlayerActivity` κατά τη μετάβαση.
 *
 * ΣΗΜΕΙΩΣΗ: η αντίστοιχη ρύθμιση του ExoPlayer δεν γίνεται εδώ. Είναι κλειδωμένη
 * στον constructor του player, οπότε η αλλαγή της θα απαιτούσε release() —
 * δηλαδή διακοπή της ροής που ήδη παίζει. Η μηχανή τη διαβάζει μόνη της τη
 * στιγμή που χτίζεται.
 *
 * @param active ζητάμε συχνότητα ΜΟΝΟ σε πλήρη οθόνη: σε μικρή προεπισκόπηση
 *   δίπλα σε λίστες, μια αλλαγή λειτουργίας HDMI θα μαύριζε όλη την οθόνη κάθε
 *   φορά που ο χρήστης μετακινείται σε άλλο κανάλι.
 * @param contentFrameRate 0 όσο δεν είναι γνωστός — τότε δεν ζητάμε τίποτα.
 */
@Composable
fun PlayerFrameRateSync(active: Boolean, contentFrameRate: Float) {
    val activity = LocalContext.current.findActivity() ?: return
    val controller = remember(activity) { DisplayFrameRateController(activity) }
    val mode = remember(activity) {
        AutoFrameRateMode.fromStorage(PlaylistStore(activity).autoFrameRateMode)
    }

    LaunchedEffect(active, contentFrameRate, mode) {
        if (active && contentFrameRate > 0f) controller.request(mode, contentFrameRate)
        else controller.suspendForBackground()
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
}

/**
 * Το [Activity] πίσω από ένα Compose context.
 *
 * Ο συγχρονισμός συχνότητας ρυθμίζεται στο ΠΑΡΑΘΥΡΟ, όχι στην επιφάνεια βίντεο —
 * οπότε χρειάζεται το Activity. Το LocalContext μέσα σε Compose είναι συχνά
 * τυλιγμένο σε ContextWrapper, γι' αυτό ξετυλίγουμε αντί για απλό cast που θα
 * επέστρεφε σιωπηλά null.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
