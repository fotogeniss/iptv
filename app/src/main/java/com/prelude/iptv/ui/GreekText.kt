package com.prelude.iptv.ui

import java.text.Normalizer

/**
 * Κεφαλαία με σωστή ελληνική ορθογραφία.
 *
 * Το `String.uppercase()` της Kotlin μετατρέπει «ά» σε «Ά» και «Ξένες Ταινίες»
 * σε «ΞΈΝΕΣ ΤΑΙΝΊΕΣ». Στα ελληνικά όμως **τα κεφαλαία δεν τονίζονται** — η μόνη
 * εξαίρεση είναι το διαλυτικό (Ϊ, Ϋ), που πρέπει να διατηρηθεί.
 *
 * Η υλοποίηση αποσυνθέτει τους χαρακτήρες (NFD), αφαιρεί ΜΟΝΟ τον τόνο
 * (U+0301) αφήνοντας το διαλυτικό (U+0308) ανέπαφο, και ξανασυνθέτει.
 *
 * Το τελικό «ς» δεν μας απασχολεί: γίνεται «Σ» σωστά από το uppercase().
 */
fun String.greekUppercase(): String {
    val upper = uppercase()
    val decomposed = Normalizer.normalize(upper, Normalizer.Form.NFD)
    val stripped = buildString(decomposed.length) {
        for (ch in decomposed) {
            // U+0301 = οξεία. Το U+0308 (διαλυτικά) μένει: ΑΫΠΝΙΑ, ΠΡΟΪΟΝ.
            if (ch != '́') append(ch)
        }
    }
    return Normalizer.normalize(stripped, Normalizer.Form.NFC)
}
