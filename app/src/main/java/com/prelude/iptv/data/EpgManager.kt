package com.prelude.iptv.data

import android.content.Context
import android.util.Xml
import com.prelude.iptv.net.Http
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * XMLTV EPG: κατέβασμα, parse, ευρετήριο και ερωτήματα.
 *
 * ΑΝΤΙΚΑΘΙΣΤΑ ΠΛΗΡΩΣ το προηγούμενο EpgManager.kt — κρατάει ΤΟ ΙΔΙΟ δημόσιο
 * API (load / isLoaded / currentSource / clear / nowNext / upcoming) και
 * προσθέτει ό,τι χρειάζεται το EPG grid + το catch-up.
 *
 * Τι κερδίζεις σε σχέση με μια απλή υλοποίηση:
 *  1. STREAMING parse (XmlPullParser): ένα XMLTV των 40MB ΔΕΝ φορτώνεται ποτέ
 *     ολόκληρο ως String στη μνήμη — αλλιώς OOM σε φθηνά TV boxes.
 *  2. GZIP με ΑΝΑΓΝΩΡΙΣΗ ΠΕΡΙΕΧΟΜΕΝΟΥ (magic bytes 1f 8b), όχι με βάση την
 *     κατάληξη: πολλοί πάροχοι σερβίρουν gzip από URL χωρίς .gz.
 *  3. DISK CACHE: πριν, το EPG ξανακατέβαινε σε ΚΑΘΕ εκκίνηση (isLoaded()=false
 *     μετά από restart) — δεκάδες MB και ~10-30" αναμονή κάθε φορά. Τώρα το
 *     ευρετήριο σώζεται σε compact binary και επαναφορτώνεται σε <1".
 *  4. Κλειδιά case-insensitive + trim: τα tvg-id των λιστών σπάνια ταιριάζουν
 *     ακριβώς με τα id του XMLTV (π.χ. "SKAI.gr" vs "skai.gr").
 */
object EpgManager {

    /** Πρόγραμμα με ΑΠΟΛΥΤΟΥΣ χρόνους — ό,τι χρειάζεται grid/now-next/catch-up. */
    data class Prog(
        val title: String,
        val desc: String,
        val startMs: Long,
        val stopMs: Long
    )

    private const val CACHE_VERSION = 1
    private const val CACHE_FILE = "epg_index.bin"

    /** tvgId(κανονικοποιημένο) -> προγράμματα, ταξινομημένα κατά startMs. */
    @Volatile private var index: Map<String, List<Prog>> = emptyMap()
    @Volatile private var source: String? = null
    @Volatile private var loadedAt: Long = 0L

    /**
     * Fully parsed EPG candidate that is not visible until [installSnapshot].
     * This lets callers preserve the currently working guide while a replacement
     * is downloading/parsing and discard stale candidates after a source switch.
     */
    data class Snapshot internal constructor(
        internal val programmes: Map<String, List<Prog>>,
        val source: String,
        val loadedAtMs: Long
    )

    private fun key(s: String) = s.trim().lowercase()

    // ---------------------------------------------------------------- API

    fun isLoaded(): Boolean = index.isNotEmpty()
    fun currentSource(): String? = source
    fun loadedAtMs(): Long = loadedAt

    fun clear() {
        index = emptyMap(); source = null; loadedAt = 0L
    }

    /** Πόσα κανάλια έχει το φορτωμένο EPG (για μηνύματα/διαγνωστικά). */
    fun channelCount(): Int = index.size

    /**
     * Κατεβάζει και κάνει parse. ΜΠΛΟΚΑΡΕΙ — κάλεσέ το από Dispatchers.IO.
     * Πετάει exception σε δικτυακό/HTTP σφάλμα (το UI δείχνει το μήνυμα).
     */
    fun load(url: String) {
        installSnapshot(fetchSnapshot(url))
    }

    /** Downloads/parses without mutating the guide currently visible to the UI. */
    fun fetchSnapshot(url: String): Snapshot {
        val u = url.trim()
        if (u.isEmpty()) throw IllegalArgumentException("Κενό URL EPG")
        val parsed = Http.stream(u).use { parse(it) }
        if (parsed.isEmpty()) throw RuntimeException("Το XMLTV δεν περιείχε προγράμματα")
        return Snapshot(parsed, u, System.currentTimeMillis())
    }

    /** Atomic in-memory publication of a fully validated guide candidate. */
    fun installSnapshot(snapshot: Snapshot) {
        index = snapshot.programmes
        source = snapshot.source
        loadedAt = snapshot.loadedAtMs
    }

