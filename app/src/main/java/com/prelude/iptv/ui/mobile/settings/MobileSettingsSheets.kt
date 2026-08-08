package com.prelude.iptv.ui.mobile.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prelude.iptv.billing.PreludeBilling
import com.prelude.iptv.billing.PremiumTier
import com.prelude.iptv.billing.PurchaseState
import com.prelude.iptv.billing.billingActivity
import com.prelude.iptv.billing.effectivePremiumTier
import com.prelude.iptv.billing.hasQaPremiumOverride
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingRadius
import com.prelude.iptv.ui.components.settings.PremiumSettingsRow
import com.prelude.iptv.ui.components.settings.SettingsSourceUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileSourcesSheet(
    sources: List<SettingsSourceUi>,
    onDismiss: () -> Unit,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onRefresh: () -> Unit,
    onAdd: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.BackgroundRaised,
        contentColor = IptvColors.TextPrimary
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("Οι πηγές μου", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text("Οι κωδικοί παραμένουν κρυφοί στη συσκευή", color = IptvColors.TextTertiary, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            sources.forEach { source ->
                MobileSettingsSourceCard(
                    source = source,
                    onOpen = { onOpen(source.index) },
                    onEdit = { onEdit(source.index) },
                    onDelete = { onDelete(source.index) },
                    onRefresh = if (source.current) onRefresh else null
                )
                Spacer(Modifier.height(9.dp))
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(StreamingRadius.Card),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Add, null)
                Text("  Προσθήκη νέας πηγής", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileAddSourceSheet(
    onDismiss: () -> Unit,
    onSelectType: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.SurfaceRaised,
        contentColor = IptvColors.TextPrimary
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text("Νέα πηγή", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Επίλεξε τον πραγματικό τύπο σύνδεσης", color = IptvColors.TextTertiary, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            SourceTypeButton("M3U playlist", "URL ή τοπικό αρχείο") { onSelectType(0) }
            SourceTypeButton("Xtream Codes", "Server, username και password") { onSelectType(1) }
            SourceTypeButton("Stalker Portal", "Portal URL και MAC address") { onSelectType(2) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePremiumSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { PreludeBilling.repository(context) }
    val billing by repository.state.collectAsStateWithLifecycle()
    val activity = context.billingActivity()
    val qaAccess = hasQaPremiumOverride()
    val premiumActive = effectivePremiumTier(billing.entitlement.tier) == PremiumTier.PREMIUM
    val premiumOffer = billing.offer
    LaunchedEffect(Unit) { repository.start() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.BackgroundRaised,
        contentColor = IptvColors.TextPrimary
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("PRELUDE+ Premium", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(
                when {
                    qaAccess -> "Πλήρης πρόσβαση ιδιοκτήτη · QA build"
                    premiumActive -> "Ενεργό σε αυτή τη συσκευή"
                    else -> "Μία αγορά. Όλες οι προηγμένες δυνατότητες."
                },
                color = if (premiumActive) Color(0xFF5AC98B) else IptvColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (premiumActive) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.height(16.dp))
            listOf(
                "Πολλαπλές πηγές περιεχομένου",
                "Multiview ζωντανών καναλιών",
                "Online αναζήτηση υποτίτλων",
                "Πολλαπλά και προστατευμένα προφίλ",
                "Προτάσεις και εξατομίκευση αρχικής"
            ).forEach { benefit ->
                Text(
                    "•  $benefit",
                    color = IptvColors.TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            billing.message?.let { message ->
                Text(
                    message,
                    color = if (billing.entitlement.state == PurchaseState.PENDING) {
                        Color(0xFFFFC857)
                    } else {
                        IptvColors.TextSecondary
                    },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = { activity?.let(repository::launchPremiumPurchase) },
                enabled = !premiumActive && !billing.working && activity != null && premiumOffer != null,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(StreamingRadius.Card),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = IptvColors.SurfaceRaised,
                    disabledContentColor = IptvColors.TextTertiary,
                ),
            ) {
                if (billing.working) {
                    CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(
                        when {
                            qaAccess -> "QA · Όλα ξεκλειδωμένα"
                            premiumActive -> "Premium ενεργό"
                            premiumOffer != null -> "Αγορά ${premiumOffer.formattedPrice}"
                            else -> "Μη διαθέσιμο στο Google Play"
                        },
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick = repository::restorePurchases,
                enabled = !billing.working && !qaAccess,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(StreamingRadius.Card),
            ) {
                Text("Επαναφορά αγορών", fontWeight = FontWeight.Bold)
            }
            Text(
                "Η αγορά και η επαναφορά γίνονται αποκλειστικά μέσω Google Play.",
                color = IptvColors.TextTertiary,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 9.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileInfoSheet(info: MobileSettingsInfo, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.BackgroundRaised,
        contentColor = IptvColors.TextPrimary
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(info.title, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text(info.body, color = IptvColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SourceTypeButton(title: String, subtitle: String, onClick: () -> Unit) {
    PremiumSettingsRow(
        title = title,
        subtitle = subtitle,
        icon = Icons.Default.Add,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 5.dp)
    )
}

internal enum class MobileSettingsInfo(val title: String, val body: String) {
    Help(
        "Κέντρο βοήθειας",
        "Διαχειρίσου τις πηγές σου από την πρώτη ενότητα. Για προβλήματα αναπαραγωγής δοκίμασε πρώτα την αυτόματη επιλογή player και έλεγξε ότι η πηγή σου λειτουργεί."
    )
}
