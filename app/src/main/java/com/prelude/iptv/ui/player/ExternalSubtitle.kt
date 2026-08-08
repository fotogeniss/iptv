package com.prelude.iptv.ui.player

/**
 * Απομακρυσμένο αρχείο υποτίτλων που πρέπει να ληφθεί πριν επιλεγεί.
 * Διαφέρει από τα ενσωματωμένα [com.prelude.iptv.player.PlaybackEngine.TrackOption].
 */
data class ExternalSubtitle(
    val id: Int,
    val label: String,
    val language: String,
    /** Τοπικός βαθμός αντιστοίχισης τίτλου/έτους/σεζόν/επεισοδίου, 0..100. */
    val matchPercent: Int = 0,
)
