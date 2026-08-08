package com.prelude.iptv.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Πλήρης οθόνη σε κινητό: **γυρίζει** τη συσκευή και κρύβει τις μπάρες.
 *
 * ΤΙ ΔΕΝ ΕΦΤΑΝΕ: το να κάνει η εικόνα `fillMaxSize()`. Σε κατακόρυφη οθόνη ένα
 * βίντεο 16:9 μέσα σε κουτί 9:16 απλώς αποκτά μαύρες λωρίδες πάνω και κάτω — ίδιο
 * μέγεθος εικόνας, περισσότερο μαύρο. Αυτό ακριβώς περιέγραψε ο χρήστης.
 *
 * Πλήρης οθόνη σε ταινία σημαίνει **οριζόντιος προσανατολισμός**. Χωρίς αυτό, το
 * κουμπί μοιάζει χαλασμένο.
 *
 * ΓΙΑΤΙ `SENSOR_LANDSCAPE` ΚΑΙ ΟΧΙ `LANDSCAPE`: επιτρέπει και τις δύο οριζόντιες
 * φορές, ώστε ο χρήστης να κρατά το τηλέφωνο όπως θέλει. Το σκέτο `LANDSCAPE`
 * κλειδώνει μία φορά και όποιος το κρατά ανάποδα βλέπει ανάποδη εικόνα.
 *
 * ΤΟ `DisposableEffect` ΕΙΝΑΙ ΤΟ ΚΡΙΣΙΜΟ: αν ο προσανατολισμός δεν επιστραφεί, η
 * εφαρμογή μένει κλειδωμένη οριζόντια αφού κλείσει ο player — και ο χρήστης
 * νομίζει ότι κόλλησε.
 */
@Composable
internal fun FullscreenEffect(active: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    DisposableEffect(active) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        if (active) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Σύρσιμο από την άκρη τις επαναφέρει προσωρινά. Η εναλλακτική
            // (`BEHAVIOR_SHOW_BARS_BY_TOUCH`) τις κρατά μόνιμα μόλις αγγίξεις
            // οθόνη — δηλαδή σε κάθε εμφάνιση χειριστηρίων.
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            // Εκτελείται και όταν ο player κλείνει ΜΕΣΑ σε πλήρη οθόνη, γιατί
            // τότε το effect φεύγει μαζί του. Αυτό είναι το σημείο που κάνει την
            // επαναφορά αξιόπιστη — ένα σκέτο `else` δεν θα έτρεχε ποτέ.
            if (active) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.setDecorFitsSystemWindows(window, true)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

/**
 * Καταπίνει ό,τι αγγίγματα δεν χρησιμοποίησαν τα παιδιά.
 *
 * ΤΟ ΠΡΟΒΛΗΜΑ: το `Modifier.background(Color.Black)` **ζωγραφίζει** μαύρο αλλά δεν
 * πιάνει αγγίγματα. Ο player του κινητού είναι επίστρωση πάνω από τη λίστα, οπότε
 * κάθε πάτημα στη μαύρη περιοχή κάτω από τον τίτλο περνούσε από πάνω του και
 * ενεργοποιούσε κάρτες της **προηγούμενης οθόνης** — αόρατες, αλλά ζωντανές.
 *
 * Στο Compose τα παιδιά παίρνουν τα γεγονότα πρώτα, οπότε αυτό μπαίνει στη ρίζα
 * χωρίς να χαλάει κουμπιά ή χειρονομίες: καταναλώνει μόνο ό,τι έμεινε.
 */
internal fun Modifier.consumeAllTouches(): Modifier = this
    // Καταπίνει μόνο taps. Τα drags ανήκουν στο verticalScroll του mobile player,
    // ώστε τα επεισόδια και οι προτάσεις κάτω από την εικόνα να σκρολάρουν.
    .pointerInput(Unit) { detectTapGestures { } }