    /**
     * Τι παίζει ΤΩΡΑ και τι ΜΕΤΑ.
     * Δυαδική αναζήτηση: με 100.000 προγράμματα, γραμμική σάρωση σε κάθε
     * γραμμή λίστας θα κόλλαγε το scrolling.
     */
    fun nowNext(tvgId: String, atMs: Long = System.currentTimeMillis()): Pair<Prog?, Prog?> {
        val list = index[key(tvgId)] ?: return null to null
        val i = indexAt(list, atMs)
        val now = list.getOrNull(i)?.takeIf { atMs in it.startMs until it.stopMs }
        val next = if (now != null) list.getOrNull(i + 1)
        else list.getOrNull(i + 1)?.takeIf { it.startMs >= atMs } ?: list.firstOrNull { it.startMs >= atMs }
        return now to next
    }

    /** Τα επόμενα [n] προγράμματα (μαζί με το τρέχον, αν υπάρχει). */
    fun upcoming(tvgId: String, n: Int, fromMs: Long = System.currentTimeMillis()): List<Prog> {
        val list = index[key(tvgId)] ?: return emptyList()
        val start = indexAt(list, fromMs).coerceAtLeast(0)
        return list.drop(start).filter { it.stopMs > fromMs }.take(n)
    }

    /** Ό,τι πέφτει μέσα σε ένα παράθυρο ωρών — η βάση του EPG grid. */
    fun programmes(tvgId: String, fromMs: Long, toMs: Long): List<Prog> {
        val list = index[key(tvgId)] ?: return emptyList()
        return list.filter { it.stopMs > fromMs && it.startMs < toMs }
    }

    /** Το πρόγραμμα που παίζει σε δεδομένη στιγμή (catch-up: τι έπαιζε τότε). */
    fun progAt(tvgId: String, atMs: Long): Prog? {
        val list = index[key(tvgId)] ?: return null
        return list.getOrNull(indexAt(list, atMs))?.takeIf { atMs in it.startMs until it.stopMs }
    }

    fun hasChannel(tvgId: String): Boolean = index.containsKey(key(tvgId))

    // ------------------------------------------------------- Disk cache

    /** Σώζει το ορατό ευρετήριο. Κάλεσέ το από IO. */
    fun saveCache(ctx: Context) {
        val src = source ?: return
        saveSnapshotCache(ctx, Snapshot(index, src, loadedAt))
    }

    /** Saves an explicit candidate without reading mutable global EPG state. */
    fun saveSnapshotCache(ctx: Context, snapshot: Snapshot) {
        val idx = snapshot.programmes
        if (idx.isEmpty()) return
        runCatching {
            val f = File(ctx.filesDir, CACHE_FILE)
            val tmp = File(ctx.filesDir, "$CACHE_FILE.tmp")
            DataOutputStream(tmp.outputStream().buffered()).use { o ->
                o.writeInt(CACHE_VERSION)
                o.writeUTF(snapshot.source)
                o.writeLong(snapshot.loadedAtMs)
                o.writeInt(idx.size)
                for ((k, list) in idx) {
                    o.writeUTF(k)
                    o.writeInt(list.size)
                    for (p in list) {
                        o.writeUTF(p.title.take(300))
                        o.writeUTF(p.desc.take(1200))   // φράγμα: αλλιώς writeUTF σκάει >64KB
                        o.writeLong(p.startMs); o.writeLong(p.stopMs)
                    }
                }
            }
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        }
    }

    /**
     * Φορτώνει το ευρετήριο από τον δίσκο.
     * @return true αν φορτώθηκε και είναι ακόμα χρήσιμο (όχι πολύ παλιό).
     */
    fun loadCache(ctx: Context, maxAgeMs: Long = 12 * 60 * 60 * 1000L): Boolean {
        val snapshot = readCacheSnapshot(ctx, maxAgeMs) ?: return false
        installSnapshot(snapshot)
        return true
    }

    /** Reads a disk candidate without publishing it to global EPG state. */
    fun readCacheSnapshot(
        ctx: Context,
        maxAgeMs: Long = 12 * 60 * 60 * 1000L
    ): Snapshot? = runCatching {
        val f = File(ctx.filesDir, CACHE_FILE)
        if (!f.exists()) return null
        DataInputStream(f.inputStream().buffered()).use { i ->
            if (i.readInt() != CACHE_VERSION) { f.delete(); return null }
            val src = i.readUTF()
            val at = i.readLong()
            if (System.currentTimeMillis() - at > maxAgeMs) return null
            val n = i.readInt()
            val map = HashMap<String, List<Prog>>(n)
            repeat(n) {
                val k = i.readUTF()
                val cnt = i.readInt()
                val list = ArrayList<Prog>(cnt)
                repeat(cnt) {
                    val t = i.readUTF(); val d = i.readUTF()
                    list.add(Prog(t, d, i.readLong(), i.readLong()))
                }
                map[k] = list
            }
            Snapshot(map, src, at)
        }
    }.getOrNull()

