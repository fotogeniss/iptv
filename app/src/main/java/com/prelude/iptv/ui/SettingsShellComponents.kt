package com.prelude.iptv.ui

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.app.UiModeManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration
import com.prelude.iptv.ui.design.motionScale


/* ---- ίδια χρώματα με το υπόλοιπο app ---- */
private val ShBg      = IptvColors.Background
private val ShBar     = IptvColors.BackgroundRaised
private val ShElev    = IptvColors.Surface
private val ShLine    = IptvColors.DividerStrong
private val ShAccent  = IptvColors.Primary
private val ShAccent2 = IptvColors.Primary
private val ShHi      = IptvColors.TextPrimary
private val ShMid     = IptvColors.TextSecondary
private val ShLo      = IptvColors.TextTertiary


@Composable
fun SettingRow(
    label: String,
    icon: ImageVector,
    value: String = "",
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (!isTvDevice()) {
        MobileV2SettingRow(
            title = label,
            icon = icon,
            value = value,
            onClick = onClick,
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        return
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ShElev)
            .border(1.dp, ShLine, RoundedCornerShape(12.dp))
            .tvFocus(RoundedCornerShape(12.dp), tint = false)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(ShBar)
                .border(1.dp, ShLine, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = ShHi, modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.width(14.dp))
        Text(label, color = ShHi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        if (value.isNotBlank()) {
            Text(value, color = ShMid, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = ShLo, modifier = Modifier.size(21.dp))
    }
}

/* ========================================================= μενού «＋» ===== */
//
// ΕΔΩ ΗΤΑΝ ΤΟ AddMenuSheet ΚΑΙ ΤΟ AddAction. ΑΦΑΙΡΕΘΗΚΑΝ ΟΛΟΚΛΗΡΑ.
//
// Το φύλλο ρωτούσε «από πού θέλεις να προσθέσεις;» και έδινε έξι επιλογές. Οι
// τέσσερις πρώτες (URL playlist, Xtream, MAC portal, συσκευή) απλώς άνοιγαν το
// AddPlaylistScreen με προεπιλεγμένη καρτέλα — καρτέλα που ο χρήστης βλέπει και
// αλλάζει μέσα στην ίδια οθόνη. Ήταν ένα βήμα για να διαλέξεις κάτι που
// διαλέγεις ούτως ή άλλως στο επόμενο δευτερόλεπτο.
//
// Το «＋» της κάτω μπάρας πηγαίνει πλέον απευθείας στο AddPlaylistScreen, όπως
// πήγαινε και το κουμπί «＋ Νέα πηγή» της κεφαλίδας, το οποίο επίσης αφαιρέθηκε:
// η ίδια οθόνη είχε δύο «＋» με ίδιο εικονίδιο και διαφορετικό αριθμό βημάτων.
//
// Τι έγιναν οι άλλες δύο επιλογές: η «Εισαγωγή EPG» υπάρχει ήδη στην καρτέλα
// EPG. Η «Αναπαραγωγή μεμονωμένου stream» είχε ΜΟΝΟ αυτή την πόρτα και έμεινε
// προς το παρόν χωρίς — δες τη σημείωση πάνω από τον `SingleStreamDialog`.

/* ==================================================== Android TV focus ==== */

/**
 * Ορατή ένδειξη για το πού βρίσκεται το τηλεχειριστήριο.
 * Χωρίς αυτό, σε τηλεόραση δεν ξέρεις τι έχεις επιλεγμένο.
 *
 * Μπαίνει ΠΡΙΝ το .clickable{} ώστε να παίρνει το focus event.
 */
