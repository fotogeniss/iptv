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
    private val iso639Aliases = mapOf(
        "ell" to "el", "gre" to "el", "eng" to "en",
        "deu" to "de", "ger" to "de", "fra" to "fr", "fre" to "fr",
        "ita" to "it", "spa" to "es", "por" to "pt", "rus" to "ru",
        "tur" to "tr", "bul" to "bg", "ron" to "ro", "rum" to "ro",
        "srp" to "sr", "ara" to "ar", "nld" to "nl", "dut" to "nl",
        "pol" to "pl", "hin" to "hi", "jpn" to "ja", "zho" to "zh",
        "chi" to "zh",
    )

    /**
     * Όνομα γλώσσας στο ενεργό locale, ή κενό αν δεν αναγνωρίζεται.
     *
     * Το «und» (undetermined) δεν είναι γλώσσα — είναι δήλωση άγνοιας, και δεν
     * πρέπει να εμφανίζεται ως επιλογή με όνομα.
     */
    fun languageName(code: String?, displayLocale: Locale): String {
        val normalized = code?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (normalized.isBlank() || normalized == "und") return ""
        val base = normalized.substringBefore('-').substringBefore('_')
        val language = iso639Aliases[base] ?: base
        if (language.length != 2) return base.uppercase(Locale.ROOT)
        return Locale.forLanguageTag(language).getDisplayLanguage(displayLocale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString() }
    }

    /**
     * Η ετικέτα που βλέπει ο χρήστης σε μενού ήχου ή υποτίτλων.
     *
     * Προτεραιότητα στη ΓΛΩΣΣΑ, γιατί αυτό ψάχνει ο άνθρωπος. Το label του
     * παρόχου έρχεται δεύτερο και μόνο αν προσθέτει κάτι — συχνά επαναλαμβάνει τη
     * γλώσσα («Greek»), και δύο φορές το ίδιο δεν βοηθά κανέναν.
     *
     * @param fallbackLabel resource-owned label for tracks without metadata.
     */
    fun trackLabel(
        language: String?,
        label: String?,
        fallbackLabel: String,
        displayLocale: Locale,
    ): String {
        val name = languageName(language, displayLocale)
        val extra = label?.trim().orEmpty().takeIf { candidate ->
            candidate.isNotBlank() && !candidate.equals(name, ignoreCase = true) &&
                !candidate.equals(language?.trim(), ignoreCase = true)
        }
        return when {
            name.isNotBlank() && extra != null -> "$name · $extra"
            name.isNotBlank() -> name
            extra != null -> extra
            else -> fallbackLabel
        }
    }
}
