package com.prelude.iptv.data

import java.text.Normalizer

/**
 * Ταίριασμα ελληνικών τίτλων γραμμένων με λατινικούς χαρακτήρες («greeklish»).
 *
 * ΤΟ ΠΡΟΒΛΗΜΑ: οι ελληνικές λίστες γράφουν τη «Χαρά» άλλοτε `Xara`, άλλοτε
 * `Hara`, άλλοτε `Chara`· το «η» άλλοτε `i`, άλλοτε `h`· το «θ» άλλοτε `th`,
 * άλλοτε `8`. Το TMDB γνωρίζει μόνο τον ελληνικό τίτλο, οπότε δεν έβρισκε
 * τίποτα και η εφαρμογή έπεφτε στη γενική περίληψη της σειράς αντί για την
 * περίληψη του κάθε επεισοδίου.
 *
 * ΤΟ ΚΛΕΙΔΙ ΤΗΣ ΛΥΣΗΣ: η μεταγραφή greeklish -> ελληνικά είναι ΑΜΦΙΣΗΜΗ (το
 * `i` μπορεί να είναι ι, η, υ, ει ή οι), ενώ η αντίστροφη κατεύθυνση είναι
 * σχεδόν ντετερμινιστική. Γι' αυτό δεν προσπαθούμε να μαντέψουμε τον ακριβή
 * τίτλο και να τον εμπιστευτούμε: παράγουμε ΕΝΑ εύλογο ελληνικό ερώτημα με το
 * [toGreek] για να πάρουμε υποψήφιες σειρές, και μετά ΕΠΑΛΗΘΕΥΟΥΜΕ ποια είναι
 * η σωστή συγκρίνοντας [latinSkeleton]. Ο σκελετός ισοπεδώνει ακριβώς τις
 * διαφορές που χωρίζουν τις συμβάσεις greeklish, οπότε και οι δύο γραφές
 * καταλήγουν στην ίδια συμβολοσειρά.
 *
 * ΔΕΝ ΕΙΝΑΙ ΜΕΤΑΦΡΑΣΗ ΚΑΙ ΔΕΝ ΕΜΦΑΝΙΖΕΤΑΙ ΠΟΤΕ. Τα αποτελέσματα εδώ
 * χρησιμοποιούνται μόνο ως ερώτημα αναζήτησης και ως κλειδί σύγκρισης. Ο
 * τίτλος που βλέπει ο χρήστης παραμένει αυτός που έδωσε ο πάροχος — δες τον
 * κανόνα για τα δεδομένα παρόχου στο `docs/MAINTENANCE.md`.
 */
object GreeklishTitlePolicy {

    private val VOWELS = "aeiouyw".toSet()

    /**
     * Λέξεις-εργαλεία με ΓΝΩΣΤΗ απόδοση.
     *
     * Γράμμα-προς-γράμμα το `tis` δίνει «τις», ενώ σχεδόν πάντα είναι «της».
     * Τα άρθρα και οι αντωνυμίες είναι μεγάλο μέρος ενός τίτλου και πεπερασμένο
     * σύνολο, οπότε ένα μικρό λεξικό ανεβάζει πολύ την ακρίβεια του ερωτήματος.
     */
    private val FUNCTION_WORDS = mapOf(
        "o" to "ο", "h" to "η", "i" to "η", "to" to "το", "ta" to "τα",
        "oi" to "οι", "ths" to "της", "tis" to "της", "tou" to "του",
        "ton" to "τον", "thn" to "την", "tin" to "την", "ti" to "τη",
        "th" to "τη", "twn" to "των", "ton2" to "των", "tous" to "τους",
        "sto" to "στο", "sth" to "στη", "sti" to "στη", "stou" to "στου",
        "stis" to "στις", "stous" to "στους", "sta" to "στα", "ston" to "στον",
        "kai" to "και", "ki" to "κι", "me" to "με", "se" to "σε", "gia" to "για",
        "apo" to "από", "mou" to "μου", "sou" to "σου", "mas" to "μας",
        "sas" to "σας", "enas" to "ένας", "mia" to "μια", "ena" to "ένα",
        "den" to "δεν", "min" to "μην", "mhn" to "μην", "pou" to "που",
        "pws" to "πως", "pos" to "πως", "otan" to "όταν", "an" to "αν",
        "na" to "να", "tha" to "θα", "8a" to "θα", "ola" to "όλα",
        "oloi" to "όλοι", "oles" to "όλες", "san" to "σαν", "xwris" to "χωρίς",
        "xoris" to "χωρίς", "meta" to "μετά", "prin" to "πριν",
    )

    // ---------------------------------------------------------------- σκελετός

    private val GREEK_DIGRAPHS = listOf(
        "ου" to "u", "αι" to "e", "ει" to "i", "οι" to "i", "υι" to "i",
        "αυ" to "av", "ευ" to "ev", "μπ" to "b", "ντ" to "d", "γκ" to "g",
        "γγ" to "g", "τσ" to "ts", "τζ" to "tz",
    )

