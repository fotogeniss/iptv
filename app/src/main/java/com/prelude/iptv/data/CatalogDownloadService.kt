package com.prelude.iptv.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.prelude.iptv.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Κρατά τη διεργασία ζωντανή όσο κατεβαίνει ο κατάλογος.
 *
 * ---
 *
 * ΤΙ **ΔΕΝ** ΚΑΝΕΙ: δεν κατεβάζει. Η λήψη τρέχει εκεί που έτρεχε πάντα, μέσα στο
 * [CatalogDownloadManager], στο νήμα του καλούντα.
 *
 * ΓΙΑΤΙ ΕΤΣΙ: μεταφέροντας τη λήψη μέσα στην υπηρεσία θα χρειαζόταν να ξαναγραφεί
 * όλη η ροή φόρτωσης ως ασύγχρονη επικοινωνία με Service — δηλαδή δεύτερη
 * υλοποίηση της ίδιας δουλειάς, με δικά της σφάλματα. Η υπηρεσία εδώ κάνει **ένα**
 * πράγμα: λέει στο Android «μη σκοτώσεις αυτή τη διεργασία».
 *
 * Αυτό αρκεί. Το Android δεν σκοτώνει κώδικα — σκοτώνει **διεργασίες**. Όσο υπάρχει
 * ενεργή υπηρεσία προσκηνίου, η λήψη συνεχίζει ό,τι κι αν κάνει ο χρήστης.
 *
 * ---
 *
 * ΓΙΑΤΙ ΧΡΕΙΑΖΕΤΑΙ ΚΑΘΟΛΟΥ: μια εφαρμογή που χάνει την εστίαση μπαίνει σε
 * «cached» κατάσταση και το σύστημα μπορεί να τη σκοτώσει ανά πάσα στιγμή. Σε
 * ColorOS (OPPO), MIUI και One UI αυτό γίνεται **επιθετικά** και μέσα σε
 * δευτερόλεπτα. Χωρίς υπηρεσία προσκηνίου, μια λήψη 200 MB δεν έχει καμία ελπίδα
 * να τελειώσει με την οθόνη κλειστή.
 */
class CatalogDownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // ΑΜΕΣΩΣ, ΠΡΙΝ ΟΤΙΔΗΠΟΤΕ ΑΛΛΟ: το Android σκοτώνει την υπηρεσία με
        // ForegroundServiceDidNotStartInTimeException αν το startForeground
        // αργήσει πάνω από πέντε δευτερόλεπτα.
        startForeground(NOTIFICATION_ID, buildNotification(null))

        scope.launch {
            CatalogDownloadManager.progress.collectLatest { progress ->
                if (progress == null) return@collectLatest
                notificationManager().notify(NOTIFICATION_ID, buildNotification(progress))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // NOT_STICKY: αν το σύστημα μας σκοτώσει παρ' όλα αυτά, ΔΕΝ θέλουμε να
        // ξαναξεκινήσει μόνη της. Η λήψη ανήκει σε μια ενέργεια του χρήστη· μια
        // υπηρεσία που αναστήνεται χωρίς αυτή την ενέργεια θα κατέβαζε στα κρυφά.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(progress: CatalogDownloadManager.Progress?): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Λήψη λίστας")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val total = progress?.totalBytes
        val bytes = progress?.bytesRead ?: 0L
        if (total != null && total > 0L) {
            val percent = ((bytes.toDouble() / total) * 100).toInt().coerceIn(0, 100)
            builder.setContentText("$percent% · ${megabytes(bytes)} από ${megabytes(total)}")
            builder.setProgress(100, percent, false)
        } else {
            // Άγνωστο μέγεθος: αόριστη μπάρα και μόνο όσα κατέβηκαν. Ένα ψεύτικο
            // ποσοστό είναι χειρότερο από κανένα — ο χρήστης το πιστεύει.
            builder.setContentText(megabytes(bytes))
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun megabytes(bytes: Long): String = "%.1f MB".format(bytes / 1_048_576.0)

    private fun notificationManager(): NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Λήψη λίστας",
            // LOW: χωρίς ήχο και χωρίς αναδυόμενο. Η λήψη είναι ενημέρωση, όχι
            // συμβάν που απαιτεί προσοχή.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Πρόοδος λήψης καταλόγου στο παρασκήνιο"
            setShowBadge(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "catalog_download"
        private const val NOTIFICATION_ID = 4711

        /**
         * Ξεκινά την υπηρεσία. Ασφαλές να κληθεί όταν τρέχει ήδη.
         *
         * Τα σφάλματα καταπίνονται επίτηδες: αν το σύστημα αρνηθεί (π.χ. ο χρήστης
         * έχει απαγορεύσει ειδοποιήσεις σε παλιά έκδοση, ή είμαστε σε περιορισμένη
         * κατάσταση), η λήψη πρέπει να **συνεχίσει χωρίς προστασία** αντί να
         * αποτύχει. Χειρότερη προστασία είναι καλύτερη από καμία λήψη.
         */
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, CatalogDownloadService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, CatalogDownloadService::class.java))
            }
        }
    }
}
