package com.prelude.iptv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.PlaybackQueue
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.player.PlayerLaunchRequest
import com.prelude.iptv.ui.IptvTheme
import com.prelude.iptv.ui.player.PlayerExtraAction
import com.prelude.iptv.ui.player.PlayerEpgDialog
import com.prelude.iptv.ui.player.PlayerProgramme
import com.prelude.iptv.ui.player.SubtitleWiring
import com.prelude.iptv.ui.player.TvPlaybackOverlay
import com.prelude.iptv.ui.player.upcomingProgrammes

/**
 * Ο player για ό,τι έρχεται από ΕΞΩ με Intent.
 *
 * ΤΙ ΗΤΑΝ ΠΡΙΝ: 3.496 γραμμές. Ένας δεύτερος, πλήρης player γραμμένος σε Android
 * Views — δικό του ExoPlayer, δική του εφεδρεία VLC, δικές του χειρονομίες, δικά
 * του μενού ήχου και υποτίτλων, δική του λίστα καναλιών, δικός του υπολογισμός
 * αναλογίας, δικό του ρολόι απόκρυψης χειριστηρίων.
 *
 * ΓΙΑΤΙ ΕΦΥΓΕ: κάθε διόρθωση έπρεπε να γίνει δύο φορές. Ο συγχρονισμός χειλιών,
 * το judder, ο φύλακας κολλήματος, οι γλώσσες κομματιών, το πάνελ υποτίτλων —
 * όλα μπήκαν στη νέα μηχανή, και εδώ έμεναν άφταστα. Ο χρήστης έβλεπε
 * διαφορετικό player ανάλογα με το ΑΠΟ ΠΟΥ ξεκίνησε το βίντεο, και ό,τι φτιαχνόταν
 * στον έναν έλειπε από τον άλλον. Δεν είναι πρόβλημα μεγέθους· είναι δύο
 * υλοποιήσεις της ίδιας ιδέας.
 *
 * ΤΙ ΜΕΝΕΙ: ένα Activity, γιατί ένα Intent χρειάζεται Activity — δεν μπορεί να
 * προσγειωθεί σε Composable. Μέσα του τρέχει το ΙΔΙΟ [TvPlaybackOverlay] που
 * χρησιμοποιεί όλη η εφαρμογή.
 *
 * ΤΟ ΟΝΟΜΑ ΤΗΣ ΚΛΑΣΗΣ ΕΜΕΙΝΕ ΙΔΙΟ επίτηδες: το manifest, το
 * [PlayerLaunchRequest.toIntent] και οι τέσσερις διαδρομές που το ανοίγουν
 * (υπενθυμίσεις, αρχική Android TV, δοκιμή ροής στην εισαγωγή πηγής, catch-up)
 * δεν χρειάστηκε να αγγιχτούν. Μια μετονομασία θα ήταν τέσσερα σημεία ακόμη που
 * μπορούσαν να ξεχαστούν, χωρίς κανένα κέρδος.
 *
 * ΤΙ ΔΕΝ ΥΠΟΣΤΗΡΙΖΕΙ ΠΛΕΟΝ, ΚΑΙ ΓΙΑΤΙ ΔΕΝ ΠΕΙΡΑΖΕΙ: λίστα καναλιών και αλλαγή
 * καναλιού. Και οι τέσσερις είσοδοι φέρνουν ΕΝΑ stream χωρίς κατάλογο γύρω του —
 * μια υπενθύμιση για ένα κανάλι, μια κάρτα της αρχικής TV, μια δοκιμή σύνδεσης,
 * ένα αρχειοθετημένο πρόγραμμα. Δεν υπάρχει λίστα να διατρέξεις.
 */
class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Το `PlaybackQueue.sourceId` είναι η εφεδρεία για Intents που δεν
        // δήλωσαν πηγή (π.χ. παλιά προγραμματισμένη υπενθύμιση). Χωρίς αυτό, η
        // θέση της ταινίας θα γραφόταν σε λάθος χώρο και το «Συνέχισε» δεν θα τη
        // βρισκε ποτέ.
        val request = PlayerLaunchRequest.fromIntent(intent, PlaybackQueue.sourceId)
        if (request == null) {
            // Intent χωρίς URL. Ένας player με μαύρη οθόνη είναι χειρότερος από
            // καθόλου player: ο χρήστης νομίζει ότι φορτώνει.
            finish()
            return
        }

        setContent { IptvTheme { ExternalPlayer(request, onClose = { finish() }) } }
    }
}

