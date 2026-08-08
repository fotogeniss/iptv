package com.prelude.iptv.source

import com.prelude.iptv.data.Channel

object M3uParser {
    private val EXTINF = Regex("""#EXTINF:(-?\d+(?:\.\d+)?)\s*(.*?),(.*)$""")
    private val ATTR = Regex("""([A-Za-z0-9\-_]+)="(.*?)"""")

    /**
     * Το EPG που δηλώνει ο ίδιος ο πάροχος στην κεφαλίδα:
     *   #EXTM3U url-tvg="http://..."   (ή x-tvg-url / tvg-url)
     * Μέχρι τώρα το αγνοούσαμε εντελώς.
     */
    fun epgUrlFrom(text: String): String {
        val header = text.lineSequence().firstOrNull { it.trimStart().startsWith("#EXTM3U") } ?: return ""
        for (a in ATTR.findAll(header)) {
            val k = a.groupValues[1].lowercase()
            if (k == "url-tvg" || k == "x-tvg-url" || k == "tvg-url") {
                // μπορεί να δίνει πολλά, χωρισμένα με κόμμα
                return a.groupValues[2].split(",").firstOrNull { it.isNotBlank() }?.trim() ?: ""
            }
        }
        return ""
    }

    // S01E02 / s1 e2 -> σειρά
    private val SERIES_TAG = Regex("\\bS\\d{1,2}\\s?E\\d{1,3}\\b", RegexOption.IGNORE_CASE)

    /**
     * Ξεχωρίζει live / ταινίες / σειρές μέσα σε ένα M3U.
     *
     * Το πιο αξιόπιστο σημάδι είναι το ίδιο το πρότυπο: #EXTINF:-1 σημαίνει
     * ζωντανή ροή, ενώ θετική διάρκεια (π.χ. #EXTINF:7200) σημαίνει αρχείο.
     * Το συμπληρώνουμε με τη διαδρομή του URL και το group-title, όπως τα
     * γράφουν στην πράξη οι πάροχοι.
     */
    // Κατάληξη αρχείου = σχεδόν σίγουρα VOD. Τα live είναι .ts/.m3u8/χωρίς κατάληξη.
    private val FILE_EXT = Regex("""\.(mp4|mkv|avi|mov|m4v|wmv|flv)(\?.*)?$""")

    // internal (όχι private): ώστε το unit test να το ελέγχει άμεσα. Καθαρή
    // συνάρτηση, καμία παρενέργεια — ιδανικός στόχος τεστ.
    internal fun classify(duration: String, url: String, group: String, name: String): String {
        val u = url.lowercase()
        val g = group.lowercase()
        val dur = duration.toDoubleOrNull() ?: -1.0

        // 1) η διαδρομή του παρόχου είναι η πιο ξεκάθαρη ένδειξη
        if (u.contains("/series/")) return "series"
        if (u.contains("/movie/") || u.contains("/movies/") || u.contains("/vod/")) return "vod"

        // 2) Θέλουμε ΑΠΟΔΕΙΞΗ ότι είναι αρχείο: θετική διάρκεια ή κατάληξη αρχείου.
        //    ΠΡΟΣΟΧΗ: πριν, το όνομα του group ΑΡΚΟΥΣΕ μόνο του — live κανάλια σε
        //    groups τύπου «MOVIES HD» / «CINEMA» (πολύ συνηθισμένα live groups)
        //    κατέληγαν στις Ταινίες και έχαναν EPG, zapping και LIVE badge.
        //    Το λάθος «vod να πάει στο live» είναι ακίνδυνο (παίζει κανονικά)·
        //    το ανάποδο χαλάει λειτουργίες — γι' αυτό γέρνουμε προς live.
        val looksLikeFile = dur > 0 || FILE_EXT.containsMatchIn(u)
        if (!looksLikeFile) return "live"

        // 3) είναι αρχείο: το group/όνομα ξεχωρίζει πλέον ΜΟΝΟ vod ↔ series
        val seriesGrp = g.contains("series") || g.contains("serie") || g.contains("σειρ")
        return if (seriesGrp || SERIES_TAG.containsMatchIn(name)) "series" else "vod"
    }

    /**
     * Ανάλυση από ολόκληρο κείμενο.
     *
     * ΠΡΟΣΟΧΗ ΣΤΟ ΜΕΓΕΘΟΣ: μια λίστα με πλήρη κατάλογο VOD φτάνει εύκολα τα
     * 100–300 MB. Ένα `String` της Java κρατά **2 bytes ανά χαρακτήρα**, οπότε
     * αρχείο 150 MB γίνεται ~300 MB στη μνήμη — και όσο χτίζεται, ζει ταυτόχρονα
     * και το buffer από το οποίο προήλθε. Εκεί σκάει το `OutOfMemoryError`.
     *
     * Αυτή η υπερφόρτωση μένει για δοκιμές και για μικρά αρχεία. **Η φόρτωση
     * πηγής πρέπει να χρησιμοποιεί την εκδοχή με [Sequence]**, που δεν κρατά ποτέ
     * περισσότερο από μία γραμμή.
     */
    fun parse(text: String, onProgress: ((processedLines: Int, totalLines: Int) -> Unit)? = null): List<Channel> =
        parse(text.lineSequence(), text.count { it == '\n' } + 1, onProgress)

    /**
     * Ανάλυση γραμμή προς γραμμή, χωρίς να υπάρχει ποτέ ολόκληρο το αρχείο στη μνήμη.
     *
     * @param estimatedLines μόνο για τη μπάρα προόδου. Δεν χρειάζεται ακρίβεια —
     *   άγνωστο μέγεθος δίνει `0` και η πρόοδος απλώς δεν δείχνει ποσοστό, αντί να
     *   χρειαστεί να διαβαστεί το αρχείο δύο φορές για να μετρηθεί.
     */
    fun parse(
        lines: Sequence<String>,
        estimatedLines: Int = 0,
        onProgress: ((processedLines: Int, totalLines: Int) -> Unit)? = null,
        /**
         * Καλείται με τη γραμμή `#EXTM3U`, αν υπάρχει.
         *
         * Έτσι το EPG της κεφαλίδας βγαίνει στο **ίδιο πέρασμα**. Πριν, γινόταν
         * δεύτερη σάρωση ολόκληρου του κειμένου — που είναι και ο λόγος που το
         * κείμενο έπρεπε να μείνει ολόκληρο στη μνήμη.
         */
        onHeader: ((String) -> Unit)? = null,
    ): List<Channel> {
        val channels = ArrayList<Channel>()
        var name = ""
        var group = ""
        var logo = ""
        var tvg = ""
        var dur = "-1"
        var pending = false
        var currentGrp = ""
        val totalLines = estimatedLines
        var processedLines = 0

        for (raw in lines) {
            processedLines++
            if (processedLines % 250 == 0) {
                onProgress?.invoke(processedLines, totalLines.coerceAtLeast(processedLines))
            }
            val line = raw.trim()
            if (line.isEmpty()) continue
            when {
                line.startsWith("#EXTINF") -> {
                    val m = EXTINF.find(line)
                    val attrs = HashMap<String, String>()
                    var nm = ""
                    dur = "-1"
                    if (m != null) {
                        dur = m.groupValues[1]       // -1 = live, θετικό = αρχείο
                        for (a in ATTR.findAll(m.groupValues[2])) {
                            attrs[a.groupValues[1]] = a.groupValues[2]
                        }
                        nm = m.groupValues[3].trim()
                    }
                    name = if (nm.isNotEmpty()) nm else attrs["tvg-name"] ?: "Άγνωστο"
                    group = attrs["group-title"] ?: currentGrp
                    logo = attrs["tvg-logo"] ?: ""
                    tvg = attrs["tvg-id"] ?: ""
                    pending = true
                }
                line.startsWith("#EXTGRP:") -> {
                    currentGrp = line.substringAfter(":").trim()
                    if (pending && group.isEmpty()) group = currentGrp
                }
                line.startsWith("#EXTM3U") -> onHeader?.invoke(line)
                line.startsWith("#") -> { /* skip */ }
                else -> {
                    if (pending) {
                        val grp = group.ifEmpty { "Χωρίς ομάδα" }
                        channels.add(
                            Channel(
                                name = name,
                                group = grp,
                                logo = logo,
                                tvgId = tvg,
                                url = line,
                                kind = classify(dur, line, grp, name)
                            )
                        )
                        pending = false
                    }
                }
            }
        }
        onProgress?.invoke(processedLines, processedLines)
        return channels
    }

    /**
     * Το EPG της κεφαλίδας, από ροή.
     *
     * Η κεφαλίδα `#EXTM3U` είναι η **πρώτη μη κενή γραμμή**. Διαβάζοντάς τη χωριστά
     * γλιτώνουμε δεύτερο πέρασμα πάνω σε αρχείο εκατοντάδων megabyte — που ήταν ο
     * λόγος που το κείμενο έπρεπε να μείνει ολόκληρο στη μνήμη.
     */
    fun epgUrlFromHeader(header: String): String {
        if (!header.trimStart().startsWith("#EXTM3U")) return ""
        for (a in ATTR.findAll(header)) {
            val k = a.groupValues[1].lowercase()
            if (k == "url-tvg" || k == "x-tvg-url" || k == "tvg-url") {
                return a.groupValues[2].split(",").firstOrNull { it.isNotBlank() }?.trim() ?: ""
            }
        }
        return ""
    }
}
