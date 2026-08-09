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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.localization.explanationRes
import com.prelude.iptv.ui.localization.localizedText
import com.prelude.iptv.ui.localization.titleRes

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
                stringResource(feature.titleRes()),
                fontWeight = FontWeight.Black,
                fontSize = 19.sp
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(feature.explanationRes()), fontSize = 14.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(14.dp))
                when {
                    unavailable -> Text(
                        stringResource(R.string.billing_purchases_unavailable_device),
                        color = IptvColors.TextTertiary,
                        fontSize = 13.sp
                    )
                    // Η τιμή έρχεται από το Play, όχι από εμάς: αλλιώς θα δείχναμε
                    // λάθος νόμισμα σε κάθε χώρα εκτός της δικής μας.
                    state.offer != null -> Text(
                        stringResource(
                            R.string.billing_offer_once,
                            state.offer?.formattedPrice.orEmpty(),
                        ),
                        color = IptvColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    else -> Text(
                        stringResource(R.string.billing_connecting_to_play),
                        color = IptvColors.TextTertiary,
                        fontSize = 13.sp
                    )
                }
                state.message?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(message.localizedText(), color = IptvColors.Error, fontSize = 13.sp)
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
                    stringResource(
                        if (state.working) R.string.billing_waiting else R.string.billing_get_prelude_plus
                    ),
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
                    Text(stringResource(R.string.billing_restore_purchase), color = IptvColors.TextSecondary)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.billing_not_now), color = IptvColors.TextTertiary)
                }
            }
        }
    )
}
