package com.prelude.iptv.player

/** Πόσο απόθεμα εικόνας κρατά ο player μπροστά από τη στιγμή που παίζει. */
enum class BufferProfile(val storageValue: String) {
    /** Ελάχιστη καθυστέρηση: γρήγορη αλλαγή καναλιού, λιγότερη αντοχή. */
    LOW("low"),

    /** Ισορροπία — η προεπιλογή. */
    NORMAL("normal"),

    /** Μεγάλο απόθεμα: αντέχει κακό δίκτυο, αργεί λίγο περισσότερο να ξεκινήσει. */
    HIGH("high"),
}

/**
 * Το απόθεμα δεν είναι μία τιμή αλλά τέσσερις, και έχουν σημασία οι σχέσεις τους.
 *
 * Καθαρή πολιτική ώστε οι σχέσεις αυτές να ελέγχονται με tests: μια λάθος
 * τετράδα (π.χ. ελάχιστο μεγαλύτερο από μέγιστο) δεν βγάζει σφάλμα μεταγλώττισης
 * — βγάζει player που ξαναφορτώνει συνεχώς, και αυτό φαίνεται μόνο πάνω στη
 * συσκευή.
 */
object BufferPolicy {

    /**
     * @param minMs Κάτω από αυτό το απόθεμα, ο player ζητά κι άλλα δεδομένα.
     * @param maxMs Πάνω από αυτό σταματά να κατεβάζει.
     * @param forPlaybackMs Πόσο πρέπει να μαζευτεί πριν ξεκινήσει η εικόνα.
     * @param afterRebufferMs Το ίδιο, μετά από διακοπή — μεγαλύτερο σκόπιμα,
     *   ώστε να μην ξανακολλήσει αμέσως στο ίδιο σημείο.
     */
    data class Durations(
        val minMs: Int,
        val maxMs: Int,
        val forPlaybackMs: Int,
        val afterRebufferMs: Int,
    )

    fun durationsFor(profile: BufferProfile): Durations = when (profile) {
        BufferProfile.LOW -> Durations(
            minMs = 5_000,
            maxMs = 20_000,
            forPlaybackMs = 1_500,
            afterRebufferMs = 3_000,
        )
        BufferProfile.NORMAL -> Durations(
            minMs = 20_000,
            maxMs = 60_000,
            forPlaybackMs = 2_500,
            afterRebufferMs = 6_000,
        )
        BufferProfile.HIGH -> Durations(
            minMs = 45_000,
            maxMs = 120_000,
            forPlaybackMs = 4_000,
            afterRebufferMs = 10_000,
        )
    }

    fun fromStorage(value: String?): BufferProfile =
        BufferProfile.entries.firstOrNull { it.storageValue == value?.trim()?.lowercase() }
            ?: BufferProfile.NORMAL

    /** Σύντομη περιγραφή για τη λίστα ρυθμίσεων. */
    fun label(profile: BufferProfile): String = when (profile) {
        BufferProfile.LOW -> "Χαμηλό — γρήγορη αλλαγή καναλιού"
        BufferProfile.NORMAL -> "Κανονικό — ισορροπία"
        BufferProfile.HIGH -> "Υψηλό — για ασταθές δίκτυο"
    }
}
