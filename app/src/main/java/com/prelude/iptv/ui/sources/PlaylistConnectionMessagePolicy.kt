package com.prelude.iptv.ui.sources

/** Converts provider/library failures into short, actionable onboarding copy. */
object PlaylistConnectionMessagePolicy {
    fun failure(raw: String?): String {
        val message = raw.orEmpty().trim()
        val normalized = message.lowercase()
        return when {
            "δεν μοιάζει με m3u" in normalized || "#extm3u" in normalized ->
                "Το αρχείο ή ο σύνδεσμος δεν περιέχει έγκυρη λίστα M3U."
            "λάθος στοιχεία" in normalized || "unauthorized" in normalized || "forbidden" in normalized ||
                "401" in normalized || "403" in normalized ->
                "Ο server απέρριψε τα στοιχεία. Έλεγξε όνομα χρήστη και κωδικό."
            "unknown host" in normalized || "unable to resolve" in normalized || "no address associated" in normalized ->
                "Δεν βρέθηκε ο server. Έλεγξε προσεκτικά τη διεύθυνση."
            "timed out" in normalized || "timeout" in normalized || "time out" in normalized ->
                "Ο server δεν απάντησε εγκαίρως. Δοκίμασε ξανά σε λίγο."
            "connection refused" in normalized || "failed to connect" in normalized ->
                "Ο server δεν δέχτηκε τη σύνδεση. Έλεγξε διεύθυνση και θύρα."
            "network is unreachable" in normalized || "no internet" in normalized ->
                "Δεν υπάρχει σύνδεση στο Internet. Έλεγξε το δίκτυό σου."
            else -> "Η σύνδεση απέτυχε. Έλεγξε τα στοιχεία και δοκίμασε ξανά."
        }
    }
}
