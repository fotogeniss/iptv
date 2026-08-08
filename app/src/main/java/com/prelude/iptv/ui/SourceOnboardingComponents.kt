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
private fun CurvyArrow(modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 110.dp, height = 170.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val p = Path().apply {
            moveTo(w * 0.52f, 0f)
            // μικρή θηλιά στη μέση
            cubicTo(w * 0.05f, h * 0.28f, w * 0.98f, h * 0.42f, w * 0.44f, h * 0.60f)
            cubicTo(w * 0.28f, h * 0.70f, w * 0.44f, h * 0.86f, w * 0.50f, h * 0.97f)
        }
        drawPath(p, ShLo.copy(alpha = 0.7f), style = stroke)
        // μύτη βέλους
        val tipX = w * 0.50f
        val tipY = h
        drawLine(ShLo.copy(alpha = 0.7f), androidx.compose.ui.geometry.Offset(tipX, tipY),
            androidx.compose.ui.geometry.Offset(tipX - 9.dp.toPx(), tipY - 13.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        drawLine(ShLo.copy(alpha = 0.7f), androidx.compose.ui.geometry.Offset(tipX, tipY),
            androidx.compose.ui.geometry.Offset(tipX + 8.dp.toPx(), tipY - 13.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    }
}

/** Κενή οθόνη με τίτλο, υπότιτλο και βελάκι προς το κουμπί ＋. */
@Composable
fun EmptyWithArrow(title: String, subtitle: String, bold: String) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = ShHi, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(subtitle, color = ShMid, fontSize = 16.sp, textAlign = TextAlign.Center)
        Text(bold, color = ShHi, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        CurvyArrow()
        Spacer(Modifier.height(4.dp))
    }
}

/* ==================================================== Xtream landing ====== */

/**
 * Οθόνη «Xtream» πριν μπει προφίλ: εικαστικό, περιγραφή, τι περιλαμβάνει
 * και κουμπί προσθήκης.
 */
@Composable
fun XtreamLanding(onAddProfile: () -> Unit) {
    // ΤΗΛΕΟΡΑΣΗ: όταν δεν υπάρχουν προφίλ, το tab δεν είχε ΚΑΝΕΝΑ focusable
    // με αρχικό focus (το firstX του caller ενεργοποιείται μόνο με προφίλ) —
    // το τηλεχειριστήριο έμενε νεκρό. Το κουμπί προσθήκης παίρνει το focus.
    val fAdd = rememberInitialFocus(enabled = isTvDevice())
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // «τηλεόραση» με grid από πλακίδια — δικό μας εικαστικό
        Box(
            Modifier.fillMaxWidth().height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ShElev)
                .border(1.dp, ShLine, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(Modifier.padding(14.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(ShAccent.copy(alpha = 0.35f), Color(0xFF2A1015))))
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) {
                        Box(
                            Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF23232B))
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) {
                        Box(
                            Modifier.weight(1f).height(26.dp).clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1B1B22))
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bolt, null, tint = ShAccent, modifier = Modifier.size(42.dp))
            Spacer(Modifier.width(8.dp))
            Text("XTREAM", color = ShHi, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Ο πιο διαδεδομένος τρόπος σύνδεσης σε IPTV: βάζεις server, όνομα χρήστη και κωδικό.",
            color = ShMid, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Feature("Live TV", Icons.Outlined.LiveTv)
            VDivider()
            Feature("Ταινίες", Icons.Default.Movie)
            VDivider()
            Feature("Σειρές", Icons.Default.Tv)
        }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = onAddProfile,
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ShAccent),
            modifier = Modifier.height(52.dp).fillMaxWidth(0.75f)
                .focusRequester(fAdd).tvFocus(RoundedCornerShape(26.dp), tint = false)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Προσθήκη προφίλ", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Feature(label: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ShHi, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = ShHi, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.height(24.dp).width(1.dp).background(ShLine))
}

/* ========================================================= settings row === */

/** Γραμμή ρύθμισης: στρογγυλό εικονίδιο + τίτλος (+ τιμή) + βελάκι. */
