package com.prelude.iptv.ui.mobile.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.billing.PremiumPolicy
import com.prelude.iptv.billing.hasQaPremiumOverride
import com.prelude.iptv.billing.rememberPremiumTier
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.mobile.navigation.MobileSettingsAction

/**
 * Η κεφαλίδα της αρχικής κινητού: σήμανση, λογότυπο, «Γρήγορα».
 *
 * ΔΙΑΦΑΝΗΣ ΣΤΗΝ ΚΟΡΥΦΗ, ΣΥΜΠΑΓΗΣ ΜΕΤΑ: πάνω από την εικόνα του hero μια αδιαφανής
 * μπάρα κόβει τη φωτογραφία στα δύο. Όταν όμως έχεις κατέβει, από πίσω περνούν
 * αφίσες και το κείμενο γίνεται δυσανάγνωστο. Γι' αυτό αλλάζει με το scroll και
 * όχι μια για πάντα.
 */
@Composable
internal fun MobileHomeHeader(
    /** true όταν έχει φύγει το hero από την οθόνη — τότε γίνεται συμπαγής. */
    solid: Boolean,
    onUpdateContents: () -> Unit,
    onEditHome: () -> Unit,
    onCategories: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (solid) IptvColors.Background.copy(alpha = .96f) else Color.Transparent,
        tween(220), label = "headerBg"
    )
    Row(
        modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            // Δεν είναι διακοσμητικό: γράφει «ΔΩΡΕΑΝ» σε όποιον δεν έχει αγοράσει.
            //
            // Το «· QA» μπαίνει όταν το premium προέρχεται από το BuildConfig και
            // όχι από αγορά. Χωρίς αυτό, δοκιμάζοντας δεν ξεχωρίζεις αν κάτι
            // δουλεύει επειδή το πλήρωσες ή επειδή η σημαία το ανοίγει — και τα
            // κλειδώματα δεν δοκιμάζονται ποτέ στα σοβαρά.
            PremiumPolicy.label(rememberPremiumTier()) +
                if (hasQaPremiumOverride()) " · QA" else "",
            color = IptvColors.TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(5.dp).clip(CircleShape).background(IptvColors.Primary)
            )
            Spacer(Modifier.width(5.dp))
            Icon(
                Icons.Default.PlayArrow, null,
                tint = IptvColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "PRELUDE+",
                color = IptvColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .5.sp
            )
        }
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickMenu(
                onUpdateContents = onUpdateContents,
                onEditHome = onEditHome,
                onCategories = onCategories,
                onExport = onExport
            )
            Spacer(Modifier.width(4.dp))
            MobileSettingsAction(onClick = onSettings, modifier = Modifier.size(38.dp))
        }
    }
}

/**
 * Το «Γρήγορα ⌄» με τις ενέργειες που δεν χωρούν σε κουμπί.
 *
 * Κρατά μόνο του το αν είναι ανοιχτό: κανείς άλλος δεν χρειάζεται να το ξέρει,
 * και η αρχική έχει ήδη αρκετή κατάσταση να θυμάται.
 */
@Composable
private fun QuickMenu(
    onUpdateContents: () -> Unit,
    onEditHome: () -> Unit,
    onCategories: () -> Unit,
    onExport: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (open) 180f else 0f, tween(180), label = "quickArrow")
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(99.dp))
                .clickable { open = true }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Γρήγορα",
                color = IptvColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.KeyboardArrowDown, null,
                tint = IptvColors.TextPrimary,
                modifier = Modifier.size(18.dp).rotate(arrow)
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = IptvColors.Surface
        ) {
            QuickItem("Ανανέωση περιεχομένου", Icons.Default.Refresh) { open = false; onUpdateContents() }
            QuickItem("Επεξεργασία αρχικής", Icons.Default.Tune) { open = false; onEditHome() }
            QuickItem("Κατηγορίες / Ομάδες", Icons.Default.Category) { open = false; onCategories() }
            QuickItem("Εξαγωγή λίστας", Icons.Default.IosShare) { open = false; onExport() }
        }
    }
}

@Composable
private fun QuickItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(label, color = IptvColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        },
        trailingIcon = { Icon(icon, null, tint = IptvColors.TextSecondary) },
        onClick = onClick
    )
}