    private val GREEK_LETTERS = mapOf(
        'α' to "a", 'β' to "v", 'γ' to "g", 'δ' to "d", 'ε' to "e", 'ζ' to "z",
        'η' to "i", 'θ' to "t", 'ι' to "i", 'κ' to "k", 'λ' to "l", 'μ' to "m",
        'ν' to "n", 'ξ' to "ks", 'ο' to "o", 'π' to "p", 'ρ' to "r", 'σ' to "s",
        'ς' to "s", 'τ' to "t", 'υ' to "i", 'φ' to "f", 'χ' to "h", 'ψ' to "ps",
        'ω' to "o",
    )

    /**
     * Δίψηφα με `h` που ισχύουν ΜΟΝΟ όταν ακολουθεί φωνήεν.
     *
     * Το `ths` είναι «της» (t + η + ς) και όχι «θς»: χωρίς αυτόν τον έλεγχο το
     * `th` καταβρόχθιζε το «η» και ο σκελετός δεν ταίριαζε ποτέ σε λίστες που
     * γράφουν το «η» ως `h`.
     */
    private val H_DIGRAPHS = listOf("th" to "t", "ch" to "h", "kh" to "h", "ph" to "f")

    private val LATIN_DIGRAPHS = listOf(
        "gk" to "g", "gg" to "g", "mp" to "b", "nt" to "d",
        "ou" to "u", "ai" to "e", "ei" to "i", "oi" to "i", "au" to "av", "eu" to "ev",
    )

    private val LATIN_LETTERS = mapOf(
        'a' to "a", 'b' to "v", 'c' to "k", 'd' to "d", 'e' to "e", 'f' to "f",
        'g' to "g", 'i' to "i", 'j' to "i", 'k' to "k", 'l' to "l", 'm' to "m",
        'n' to "n", 'o' to "o", 'p' to "p", 'q' to "k", 'r' to "r", 's' to "s",
        't' to "t", 'u' to "u", 'v' to "v", 'w' to "o", 'x' to "h", 'y' to "i",
        'z' to "z", '8' to "t", '9' to "t", '3' to "ks", '0' to "o",
    )

    /**
     * Κοινή, «χοντρική» λατινική μορφή στην οποία καταλήγουν και ο ελληνικός
     * τίτλος και οποιαδήποτε γραφή του σε greeklish.
     *
     * Οι συγχωνεύσεις είναι σκόπιμα επιθετικές — θ/τ, β/μπ, η/ι/υ/ει/οι, ο/ω —
     * γιατί σκοπός δεν είναι να ανακατασκευαστεί ο τίτλος αλλά να αναγνωριστεί
     * η ίδια σειρά ανεξάρτητα από τη σύμβαση γραφής. Επιστρέφει κενό όταν δεν
     * μένει τίποτα συγκρίσιμο, ώστε ο καλών να μη συγκρίνει κενό με κενό.
     */
    fun latinSkeleton(title: String): String {
        val plain = stripAccents(title.lowercase()).replace(Regex("""[^\p{L}\p{N}]+"""), " ")
        val hasGreek = plain.any { it in 'α'..'ω' }
        val mapped = if (hasGreek) mapGreek(plain) else mapLatin(plain)
        return mapped
            .replace("b", "v")
            .replace(Regex("""(.)\1+"""), "$1")
            .replace(Regex("""[^a-z]"""), "")
    }

