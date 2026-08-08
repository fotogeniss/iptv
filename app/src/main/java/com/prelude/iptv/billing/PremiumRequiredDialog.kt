package com.prelude.iptv.billing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors

/**
 * Τι λέει η εφαρμογή όταν ο χρήστης ζητά κάτι κλειδωμένο.
 *
 * ΓΙΑΤΙ ΥΠΑΡΧΕΙ: μέχρι τώρα το «Επεξεργασία αρχικής» άνοιγε `editing = true` και
 * μετά ένα `if (editing && canEditHome)` το κατάπινε. Ο δωρεάν χρήστης πατούσε και
 * **δεν συνέβαινε τίποτα** — ούτε οθόνη, ούτε μήνυμα, ούτε προσφορά.
 *
 * Είναι το χειρότερο δυνατό αποτέλεσμα: ο χρήστης νομίζει ότι η εφαρμογή είναι
 * χαλασμένη, τη στιγμή ακριβώς που δήλωνε ότι θέλει τη δυνατότητα. Ένα κλείδωμα
 * που δεν εξηγείται δεν πουλά· απλώς εκνευρίζει.
 *
 * ΓΙΑΤΙ ΚΟΙΝΟΣ ΚΑΙ ΟΧΙ ΑΝΑ ΟΘΟΝΗ: υπάρχουν επτά [PremiumFeature]. Επτά διάλογοι
 * σημαίνει επτά διαφορετικά κείμενα, επτά φορές ξεχασμένη η «Επαναφορά αγοράς»,
 * και επτά σημεία να διορθωθούν όταν αλλάξει η τιμή.
 */
@Composable
fun PremiumRequiredDialog(
    feature: PremiumFeature,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        PreludeBilling.repository(context.applicationContext)
    }
    val state by repository.state.collectAsState()

    val unavailable = state.connection == BillingConnectionState.UNAVAILABLE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                featureTitle(feature),
                fontWeight = FontWeight.Black,
                fontSize = 19.sp
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(featureExplanation(feature), fontSize = 14.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(14.dp))
                when {
                    unavailable -> Text(
                        "Οι αγορές δεν είναι διαθέσιμες σε αυτή τη συσκευή.",
                        color = IptvColors.TextTertiary,
                        fontSize = 13.sp
                    )
                    // Η τιμή έρχεται από το Play, όχι από εμάς: αλλιώς θα δείχναμε
                    // λάθος νόμισμα σε κάθε χώρα εκτός της δικής μας.
                    state.offer != null -> Text(
                        "Prelude+ · ${state.offer?.formattedPrice} μία φορά",
                        color = IptvColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    else -> Text(
                        "Γίνεται σύνδεση με το Google Play…",
                        color = IptvColors.TextTertiary,
                        fontSize = 13.sp
                    )
                }
                state.message?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = IptvColors.Error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                // Ενεργό μόνο όταν υπάρχει πραγματική προσφορά. Ένα κουμπί αγοράς
                // που δεν μπορεί να αγοράσει είναι το ίδιο πρόβλημα σε άλλη θέση.
                enabled = state.offer != null && !state.working,
                onClick = {
                    context.billingActivity()?.let(repository::launchPremiumPurchase)
                }
            ) {
                Text(
                    if (state.working) "Αναμονή…" else "Απόκτησε το Prelude+",
                    color = IptvColors.Primary,
                    fontWeight = FontWeight.Black
                )
            }
        },
        dismissButton = {
            Column {
                // ΠΑΝΤΑ ΟΡΑΤΗ, ΑΚΟΜΗ ΚΑΙ ΧΩΡΙΣ ΠΡΟΣΦΟΡΑ: ο χρήστης που άλλαξε
                // συσκευή ή έκανε επανεγκατάσταση ΕΧΕΙ ήδη πληρώσει. Το να πρέπει
                // να ξαναπληρώσει επειδή δεν βρήκε το κουμπί είναι ό,τι χειρότερο.
                TextButton(
                    enabled = !state.working,
                    onClick = { repository.restorePurchases() }
                ) {
                    Text("Επαναφορά αγοράς", color = IptvColors.TextSecondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("Όχι τώρα", color = IptvColors.TextTertiary)
                }
            }
        }
    )
}

/**
 * Τα κείμενα ζουν εδώ και όχι στα σημεία κλήσης.
 *
 * Είναι η υπόσχεση που δίνουμε για κάθε δυνατότητα. Σκόρπια, θα έλεγαν
 * διαφορετικά πράγματα για το ίδιο πράγμα ανάλογα με το ποιος τα έγραψε.
 */
private fun featureTitle(feature: PremiumFeature): String = when (feature) {
    PremiumFeature.EDIT_HOME -> "Φτιάξε την αρχική σου"
    PremiumFeature.SUGGESTIONS -> "Προτάσεις για σένα"
    PremiumFeature.PROFILES -> "Πολλαπλά προφίλ"
    PremiumFeature.MULTIVIEW -> "Δύο κανάλια μαζί"
    PremiumFeature.SUBTITLES_ONLINE -> "Υπότιτλοι από το διαδίκτυο"
    PremiumFeature.MULTIPLE_SOURCES -> "Περισσότερες από μία λίστες"
    PremiumFeature.BACKUP -> "Αντίγραφα ασφαλείας"
}

private fun featureExplanation(feature: PremiumFeature): String = when (feature) {
    PremiumFeature.EDIT_HOME ->
        "Άλλαξε τη σειρά των ενοτήτων, κρύψε όσες δεν θέλεις και διάλεξε ποια " +
            "κατηγορία δείχνει κάθε σειρά."
    PremiumFeature.SUGGESTIONS ->
        "Προτάσεις με βάση όσα βλέπεις και όσα έχεις στα αγαπημένα."
    PremiumFeature.PROFILES ->
        "Ξεχωριστά αγαπημένα, ιστορικό και γονικός έλεγχος για κάθε άτομο του σπιτιού."
    PremiumFeature.MULTIVIEW ->
        "Δύο κανάλια στην ίδια οθόνη — για όταν παίζουν δύο αγώνες μαζί."
    PremiumFeature.SUBTITLES_ONLINE ->
        "Αυτόματη λήψη και χειροκίνητη αναζήτηση υποτίτλων από το OpenSubtitles."
    PremiumFeature.MULTIPLE_SOURCES ->
        "Κράτα περισσότερες από μία λίστες και άλλαξε ανάμεσά τους."
    PremiumFeature.BACKUP ->
        "Εξαγωγή και επαναφορά των δεδομένων σου."
}
