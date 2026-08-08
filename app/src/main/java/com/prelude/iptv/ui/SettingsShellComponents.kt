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

/** Μια επιλογή του μενού προσθήκης. */
enum class AddAction { M3U_URL, XTREAM, MAC, DEVICE, SINGLE_STREAM, EPG }

/**
 * Το φύλλο που ανοίγει με το ＋: από πού θέλει ο χρήστης να προσθέσει.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuSheet(onPick: (AddAction) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ShBar,
        dragHandle = null
    ) {
        val first = rememberInitialFocus()
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = ShHi) }
                Text(
                    "Μενού", color = ShHi, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(48.dp))   // ισορροπία με το back
            }
            // Σε τηλεόραση (landscape) τα 6 στοιχεία δεν χωράνε: κάνε το φύλλο κυλιόμενο.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MenuItem("Εισαγωγή από URL playlist", Icons.Default.Link,
                    Modifier.focusRequester(first)) { onPick(AddAction.M3U_URL) }
                MenuItem("Εισαγωγή από Xtream Codes API", Icons.Default.Download) { onPick(AddAction.XTREAM) }
                MenuItem("Εισαγωγή από MAC portal", Icons.Default.SettingsInputAntenna) { onPick(AddAction.MAC) }
                MenuItem("Εισαγωγή από τη συσκευή", Icons.Default.PlayCircleOutline) { onPick(AddAction.DEVICE) }
                MenuItem("Αναπαραγωγή μεμονωμένου stream", Icons.Default.OpenInNew) { onPick(AddAction.SINGLE_STREAM) }
                MenuItem("Εισαγωγή EPG", Icons.Default.GridView) { onPick(AddAction.EPG) }
            }
        }
    }
}

@Composable
private fun MenuItem(
    label: String, icon: ImageVector,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Row(
        modifier.fillMaxWidth().tvFocus(RoundedCornerShape(8.dp))
            .clickable { onClick() }.padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ShHi, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, color = ShHi, fontSize = 16.sp)
    }
}

/* ==================================================== Android TV focus ==== */

/**
 * Ορατή ένδειξη για το πού βρίσκεται το τηλεχειριστήριο.
 * Χωρίς αυτό, σε τηλεόραση δεν ξέρεις τι έχεις επιλεγμένο.
 *
 * Μπαίνει ΠΡΙΝ το .clickable{} ώστε να παίρνει το focus event.
 */