    private fun mapGreek(text: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val digraph = GREEK_DIGRAPHS.firstOrNull { text.startsWith(it.first, i) }
            if (digraph != null) {
                out.append(digraph.second)
                i += digraph.first.length
            } else {
                out.append(GREEK_LETTERS[text[i]].orEmpty())
                i++
            }
        }
        return out.toString()
    }

    private fun mapLatin(text: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val afterDigraph = text.getOrNull(i + 2) ?: ' '
            val hDigraph = H_DIGRAPHS.firstOrNull {
                text.startsWith(it.first, i) && afterDigraph in VOWELS
            }
            if (hDigraph != null) {
                out.append(hDigraph.second)
                i += 2
                continue
            }
            val digraph = LATIN_DIGRAPHS.firstOrNull { text.startsWith(it.first, i) }
            if (digraph != null) {
                out.append(digraph.second)
                i += digraph.first.length
                continue
            }
            val ch = text[i]
            if (ch == 'h') {
                // «χ» όταν ακολουθεί φωνήεν, αλλιώς είναι το «η».
                out.append(if ((text.getOrNull(i + 1) ?: ' ') in VOWELS) "h" else "i")
            } else {
                out.append(LATIN_LETTERS[ch].orEmpty())
            }
            i++
        }
        return out.toString()
    }

    // --------------------------------------------------------------- ερώτημα

    private val QUERY_DIGRAPHS = listOf(
        "ou" to "ου", "ai" to "αι", "ei" to "ει", "oi" to "οι", "au" to "αυ",
        "eu" to "ευ", "ps" to "ψ", "ks" to "ξ", "gk" to "γκ", "gg" to "γγ",
        "mp" to "μπ", "nt" to "ντ", "ts" to "τσ", "tz" to "τζ",
    )

    private val QUERY_LETTERS = mapOf(
        'a' to "α", 'b' to "μπ", 'c' to "κ", 'd' to "δ", 'e' to "ε", 'f' to "φ",
        'g' to "γ", 'i' to "ι", 'j' to "ι", 'k' to "κ", 'l' to "λ", 'm' to "μ",
        'n' to "ν", 'o' to "ο", 'p' to "π", 'q' to "κ", 'r' to "ρ", 's' to "σ",
        't' to "τ", 'u' to "ου", 'v' to "β", 'w' to "ω", 'x' to "χ", 'y' to "υ",
        'z' to "ζ", '8' to "θ", '9' to "θ", '3' to "ξ",
    )

    /**
     * Ένα εύλογο ελληνικό ερώτημα αναζήτησης για έναν τίτλο σε greeklish.
     *
     * Δεν είναι —και δεν χρειάζεται να είναι— ο ακριβής τίτλος: οι τόνοι
     * λείπουν και κάποιες καταλήξεις πέφτουν έξω. Αρκεί να φέρει τη σωστή
     * σειρά στα αποτελέσματα· ποια είναι η σωστή το κρίνει ο [latinSkeleton].
     */
    fun toGreek(title: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < title.length) {
            val ch = title[i]
            if (!ch.isLetterOrDigit()) {
                out.append(ch)
                i++
                continue
            }
            var end = i
            while (end < title.length && title[end].isLetterOrDigit()) end++
            val word = title.substring(i, end)
            val known = FUNCTION_WORDS[word.lowercase()]
            out.append(known ?: transliterateWord(word.lowercase()))
            i = end
        }
        return out.toString().trim()
    }

    private fun transliterateWord(word: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < word.length) {
            val afterDigraph = word.getOrNull(i + 2) ?: ' '
            if (word.startsWith("th", i) && afterDigraph in VOWELS) {
                out.append("θ"); i += 2; continue
            }
            if (word.startsWith("ch", i) && afterDigraph in VOWELS) {
                out.append("χ"); i += 2; continue
            }
            val digraph = QUERY_DIGRAPHS.firstOrNull { word.startsWith(it.first, i) }
            if (digraph != null) {
                out.append(digraph.second)
                i += digraph.first.length
                continue
            }
            val ch = word[i]
            if (ch == 'h') {
                out.append(if ((word.getOrNull(i + 1) ?: ' ') in VOWELS) "χ" else "η")
            } else {
                out.append(QUERY_LETTERS[ch] ?: ch.toString())
            }
            i++
        }
        val result = out.toString()
        // Τελικό σίγμα: το TMDB ταιριάζει καλύτερα με σωστή ορθογραφία.
        return if (result.endsWith("σ")) result.dropLast(1) + "ς" else result
    }

    // --------------------------------------------------------------- ανίχνευση

    /** Λέξεις που δεν υπάρχουν σε ελληνικό τίτλο γραμμένο greeklish. */
    private val ENGLISH_MARKERS = setOf(
        "the", "of", "and", "a", "an", "in", "on", "at", "for", "with", "from",
        "my", "your", "his", "her", "our", "their", "is", "are", "was", "were",
        "be", "not", "no", "all", "how", "why", "what", "who", "when", "where",
        "season", "episode", "part", "vol", "series", "show",
    )

    /**
     * Αξίζει να δοκιμάσουμε ελληνικό ερώτημα για αυτόν τον τίτλο;
     *
     * Η λογική είναι «απόκλεισε ό,τι είναι προφανώς αγγλικό, δοκίμασε τα
     * υπόλοιπα» και όχι «αναγνώρισε το greeklish». Μια λίστα ελληνικών σειρών
     * γράφει συχνά τίτλους χωρίς καμία λέξη-εργαλείο —«Agries Melisses»— οπότε
     * ένας αυστηρός ανιχνευτής θα τους έχανε ακριβώς εκεί που χρειάζονται.
     *
     * Το κόστος ενός λανθασμένου «ναι» είναι μικρό: ο ελληνικός υποψήφιος
     * μπαίνει ΤΕΛΕΥΤΑΙΟΣ στη σειρά αναζήτησης, και η αναζήτηση σταματά στο
     * πρώτο αποτέλεσμα — άρα οι επιπλέον κλήσεις γίνονται μόνο για τίτλους που
     * ούτως ή άλλως δεν βρίσκονταν με κανέναν άλλο τρόπο.
     */
    fun looksGreeklish(title: String): Boolean {
        val plain = stripAccents(title.lowercase())
        if (plain.any { it in 'α'..'ω' }) return false
        val words = plain.split(Regex("""[^a-z0-9]+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        if (words.any { it in ENGLISH_MARKERS }) return false
        // Χρειάζεται τουλάχιστον μία πραγματική λέξη: ένα σκέτο «2024» δεν
        // είναι τίτλος προς μεταγραφή.
        return words.any { word -> word.length >= 2 && word.any(Char::isLetter) }
    }

    private fun stripAccents(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
}
