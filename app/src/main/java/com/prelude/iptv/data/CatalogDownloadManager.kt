package com.prelude.iptv.data

import android.content.Context
import com.prelude.iptv.net.Http
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicInteger

/**
 * Λήψη καταλόγου που επιβιώνει όταν ο χρήστης αλλάξει εφαρμογή.
 *
 * ---
 *
 * ΤΡΙΑ ΠΡΑΓΜΑΤΑ, ΚΑΙ ΤΑ ΤΡΙΑ ΑΠΑΡΑΙΤΗΤΑ:
 *
 * 1. **Κρατά τη διεργασία ζωντανή** μέσω [CatalogDownloadService]. Χωρίς αυτό, το
 *    ColorOS σκοτώνει την εφαρμογή δευτερόλεπτα μετά την εναλλαγή και η λήψη
 *    πεθαίνει σιωπηλά.
 *
 * 2. **Δεν ξεκινά δεύτερη λήψη για την ίδια διεύθυνση.** Αυτό είναι το πιο λεπτό:
 *    επιστρέφοντας στην εφαρμογή, η οθόνη ζητά ξανά τον κατάλογο. Χωρίς κλείδωμα,
 *    δύο λήψεις θα έγραφαν **στο ίδιο αρχείο ταυτόχρονα** — και το αποτέλεσμα δεν
 *    είναι «διπλή δουλειά», είναι κατεστραμμένο αρχείο που σκάει στην ανάλυση με
 *    μήνυμα άσχετο με την αιτία.
 *
 *    Ο δεύτερος καλών **περιμένει** τον πρώτο και παίρνει το ίδιο αρχείο.
 *
 * 3. **Δημοσιεύει πρόοδο** που διαβάζει η ειδοποίηση, ώστε ο χρήστης να βλέπει τι
 *    γίνεται χωρίς να ανοίξει την εφαρμογή.
 */
object CatalogDownloadManager {

    data class Progress(
        val url: String,
        val bytesRead: Long,
        val totalBytes: Long?,
    )

    private val _progress = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?> = _progress.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    /**
     * Η ενεργή λήψη ανά διεύθυνση, αν υπάρχει.
     *
     * ΓΙΑΤΙ `FutureTask` ΚΑΙ ΟΧΙ `synchronized(lock)`: η προηγούμενη εκδοχή
     * μπλόκαρε τον δεύτερο καλούντα πίσω από ένα `synchronized`, αλλά μόλις
     * ξεμπλόκαρε ξεκινούσε ΔΙΚΗ ΤΟΥ λήψη από την αρχή — διπλή κίνηση δικτύου για
     * τίποτα, παρότι το σχόλιο (και το changelog) έλεγε «παίρνει το ίδιο αρχείο».
     * Με `FutureTask`, ο δεύτερος καλών περιμένει το ΑΠΟΤΕΛΕΣΜΑ της πρώτης λήψης
     * και αντιγράφει το έτοιμο αρχείο αντί να το ξανακατεβάσει.
     *
     * `ConcurrentHashMap` και όχι απλό `HashMap`: οι κλήσεις έρχονται από νήματα
     * IO και από το κύριο νήμα, και ένα `HashMap` που γράφεται ταυτόχρονα δεν
     * πετάει σφάλμα — απλώς χαλάει, και το βρίσκεις μήνες μετά.
     */
    private val inFlight = ConcurrentHashMap<String, FutureTask<File>>()

    /** Πόσες λήψεις τρέχουν. Η υπηρεσία σταματά μόνο όταν μηδενίσει. */
    private val active = AtomicInteger(0)

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Κατεβάζει τη διεύθυνση σε αρχείο και το επιστρέφει.
     *
     * **Μπλοκάρει** — ο καλών είναι ήδη σε νήμα IO. Αν τρέχει ήδη λήψη για την ίδια
     * διεύθυνση, περιμένει εκείνη και επιστρέφει το ίδιο αρχείο.
     *
     * Ο καλών είναι υπεύθυνος να **σβήσει** το αρχείο όταν τελειώσει μαζί του.
     */
    fun download(
        url: String,
        headers: Map<String, String>,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null,
    ): File {
        while (true) {
            val existing = inFlight[url]
            if (existing != null) {
                // Κάποιος άλλος κατεβάζει ήδη αυτή τη διεύθυνση. Περιμένουμε το
                // ΑΠΟΤΕΛΕΣΜΑ του (όχι δική μας λήψη) και παίρνουμε αντίγραφο —
                // αντίγραφο και όχι το ίδιο File, γιατί κάθε καλών σβήνει το δικό
                // του αρχείο όταν τελειώσει μαζί του.
                val finished = try {
                    existing.get()
                } catch (wrapped: java.util.concurrent.ExecutionException) {
                    // FutureTask τυλίγει κάθε σφάλμα σε ExecutionException. Το
                    // ξετυλίγουμε ώστε ο καλών να δει το πραγματικό σφάλμα δικτύου
                    // (π.χ. HTTP 404), όχι ένα γενικό "ExecutionException".
                    throw wrapped.cause ?: wrapped
                }
                val copy = File.createTempFile("catalog_", ".tmp")
                finished.copyTo(copy, overwrite = true)
                return copy
            }

            val task = FutureTask {
                val target = File.createTempFile("catalog_", ".tmp")
                try {
                    Http.providerDownloadTo(url, target, headers) { read, total ->
                        _progress.value = Progress(url, read, total)
                        onProgress?.invoke(read, total)
                    }
                    target
                } catch (error: Throwable) {
                    // Το μισοκατεβασμένο αρχείο δεν χρησιμεύει σε κανέναν και
                    // πιάνει εκατοντάδες megabyte.
                    target.delete()
                    throw error
                }
            }

            // putIfAbsent: αν χάσαμε τον αγώνα (κάποιος πρόλαβε ανάμεσα στο
            // παραπάνω get() και εδώ), ξαναγυρνάμε στην αρχή του βρόχου και
            // περιμένουμε ΕΚΕΙΝΟΝ αντί να ξεκινήσουμε δεύτερη λήψη.
            if (inFlight.putIfAbsent(url, task) != null) continue

            val context = appContext
            if (active.getAndIncrement() == 0 && context != null) {
                CatalogDownloadService.start(context)
            }
            try {
                task.run()
                return task.get()
            } finally {
                if (active.decrementAndGet() == 0) {
                    _progress.value = null
                    context?.let(CatalogDownloadService::stop)
                }
                inFlight.remove(url, task)
            }
        }
    }
}
