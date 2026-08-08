package com.prelude.iptv.player

import kotlin.math.roundToInt

/**
 * Μετατρέπει τα τεχνικά στοιχεία μιας ροής σε ετικέτα που διαβάζεται από άνθρωπο.
 *
 * Android-free και καθαρή, ώστε να δοκιμάζεται με unit tests: οι ροές IPTV δίνουν
 * κάθε λογής παραλλαγή (ύψος 1082 αντί 1080, ρυθμό καρέ 29.97, κωδικοποιητές με
 * ονόματα MIME που δεν λένε τίποτα σε κανέναν) και η ετικέτα πρέπει να παραμένει
 * σωστή σε όλες.
 */
object PlaybackQualityPolicy {

    /**
     * Εμπορική ονομασία ανάλυσης.
     *
     * Με ανοχή προς τα κάτω: πολλοί πάροχοι στέλνουν 1912x1072 ή 1440x1080 και θα
     * ήταν παραπλανητικό να εμφανίζεται «720p» σε μια ροή που είναι στην πράξη
     * Full HD. Κρίνουμε στο ύψος, γιατί το πλάτος αλλάζει με την αναλογία.
     */
    fun resolutionLabel(height: Int): String = when {
        height <= 0 -> ""
        height >= 2000 -> "4K"
        height >= 1300 -> "1440p"
        height >= 1000 -> "1080p"
        height >= 700 -> "720p"
        height >= 540 -> "576p"
        height >= 400 -> "480p"
        else -> "${height}p"
    }

    /** Φιλικό όνομα κωδικοποιητή από τον τύπο MIME. */
    fun codecLabel(mimeType: String): String = when (mimeType.lowercase()) {
        "video/avc" -> "H.264"
        "video/hevc" -> "H.265"
        "video/x-vnd.on2.vp9", "video/vp9" -> "VP9"
        "video/vp8" -> "VP8"
        "video/av01" -> "AV1"
        "video/mp4v-es" -> "MPEG-4"
        "video/mpeg2", "video/mpeg" -> "MPEG-2"
        else -> mimeType.substringAfter('/', "").uppercase()
    }

    /**
     * Ρυθμός καρέ χωρίς περιττά δεκαδικά: το 29.97 γράφεται «30», αλλά το 23.976
     * γράφεται «24» — κανείς δεν αναγνωρίζει τους πραγματικούς αριθμούς.
     */
    fun frameRateLabel(frameRate: Float): String {
        if (frameRate <= 0f) return ""
        return "${frameRate.roundToInt()} fps"
    }

    /** Ρυθμός δεδομένων σε μονάδα που ταιριάζει στο μέγεθός του. */
    fun bitrateLabel(bitrateBps: Int): String = when {
        bitrateBps <= 0 -> ""
        bitrateBps >= 1_000_000 -> {
            val mbps = bitrateBps / 1_000_000.0
            // Ένα δεκαδικό κάτω από τα 10 Mbps, όπου η διαφορά μετράει· κανένα
            // πάνω από αυτά. Και ποτέ «8.0» — το περιττό δεκαδικό κάνει την
            // ετικέτα να μοιάζει με έξοδο μηχανήματος αντί για πληροφορία.
            val rounded = if (mbps >= 10) mbps.roundToInt().toDouble()
            else (mbps * 10).roundToInt() / 10.0
            val text = if (rounded == rounded.toInt().toDouble()) {
                rounded.toInt().toString()
            } else {
                rounded.toString()
            }
            "$text Mbps"
        }
        else -> "${bitrateBps / 1_000} kbps"
    }

    /**
     * Η πλήρης ετικέτα, π.χ. «1080p · 50 fps · H.264 · 6.4 Mbps».
     *
     * Ό,τι λείπει απλώς παραλείπεται — καλύτερα μια σύντομη αληθινή ετικέτα παρά
     * μια πλήρης με μηδενικά. Κενή συμβολοσειρά σημαίνει «τίποτα να δείξουμε
     * ακόμη» και η διεπαφή δεν πρέπει να ζωγραφίσει καθόλου την ένδειξη.
     */
    fun label(quality: PlaybackEngine.VideoQuality): String = listOf(
        resolutionLabel(quality.height),
        frameRateLabel(quality.frameRate),
        codecLabel(quality.codec),
        bitrateLabel(quality.bitrateBps),
    ).filter { it.isNotBlank() }.joinToString(" · ")
}
