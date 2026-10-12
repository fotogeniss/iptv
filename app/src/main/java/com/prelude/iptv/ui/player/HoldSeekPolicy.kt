package com.prelude.iptv.ui.player

/** Κάθε πόσο προχωρά η γρήγορη κίνηση όσο κρατιέται το δάχτυλο. */
internal const val HOLD_TICK_MS = 100L

/**
 * Γρήγορη κίνηση με κράτημα: ξεκινά ×2 και επιταχύνει σε ×4 μετά από 2″ και
 * σε ×8 μετά από 4″. Το «×» είναι ετικέτα βαθμίδας· η πραγματική ταχύτητα είναι
 * σε δευτερόλεπτα ταινίας ανά δευτερόλεπτο κρατήματος, ώστε ένα δίωρο φιλμ να
 * διασχίζεται σε λογικό χρόνο.
 */
internal object HoldSeekPolicy {
    fun level(heldMs: Long): Int = when {
        heldMs < 2_000L -> 2
        heldMs < 4_000L -> 4
        else -> 8
    }

    /** Δευτερόλεπτα ταινίας ανά δευτερόλεπτο κρατήματος, ανά βαθμίδα. */
    fun mediaMsPerSecond(level: Int): Long = when (level) {
        2 -> 10_000L
        4 -> 30_000L
        else -> 90_000L
    }

    fun stepMs(heldMs: Long, tickMs: Long): Long = mediaMsPerSecond(level(heldMs)) * tickMs / 1_000L
}
