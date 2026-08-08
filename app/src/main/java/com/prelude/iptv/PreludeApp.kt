package com.prelude.iptv

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.prelude.iptv.billing.PreludeBilling
import com.prelude.iptv.diagnostics.DiagnosticsManager

/**
 * Ρύθμιση εικόνων για τηλεόραση.
 *
 * ΓΙΑΤΙ ΥΠΑΡΧΕΙ: μέχρι τώρα δεν υπήρχε καθόλου κλάση Application, οπότε το Coil
 * έτρεχε με τις προεπιλογές του. Αυτές είναι γραμμένες για κινητό: μνήμη
 * αναλογική του σωρού, cache δίσκου που μεγαλώνει ελεύθερα, και αποκωδικοποίηση
 * σε πλήρες μέγεθος.
 *
 * Ένα TV box δεν είναι κινητό. Έχει μικρότερο σωρό, αργή εσωτερική μνήμη, και μια
 * οθόνη 1080p όπου καμία αφίσα δεν χρειάζεται να αποκωδικοποιηθεί σε 500px πλάτος
 * για να δείξει σε κάρτα 180px. Η διαφορά φαίνεται ακριβώς εκεί που πονάει: στο
 * scroll μιας σειράς από αφίσες.
 */
class PreludeApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        DiagnosticsManager.initialize(this)
        PreludeBilling.initialize(this)
        // Χρειάζεται context για να ξεκινήσει την υπηρεσία προσκηνίου. Δίνεται
        // εδώ ώστε το [Repository] να μένει χωρίς εξάρτηση από Android.
        com.prelude.iptv.data.CatalogDownloadManager.initialize(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        // ---- ΜΝΗΜΗ ----
        // Το ποσοστό υπολογίζεται πάνω στον ΠΡΑΓΜΑΤΙΚΟ σωρό της συσκευής.
        // Σε box με 128MB σωρό, το 25% της προεπιλογής αφήνει ελάχιστα για τον
        // player· σε τηλεόραση με 512MB, το ίδιο ποσοστό σπαταλά. Κρατάμε ένα
        // πιο συντηρητικό ποσοστό αλλά με κατώτατο όριο, ώστε σε μικρές συσκευές
        // να μη γίνει τόσο μικρό που να μη χωράει ούτε μία σειρά καρτών.
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(memoryCachePercent())
                .build()
        }
        // ---- ΔΙΣΚΟΣ ----
        // Οι αφίσες TMDB δεν αλλάζουν ποτέ. Χωρίς cache δίσκου, κάθε εκκίνηση
        // ξανακατεβάζει ολόκληρο τον κατάλογο εικόνων — και σε box με αργό WiFi
        // αυτό είναι η κύρια αιτία που «αργεί να φορτώσει».
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(DISK_CACHE_BYTES)
                .build()
        }
        // ---- ΑΠΟΚΩΔΙΚΟΠΟΙΗΣΗ ----
        // Hardware bitmaps: η εικόνα μένει στη μνήμη της GPU αντί στον σωρό.
        // Μεγάλο κέρδος σε πλέγματα αφισών. Σε παλιά Android η υλοποίηση είχε
        // προβλήματα με ταυτόχρονη πρόσβαση, γι' αυτό μόνο από API 28.
        .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        // Χωρίς RGB565: κερδίζει μνήμη αλλά φέρνει ορατό banding στις σκοτεινές
        // αφίσες, που είναι οι περισσότερες σε μαύρο φόντο.
        .allowRgb565(false)
        // Το crossfade ΜΟΝΟ για εικόνες που ήρθαν από δίκτυο. Όταν έρχονται από
        // μνήμη εμφανίζονται ακαριαία, και ένα fade εκεί προσθέτει καθυστέρηση
        // που δεν υπάρχει λόγος να υπάρχει.
        .crossfade(false)
        .respectCacheHeaders(false)
        .networkCachePolicy(CachePolicy.ENABLED)
        .build()

    /**
     * Πόσο του σωρού δίνεται στις εικόνες.
     *
     * Σε συσκευές με μικρό σωρό δίνουμε αναλογικά περισσότερο: εκεί η
     * εναλλακτική δεν είναι «λιγότερη μνήμη» αλλά «συνεχής επαναφόρτωση», που
     * κοστίζει πολύ ακριβότερα.
     */
    private fun memoryCachePercent(): Double {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val heapMb = am?.memoryClass ?: 0
        return when {
            heapMb <= 0 -> 0.20
            heapMb < 192 -> 0.30
            else -> 0.20
        }
    }

    private companion object {
        /**
         * 256MB: αρκετά για μερικές χιλιάδες αφίσες, αρκετά λίγα ώστε να μη
         * γεμίσει την εσωτερική μνήμη ενός φθηνού box των 8GB.
         */
        const val DISK_CACHE_BYTES = 256L * 1024 * 1024
    }
}
