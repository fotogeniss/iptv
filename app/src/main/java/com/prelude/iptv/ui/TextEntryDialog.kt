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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEntryDialog(
    title: String,
    initial: String,
    isPassword: Boolean = false,
    onDismiss: () -> Unit,
    onOk: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    // Εδώ θέλουμε ρητά focus στο πεδίο ώστε να εμφανιστεί το πληκτρολόγιο.
    val fr = rememberInitialFocus()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ShElev,
        title = { Text(title, color = ShHi, fontSize = 16.sp) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ShHi, unfocusedTextColor = ShHi,
                    focusedBorderColor = ShAccent, unfocusedBorderColor = ShLine,
                    unfocusedContainerColor = ShBar, focusedContainerColor = ShBar,
                    cursorColor = ShAccent
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(fr)
            )
        },
        confirmButton = { TvDialogTextButton(label = "Εντάξει", color = ShAccent2, onClick = { onOk(text) }) },
        dismissButton = { TvDialogTextButton(label = "Άκυρο", color = ShMid, onClick = onDismiss) }
    )
}