    // ----------------------------------------------------------- internals

    /** Δείκτης του τελευταίου προγράμματος που ξεκινά <= atMs (δυαδική). */
    internal fun indexAt(list: List<Prog>, atMs: Long): Int {
        var lo = 0; var hi = list.size - 1; var res = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (list[mid].startMs <= atMs) { res = mid; lo = mid + 1 } else hi = mid - 1
        }
        return res
    }

    private fun parse(raw: InputStream): Map<String, List<Prog>> {
        // gzip με βάση το ΠΕΡΙΕΧΟΜΕΝΟ, όχι την κατάληξη του URL
        val buf = BufferedInputStream(raw, 1 shl 16)
        buf.mark(2)
        val b0 = buf.read(); val b1 = buf.read()
        buf.reset()
        val stream: InputStream =
            if (b0 == 0x1f && b1 == 0x8b) GZIPInputStream(buf, 1 shl 16) else buf

        val out = HashMap<String, ArrayList<Prog>>()
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(stream, null)

        var ev = p.eventType
        var chId = ""; var start = 0L; var stop = 0L
        var title = ""; var desc = ""
        var inProg = false
        var textTarget = 0        // 1 = title, 2 = desc

        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "programme" -> {
                        inProg = true
                        chId = p.getAttributeValue(null, "channel") ?: ""
                        start = parseXmltvTime(p.getAttributeValue(null, "start"))
                        stop = parseXmltvTime(p.getAttributeValue(null, "stop"))
                        title = ""; desc = ""
                    }
                    "title" -> if (inProg && title.isEmpty()) textTarget = 1
                    "desc" -> if (inProg && desc.isEmpty()) textTarget = 2
                }
                XmlPullParser.TEXT -> if (textTarget != 0) {
                    val t = p.text ?: ""
                    if (textTarget == 1) title += t else desc += t
                }
                XmlPullParser.END_TAG -> when (p.name) {
                    "title", "desc" -> textTarget = 0
                    "programme" -> {
                        // κράτα μόνο έγκυρα: χωρίς χρόνους δεν χρησιμεύουν πουθενά
                        if (inProg && chId.isNotBlank() && start > 0 && stop > start) {
                            out.getOrPut(key(chId)) { ArrayList() }
                                .add(Prog(title.trim(), desc.trim(), start, stop))
                        }
                        inProg = false
                    }
                }
            }
            ev = p.next()
        }
        // ταξινόμηση: η δυαδική αναζήτηση το ΑΠΑΙΤΕΙ — τα XMLTV δεν είναι πάντα σε σειρά
        val res = HashMap<String, List<Prog>>(out.size)
        for ((k, v) in out) res[k] = v.sortedBy { it.startMs }
        return res
    }

    /** XMLTV: "20260718120000 +0300" ή "20260718120000". */
    internal fun parseXmltvTime(s: String?): Long {
        if (s.isNullOrBlank()) return 0L
        val t = s.trim()
        val digits = t.takeWhile { it.isDigit() }
        if (digits.length < 14) return 0L
        return runCatching {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.clear()
            cal.set(
                digits.substring(0, 4).toInt(),
                digits.substring(4, 6).toInt() - 1,
                digits.substring(6, 8).toInt(),
                digits.substring(8, 10).toInt(),
                digits.substring(10, 12).toInt(),
                digits.substring(12, 14).toInt()
            )
            var ms = cal.timeInMillis
            // ζώνη ώρας: "+0300" -> αφαιρείται για να βγει σωστό UTC
            val tz = t.drop(digits.length).trim()
            if (tz.length >= 5 && (tz[0] == '+' || tz[0] == '-')) {
                val off = tz.substring(1, 3).toInt() * 3_600_000L + tz.substring(3, 5).toInt() * 60_000L
                ms += if (tz[0] == '+') -off else off
            }
            ms
        }.getOrDefault(0L)
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   ΑΝ το build πει «Unresolved reference: EpgEntry», σημαίνει ότι το EpgEntry
   ήταν ορισμένο μέσα στο ΠΑΛΙΟ EpgManager.kt. Τότε ξεσχόλιασε το παρακάτω:

data class EpgEntry(
    val title: String,
    val desc: String,
    val start: String,
    val end: String
)
   ───────────────────────────────────────────────────────────────────────── */
