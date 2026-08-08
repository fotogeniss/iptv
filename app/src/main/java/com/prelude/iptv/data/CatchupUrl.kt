package com.prelude.iptv.data

/**
 * Καθαρή κατασκευή catch-up (timeshift) URL — απομονωμένη από το ViewModel
 * ώστε να δοκιμάζεται χωρίς Android/Channel. Το ViewModel απλά τραβάει τα
 * πεδία από το Channel/Playlist και καλεί αυτό.
 *
 * Πρότυπο Xtream Codes:
 *   {server}/timeshift/{user}/{pass}/{durationMin}/{yyyy-MM-dd:HH-mm}/{streamId}.ts
 */
object CatchupUrl {
    fun build(
        server: String,
        user: String,
        pass: String,
        streamId: String,
        startMs: Long,
        stopMs: Long,
        now: Long = System.currentTimeMillis()
    ): String? {
        if (streamId.isBlank() || server.isBlank()) return null
        // catch-up έχει νόημα μόνο για ΠΕΡΑΣΜΕΝΟ πρόγραμμα
        if (stopMs > now) return null
        val durMin = ((stopMs - startMs) / 60_000L)
        if (durMin < 1) return null
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", java.util.Locale.US)
        val startStr = fmt.format(java.util.Date(startMs))
        val base = server.trimEnd('/')
        val u = java.net.URLEncoder.encode(user, "UTF-8")
        val p = java.net.URLEncoder.encode(pass, "UTF-8")
        return "$base/timeshift/$u/$p/$durMin/$startStr/$streamId.ts"
    }
}
