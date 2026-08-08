package com.prelude.iptv.player

/**
 * Ανάλυση SRT υποτίτλων — καθαρή λογική, βγαλμένη από το PlayerActivity ώστε
 * (α) να ξεφορτώσει τον 2.100-γραμμών god-class και (β) να δοκιμάζεται σε JVM.
 * Καμία εξάρτηση από Android.
 */
data class Cue(val startMs: Long, val endMs: Long, val text: String)

object SrtParser {
    private val TIME = Regex(
        """(\d{2}):(\d{2}):(\d{2}),(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2}),(\d{3})"""
    )
    private val TAGS = Regex("<[^>]+>")

    fun parse(text: String): List<Cue> {
        val out = ArrayList<Cue>()
        val blocks = text.replace("\r\n", "\n").split(Regex("\n[ \t]*\n"))
        for (b in blocks) {
            val m = TIME.find(b) ?: continue
            val g = m.groupValues
            val start = ((g[1].toLong() * 3600 + g[2].toLong() * 60 + g[3].toLong()) * 1000) + g[4].toLong()
            val end = ((g[5].toLong() * 3600 + g[6].toLong() * 60 + g[7].toLong()) * 1000) + g[8].toLong()
            val lines = b.split("\n")
            val timeIdx = lines.indexOfFirst { TIME.containsMatchIn(it) }
            val txt = lines.drop(timeIdx + 1).joinToString("\n").replace(TAGS, "").trim()
            if (txt.isNotEmpty()) out.add(Cue(start, end, txt))
        }
        return out
    }
}
