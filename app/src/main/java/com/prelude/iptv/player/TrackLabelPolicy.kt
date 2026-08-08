package com.prelude.iptv.player

import java.util.Locale

/**
 * Μετατρέπει τα στοιχεία ενός κομματιού ήχου/υποτίτλων σε ετικέτα για άνθρωπο.
 *
 * ΓΙΑΤΙ ΧΡΕΙΑΖΕΤΑΙ: ο player δίνει κωδικό γλώσσας («ell», «gre», «el») και ένα
 * προαιρετικό `label` που ο πάροχος γεμίζει όπως θέλει — συχνά καθόλου. Το μενού
 * έδειχνε «Κομμάτι 1, Κομμάτι 2» και έπρεπε να τα δοκιμάσεις ένα-ένα για να βρεις
 * ποιο είναι στα ελληνικά.
 *
 * Καθαρό και δοκιμασμένο, γιατί οι παραλλαγές των κωδικών είναι ακριβώς το είδος
 * λεπτομέρειας που φαίνεται μόνο πάνω σε πραγματική ροή.
 */
object TrackLabelPolicy {

    /**
     * Τα ελληνικά έχουν ΤΡΕΙΣ κωδικούς σε χρήση: «el» (ISO 639-1), «ell» (639-2/T)
     * και «gre» (639-2/B). Οι πάροχοι χρησιμοποιούν και τους τρεις, συχνά στην
     * ίδια λίστα. Το ίδιο ισχύει για γερμανικά, γαλλικά και ολλανδικά.
     */
    private val names = mapOf(
        "el" to "Ελληνικά", "ell" to "Ελληνικά", "gre" to "Ελληνικά",
        "en" to "Αγγλικά", "eng" to "Αγγλικά",
        "de" to "Γερμανικά", "deu" to "Γερμανικά", "ger" to "Γερμανικά",
        "fr" to "Γαλλικά", "fra" to "Γαλλικά", "fre" to "Γαλλικά",
        "it" to "Ιταλικά", "ita" to "Ιταλικά",
        "es" to "Ισπανικά", "spa" to "Ισπανικά",
        "pt" to "Πορτογαλικά", "por" to "Πορτογαλικά",
        "ru" to "Ρωσικά", "rus" to "Ρωσικά",
        "tr" to "Τουρκικά", "tur" to "Τουρκικά",
        "bg" to "Βουλγαρικά", "bul" to "Βουλγαρικά",
        "ro" to "Ρουμανικά", "ron" to "Ρουμανικά", "rum" to "Ρουμανικά",
        "sr" to "Σερβικά", "srp" to "Σερβικά",
        "ar" to "Αραβικά", "ara" to "Αραβικά",
        "nl" to "Ολλανδικά", "nld" to "Ολλανδικά", "dut" to "Ολλανδικά",
        "pl" to "Πολωνικά", "pol" to "Πολωνικά",
        "hi" to "Χίντι", "hin" to "Χίντι",
        "ja" to "Ιαπωνικά", "jpn" to "Ιαπωνικά",
        "zh" to "Κινέζικα", "zho" to "Κινέζικα", "chi" to "Κινέζικα",
    )

    /**
     * Όνομα γλώσσας στα ελληνικά, ή κενό αν δεν αναγνωρίζεται.
     *
     * Το «und» (undetermined) δεν είναι γλώσσα — είναι δήλωση άγνοιας, και δεν
     * πρέπει να εμφανίζεται ως επιλογή με όνομα.
     */
    fun languageName(code: String?): String {
        val normalized = code?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (normalized.isBlank() || normalized == "und") return ""
        // Κωδικοί τύπου «el-GR» ή «pt_BR»: κρατάμε το πρώτο σκέλος.
        val base = normalized.substringBefore('-').substringBefore('_')
        return names[base] ?: base.uppercase(Locale.ROOT)
    }

    /**
     * Η ετικέτα που βλέπει ο χρήστης σε μενού ήχου ή υποτίτλων.
     *
     * Προτεραιότητα στη ΓΛΩΣΣΑ, γιατί αυτό ψάχνει ο άνθρωπος. Το label του
     * παρόχου έρχεται δεύτερο και μόνο αν προσθέτει κάτι — συχνά επαναλαμβάνει τη
     * γλώσσα («Greek»), και δύο φορές το ίδιο δεν βοηθά κανέναν.
     *
     * @param fallbackIndex 1-based, για κομμάτια χωρίς καμία πληροφορία.
     */
    fun trackLabel(language: String?, label: String?, fallbackIndex: Int): String {
        val name = languageName(language)
        val extra = label?.trim().orEmpty().takeIf { candidate ->
            candidate.isNotBlank() && !candidate.equals(name, ignoreCase = true) &&
                !candidate.equals(language?.trim(), ignoreCase = true)
        }
        return when {
            name.isNotBlank() && extra != null -> "$name · $extra"
            name.isNotBlank() -> name
            extra != null -> extra
            else -> "Κομμάτι $fallbackIndex"
        }
    }
}