@Composable
private fun ExternalPlayer(request: PlayerLaunchRequest, onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PlaylistStore(context.applicationContext) }

    // Το Intent φέρνει πεδία, όχι αντικείμενο. Το ξαναφτιάχνουμε ώστε ο κοινός
    // player να δει ό,τι βλέπει από τον κατάλογο — και να δουλέψουν οι υπότιτλοι,
    // που ψάχνουν με τίτλο, έτος και είδος.
    val channel = remember(request) {
        Channel(
            name = request.title,
            logo = request.logo,
            tvgId = request.tvgId,
            url = request.url,
            kind = request.kind,
            plot = request.plot,
            cast = request.cast,
            director = request.director,
            genre = request.genre,
            year = request.year,
            duration = request.duration,
        )
    }

    val isLive = request.kind == "live"
    var epgOpen by remember { mutableStateOf(false) }

    // ΤΟ ΠΡΟΓΡΑΜΜΑ ΕΡΧΕΤΑΙ ΜΕ ΔΥΟ ΤΡΟΠΟΥΣ.
    //
    // Ο καλών μπορεί να το έχει στείλει μέσα στο Intent (η αρχική Android TV το
    // ξέρει ήδη και δεν θέλουμε δεύτερο αίτημα δικτύου). Αν δεν το έστειλε, το
    // ζητάμε από τον EpgManager όπως όλη η εφαρμογή. Χωρίς το πρώτο σκέλος, μια
    // υπενθύμιση σε πηγή που δεν έχει φορτωμένο EPG θα έδειχνε άδειο πρόγραμμα
    // ενώ οι πληροφορίες ταξίδεψαν μαζί με το Intent.
    val intentProgrammes = remember(request) {
        request.epgTitles.indices.map { index ->
            PlayerProgramme(
                time = request.epgTimes.getOrElse(index) { "" },
                title = request.epgTitles[index],
                description = request.epgDescriptions.getOrElse(index) { "" },
                // Το «τώρα» δεν το ξέρουμε από το Intent: οι ώρες είναι κείμενο.
                // Καλύτερα κανένα σημάδι από λάθος σημάδι.
                isNow = false,
            )
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        TvPlaybackOverlay(
            channel = channel,
            title = request.title,
            subtitle = listOf(request.year, request.genre)
                .filter(String::isNotBlank).joinToString(" · "),
            // Το URL ήρθε έτοιμο μέσα στο Intent — ο καλών το έλυσε πριν μας
            // ξεκινήσει. Δεν ξαναρωτάμε τον πάροχο.
            resolveUrl = { request.url },
            loadResumeMs = {
                if (isLive || request.positionKey.isBlank()) 0L
                // Το store επιστρέφει (θέση, διάρκεια)· εδώ χρειάζεται μόνο η θέση.
                else store.loadPosition(request.sourceId, request.positionKey)?.first ?: 0L
            },
            saveResumeMs = { _, position, duration ->
                if (!isLive && request.positionKey.isNotBlank()) {
                    store.savePosition(request.sourceId, request.positionKey, position, duration)
                }
            },
            onClose = onClose,
            fetchSubtitles = { engine -> SubtitleWiring.autoFetch(context, channel, engine) },
            searchSubtitles = { query -> SubtitleWiring.search(context, channel, query) },
            applySubtitle = { engine, choice -> SubtitleWiring.apply(context, engine, choice) },
            extraActions = if (isLive) {
                { PlayerExtraAction("Πρόγραμμα") { epgOpen = true } }
            } else null,
            overlayOpen = epgOpen,
        )
        if (epgOpen) {
            PlayerEpgDialog(
                channelName = request.title,
                load = { intentProgrammes.ifEmpty { upcomingProgrammes(request.tvgId) } },
                onDismiss = { epgOpen = false }
            )
        }
    }
}
