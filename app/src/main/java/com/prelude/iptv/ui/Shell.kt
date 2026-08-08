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


enum class Tab { PLAYLIST, XTREAM, EPG, SETTINGS }

/* ==================================================== bottom bar + FAB ==== */

/** Σχήμα μπάρας με ημικυκλική «εγκοπή» στο κέντρο για το FAB. */
@Composable
private fun notchedBarShape(radius: Dp): Shape {
    val d = LocalDensity.current
    return remember(radius, d) {
        androidx.compose.foundation.shape.GenericShape { size, _ ->
            val r = with(d) { radius.toPx() }
            val cx = size.width / 2f
            moveTo(0f, 0f)
            lineTo(cx - r, 0f)
            // ημικύκλιο προς τα κάτω = η εγκοπή
            arcTo(Rect(cx - r, -r, cx + r, r), 180f, -180f, false)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
    }
}

/**
 * Κάτω μπάρα με 4 tabs και κεντρικό κουμπί ＋ που «κάθεται» μέσα στην εγκοπή.
 */
@Composable
fun NotchedBottomBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    onAdd: () -> Unit
) {
    StreamingBottomNavigation(
        items = listOf(
            StreamingNavItem(Tab.PLAYLIST, "Αρχική", Icons.Default.VideoLibrary),
            StreamingNavItem(Tab.XTREAM, "Xtream", Icons.Default.Bolt),
            StreamingNavItem<Tab>(null, "Προσθήκη", Icons.Default.AddCircleOutline, action = onAdd),
            StreamingNavItem(Tab.EPG, "EPG", Icons.Default.GridView),
            StreamingNavItem(Tab.SETTINGS, "Ρυθμίσεις", Icons.Default.Settings)
        ),
        selected = current,
        onSelect = onSelect
    )
}

/* ======================================================= empty states ===== */

/** Καμπύλο βελάκι που δείχνει προς το ＋ (σχεδιασμένο, όχι εικόνα). */
